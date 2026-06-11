package com.ecosystem.core.security;

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
public final class CryptoManagerImpl_Factory implements Factory<CryptoManagerImpl> {
  private final Provider<SecureStorage> secureStorageProvider;

  public CryptoManagerImpl_Factory(Provider<SecureStorage> secureStorageProvider) {
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public CryptoManagerImpl get() {
    return newInstance(secureStorageProvider.get());
  }

  public static CryptoManagerImpl_Factory create(Provider<SecureStorage> secureStorageProvider) {
    return new CryptoManagerImpl_Factory(secureStorageProvider);
  }

  public static CryptoManagerImpl newInstance(SecureStorage secureStorage) {
    return new CryptoManagerImpl(secureStorage);
  }
}
