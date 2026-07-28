package com.esde.companion

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder

class CompanionApplication : Application(), SingletonImageLoader.Factory {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    // Registers SVG decoding once, app-wide, so every AsyncImage call (including the
    // system-logo overlay, which loads .svg files from assets) can decode them without
    // each call site needing to build its own ImageLoader.
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
}