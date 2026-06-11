package com.ecosystem.feature.pairing.presentation;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0004\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\b\u0010\u0010\u001a\u00020\u0011H\u0014J\u000e\u0010\u0012\u001a\u00020\u00112\u0006\u0010\u0013\u001a\u00020\u0014J\u0006\u0010\u0015\u001a\u00020\u0011J\u000e\u0010\u0016\u001a\u00020\u00112\u0006\u0010\u0017\u001a\u00020\u0007R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u000e\u0010\f\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u000e0\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u000b\u00a8\u0006\u0018"}, d2 = {"Lcom/ecosystem/feature/pairing/presentation/PairingViewModel;", "Landroidx/lifecycle/ViewModel;", "pairingManager", "Lcom/ecosystem/core/pairing/PairingManager;", "(Lcom/ecosystem/core/pairing/PairingManager;)V", "_hasCameraPermission", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "hasCameraPermission", "Lkotlinx/coroutines/flow/StateFlow;", "getHasCameraPermission", "()Lkotlinx/coroutines/flow/StateFlow;", "hasScanned", "pairingState", "Lcom/ecosystem/core/pairing/PairingState;", "getPairingState", "onCleared", "", "onQrCodeScanned", "qrPayload", "", "onRetry", "setCameraPermissionGranted", "granted", "pairing_release"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class PairingViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.ecosystem.core.pairing.PairingManager pairingManager = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.ecosystem.core.pairing.PairingState> pairingState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _hasCameraPermission = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> hasCameraPermission = null;
    private boolean hasScanned = false;
    
    @javax.inject.Inject()
    public PairingViewModel(@org.jetbrains.annotations.NotNull()
    com.ecosystem.core.pairing.PairingManager pairingManager) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.ecosystem.core.pairing.PairingState> getPairingState() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> getHasCameraPermission() {
        return null;
    }
    
    public final void setCameraPermissionGranted(boolean granted) {
    }
    
    public final void onQrCodeScanned(@org.jetbrains.annotations.NotNull()
    java.lang.String qrPayload) {
    }
    
    public final void onRetry() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
}