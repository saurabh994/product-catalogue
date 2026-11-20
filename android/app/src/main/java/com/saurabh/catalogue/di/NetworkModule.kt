package com.saurabh.catalogue.di

import com.saurabh.catalogue.BuildConfig
import com.saurabh.catalogue.data.remote.ApiBaseUrl
import com.saurabh.catalogue.data.remote.CatalogueApi
import com.saurabh.catalogue.data.remote.NetworkConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Builds the whole network stack. Each step is its own @Provides so it can be replaced
 * independently (e.g. a test OkHttpClient) and so tests can build the exact production config.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @ApiBaseUrl
    fun provideApiBaseUrl(): HttpUrl = NetworkConstants.BASE_URL.toHttpUrl()

    /** Tolerate new fields the backend may add later; omit nulls when sending. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    /** Shared by Retrofit and Coil, so API calls and images use one connection pool. */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(@ApiBaseUrl baseUrl: HttpUrl, client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(NetworkConstants.CONTENT_TYPE_JSON.toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideCatalogueApi(retrofit: Retrofit): CatalogueApi = retrofit.create()
}
