package com.saurabh.catalogue

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class CatalogueApp : Application(), SingletonImageLoader.Factory {

    /** Lazy: Coil asks for its loader after injection, and only builds the client when an image loads. */
    @Inject lateinit var okHttpClient: Lazy<OkHttpClient>

    /** Coil loads product images through the same OkHttp client as the API. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient.get() })) }
            .crossfade(true)
            .build()
}
