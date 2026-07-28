package com.esde.companion.ui.main

/**
 * Display-only summary of the expected cover-art location for whichever game is
 * currently in view.
 *
 * [exampleFilePath] is illustrative, not necessarily the real file: the true extension
 * (.jpg/.png/.webp) is only known once a matching file is actually found on disk, so
 * this always shows ".png" as a representative example of the naming convention.
 * [found] is the real answer to "does *some* valid-extension cover file exist here",
 * independent of which extension it turned out to be.
 */
data class CoverImageStatus(
    val exampleFilePath: String,
    val found: Boolean,
)