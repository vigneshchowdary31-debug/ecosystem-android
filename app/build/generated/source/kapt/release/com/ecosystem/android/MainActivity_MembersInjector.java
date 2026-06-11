package com.ecosystem.android;

import com.ecosystem.core.discovery.domain.repository.DiscoveryService;
import com.ecosystem.core.identity.IdentityManager;
import com.ecosystem.core.trusteddevices.TrustedDeviceRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<IdentityManager> identityManagerProvider;

  private final Provider<DiscoveryService> discoveryServiceProvider;

  private final Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider;

  public MainActivity_MembersInjector(Provider<IdentityManager> identityManagerProvider,
      Provider<DiscoveryService> discoveryServiceProvider,
      Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider) {
    this.identityManagerProvider = identityManagerProvider;
    this.discoveryServiceProvider = discoveryServiceProvider;
    this.trustedDeviceRepositoryProvider = trustedDeviceRepositoryProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<IdentityManager> identityManagerProvider,
      Provider<DiscoveryService> discoveryServiceProvider,
      Provider<TrustedDeviceRepository> trustedDeviceRepositoryProvider) {
    return new MainActivity_MembersInjector(identityManagerProvider, discoveryServiceProvider, trustedDeviceRepositoryProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectIdentityManager(instance, identityManagerProvider.get());
    injectDiscoveryService(instance, discoveryServiceProvider.get());
    injectTrustedDeviceRepository(instance, trustedDeviceRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.ecosystem.android.MainActivity.identityManager")
  public static void injectIdentityManager(MainActivity instance, IdentityManager identityManager) {
    instance.identityManager = identityManager;
  }

  @InjectedFieldSignature("com.ecosystem.android.MainActivity.discoveryService")
  public static void injectDiscoveryService(MainActivity instance,
      DiscoveryService discoveryService) {
    instance.discoveryService = discoveryService;
  }

  @InjectedFieldSignature("com.ecosystem.android.MainActivity.trustedDeviceRepository")
  public static void injectTrustedDeviceRepository(MainActivity instance,
      TrustedDeviceRepository trustedDeviceRepository) {
    instance.trustedDeviceRepository = trustedDeviceRepository;
  }
}
