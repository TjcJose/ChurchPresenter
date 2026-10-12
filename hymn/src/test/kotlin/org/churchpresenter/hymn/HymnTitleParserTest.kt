package org.churchpresenter.hymn

import java.io.File
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HymnTitleParserTest {

    @Test
    fun readsTheTitleFileFromTheSystemSettings() {
        val settings = readHymnSettingsFromSystemConfig()
        assumeTrue(settings.hymnTitleDirectory.isNotBlank())
        val file = File(settings.hymnTitleDirectory, HymnLibrary.TITLE_FILE_NAME)
        assumeTrue(file.isFile)
        val text = file.readText()
        // Every entry the file carries becomes a number -> title pair, split on
        // the first run of whitespace, the later entry winning when a number
        // repeats.
        val expected = HymnTitleParser.quotedEntries(text)
            .ifEmpty { HymnTitleParser.lineEntries(text) }
            .mapNotNull { entry -> entry.splitAtFirstSpace() }
            .toMap(LinkedHashMap())
        assertEquals(expected, HymnTitleParser.parse(text))
    }

    @Test
    fun splitsOnFirstWhitespaceOnlySoTitlesKeepTheirSpaces() {
        val titles = HymnTitleParser.parse("'044 A Phrase, With Commas And Spaces'")
        assertEquals("A Phrase, With Commas And Spaces", titles["044"])
    }

    @Test
    fun laterEntryWinsForARepeatedNumber() {
        val titles = HymnTitleParser.parse("'001 First Entry'\n'001 Second Entry'")
        assertEquals("Second Entry", titles["001"])
    }

    @Test
    fun entriesWithoutANumberOrTitleAreDropped() {
        val titles = HymnTitleParser.parse("'BarewordNoSpace'\n'443'\n'   '\n'001 Kept'")
        assertEquals(mapOf("001" to "Kept"), titles)
    }

    @Test
    fun emptyTextIsAnEmptyMap() {
        assertTrue(HymnTitleParser.parse("").isEmpty())
        assertTrue(HymnTitleParser.parse(";;;[[[ \n ]]]").isEmpty())
    }

    /** `number title` split on the first whitespace run, or null when either half is missing. */
    private fun String.splitAtFirstSpace(): Pair<String, String>? {
        val split = indexOfFirst { it.isWhitespace() }
        if (split <= 0) return null
        val number = substring(0, split).trim()
        val title = substring(split + 1).trim()
        return if (number.isEmpty() || title.isEmpty()) null else number to title
    }
}
