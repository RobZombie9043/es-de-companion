package com.esde.companion

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.esde.companion.data.media.NormalizingSvgDecoder

class CompanionApplication : Application(), SingletonImageLoader.Factory {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    // Registers SVG decoding (built-in system logo assets, and any SVG picked via the
    // Custom Image widget) and animated GIF/WebP decoding (custom system images/logos -
    // see Settings > Setup) once, app-wide, so every AsyncImage/CrossfadeAsyncImage call
    // site can decode them without building its own ImageLoader. AnimatedImageDecoder is
    // backed by the platform ImageDecoder API (API 28+), which this app already requires
    // (minSdk 29) - it handles both GIF and animated WebP, so a separate legacy
    // GifDecoder isn't needed. Animation itself is automatic once decoded: coil3-compose's
    // AsyncImagePainter drives Animatable drawables without any change to
    // CrossfadeAsyncImage or WidgetContentView.
    //
    // NormalizingSvgDecoder claims every SVG (same applicability check SvgDecoder.Factory
    // itself uses) and delegates the actual rasterization to a real SvgDecoder internally
    // - see its kdoc - so it fully replaces registering SvgDecoder.Factory separately here.
    //
    // OkHttpNetworkFetcherFactory is what actually lets AsyncImage load an http(s) URL at
    // all - Coil 3 split network support out of coil-compose into its own artifact with no
    // ServiceLoader auto-registration, so without this, every network-backed AsyncImage
    // (RetroAchievements badge/candidate icons) silently fails to load. Plain OkHttp, not
    // a new sanctioned network exception (see CLAUDE.md) - this is Coil's own image
    // transport, not a domain-level API integration, and OkHttp is already a transitive
    // dependency via RetroAchievements/api-kotlin.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(NormalizingSvgDecoder.Factory())
                add(AnimatedImageDecoder.Factory())
                add(OkHttpNetworkFetcherFactory())
            }
            .build()
}
