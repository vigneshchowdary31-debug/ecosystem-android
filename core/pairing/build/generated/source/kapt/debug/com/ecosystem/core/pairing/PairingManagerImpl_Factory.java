package com.ecosystem.core.pairing;

import com.ecosystem.core.identity.IdentityManager;
import com.ecosystem.core.networking.BleChannel;
import com.ecosystem.core.security.CryptoManager;
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class PairingManagerImpl_Factory implements Factory<PairingManagerImpl> {
  private final Provider<BleChannel> bleChannelProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<IdentityManager> identityManagerProvider;

  private final Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider;

  public PairingManagerImpl_Factory(Provider<BleChannel> bleChannelProvider,
      Provider<CryptoManager> cryptoManagerProvider,
      Provider<IdentityManager> identityManagerProvider,
      Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider) {
    this.bleChannelProvider = bleChannelProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.identityManagerProvider = identityManagerProvider;
    this.trustedDeviceRepositoryProvider = trustedDeviceRepositoryProvider;
  }

  @Override
  public PairingManagerImpl get() {
    return newInstance(bleChannelProvider.get(), cryptoManagerProvider.get(), identityManagerProvider.get(), trustedDeviceRepositoryProvider.get());
  }

  public static PairingManagerImpl_Factory create(Provider<BleChannel> bleChannelProvider,
      Provider<CryptoManager> cryptoManagerProvider,
      Provider<IdentityManager> identityManagerProvider,
      Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider) {
    return new PairingManagerImpl_Factory(bleChannelProvider, cryptoManagerProvider, identityManagerProvider, trustedDeviceRepositoryProvider);
  }

  public static PairingManagerImpl newInstance(BleChannel bleChannel, CryptoManager cryptoManager,
      IdentityManager identityManager, TrustedDeviceRepository trustedDeviceRepository) {
    return new PairingManagerImpl(bleChannel, cryptoManager, identityManager, trustedDeviceRepository);
  }
}
