package com.esde.companion.domain.model

/**
 * A game's ROM hash parsed from its system's gamelist.xml, if any - see
 * [com.esde.companion.domain.repository.GameRomHashRepository]. [gamelistPath] is the
 * specific gamelist.xml file [value] was (or would have been) parsed from - standard or
 * legacy location, see `FileGameRomHashRepository` - null only when no gamelist.xml could
 * be found at either location. Carried on the model (rather than discarded once [value] is
 * extracted) purely so callers like `LoggingGameRomHashRepository` can report exactly which
 * file a lookup used, the same way [GameDescription] does.
 */
data class GameRomHash(val value: String?, val gamelistPath: String? = null)
