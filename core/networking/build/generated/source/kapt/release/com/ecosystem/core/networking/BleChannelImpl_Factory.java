package com.ecosystem.core.networking;

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
public final class BleChannelImpl_Factory implements Factory<BleChannelImpl> {
  private final Provider<Context> contextProvider;

  public BleChannelImpl_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BleChannelImpl get() {
    return newInstance(contextProvider.get());
  }

  public static BleChannelImpl_Factory create(Provider<Context> contextProvider) {
    return new BleChannelImpl_Factory(contextProvider);
  }

  public static BleChannelImpl newInstance(Context context) {
    return new BleChannelImpl(context);
  }
}
