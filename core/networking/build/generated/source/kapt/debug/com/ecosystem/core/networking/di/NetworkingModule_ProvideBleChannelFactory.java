package com.ecosystem.core.networking.di;

import android.content.Context;
import com.ecosystem.core.networking.BleChannel;
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
public final class NetworkingModule_ProvideBleChannelFactory implements Factory<BleChannel> {
  private final Provider<Context> contextProvider;

  public NetworkingModule_ProvideBleChannelFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BleChannel get() {
    return provideBleChannel(contextProvider.get());
  }

  public static NetworkingModule_ProvideBleChannelFactory create(
      Provider<Context> contextProvider) {
    return new NetworkingModule_ProvideBleChannelFactory(contextProvider);
  }

  public static BleChannel provideBleChannel(Context context) {
    return Preconditions.checkNotNullFromProvides(NetworkingModule.INSTANCE.provideBleChannel(context));
  }
}
