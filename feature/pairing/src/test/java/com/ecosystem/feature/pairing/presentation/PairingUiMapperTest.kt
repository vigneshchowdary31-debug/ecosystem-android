package com.ecosystem.feature.pairing.presentation

import com.ecosystem.core.pairing.PairedDeviceSummary
import com.ecosystem.core.pairing.PairingFailure
import com.ecosystem.core.pairing.PairingState
import com.ecosystem.core.protocol.qr.QrError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingUiMapperTest {

    private val activeStates = listOf(
        PairingState.Scanning,
        PairingState.CandidateFound(1),
        PairingState.Connecting(2),
        PairingState.DiscoveringServices,
        PairingState.EnablingNotifications,
        PairingState.ExchangingKeys,
        PairingState.Authenticating,
        PairingState.Connected("Studio Mac"),
    )

    @Test
    fun `idle shows the scanner and nothing else`() {
        val ui = PairingUiMapper.map(PairingState.Idle)
        assertTrue(ui.showScanner)
        assertFalse(ui.inProgress)
        assertEquals(null, ui.primaryAction)
    }

    @Test
    fun `every active state hides the scanner, shows progress and offers cancel`() {
        for (state in activeStates) {
            val ui = PairingUiMapper.map(state)
            assertFalse("$state", ui.showScanner)
            assertTrue("$state", ui.inProgress)
            assertEquals("$state", PairingAction.CANCEL, ui.primaryAction)
            assertEquals("$state", PairingOutcome.NONE, ui.outcome)
        }
    }

    @Test
    fun `connected is still progress, not success`() {
        val ui = PairingUiMapper.map(PairingState.Connected("Studio Mac"))
        assertNotEquals(PairingOutcome.SUCCESS, ui.outcome)
        assertTrue(ui.inProgress)
    }

    @Test
    fun `success offers pairing another device`() {
        val ui = PairingUiMapper.map(PairingState.PairingSucceeded(PairedDeviceSummary("id", "Studio Mac")))
        assertEquals(PairingOutcome.SUCCESS, ui.outcome)
        assertEquals(PairingAction.PAIR_ANOTHER, ui.primaryAction)
        assertTrue(ui.title.contains("Studio Mac"))
    }

    @Test
    fun `bluetooth permission and location failures offer the matching fix`() {
        assertEquals(PairingAction.ENABLE_BLUETOOTH, PairingUiMapper.map(PairingState.PairingFailed(PairingFailure.BluetoothDisabled)).primaryAction)
        assertEquals(PairingAction.GRANT_PERMISSION, PairingUiMapper.map(PairingState.PairingFailed(PairingFailure.PermissionDenied)).primaryAction)
        assertEquals(
            PairingAction.OPEN_LOCATION_SETTINGS,
            PairingUiMapper.map(PairingState.PairingFailed(PairingFailure.LocationServicesDisabled)).primaryAction,
        )
    }

    @Test
    fun `timeout disconnect and cancel are distinct and retryable`() {
        val titles = listOf(PairingState.TimedOut, PairingState.Disconnected, PairingState.Cancelled).map {
            val ui = PairingUiMapper.map(it)
            assertEquals(PairingAction.RETRY, ui.primaryAction)
            ui.title
        }
        assertEquals(3, titles.toSet().size)
        assertEquals(PairingOutcome.CANCELLED, PairingUiMapper.map(PairingState.Cancelled).outcome)
    }

    @Test
    fun `every failure maps to a failure card with an action`() {
        val failures = listOf(
            PairingFailure.InvalidQr(QrError.NOT_A_PAIRING_CODE),
            PairingFailure.InvalidQr(QrError.LEGACY_FORMAT),
            PairingFailure.InvalidQr(QrError.UNSUPPORTED_VERSION),
            PairingFailure.InvalidQr(QrError.INVALID_PUBLIC_KEY),
            PairingFailure.BluetoothUnavailable,
            PairingFailure.BluetoothDisabled,
            PairingFailure.PermissionDenied,
            PairingFailure.LocationServicesDisabled,
            PairingFailure.IdentityUnavailable,
            PairingFailure.NoDeviceFound,
            PairingFailure.ConnectionFailed,
            PairingFailure.NotAConnectFlowDevice,
            PairingFailure.PeerNotResponding,
            PairingFailure.ProtocolVersionMismatch,
            PairingFailure.ProtocolError,
            PairingFailure.AuthenticationFailed,
            PairingFailure.PeerRejected(0x04),
            PairingFailure.StorageFailed,
            PairingFailure.Internal,
        )
        for (failure in failures) {
            val ui = PairingUiMapper.map(PairingState.PairingFailed(failure))
            assertEquals("$failure", PairingOutcome.FAILURE, ui.outcome)
            assertFalse("$failure", ui.showScanner)
            assertTrue("$failure", ui.primaryAction != null)
            assertTrue("$failure", ui.title.isNotBlank())
        }
    }

    @Test
    fun `invalid qr codes send the user back to the scanner`() {
        val ui = PairingUiMapper.map(PairingState.PairingFailed(PairingFailure.InvalidQr(QrError.NOT_A_PAIRING_CODE)))
        assertEquals(PairingAction.SCAN_ANOTHER, ui.primaryAction)
    }
}
