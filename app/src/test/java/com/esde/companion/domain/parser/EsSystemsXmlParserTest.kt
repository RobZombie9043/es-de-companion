package com.esde.companion.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EsSystemsXmlParserTest {
    @Test
    fun `resolves a literal absolute path with no substitution needed`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>ps2</name>
                    <path>/storage/emulated/0/Roms/PS2</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "ps2", romDirectory = null)

        assertEquals("/storage/emulated/0/Roms/PS2", result)
    }

    @Test
    fun `substitutes ROMPATH with the given romDirectory`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "/storage/E2AB-E84A/ROMs")

        assertEquals("/storage/E2AB-E84A/ROMs/DS", result)
    }

    @Test
    fun `strips a trailing slash from romDirectory before substitution`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "/storage/E2AB-E84A/ROMs/")

        assertEquals("/storage/E2AB-E84A/ROMs/DS", result)
    }

    @Test
    fun `returns null for a ROMPATH-relative path when romDirectory is null`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = null)

        assertNull(result)
    }

    @Test
    fun `returns null for a ROMPATH-relative path when romDirectory is blank`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "   ")

        assertNull(result)
    }

    @Test
    fun `returns null when no system matches the given shortname`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "ps2", romDirectory = "/roms")

        assertNull(result)
    }

    @Test
    fun `does not match a system name as a substring of a different name`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>snes-msu1</name>
                    <path>%ROMPATH%/snes-msu1</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "snes", romDirectory = "/roms")

        assertNull(result)
    }

    @Test
    fun `finds the matching system among several entries`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>gc</name>
                    <path>%ROMPATH%/gc</path>
                </system>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                </system>
                <system>
                    <name>ps2</name>
                    <path>/storage/emulated/0/Roms/PS2</path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "ps2", romDirectory = "/storage/emulated/0/Roms")

        assertEquals("/storage/emulated/0/Roms/PS2", result)
    }

    @Test
    fun `ignores unrelated child elements like multiple command tags per system`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                    <path>%ROMPATH%/DS</path>
                    <command label="melonDS DS">some command text</command>
                    <command label="melonDS">other command text</command>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "/roms")

        assertEquals("/roms/DS", result)
    }

    @Test
    fun `trims whitespace around the path value`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>ps2</name>
                    <path>
                        /storage/emulated/0/Roms/PS2
                    </path>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "ps2", romDirectory = null)

        assertEquals("/storage/emulated/0/Roms/PS2", result)
    }

    @Test
    fun `returns null instead of throwing on malformed XML`() {
        val xml = "<systemList><system><name>nds</name><path>%ROMPATH%/DS</path>"

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "/roms")

        assertNull(result)
    }

    @Test
    fun `returns null when the matching system has no path element`() {
        val xml =
            """
            <systemList>
                <system>
                    <name>nds</name>
                </system>
            </systemList>
            """.trimIndent()

        val result = EsSystemsXmlParser.findSystemPath(xml, "nds", romDirectory = "/roms")

        assertNull(result)
    }
}
