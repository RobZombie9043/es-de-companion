package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EsdeSettingsParserTest {
    @Test
    fun `parses a string value`() {
        val xml = """<string name="MediaDirectory" value="/storage/emulated/0/ES-DE/downloaded_media" />"""
        assertEquals(
            "/storage/emulated/0/ES-DE/downloaded_media",
            EsdeSettingsParser.findStringValue(xml, "MediaDirectory"),
        )
    }

    @Test
    fun `parses a true bool value`() {
        val xml = """<bool name="DebugMode" value="true" />"""
        assertEquals(true, EsdeSettingsParser.findBoolValue(xml, "DebugMode"))
    }

    @Test
    fun `parses a false bool value`() {
        val xml = """<bool name="CustomEventScripts" value="false" />"""
        assertEquals(false, EsdeSettingsParser.findBoolValue(xml, "CustomEventScripts"))
    }

    @Test
    fun `finds the right tag among many others in a realistic file`() {
        val xml =
            """
            <?xml version="1.0"?>
            <config>
                <string name="ROMDirectory" value="" />
                <bool name="CustomEventScripts" value="true" />
                <bool name="CustomEventScriptsBrowsing" value="false" />
                <bool name="DebugMode" value="true" />
                <string name="MediaDirectory" value="/storage/emulated/0/ES-DE/downloaded_media" />
            </config>
            """.trimIndent()

        assertEquals(true, EsdeSettingsParser.findBoolValue(xml, "CustomEventScripts"))
        assertEquals(false, EsdeSettingsParser.findBoolValue(xml, "CustomEventScriptsBrowsing"))
        assertEquals(true, EsdeSettingsParser.findBoolValue(xml, "DebugMode"))
        assertEquals(
            "/storage/emulated/0/ES-DE/downloaded_media",
            EsdeSettingsParser.findStringValue(xml, "MediaDirectory"),
        )
    }

    @Test
    fun `missing string tag returns null`() {
        val xml = """<bool name="DebugMode" value="true" />"""
        assertNull(EsdeSettingsParser.findStringValue(xml, "MediaDirectory"))
    }

    @Test
    fun `missing bool tag returns null`() {
        val xml = """<string name="MediaDirectory" value="/roms" />"""
        assertNull(EsdeSettingsParser.findBoolValue(xml, "DebugMode"))
    }

    @Test
    fun `malformed bool value returns null rather than throwing`() {
        val xml = """<bool name="DebugMode" value="maybe" />"""
        assertNull(EsdeSettingsParser.findBoolValue(xml, "DebugMode"))
    }

    @Test
    fun `empty file returns null`() {
        assertNull(EsdeSettingsParser.findStringValue("", "MediaDirectory"))
        assertNull(EsdeSettingsParser.findBoolValue("", "DebugMode"))
    }

    @Test
    fun `truncated tag returns null rather than throwing`() {
        val xml = """<string name="MediaDirectory" value="/roms"""
        assertNull(EsdeSettingsParser.findStringValue(xml, "MediaDirectory"))
    }
}
