package cr.ac.una.glifo.di;

import cr.ac.una.glifo.core.network.GlifoApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import retrofit2.Retrofit;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class NetworkModule_ProvideGlifoApiFactory implements Factory<GlifoApi> {
  private final Provider<Retrofit> retrofitProvider;

  private NetworkModule_ProvideGlifoApiFactory(Provider<Retrofit> retrofitProvider) {
    this.retrofitProvider = retrofitProvider;
  }

  @Override
  public GlifoApi get() {
    return provideGlifoApi(retrofitProvider.get());
  }

  public static NetworkModule_ProvideGlifoApiFactory create(Provider<Retrofit> retrofitProvider) {
    return new NetworkModule_ProvideGlifoApiFactory(retrofitProvider);
  }

  public static GlifoApi provideGlifoApi(Retrofit retrofit) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideGlifoApi(retrofit));
  }
}
