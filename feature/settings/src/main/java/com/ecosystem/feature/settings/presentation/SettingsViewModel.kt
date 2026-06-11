package com.ecosystem.feature.settings.presentation

import androidx.lifecycle.ViewModel
import com.ecosystem.core.security.SecureStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val secureStorage: SecureStorage
) : ViewModel() {

    companion object {
        private const val KEY_SECURE_TOKEN = "companion_secure_token"
    }

    private val _secureTokenState = MutableStateFlow("")
    val secureTokenState: StateFlow<String> = _secureTokenState.asStateFlow()

    init {
        loadSecureToken()
    }

    private fun loadSecureToken() {
        _secureTokenState.value = secureStorage.getString(KEY_SECURE_TOKEN) ?: ""
    }

    fun saveSecureToken(token: String) {
        secureStorage.putString(KEY_SECURE_TOKEN, token)
        _secureTokenState.value = token
    }

    fun clearSecureToken() {
        secureStorage.remove(KEY_SECURE_TOKEN)
        _secureTokenState.value = ""
    }
}
