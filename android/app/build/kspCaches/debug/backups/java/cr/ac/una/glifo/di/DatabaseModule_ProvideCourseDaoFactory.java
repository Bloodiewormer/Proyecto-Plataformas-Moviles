package cr.ac.una.glifo.di;

import cr.ac.una.glifo.core.database.GlifoDatabase;
import cr.ac.una.glifo.core.database.dao.CourseDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class DatabaseModule_ProvideCourseDaoFactory implements Factory<CourseDao> {
  private final Provider<GlifoDatabase> databaseProvider;

  private DatabaseModule_ProvideCourseDaoFactory(Provider<GlifoDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public CourseDao get() {
    return provideCourseDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideCourseDaoFactory create(
      Provider<GlifoDatabase> databaseProvider) {
    return new DatabaseModule_ProvideCourseDaoFactory(databaseProvider);
  }

  public static CourseDao provideCourseDao(GlifoDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideCourseDao(database));
  }
}
