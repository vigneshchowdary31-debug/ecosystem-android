package com.ecosystem.core.discovery.data.ble;

@javax.inject.Singleton()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000p\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010%\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0007\b\u0007\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0018\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020#2\u0006\u0010/\u001a\u00020#H\u0017J\u0012\u00100\u001a\u00020-2\b\u00101\u001a\u0004\u0018\u00010#H\u0017J\b\u00102\u001a\u00020-H\u0017J\b\u00103\u001a\u00020-H\u0017R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0016\u0010\u000e\u001a\u0004\u0018\u00010\u000f8BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011R\u0016\u0010\u0012\u001a\u0004\u0018\u00010\u00138BX\u0082\u0004\u00a2\u0006\u0006\u001a\u0004\b\u0014\u0010\u0015R\u001d\u0010\u0016\u001a\u0004\u0018\u00010\u00178BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001a\u0010\u001b\u001a\u0004\b\u0018\u0010\u0019R\u001b\u0010\u001c\u001a\u00020\u001d8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b \u0010\u001b\u001a\u0004\b\u001e\u0010\u001fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010!\u001a\u000e\u0012\u0004\u0012\u00020#\u0012\u0004\u0012\u00020\u000b0\"X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010$\u001a\b\u0012\u0004\u0012\u00020\u00070%X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010&R\u001a\u0010\'\u001a\b\u0012\u0004\u0012\u00020\u00070%X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010&R\u000e\u0010(\u001a\u00020)X\u0082\u0004\u00a2\u0006\u0002\n\u0000R \u0010*\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u000b0\n0%X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010&\u00a8\u00064"}, d2 = {"Lcom/ecosystem/core/discovery/data/ble/DiscoveryServiceImpl;", "Lcom/ecosystem/core/discovery/domain/repository/DiscoveryService;", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "_isAdvertising", "Lkotlinx/coroutines/flow/MutableStateFlow;", "", "_isScanning", "_scanResults", "", "Lcom/ecosystem/core/discovery/domain/model/DiscoveredDevice;", "advertiseCallback", "Landroid/bluetooth/le/AdvertiseCallback;", "bleAdvertiser", "Landroid/bluetooth/le/BluetoothLeAdvertiser;", "getBleAdvertiser", "()Landroid/bluetooth/le/BluetoothLeAdvertiser;", "bleScanner", "Landroid/bluetooth/le/BluetoothLeScanner;", "getBleScanner", "()Landroid/bluetooth/le/BluetoothLeScanner;", "bluetoothAdapter", "Landroid/bluetooth/BluetoothAdapter;", "getBluetoothAdapter", "()Landroid/bluetooth/BluetoothAdapter;", "bluetoothAdapter$delegate", "Lkotlin/Lazy;", "bluetoothManager", "Landroid/bluetooth/BluetoothManager;", "getBluetoothManager", "()Landroid/bluetooth/BluetoothManager;", "bluetoothManager$delegate", "discoveredDevicesMap", "", "", "isAdvertising", "Lkotlinx/coroutines/flow/StateFlow;", "()Lkotlinx/coroutines/flow/StateFlow;", "isScanning", "scanCallback", "Landroid/bluetooth/le/ScanCallback;", "scanResults", "getScanResults", "startAdvertising", "", "localDeviceId", "localName", "startScanning", "serviceUuid", "stopAdvertising", "stopScanning", "discovery_debug"})
public final class DiscoveryServiceImpl implements com.ecosystem.core.discovery.domain.repository.DiscoveryService {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy bluetoothManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy bluetoothAdapter$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.util.List<com.ecosystem.core.discovery.domain.model.DiscoveredDevice>> _scanResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.util.List<com.ecosystem.core.discovery.domain.model.DiscoveredDevice>> scanResults = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isScanning = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isScanning = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<java.lang.Boolean> _isAdvertising = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isAdvertising = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Map<java.lang.String, com.ecosystem.core.discovery.domain.model.DiscoveredDevice> discoveredDevicesMap = null;
    @org.jetbrains.annotations.NotNull()
    private final android.bluetooth.le.ScanCallback scanCallback = null;
    @org.jetbrains.annotations.NotNull()
    private final android.bluetooth.le.AdvertiseCallback advertiseCallback = null;
    
    @javax.inject.Inject()
    public DiscoveryServiceImpl(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final android.bluetooth.BluetoothManager getBluetoothManager() {
        return null;
    }
    
    private final android.bluetooth.BluetoothAdapter getBluetoothAdapter() {
        return null;
    }
    
    private final android.bluetooth.le.BluetoothLeScanner getBleScanner() {
        return null;
    }
    
    private final android.bluetooth.le.BluetoothLeAdvertiser getBleAdvertiser() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<java.util.List<com.ecosystem.core.discovery.domain.model.DiscoveredDevice>> getScanResults() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isScanning() {
        return null;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public kotlinx.coroutines.flow.StateFlow<java.lang.Boolean> isAdvertising() {
        return null;
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    public void startScanning(@org.jetbrains.annotations.Nullable()
    java.lang.String serviceUuid) {
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    public void stopScanning() {
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    public void startAdvertising(@org.jetbrains.annotations.NotNull()
    java.lang.String localDeviceId, @org.jetbrains.annotations.NotNull()
    java.lang.String localName) {
    }
    
    @java.lang.Override()
    @android.annotation.SuppressLint(value = {"MissingPermission"})
    public void stopAdvertising() {
    }
}