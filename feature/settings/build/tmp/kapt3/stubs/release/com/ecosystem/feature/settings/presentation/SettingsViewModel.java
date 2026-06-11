package com.ecosystem.feature.settings.presentation;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0005\b\u0007\u0018\u0000 \u00112\u00020\u0001:\u0001\u0011B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\b\u0010\u000e\u001a\u00020\rH\u0002J\u000e\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\u0007R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0012"}, d2 = {"Lcom/ecosystem/feature/settings/presentation/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "secureStorage", "Lcom/ecosystem/core/security/SecureStorage;", "(Lcom/ecosystem/core/security/SecureStorage;)V", "_secureTokenState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "secureTokenState", "Lkotlinx/coroutines/flow/StateFlow;", "getSecureTokenState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearSecureToken", "", "loadSecureToken", "saveSecureToken", "token", "Companion", "settings_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.security.SecureStorage secureStorage = null;
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String KEY_SECURE_TOKEN = "companion_secure_token";
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.String> _secureTokenState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.String> secureTokenState = null;
    @org.jetbrains.annotations.NotNull()
    public static final com.ecosystem.feature.settings.presentation.SettingsViewModel.Companion Companion = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.security.SecureStorage secureStorage) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.String> getSecureTokenState() {
        return null;
    }
    
    private final void loadSecureToken() {
    }
    
    public final void saveSecureToken(@org.jetbrains.annotations.NotNull()
    java.lang.String token) {
    }
    
    public final void clearSecureToken() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/ecosystem/feature/settings/presentation/SettingsViewModel$Companion;", "", "()V", "KEY_SECURE_TOKEN", "", "settings_release"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}