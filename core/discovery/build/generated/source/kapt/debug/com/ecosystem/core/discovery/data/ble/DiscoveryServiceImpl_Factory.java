package com.ecosystem.core.discovery.data.ble;

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
public final class DiscoveryServiceImpl_Factory implements Factory<DiscoveryServiceImpl> {
  private final Provider<Context> contextProvider;

  public DiscoveryServiceImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public DiscoveryServiceImpl get() {
    return newInstance(contextProvider.get());
  }

  public static DiscoveryServiceImpl_Factory create(Provider<Context> contextProvider) {
    return new DiscoveryServiceImpl_Factory(contextProvider);
  }

  public static DiscoveryServiceImpl newInstance(Context context) {
    return new DiscoveryServiceImpl(context);
  }
}
