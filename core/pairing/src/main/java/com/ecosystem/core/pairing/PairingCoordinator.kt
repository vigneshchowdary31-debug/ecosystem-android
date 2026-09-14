package com.ecosystem.core.pairing

import com.ecosystem.core.protocol.crypto.HandshakeCrypto
import com.ecosystem.core.protocol.handshake.ExpectedPeer
import com.ecosystem.core.protocol.handshake.HandshakeException
import com.ecosystem.core.protocol.handshake.HandshakeResult
import com.ecosystem.core.protocol.handshake.InitiatorHandshake
import com.ecosystem.core.protocol.handshake.LocalPairingIdentity
import com.ecosystem.core.protocol.qr.QrPairingPayload
import com.ecosystem.core.protocol.qr.QrParseResult
import com.ecosystem.core.protocol.qr.QrPayloadCodec
import com.ecosystem.core.protocol.util.Uuids
import com.ecosystem.core.protocol.util.toHex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Orchestrates pairing: validates the QR code, checks Bluetooth readiness, scans for
 * candidates, runs one [PairingSession] at a time against them and persists the result.
 *
 * Concurrency rules:
 * - One job at a time. A new job first waits for the previous one to finish its cleanup, so
 *   two sessions never share the transport.
 * - Every state update carries the job's generation number. Once a job is cancelled or
 *   replaced, its late updates are dropped and cannot overwrite a newer session's state.
 * - No session secret is stored here. Secrets live in each [PairingSession] and are wiped when
 *   it closes.
 */
class PairingCoordinator(
    private val scope: CoroutineScope,
    private val transport: PairingTransport,
    private val identitySource: LocalIdentitySource,
    private val peerStore: TrustedPeerStore,
    private val crypto: HandshakeCrypto,
    private val addressProvider: LocalAddressProvider = LocalAddressProvider { null },
    private val clock: () -> Long = System::currentTimeMillis,
    private val logger: PairingLogger = PairingLogger.NONE,
    private val policy: PairingPolicy = PairingPolicy(),
) : PairingManager {

    private val _state = MutableStateFlow<PairingState>(PairingState.Idle)
    override val state: StateFlow<PairingState> = _state.asStateFlow()

    private val lock = Any()
    private var generation = 0L
    private var currentJob: Job? = null
    private var lastPayload: QrPairingPayload? = null

    override fun startPairing(qrText: String): StartResult = synchronized(lock) {
        if (currentJob?.isActive == true) return StartResult.IgnoredAlreadyActive
        when (val parsed = QrPayloadCodec.parse(qrText)) {
            is QrParseResult.Invalid -> {
                logger.debug("Rejected QR code: ${parsed.error}")
                generation++
                _state.value = PairingState.PairingFailed(PairingFailure.InvalidQr(parsed.error))
                StartResult.RejectedInvalidQr(parsed.error)
            }
            is QrParseResult.Valid -> {
                lastPayload = parsed.payload
                launchLocked(parsed.payload)
                StartResult.Started
            }
        }
    }

    override fun retryPairing(): Boolean = synchronized(lock) {
        val payload = lastPayload ?: return false
        launchLocked(payload)
        true
    }

    override fun cancelPairing() {
        synchronized(lock) {
            val job = currentJob ?: return
            if (!job.isActive) return
            generation++
            job.cancel()
            _state.value = PairingState.Cancelled
        }
    }

    override fun reset() {
        synchronized(lock) {
            generation++
            currentJob?.cancel()
            lastPayload = null
            _state.value = PairingState.Idle
        }
    }

    private fun launchLocked(payload: QrPairingPayload) {
        val previous = currentJob
        previous?.cancel()
        val jobGeneration = ++generation
        _state.value = PairingState.Scanning
        val job = scope.launch(start = CoroutineStart.LAZY) {
            // Wait for the previous session's cleanup (connection close, key wipe) before touching the transport.
            previous?.join()
            runPairing(payload, jobGeneration)
        }
        currentJob = job
        job.start()
    }

    private fun setState(jobGeneration: Long, newState: PairingState) {
        synchronized(lock) {
            if (jobGeneration == generation) _state.value = newState
        }
    }

    private suspend fun runPairing(payload: QrPairingPayload, jobGeneration: Long) {
        try {
            val terminal = withTimeoutOrNull(policy.overallTimeoutMillis) { pairingLoop(payload, jobGeneration) }
            setState(jobGeneration, terminal ?: PairingState.TimedOut)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Pairing stopped by an unexpected error", e)
            setState(jobGeneration, PairingState.PairingFailed(PairingFailure.Internal))
        }
    }

    private suspend fun pairingLoop(payload: QrPairingPayload, jobGeneration: Long): PairingState {
        when (transport.readiness()) {
            TransportReadiness.READY -> Unit
            TransportReadiness.BLUETOOTH_UNAVAILABLE -> return failed(PairingFailure.BluetoothUnavailable)
            TransportReadiness.BLUETOOTH_DISABLED -> return failed(PairingFailure.BluetoothDisabled)
            TransportReadiness.PERMISSION_DENIED -> return failed(PairingFailure.PermissionDenied)
            TransportReadiness.LOCATION_DISABLED -> return failed(PairingFailure.LocationServicesDisabled)
        }

        val identity = try {
            identitySource.loadIdentity()
        } catch (e: IdentityUnavailableException) {
            logger.warn("Local identity unavailable", e)
            return failed(PairingFailure.IdentityUnavailable)
        }
        val expectedPeer = ExpectedPeer(payload.deviceId, payload.identityPublicKey, payload.deviceName)

        val attemptsByHandle = HashMap<String, Int>()
        val excluded = HashSet<String>()
        val failures = ArrayList<PairingFailure>()
        var attemptNumber = 0

        while (attemptNumber < policy.maxTotalAttempts) {
            setState(jobGeneration, PairingState.Scanning)
            val candidates = try {
                transport.scanForCandidates(policy.scanTimeoutMillis, policy.scanSettleMillis)
            } catch (e: TransportException) {
                logger.warn("Scan failed: ${e.kind}", e)
                val mapped = FailureMapping.fromTransport(e)
                return terminalFor(if (failures.isEmpty() || mapped.fatal) mapped.failure else FailureMapping.mostRelevant(failures)!!)
            }.filter { it.handle !in excluded && (attemptsByHandle[it.handle] ?: 0) < policy.maxAttemptsPerCandidate }

            if (candidates.isEmpty()) break

            for (candidate in candidates) {
                if (attemptNumber >= policy.maxTotalAttempts) break
                attemptNumber++
                attemptsByHandle[candidate.handle] = (attemptsByHandle[candidate.handle] ?: 0) + 1
                setState(jobGeneration, PairingState.CandidateFound(attemptNumber))

                val outcome = attempt(candidate, attemptNumber, identity, expectedPeer, jobGeneration)
                if (outcome is HandshakeResult) return persist(outcome)

                val failure = outcome as AttemptFailure
                failures += failure.failure
                if (failure.fatal) return terminalFor(failure.failure)
                if (failure.excludeCandidate) excluded += candidate.handle else delay(policy.retryDelayMillis)
            }
        }
        return terminalFor(FailureMapping.mostRelevant(failures) ?: PairingFailure.NoDeviceFound)
    }

    /** Returns a [HandshakeResult] on success or an [AttemptFailure]. */
    private suspend fun attempt(
        candidate: PairingCandidate,
        attemptNumber: Int,
        identity: LocalPairingIdentity,
        expectedPeer: ExpectedPeer,
        jobGeneration: Long,
    ): Any {
        val handshake = InitiatorHandshake(
            crypto = crypto,
            local = identity,
            expectedPeer = expectedPeer,
            localIpAddress = addressProvider.currentLocalAddress(),
            timeouts = policy.handshakeTimeouts,
            listener = object : InitiatorHandshake.Listener {
                override fun onKeyExchangeComplete() = setState(jobGeneration, PairingState.Authenticating)
                override fun onPeerAuthenticated() = setState(jobGeneration, PairingState.Connected(expectedPeer.deviceName))
            },
        )
        val session = PairingSession(crypto.randomBytes(6).toHex(), candidate, transport, handshake)
        logger.debug("Session ${session.id}: attempt $attemptNumber (rssi ${candidate.rssi})")
        return try {
            setState(jobGeneration, PairingState.Connecting(attemptNumber))
            session.run { phase ->
                when (phase) {
                    ConnectionPhase.DISCOVERING_SERVICES -> setState(jobGeneration, PairingState.DiscoveringServices)
                    ConnectionPhase.ENABLING_NOTIFICATIONS -> setState(jobGeneration, PairingState.EnablingNotifications)
                    ConnectionPhase.READY -> setState(jobGeneration, PairingState.ExchangingKeys)
                    ConnectionPhase.CONNECTING, ConnectionPhase.NEGOTIATING_MTU -> Unit
                }
            }
        } catch (e: TransportException) {
            logger.debug("Session ${session.id}: transport failure ${e.kind} (transient=${e.transient})")
            FailureMapping.fromTransport(e)
        } catch (e: HandshakeException) {
            logger.debug("Session ${session.id}: handshake failure ${e.reason}")
            FailureMapping.fromHandshake(e)
        } finally {
            withContext(NonCancellable) { session.close() }
        }
    }

    private suspend fun persist(result: HandshakeResult): PairingState {
        val peer = PairedPeer(
            deviceId = result.peerDeviceId,
            name = result.peerDeviceName,
            identityPublicKey = result.peerIdentityPublicKey,
            ipAddress = IpAddresses.toHint(result.peerIpAddress),
            pairedAtMillis = clock(),
        )
        return try {
            peerStore.savePairedPeer(peer)
            PairingState.PairingSucceeded(PairedDeviceSummary(Uuids.canonical(peer.deviceId), peer.name))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn("Could not store the paired device", e)
            failed(PairingFailure.StorageFailed)
        }
    }

    private fun failed(failure: PairingFailure): PairingState = PairingState.PairingFailed(failure)

    private fun terminalFor(failure: PairingFailure): PairingState =
        if (failure == PairingFailure.Disconnected) PairingState.Disconnected else PairingState.PairingFailed(failure)
}
