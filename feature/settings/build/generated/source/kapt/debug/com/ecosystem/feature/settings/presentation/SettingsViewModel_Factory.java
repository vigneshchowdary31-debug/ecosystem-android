package com.ecosystem.feature.settings.presentation;

import com.ecosystem.core.security.SecureStorage;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SecureStorage> secureStorageProvider;

  public SettingsViewModel_Factory(Provider<SecureStorage> secureStorageProvider) {
    this.secureStorageProvider = secureStorageProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(secureStorageProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SecureStorage> secureStorageProvider) {
    return new SettingsViewModel_Factory(secureStorageProvider);
  }

  public static SettingsViewModel newInstance(SecureStorage secureStorage) {
    return new SettingsViewModel(secureStorage);
  }
}
