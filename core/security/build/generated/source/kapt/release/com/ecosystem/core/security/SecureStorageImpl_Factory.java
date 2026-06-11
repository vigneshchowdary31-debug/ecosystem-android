package com.ecosystem.core.security;

import android.content.Context;
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
public final class SecureStorageImpl_Factory implements Factory<SecureStorageImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<KeystoreManager> keystoreManagerProvider;

  public SecureStorageImpl_Factory(Provider<Context> contextProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    this.contextProvider = contextProvider;
    this.keystoreManagerProvider = keystoreManagerProvider;
  }

  @Override
  public SecureStorageImpl get() {
    return newInstance(contextProvider.get(), keystoreManagerProvider.get());
  }

  public static SecureStorageImpl_Factory create(Provider<Context> contextProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    return new SecureStorageImpl_Factory(contextProvider, keystoreManagerProvider);
  }

  public static SecureStorageImpl newInstance(Context context, KeystoreManager keystoreManager) {
    return new SecureStorageImpl(context, keystoreManager);
  }
}
