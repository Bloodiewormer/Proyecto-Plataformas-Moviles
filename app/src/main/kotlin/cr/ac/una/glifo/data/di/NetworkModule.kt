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
import javax.inject.Singleton

/**
 * Wires the Retrofit client. Currently consumed only by
 * [cr.ac.una.glifo.data.repository.AuthRepositoryImpl] — see the note on [GlifoApi] — kept
 * ready for when another repository swaps its dummy datasource for this one.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {


    private const val BASE_URL = "http://127.0.0.1:8080/api/v1/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
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
