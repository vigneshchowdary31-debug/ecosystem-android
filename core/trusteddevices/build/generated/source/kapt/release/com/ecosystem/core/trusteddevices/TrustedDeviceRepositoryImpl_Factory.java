package com.ecosystem.core.trusteddevices;

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
public final class TrustedDeviceRepositoryImpl_Factory implements Factory<TrustedDeviceRepositoryImpl> {
  private final Provider<TrustedDeviceDao> trustedDeviceDaoProvider;

  public TrustedDeviceRepositoryImpl_Factory(Provider<TrustedDeviceDao> trustedDeviceDaoProvider) {
    this.trustedDeviceDaoProvider = trustedDeviceDaoProvider;
  }

  @Override
  public TrustedDeviceRepositoryImpl get() {
    return newInstance(trustedDeviceDaoProvider.get());
  }

  public static TrustedDeviceRepositoryImpl_Factory create(
      Provider<TrustedDeviceDao> trustedDeviceDaoProvider) {
    return new TrustedDeviceRepositoryImpl_Factory(trustedDeviceDaoProvider);
  }

  public static TrustedDeviceRepositoryImpl newInstance(TrustedDeviceDao trustedDeviceDao) {
    return new TrustedDeviceRepositoryImpl(trustedDeviceDao);
  }
}
