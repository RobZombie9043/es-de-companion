package com.esde.companion.ui.main

import android.content.Context
import coil3.request.ImageRequest
import java.io.File

/**
 * Stable identity for a Coil [model] that reflects content, not just path. A `File`'s
 * default equals()/hashCode() is path-only, so replacing a file's *content* at the same
 * downloaded_media / custom-logo path would otherwise be invisible to both Compose's
 * remember/key mechanisms and Coil's default (also path-only) memory cache key - the old
 * bitmap would keep showing indefinitely. Folding in [File.lastModified] fixes both at
 * once. Anything else (bundled asset path strings, which are never mutated at a fixed
 * path) is used as-is.
 */
internal fun identityKeyOf(model: Any?): Any? = if (model is File) "${model.path}_${model.lastModified()}" else model

/**
 * Builds the actual Coil request for [model]. `rememberAsyncImagePainter`/`AsyncImage`/
 * `imageLoader.execute` all accept an [ImageRequest] directly as "model" - if it's
 * already one they use it as-is, otherwise they wrap it via
 * `ImageRequest.Builder(context).data(model).build()`. Building it explicitly here lets
 * us also set `memoryCacheKey` to [identityKeyOf], so a content-changed File gets a
 * fresh cache entry instead of resolving to Coil's default (path-only) key.
 */
internal fun requestFor(
    context: Context,
    model: Any?,
): ImageRequest =
    ImageRequest.Builder(context)
        .data(model)
        .apply {
            val key = identityKeyOf(model) as? String
            if (key != null) memoryCacheKey(key)
        }
        .build()
