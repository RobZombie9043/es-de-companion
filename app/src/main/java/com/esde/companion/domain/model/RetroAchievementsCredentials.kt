package com.esde.companion.domain.model

/**
 * A RetroAchievements account's Web API Key (from the account's control panel), not the
 * account password - the RetroAchievements API has no password-login mode. [toString] is
 * overridden to redact [webApiKey], since this is the one credential-shaped model this
 * codebase has ever had and it can otherwise end up in logs like any other data class.
 */
data class RetroAchievementsCredentials(
    val username: String,
    val webApiKey: String,
) {
    override fun toString(): String = "RetroAchievementsCredentials(username=$username, webApiKey=<redacted>)"
}
