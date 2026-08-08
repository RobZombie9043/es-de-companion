package com.esde.companion.domain.parser

/**
 * Parses individual named values out of ES-DE's own `settings/es_settings.xml`, e.g.:
 *   <string name="MediaDirectory" value="/storage/emulated/0/ES-DE/downloaded_media" />
 *   <bool name="DebugMode" value="true" />
 *
 * The file is a flat, non-nested list of typed settings ES-DE itself writes - a full XML
 * parser is unjustified complexity for that shape, so this matches per-tag via regex, the
 * same pragmatic approach EsdeEventParser takes for log lines. Deliberately a pure function
 * with no Android dependencies so it can be unit tested directly against fixture content.
 *
 * A missing or malformed tag returns null - an expected, routine outcome (the setting was
 * never touched, or the file doesn't match the shape we expect), not an error.
 */
object EsdeSettingsParser {
    fun findStringValue(
        xmlContent: String,
        settingName: String,
    ): String? = stringRegexFor(settingName).find(xmlContent)?.groupValues?.get(1)

    fun findBoolValue(
        xmlContent: String,
        settingName: String,
    ): Boolean? = boolRegexFor(settingName).find(xmlContent)?.groupValues?.get(1)?.toBooleanStrictOrNull()

    private fun stringRegexFor(settingName: String) = Regex("""<string\s+name="${Regex.escape(settingName)}"\s+value="([^"]*)"\s*/>""")

    private fun boolRegexFor(settingName: String) = Regex("""<bool\s+name="${Regex.escape(settingName)}"\s+value="([^"]*)"\s*/>""")
}
