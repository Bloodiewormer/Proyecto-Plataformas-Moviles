package cr.ac.una.glifo.data.di

import cr.ac.una.glifo.core.network.AuthInterceptor
import cr.ac.una.glifo.data.datasource.remote.GlifoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Wires the Retrofit client. Currently consumed only by
 * [cr.ac.una.glifo.data.repository.AuthRepositoryImpl] — see the note on [GlifoApi] — kept
 * ready for when another repository swaps its dummy datasource for this one.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Debug: 10.0.2.2 es la máquina anfitriona vista desde el emulador (backend local o Mockoon).
    // Release: Render por defecto, configurable (ver app/build.gradle.kts).
    private const val BASE_URL = cr.ac.una.glifo.BuildConfig.BASE_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            redactHeader("Authorization")
            level = if (cr.ac.una.glifo.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            // Render (plan gratis) duerme el servicio: despertarlo tarda hasta ~75 s, de ahí los 90 s de lectura.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideGlifoApi(retrofit: Retrofit): GlifoApi {
        return retrofit.create(GlifoApi::class.java)
    }
}
