package com.ecosystem.feature.pairing.presentation

import com.ecosystem.core.pairing.PairingManager
import com.ecosystem.core.pairing.PairingState
import com.ecosystem.core.pairing.StartResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private class FakePairingManager : PairingManager {
        val flow = MutableStateFlow<PairingState>(PairingState.Idle)
        override val state: StateFlow<PairingState> = flow
        val calls = mutableListOf<String>()
        var canRetry = true

        override fun startPairing(qrText: String): StartResult {
            calls += "start:$qrText"
            return StartResult.Started
        }

        override fun cancelPairing() {
            calls += "cancel"
        }

        override fun retryPairing(): Boolean {
            calls += "retry"
            return canRetry
        }

        override fun reset() {
            calls += "reset"
        }
    }

    private val manager = FakePairingManager()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `ui state follows the pairing state machine`() = runTest {
        val viewModel = PairingViewModel(manager)
        assertTrue(viewModel.uiState.value.showScanner)
        manager.flow.value = PairingState.Authenticating
        assertFalse(viewModel.uiState.value.showScanner)
        assertTrue(viewModel.uiState.value.inProgress)
    }

    @Test
    fun `user intents are forwarded`() {
        val viewModel = PairingViewModel(manager)
        viewModel.onQrCodeScanned("connectflow://pair?v=2")
        viewModel.cancel()
        viewModel.scanAnotherCode()
        assertEquals(listOf("start:connectflow://pair?v=2", "cancel", "reset"), manager.calls)
    }

    @Test
    fun `retry without a previous code returns to the scanner`() {
        manager.canRetry = false
        PairingViewModel(manager).retry()
        assertEquals(listOf("retry", "reset"), manager.calls)
    }

    @Test
    fun `camera permission is tracked`() {
        val viewModel = PairingViewModel(manager)
        assertFalse(viewModel.hasCameraPermission.value)
        viewModel.setCameraPermissionGranted(true)
        assertTrue(viewModel.hasCameraPermission.value)
    }
}
