package com.esde.companion.domain.gameguides

private val LEADING_SEPARATOR_CHARS = setOf(' ', '-', '–', '—', ':', '.')
private const val GAMEFAQS_SUFFIX = " - GameFAQs"

/**
 * Strips the redundant game name GameFAQs bakes into every guide's `document.title` (the
 * only title this app has to work with - see `GameFaqsBrowserBridge`'s DETECT_SCRIPT) before
 * it's shown in a per-game guide list, where the game name is already implied by context.
 * Confirmed against real guide pages that the separator between game name and guide title is
 * NOT consistent - some use " - " ("Persona 4 Golden - Guide and Walkthrough - ..."), others
 * concatenate directly with just a space ("Metroid Prime Walkthrough & Guide - ...") - so
 * this strips [gameName] itself off the front (case-insensitively) and trims whatever
 * separator punctuation is left, rather than assuming one fixed separator. Also drops the
 * trailing "- GameFAQs" every title ends with, which carries no information once the guide
 * is saved into this app's own library. Falls back to the raw [rawTitle] whenever [gameName]
 * doesn't actually prefix it (a mismatch between ES-DE's own game name and GameFAQs' - or a
 * blank [gameName]) or stripping would leave nothing, rather than guess.
 */
object GuideTitleCleaner {
    fun clean(
        rawTitle: String,
        gameName: String,
    ): String {
        val withoutSuffix = rawTitle.removeSuffix(GAMEFAQS_SUFFIX)
        val withoutGameName = stripGameNamePrefix(withoutSuffix, gameName)
        return withoutGameName.trim().ifBlank { rawTitle }
    }

    private fun stripGameNamePrefix(
        title: String,
        gameName: String,
    ): String {
        if (gameName.isBlank() || !title.startsWith(gameName, ignoreCase = true)) return title
        val remainder = title.substring(gameName.length).trimStart { it in LEADING_SEPARATOR_CHARS }
        return remainder.ifBlank { title }
    }
}
