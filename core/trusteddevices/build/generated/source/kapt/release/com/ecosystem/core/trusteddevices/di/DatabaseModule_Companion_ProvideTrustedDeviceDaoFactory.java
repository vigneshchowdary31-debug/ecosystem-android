package com.ecosystem.core.trusteddevices.di;

import com.ecosystem.core.trusteddevices.AppDatabase;
import com.ecosystem.core.trusteddevices.TrustedDeviceDao;
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
public final class DatabaseModule_Companion_ProvideTrustedDeviceDaoFactory implements Factory<TrustedDeviceDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_Companion_ProvideTrustedDeviceDaoFactory(
      Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public TrustedDeviceDao get() {
    return provideTrustedDeviceDao(databaseProvider.get());
  }

  public static DatabaseModule_Companion_ProvideTrustedDeviceDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_Companion_ProvideTrustedDeviceDaoFactory(databaseProvider);
  }

  public static TrustedDeviceDao provideTrustedDeviceDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.Companion.provideTrustedDeviceDao(database));
  }
}
