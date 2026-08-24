package com.esde.companion.data.retroachievements

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/**
 * Its own DataStore file, deliberately not shared with [gameListCacheDataStore] - see
 * [DataStoreUserProgressCacheStore]'s kdoc for why.
 */
internal val Context.userProgressCacheDataStore by preferencesDataStore(name = "retroachievements_user_progress_cache")
