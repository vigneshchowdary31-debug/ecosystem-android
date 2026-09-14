package com.ecosystem.feature.pairing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosystem.core.pairing.PairingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Presents the pairing state machine. The session itself lives in [PairingManager], so it
 * survives configuration changes; this ViewModel only maps state and forwards user intent.
 */
@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingManager: PairingManager,
) : ViewModel() {

    val uiState: StateFlow<PairingUiState> = pairingManager.state
        .map(PairingUiMapper::map)
        .stateIn(viewModelScope, SharingStarted.Eagerly, PairingUiMapper.map(pairingManager.state.value))

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission: StateFlow<Boolean> = _hasCameraPermission.asStateFlow()

    fun setCameraPermissionGranted(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    /** Called once per scanner session with the first decoded QR code. Ignored while pairing is active. */
    fun onQrCodeScanned(qrText: String) {
        pairingManager.startPairing(qrText)
    }

    fun cancel() = pairingManager.cancelPairing()

    /** Restarts with the same QR code in a clean session, or returns to the scanner if there is none. */
    fun retry() {
        if (!pairingManager.retryPairing()) pairingManager.reset()
    }

    /** Returns to the camera so another code (or another Mac) can be scanned. */
    fun scanAnotherCode() = pairingManager.reset()

    override fun onCleared() {
        pairingManager.reset()
    }
}
