package com.esde.companion.data.retroachievements

import android.content.Context
import com.esde.companion.domain.model.AchievementItem
import com.esde.companion.domain.model.AchievementType
import com.esde.companion.domain.model.GameAchievementSummary
import com.esde.companion.domain.model.GamePlaytimeStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

private const val DIRECTORY_NAME = "retroachievements_achievement_summaries"

// One entry per game a user has ever viewed achievements for, unlike GameListCacheStore
// (bounded by console count) or UserProgressCacheStore (one entry, the single signed-in user) -
// this is the first RetroAchievements cache needing a real eviction policy. Pruned to the
// most-recently-written entries after every write rather than left to grow indefinitely. 2000 is
// generous enough to survive even an aggressive single-session browsing binge (e.g. window-shopping
// a whole large system's game list) without evicting recently-viewed games, while keeping worst-case
// storage in the tens of MB.
private const val MAX_CACHED_GAMES = 2000

/**
 * Disk-backed [AchievementSummaryCacheStore] - one small JSON file per (username, gameId) under
 * [Context.cacheDir], rather than DataStore Preferences like [GameListCacheStore]/
 * [UserProgressCacheStore]. DataStore Preferences rewrites its *entire* backing file on every
 * `edit{}`, regardless of key count - fine for those two caches' small, bounded key spaces, but
 * this cache's key space (one entry per game ever viewed) has no such bound, so a shared
 * DataStore file would pay a full-cache rewrite on every single-game refresh once many games are
 * cached. A flat file per key keeps each write's cost independent of how many games are cached.
 * [Context.cacheDir] is also the semantically correct bucket for this data - OS-reclaimable
 * under storage pressure, excluded from backup by default - matching "freely re-fetchable,
 * non-critical" exactly. See [MAX_CACHED_GAMES] for the pruning policy.
 */
class FileAchievementSummaryCacheStore(
    private val context: Context,
) : AchievementSummaryCacheStore {
    private val directory: File by lazy {
        File(context.cacheDir, DIRECTORY_NAME).apply { mkdirs() }
    }

    override suspend fun read(
        username: String,
        gameId: Long,
    ): CachedAchievementSummary? =
        withContext(Dispatchers.IO) {
            val file = fileFor(username, gameId)
            if (!file.exists()) return@withContext null
            try {
                Json.decodeFromString<AchievementSummaryDto>(file.readText()).toDomain()
            } catch (
                @Suppress("SwallowedException") e: SerializationException,
            ) {
                null
            } catch (
                @Suppress("SwallowedException") e: IOException,
            ) {
                null
            }
        }

    override suspend fun write(
        username: String,
        gameId: Long,
        cached: CachedAchievementSummary,
    ) {
        withContext(Dispatchers.IO) {
            try {
                fileFor(username, gameId).writeText(Json.encodeToString(cached.toDto()))
            } catch (
                @Suppress("SwallowedException") e: IOException,
            ) {
                // Best-effort - a failed cache write shouldn't break achievement display.
            }
            prune()
        }
    }

    private fun prune() {
        val files = directory.listFiles() ?: return
        val excess = files.size - MAX_CACHED_GAMES
        if (excess <= 0) return
        files.sortedBy { it.lastModified() }
            .take(excess)
            .forEach { it.delete() }
    }

    private fun fileFor(
        username: String,
        gameId: Long,
    ): File = File(directory, "${sanitize(username)}_$gameId.json")

    private fun sanitize(username: String) = username.replace(Regex("[^A-Za-z0-9_.-]"), "_")
}

@Serializable
private data class AchievementSummaryDto(
    val fetchedAtMillis: Long,
    val gameId: Long,
    val gameTitle: String,
    val totalPoints: Int,
    val earnedPoints: Int,
    val completionPercent: Float,
    val achievements: List<AchievementItemDto>,
    val totalPlayers: Int = 0,
    val playtimeStats: GamePlaytimeStatsDto? = null,
)

@Serializable
private data class AchievementItemDto(
    val id: Long,
    val title: String,
    val description: String,
    val points: Int,
    val badgeUrl: String?,
    val unlocked: Boolean,
    val unlockedAt: Long?,
    val displayOrder: Int = 0,
    val numAwarded: Int = 0,
    val numAwardedHardcore: Int = 0,
    val type: AchievementType? = null,
)

@Serializable
private data class GamePlaytimeStatsDto(
    val beatSeconds: Int?,
    val beatHardcoreSeconds: Int?,
    val completedSeconds: Int?,
    val masteredSeconds: Int?,
)

private fun CachedAchievementSummary.toDto(): AchievementSummaryDto {
    return AchievementSummaryDto(
        fetchedAtMillis = fetchedAtMillis,
        gameId = summary.gameId,
        gameTitle = summary.gameTitle,
        totalPoints = summary.totalPoints,
        earnedPoints = summary.earnedPoints,
        completionPercent = summary.completionPercent,
        achievements = summary.achievements.map { it.toDto() },
        totalPlayers = summary.totalPlayers,
        playtimeStats = summary.playtimeStats?.toDto(),
    )
}

private fun AchievementItem.toDto() =
    AchievementItemDto(
        id = id,
        title = title,
        description = description,
        points = points,
        badgeUrl = badgeUrl,
        unlocked = unlocked,
        unlockedAt = unlockedAt,
        displayOrder = displayOrder,
        numAwarded = numAwarded,
        numAwardedHardcore = numAwardedHardcore,
        type = type,
    )

private fun GamePlaytimeStats.toDto(): GamePlaytimeStatsDto {
    return GamePlaytimeStatsDto(beatSeconds, beatHardcoreSeconds, completedSeconds, masteredSeconds)
}

private fun AchievementSummaryDto.toDomain(): CachedAchievementSummary {
    val summary =
        GameAchievementSummary(
            gameId = gameId,
            gameTitle = gameTitle,
            totalPoints = totalPoints,
            earnedPoints = earnedPoints,
            completionPercent = completionPercent,
            achievements = achievements.map { it.toDomain() },
            totalPlayers = totalPlayers,
            playtimeStats = playtimeStats?.toDomain(),
        )
    return CachedAchievementSummary(fetchedAtMillis, summary)
}

private fun AchievementItemDto.toDomain() =
    AchievementItem(
        id = id,
        title = title,
        description = description,
        points = points,
        badgeUrl = badgeUrl,
        unlocked = unlocked,
        unlockedAt = unlockedAt,
        displayOrder = displayOrder,
        numAwarded = numAwarded,
        numAwardedHardcore = numAwardedHardcore,
        type = type,
    )

private fun GamePlaytimeStatsDto.toDomain(): GamePlaytimeStats {
    return GamePlaytimeStats(beatSeconds, beatHardcoreSeconds, completedSeconds, masteredSeconds)
}
