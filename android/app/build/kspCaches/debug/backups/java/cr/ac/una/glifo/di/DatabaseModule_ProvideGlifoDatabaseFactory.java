package cr.ac.una.glifo.di;

import android.content.Context;
import cr.ac.una.glifo.core.database.GlifoDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DatabaseModule_ProvideGlifoDatabaseFactory implements Factory<GlifoDatabase> {
  private final Provider<Context> contextProvider;

  private DatabaseModule_ProvideGlifoDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public GlifoDatabase get() {
    return provideGlifoDatabase(contextProvider.get());
  }

  public static DatabaseModule_ProvideGlifoDatabaseFactory create(
      Provider<Context> contextProvider) {
    return new DatabaseModule_ProvideGlifoDatabaseFactory(contextProvider);
  }

  public static GlifoDatabase provideGlifoDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideGlifoDatabase(context));
  }
}
