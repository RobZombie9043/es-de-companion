package com.esde.companion.domain.repository

import com.esde.companion.domain.model.AppConfigBackup

/**
 * Pure [AppConfigBackup] <-> `String` transformation - no file/Uri I/O here, see
 * `ConfigBackupFileIo` (data layer, called directly from the Composable that owns the SAF
 * picker result, same pattern as `SafPathResolver`) for actually reading/writing the chosen
 * document.
 *
 * [deserialize] fails - as a [Result], not a thrown exception, since a user-picked file is
 * external input no more trustworthy than a raw log line - for malformed JSON, and for a
 * backup whose `version` is newer than [AppConfigBackup.CURRENT_VERSION] (a backup made by a
 * newer build of this app, opened by an older one: rejected outright rather than silently
 * dropping fields this build doesn't know about via lenient decoding).
 */
interface ConfigBackupRepository {
    fun serialize(snapshot: AppConfigBackup): String

    fun deserialize(contents: String): Result<AppConfigBackup>
}
