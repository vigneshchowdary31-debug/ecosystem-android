package com.ecosystem.core.security.di;

import android.content.Context;
import com.ecosystem.core.security.KeystoreManager;
import com.ecosystem.core.security.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SecurityModule_ProvideSecureStorageFactory implements Factory<SecureStorage> {
  private final Provider<Context> contextProvider;

  private final Provider<KeystoreManager> keystoreManagerProvider;

  public SecurityModule_ProvideSecureStorageFactory(Provider<Context> contextProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    this.contextProvider = contextProvider;
    this.keystoreManagerProvider = keystoreManagerProvider;
  }

  @Override
  public SecureStorage get() {
    return provideSecureStorage(contextProvider.get(), keystoreManagerProvider.get());
  }

  public static SecurityModule_ProvideSecureStorageFactory create(Provider<Context> contextProvider,
      Provider<KeystoreManager> keystoreManagerProvider) {
    return new SecurityModule_ProvideSecureStorageFactory(contextProvider, keystoreManagerProvider);
  }

  public static SecureStorage provideSecureStorage(Context context,
      KeystoreManager keystoreManager) {
    return Preconditions.checkNotNullFromProvides(SecurityModule.INSTANCE.provideSecureStorage(context, keystoreManager));
  }
}
