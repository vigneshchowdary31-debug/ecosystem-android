package com.ecosystem.core.trusteddevices.di;

import com.ecosystem.core.identity.LocalIdentityDao;
import com.ecosystem.core.trusteddevices.AppDatabase;
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
public final class DatabaseModule_Companion_ProvideLocalIdentityDaoFactory implements Factory<LocalIdentityDao> {
  private final Provider<AppDatabase> databaseProvider;

  public DatabaseModule_Companion_ProvideLocalIdentityDaoFactory(
      Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public LocalIdentityDao get() {
    return provideLocalIdentityDao(databaseProvider.get());
  }

  public static DatabaseModule_Companion_ProvideLocalIdentityDaoFactory create(
      Provider<AppDatabase> databaseProvider) {
    return new DatabaseModule_Companion_ProvideLocalIdentityDaoFactory(databaseProvider);
  }

  public static LocalIdentityDao provideLocalIdentityDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.Companion.provideLocalIdentityDao(database));
  }
}
