package com.ecosystem.feature.pairing.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecosystem.core.pairing.PairingManager
import com.ecosystem.core.pairing.PairingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingManager: PairingManager
) : ViewModel() {

    val pairingState: StateFlow<PairingState> = pairingManager.pairingState

    private val _hasCameraPermission = MutableStateFlow(false)
    val hasCameraPermission = _hasCameraPermission.asStateFlow()

    private var hasScanned = false

    fun setCameraPermissionGranted(granted: Boolean) {
        _hasCameraPermission.value = granted
    }

    fun onQrCodeScanned(qrPayload: String) {
        if (hasScanned) return
        hasScanned = true

        viewModelScope.launch {
            pairingManager.startPairing(qrPayload)
        }
    }

    fun onRetry() {
        hasScanned = false
        pairingManager.reset()
    }

    override fun onCleared() {
        super.onCleared()
        pairingManager.reset()
    }
}
