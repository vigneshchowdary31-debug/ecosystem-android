package com.ecosystem.feature.pairing.presentation;

import com.ecosystem.core.pairing.PairingManager;
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
public final class PairingViewModel_Factory implements Factory<PairingViewModel> {
  private final Provider<PairingManager> pairingManagerProvider;

  public PairingViewModel_Factory(Provider<PairingManager> pairingManagerProvider) {
    this.pairingManagerProvider = pairingManagerProvider;
  }

  @Override
  public PairingViewModel get() {
    return newInstance(pairingManagerProvider.get());
  }

  public static PairingViewModel_Factory create(Provider<PairingManager> pairingManagerProvider) {
    return new PairingViewModel_Factory(pairingManagerProvider);
  }

  public static PairingViewModel newInstance(PairingManager pairingManager) {
    return new PairingViewModel(pairingManager);
  }
}
