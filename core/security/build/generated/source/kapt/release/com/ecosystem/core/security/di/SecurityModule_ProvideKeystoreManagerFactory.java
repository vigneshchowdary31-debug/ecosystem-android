package com.ecosystem.core.security.di;

import com.ecosystem.core.security.KeystoreManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class SecurityModule_ProvideKeystoreManagerFactory implements Factory<KeystoreManager> {
  @Override
  public KeystoreManager get() {
    return provideKeystoreManager();
  }

  public static SecurityModule_ProvideKeystoreManagerFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static KeystoreManager provideKeystoreManager() {
    return Preconditions.checkNotNullFromProvides(SecurityModule.INSTANCE.provideKeystoreManager());
  }

  private static final class InstanceHolder {
    private static final SecurityModule_ProvideKeystoreManagerFactory INSTANCE = new SecurityModule_ProvideKeystoreManagerFactory();
  }
}
