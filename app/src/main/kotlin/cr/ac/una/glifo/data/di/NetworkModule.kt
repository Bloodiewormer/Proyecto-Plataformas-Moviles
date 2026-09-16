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

    /**
     * `10.0.2.2` is the emulator's virtual router alias for the host machine's loopback
     * (the emulator manages the whole `10.0.2/24` space: `.1` gateway, `.2` host loopback,
     * `.15` the device over ethernet, `.16` over Wi-Fi). The port is whatever the Mockoon
     * environment listens on — 3000 here, see D-25 for the route contract.
     *
     * **Known emulator gotcha:** on emulator versions before 36.5 the alias only resolves
     * over the cellular/ethernet interface, not over the emulated Wi-Fi AP — with Wi-Fi on,
     * requests to `10.0.2.2` leak out to the real LAN and hang unanswered. If the app can't
     * reach the mock, turn the emulator's Wi-Fi off (mobile data on) or update the emulator
     * in SDK Manager. Nothing machine-specific belongs in this constant — no LAN IPs.
     */
    private const val BASE_URL = "http://10.0.2.2:3000/api/v1/"

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
