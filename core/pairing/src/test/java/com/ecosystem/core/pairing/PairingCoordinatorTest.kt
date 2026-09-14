package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.crypto.Ed25519SeedSigner
import com.ecosystem.core.protocol.crypto.TinkHandshakeCrypto
import com.ecosystem.core.protocol.framing.Frame
import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.qr.QrError
import com.ecosystem.core.protocol.qr.QrPairingPayload
import com.ecosystem.core.protocol.qr.QrPayloadCodec
import com.ecosystem.core.protocol.testing.InMemoryEndpoint
import com.ecosystem.core.protocol.testing.InMemoryLink
import com.ecosystem.core.protocol.testing.ResponderHandshake
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PairingCoordinatorTest {

    private val crypto = TinkHandshakeCrypto()
    private val androidSigner = Ed25519SeedSigner(ByteArray(32) { 0x11 })
    private val macSigner = Ed25519SeedSigner(ByteArray(32) { 0x22 })
    private val attackerSigner = Ed25519SeedSigner(ByteArray(32) { 0x33 })
    private val macId = UUID.fromString("2d861d8a-6b83-4a1d-8f9c-76e48c3f8f11")
    private val androidIdentity =
        LocalPairingIdentity(UUID.fromString("5f1c2b7a-3d4e-4f60-8a9b-0c1d2e3f4a5b"), androidSigner.publicKey, "Pixel", androidSigner)
    private val macIdentity = LocalPairingIdentity(macId, macSigner.publicKey, "Studio Mac", macSigner)
    private val impostorIdentity = LocalPairingIdentity(macId, attackerSigner.publicKey, "Studio Mac", attackerSigner)
    private val macIp = byteArrayOf(192.toByte(), 168.toByte(), 1, 10)

    private val validQr = QrPayloadCodec.encode(
        QrPairingPayload(2, macId, "Studio Mac", UUID.fromString("f7823ab4-3e9a-4c28-be51-24b893a201f9"), macSigner.publicKey)
    )

    // ---- Fakes ---------------------------------------------------------------------------

    sealed interface Behavior {
        data class Honest(val identity: LocalPairingIdentity) : Behavior
        data class Attacker(val identity: LocalPairingIdentity) : Behavior
        data object Silent : Behavior
        data object DisconnectAfterFirstMessage : Behavior
        data object ConnectFailsTransient : Behavior
        data object ServiceMissing : Behavior
        data object HangWhileConnecting : Behavior
    }

    inner class FakeTransport(
        private val macScope: CoroutineScope,
        private val scans: List<List<PairingCandidate>>,
        behaviors: Map<String, List<Behavior>>,
    ) : PairingTransport {
        var readiness = TransportReadiness.READY
        private val behaviorQueues = behaviors.mapValues { ArrayDeque(it.value) }.toMutableMap()
        private var scanIndex = 0
        val connectCalls = mutableListOf<String>()
        val scanCalls = AtomicInteger()
        val open = AtomicInteger()
        val closed = AtomicInteger()
        var maxOpen = 0

        override fun readiness() = readiness

        override suspend fun scanForCandidates(timeoutMillis: Long, settleMillis: Long): List<PairingCandidate> {
            scanCalls.incrementAndGet()
            return scans.getOrElse(scanIndex++) { scans.lastOrNull() ?: emptyList() }
        }

        override suspend fun connect(candidate: PairingCandidate, onPhase: (ConnectionPhase) -> Unit): PairingConnection {
            connectCalls += candidate.handle
            val queue = behaviorQueues.getValue(candidate.handle)
            val behavior = if (queue.size > 1) queue.removeFirst() else queue.first()
            onPhase(ConnectionPhase.CONNECTING)
            when (behavior) {
                Behavior.ConnectFailsTransient ->
                    throw TransportException(TransportException.Kind.CONNECT_FAILED, "GATT 133", transient = true)
                Behavior.ServiceMissing ->
                    throw TransportException(TransportException.Kind.SERVICE_NOT_FOUND, "no service")
                Behavior.HangWhileConnecting -> awaitCancellation()
                else -> Unit
            }
            onPhase(ConnectionPhase.DISCOVERING_SERVICES)
            onPhase(ConnectionPhase.ENABLING_NOTIFICATIONS)
            onPhase(ConnectionPhase.READY)
            val (androidEnd, macEnd) = InMemoryLink.pair(chunkSize = 20)
            macScope.launch { runMac(behavior, macEnd) }
            maxOpen = maxOf(maxOpen, open.incrementAndGet())
            return FakeConnection(androidEnd) {
                open.decrementAndGet()
                closed.incrementAndGet()
            }
        }

        private suspend fun runMac(behavior: Behavior, end: InMemoryEndpoint) {
            runCatching {
                when (behavior) {
                    is Behavior.Honest -> ResponderHandshake(crypto, behavior.identity, macIp).run(end)
                    is Behavior.Attacker ->
                        ResponderHandshake(crypto, behavior.identity, macIp, verifyAndroidAuthentication = false).run(end)
                    Behavior.Silent -> awaitCancellation()
                    Behavior.DisconnectAfterFirstMessage -> {
                        end.receive(60_000)
                        end.disconnect()
                    }
                    else -> Unit
                }
            }
        }
    }

    class FakeConnection(private val endpoint: InMemoryEndpoint, private val onClose: () -> Unit) : PairingConnection {
        private var closed = false
        override suspend fun send(frame: Frame) = endpoint.send(frame)
        override suspend fun receive(timeoutMillis: Long): Frame = endpoint.receive(timeoutMillis)
        override fun close() {
            if (closed) return
            closed = true
            endpoint.disconnect()
            onClose()
        }
    }

    class FakeIdentitySource(private val identity: LocalPairingIdentity?) : LocalIdentitySource {
        override suspend fun loadIdentity(): LocalPairingIdentity =
            identity ?: throw IdentityUnavailableException("Keystore key lost")
    }

    class FakePeerStore(private val fail: Boolean = false) : TrustedPeerStore {
        val saved = mutableListOf<PairedPeer>()
        override suspend fun savePairedPeer(peer: PairedPeer) {
            if (fail) error("disk full")
            saved += peer
        }
    }

    private class Harness(
        val coordinator: PairingCoordinator,
        val transport: FakeTransport,
        val store: FakePeerStore,
        val states: MutableList<PairingState>,
    )

    private fun TestScope.harness(
        scans: List<List<PairingCandidate>> = listOf(listOf(PairingCandidate("mac", -40))),
        behaviors: Map<String, List<Behavior>> = mapOf("mac" to listOf(Behavior.Honest(macIdentity))),
        identity: LocalPairingIdentity? = androidIdentity,
        store: FakePeerStore = FakePeerStore(),
    ): Harness {
        // advanceUntilIdle() does not run backgroundScope work, so sessions and fake Macs run in their
        // own scope on the test scheduler (virtual time) and are cancelled when the test ends.
        val workScope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        backgroundScope.coroutineContext.job.invokeOnCompletion { workScope.cancel() }
        val transport = FakeTransport(workScope, scans, behaviors)
        val coordinator = PairingCoordinator(
            scope = workScope,
            transport = transport,
            identitySource = FakeIdentitySource(identity),
            peerStore = store,
            crypto = crypto,
            clock = { 1_000L },
        )
        val states = mutableListOf<PairingState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { coordinator.state.toList(states) }
        return Harness(coordinator, transport, store, states)
    }

    // ---- Tests ---------------------------------------------------------------------------

    @Test
    fun `successful pairing persists the verified mac and walks the state machine in order`() = runTest {
        val h = harness()
        assertEquals(StartResult.Started, h.coordinator.startPairing(validQr))
        advanceUntilIdle()

        val final = h.coordinator.state.value
        assertEquals(PairingState.PairingSucceeded(PairedDeviceSummary(macId.toString(), "Studio Mac")), final)
        val peer = h.store.saved.single()
        assertEquals(macId, peer.deviceId)
        assertArrayEquals(macSigner.publicKey, peer.identityPublicKey)
        assertEquals("192.168.1.10", peer.ipAddress)
        assertEquals(1_000L, peer.pairedAtMillis)

        val order = listOf(
            PairingState.Scanning::class,
            PairingState.CandidateFound::class,
            PairingState.Connecting::class,
            PairingState.DiscoveringServices::class,
            PairingState.EnablingNotifications::class,
            PairingState.ExchangingKeys::class,
            PairingState.Authenticating::class,
            PairingState.Connected::class,
            PairingState.PairingSucceeded::class,
        )
        val seen = h.states.map { it::class }.filter { it in order }.distinct()
        assertEquals(order, seen)
        assertEquals(0, h.transport.open.get())
    }

    @Test
    fun `success is never reported before the mac is verified`() = runTest {
        val h = harness()
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        val connectedIndex = h.states.indexOfFirst { it is PairingState.Connected }
        val authenticatingIndex = h.states.indexOfFirst { it is PairingState.Authenticating }
        assertTrue(authenticatingIndex in 0 until connectedIndex)
    }

    @Test
    fun `invalid and legacy qr codes fail without touching bluetooth`() = runTest {
        val h = harness()
        assertEquals(StartResult.RejectedInvalidQr(QrError.NOT_A_PAIRING_CODE), h.coordinator.startPairing("https://example.com"))
        assertEquals(
            PairingState.PairingFailed(PairingFailure.InvalidQr(QrError.NOT_A_PAIRING_CODE)),
            h.coordinator.state.value,
        )
        h.coordinator.startPairing("identity_continuity:a,b,c,d")
        assertEquals(PairingState.PairingFailed(PairingFailure.InvalidQr(QrError.LEGACY_FORMAT)), h.coordinator.state.value)
        advanceUntilIdle()
        assertEquals(0, h.transport.scanCalls.get())
    }

    @Test
    fun `bluetooth disabled and missing permission are reported before scanning`() = runTest {
        val h = harness()
        h.transport.readiness = TransportReadiness.BLUETOOTH_DISABLED
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.BluetoothDisabled), h.coordinator.state.value)

        h.transport.readiness = TransportReadiness.PERMISSION_DENIED
        h.coordinator.retryPairing()
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.PermissionDenied), h.coordinator.state.value)
        assertEquals(0, h.transport.scanCalls.get())
    }

    @Test
    fun `no advertising device is reported as not found`() = runTest {
        val h = harness(scans = listOf(emptyList()))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.NoDeviceFound), h.coordinator.state.value)
    }

    @Test
    fun `candidate failure then next candidate succeeds`() = runTest {
        val h = harness(
            scans = listOf(listOf(PairingCandidate("impostor", -30), PairingCandidate("mac", -60))),
            behaviors = mapOf(
                "impostor" to listOf(Behavior.Attacker(impostorIdentity)),
                "mac" to listOf(Behavior.Honest(macIdentity)),
            ),
        )
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
        assertEquals(listOf("impostor", "mac"), h.transport.connectCalls)
        assertArrayEquals(macSigner.publicKey, h.store.saved.single().identityPublicKey)
    }

    @Test
    fun `only an impostor nearby ends in authentication failure and nothing is stored`() = runTest {
        val h = harness(
            scans = listOf(listOf(PairingCandidate("impostor", -30))),
            behaviors = mapOf("impostor" to listOf(Behavior.Attacker(impostorIdentity))),
        )
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.AuthenticationFailed), h.coordinator.state.value)
        assertTrue(h.store.saved.isEmpty())
        assertEquals(listOf("impostor"), h.transport.connectCalls)
    }

    @Test
    fun `transient connection failure is retried on the same candidate`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.ConnectFailsTransient, Behavior.Honest(macIdentity))))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
        assertEquals(listOf("mac", "mac"), h.transport.connectCalls)
    }

    @Test
    fun `a device without the pairing service is skipped`() = runTest {
        val h = harness(
            scans = listOf(listOf(PairingCandidate("phone", -20), PairingCandidate("mac", -50))),
            behaviors = mapOf("phone" to listOf(Behavior.ServiceMissing), "mac" to listOf(Behavior.Honest(macIdentity))),
        )
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
    }

    @Test
    fun `silent mac is reported as not responding`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.Silent)))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.PeerNotResponding), h.coordinator.state.value)
        assertEquals(0, h.transport.open.get())
    }

    @Test
    fun `session that never connects times out`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.HangWhileConnecting)))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.TimedOut, h.coordinator.state.value)
    }

    @Test
    fun `disconnect during the handshake is retried once then reported`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.DisconnectAfterFirstMessage)))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.Disconnected, h.coordinator.state.value)
        assertEquals(listOf("mac", "mac"), h.transport.connectCalls)
        assertEquals(0, h.transport.open.get())
    }

    @Test
    fun `cancellation closes the connection and is not overwritten later`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.Silent)))
        h.coordinator.startPairing(validQr)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(1, h.transport.open.get())

        h.coordinator.cancelPairing()
        assertEquals(PairingState.Cancelled, h.coordinator.state.value)
        advanceUntilIdle()
        assertEquals(PairingState.Cancelled, h.coordinator.state.value)
        assertEquals(0, h.transport.open.get())
        assertEquals(1, h.transport.closed.get())
    }

    @Test
    fun `second qr scan while active is ignored`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.Silent)))
        assertEquals(StartResult.Started, h.coordinator.startPairing(validQr))
        runCurrent()
        assertEquals(StartResult.IgnoredAlreadyActive, h.coordinator.startPairing(validQr))
        advanceUntilIdle()
        assertEquals(1, h.transport.connectCalls.size)
        assertEquals(1, h.transport.maxOpen)
    }

    @Test
    fun `retry cancels the running attempt before starting a clean session`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.Silent, Behavior.Honest(macIdentity))))
        h.coordinator.startPairing(validQr)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(1, h.transport.open.get())

        assertTrue(h.coordinator.retryPairing())
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
        assertEquals(1, h.transport.maxOpen)
        assertEquals(0, h.transport.open.get())
    }

    @Test
    fun `a cancelled session cannot overwrite the state of the next one`() = runTest {
        val h = harness(behaviors = mapOf("mac" to listOf(Behavior.HangWhileConnecting, Behavior.Honest(macIdentity))))
        h.coordinator.startPairing(validQr)
        advanceTimeBy(1_000)
        h.coordinator.cancelPairing()
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
    }

    @Test
    fun `pairing another device after success starts a fresh session`() = runTest {
        val h = harness()
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)

        h.coordinator.reset()
        assertEquals(PairingState.Idle, h.coordinator.state.value)
        assertEquals(StartResult.Started, h.coordinator.startPairing(validQr))
        advanceUntilIdle()
        assertTrue(h.coordinator.state.value is PairingState.PairingSucceeded)
        assertEquals(2, h.store.saved.size)
    }

    @Test
    fun `retry without a previous qr code does nothing`() = runTest {
        val h = harness()
        assertEquals(false, h.coordinator.retryPairing())
        assertEquals(PairingState.Idle, h.coordinator.state.value)
    }

    @Test
    fun `missing identity key stops pairing`() = runTest {
        val h = harness(identity = null)
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.IdentityUnavailable), h.coordinator.state.value)
        assertEquals(0, h.transport.scanCalls.get())
    }

    @Test
    fun `storage failure is reported instead of success`() = runTest {
        val h = harness(store = FakePeerStore(fail = true))
        h.coordinator.startPairing(validQr)
        advanceUntilIdle()
        assertEquals(PairingState.PairingFailed(PairingFailure.StorageFailed), h.coordinator.state.value)
    }
}
