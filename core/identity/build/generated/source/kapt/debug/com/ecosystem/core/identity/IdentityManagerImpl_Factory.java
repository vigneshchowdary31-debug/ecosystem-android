package com.ecosystem.core.identity;

import com.ecosystem.core.security.CryptoManager;
import com.ecosystem.core.security.SecureStorage;
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
public final class IdentityManagerImpl_Factory implements Factory<IdentityManagerImpl> {
  private final Provider<LocalIdentityDao> localIdentityDaoProvider;

  private final Provider<CryptoManager> cryptoManagerProvider;

  private final Provider<SecureStorage> secureStorageProvider;

  public IdentityManagerImpl_Factory(Provider<LocalIdentityDao> localIdentityDaoProvider,
      Provider<CryptoManager> cryptoManagerProvider,
      Provider<SecureStorage> secureStorageProvider) {
    this.localIdentityDaoProvider = localIdentityDaoProvider;
    this.cryptoManagerProvider = cryptoManagerProvider;
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public IdentityManagerImpl get() {
    return newInstance(localIdentityDaoProvider.get(), cryptoManagerProvider.get(), secureStorageProvider.get());
  }

  public static IdentityManagerImpl_Factory create(
      Provider<LocalIdentityDao> localIdentityDaoProvider,
      Provider<CryptoManager> cryptoManagerProvider,
      Provider<SecureStorage> secureStorageProvider) {
    return new IdentityManagerImpl_Factory(localIdentityDaoProvider, cryptoManagerProvider, secureStorageProvider);
  }

  public static IdentityManagerImpl newInstance(LocalIdentityDao localIdentityDao,
      CryptoManager cryptoManager, SecureStorage secureStorage) {
    return new IdentityManagerImpl(localIdentityDao, cryptoManager, secureStorage);
  }
}
