package com.ecosystem.core.security.di;

import com.ecosystem.core.security.CryptoManager;
import com.ecosystem.core.security.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class SecurityModule_ProvideCryptoManagerFactory implements Factory<CryptoManager> {
  private final Provider<SecureStorage> secureStorageProvider;

  public SecurityModule_ProvideCryptoManagerFactory(Provider<SecureStorage> secureStorageProvider) {
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public CryptoManager get() {
    return provideCryptoManager(secureStorageProvider.get());
  }

  public static SecurityModule_ProvideCryptoManagerFactory create(
      Provider<SecureStorage> secureStorageProvider) {
    return new SecurityModule_ProvideCryptoManagerFactory(secureStorageProvider);
  }

  public static CryptoManager provideCryptoManager(SecureStorage secureStorage) {
    return Preconditions.checkNotNullFromProvides(SecurityModule.INSTANCE.provideCryptoManager(secureStorage));
  }
}
