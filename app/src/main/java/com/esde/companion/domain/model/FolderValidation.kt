package com.esde.companion.domain.model

/**
 * Result of checking a user-picked folder during onboarding/settings.
 *
 * Two separate sealed types rather than one shared shape - a log folder's validity
 * hinges on a specific expected file, a media folder's does not (it can legitimately
 * be empty on a fresh ES-DE install). Collapsing these into one ambiguous "found: Boolean"
 * field would blur that distinction; CLAUDE.md prefers explicit modelling here.
 */
sealed class LogFolderValidation {
    data object FolderNotFound : LogFolderValidation()

    /** The folder itself exists; [logFileFound] reflects whether logs/es_log.txt was found inside it. */
    data class FolderFound(val logFileFound: Boolean) : LogFolderValidation()
}

sealed class MediaFolderValidation {
    data object FolderNotFound : MediaFolderValidation()
    data object FolderFound : MediaFolderValidation()
}