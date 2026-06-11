package com.ecosystem.core.discovery.di;

import android.content.Context;
import com.ecosystem.core.discovery.domain.repository.DiscoveryService;
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
public final class DiscoveryModule_ProvideDiscoveryServiceFactory implements Factory<DiscoveryService> {
  private final Provider<Context> contextProvider;

  public DiscoveryModule_ProvideDiscoveryServiceFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DiscoveryService get() {
    return provideDiscoveryService(contextProvider.get());
  }

  public static DiscoveryModule_ProvideDiscoveryServiceFactory create(
      Provider<Context> contextProvider) {
    return new DiscoveryModule_ProvideDiscoveryServiceFactory(contextProvider);
  }

  public static DiscoveryService provideDiscoveryService(Context context) {
    return Preconditions.checkNotNullFromProvides(DiscoveryModule.INSTANCE.provideDiscoveryService(context));
  }
}
