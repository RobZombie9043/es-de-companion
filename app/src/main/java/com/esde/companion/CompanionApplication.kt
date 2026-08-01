package com.esde.companion

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.svg.SvgDecoder

class CompanionApplication : Application(), SingletonImageLoader.Factory {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    // Registers SVG decoding (built-in system logo assets) and animated GIF/WebP
    // decoding (custom system images/logos - see Settings > Setup) once, app-wide, so
    // every AsyncImage/CrossfadeAsyncImage call site can decode them without building
    // its own ImageLoader. AnimatedImageDecoder is backed by the platform ImageDecoder
    // API (API 28+), which this app already requires (minSdk 29) - it handles both GIF
    // and animated WebP, so a separate legacy GifDecoder isn't needed. Animation itself
    // is automatic once decoded: coil3-compose's AsyncImagePainter drives Animatable
    // drawables without any change to CrossfadeAsyncImage or WidgetContentView.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
                add(AnimatedImageDecoder.Factory())
            }
            .build()
}