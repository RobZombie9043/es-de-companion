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

    /** The folder itself exists; [settingsFileFound] reflects whether
     * settings/es_settings.xml was found inside it - the strongest cheap signal that this
     * is genuinely an ES-DE root folder (the log pipeline's actual liveness is proven
     * separately, by onboarding's final live-check step, not by this one-shot check). */
    data class FolderFound(val settingsFileFound: Boolean) : LogFolderValidation()
}

sealed class MediaFolderValidation {
    data object FolderNotFound : MediaFolderValidation()

    data object FolderFound : MediaFolderValidation()
}
