package com.esde.companion.domain.model

/**
 * A game's description text parsed from its system's gamelist.xml, if any. [gamelistPath]
 * is the specific gamelist.xml file [text] was (or would have been) parsed from - standard
 * or legacy location, see FileGameDescriptionRepository - null only when no gamelist.xml
 * could be found at either location. Carried on the model (rather than discarded once
 * [text] is extracted) purely so callers like LoggingGameDescriptionRepository can report
 * exactly which file a lookup used, the same way GameMedia carries resolved paths.
 */
data class GameDescription(val text: String?, val gamelistPath: String? = null)
