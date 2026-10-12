package org.churchpresenter.hymn

import org.churchpresenter.settings.HymnSettings
import java.io.File
import java.nio.file.Files
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HymnLibraryTest {

    private fun newDir(): File = Files.createTempDirectory("cp-hymn-test").toFile()

    private fun write(parent: File, name: String) {
        Files.writeString(File(parent, name).toPath(), "x")
    }

    @Test
    fun readsTheHymnFoldersFromTheSystemSettings() {
        val settings = readHymnSettingsFromSystemConfig()
        assumeTrue(settings.hymnTitleDirectory.isNotBlank())
        assumeTrue(settings.pptDirectory.isNotBlank())
        val hymns = HymnLibrary.scan(settings)
        val titles = HymnLibrary.readTitleMap(settings.hymnTitleDirectory)
        // Every slide file becomes a hymn named after it, its title whatever
        // HymnTitle.txt says for that number, and a track only belongs to it
        // when the track's name is that number.
        assertEquals(HymnLibrary.pptFiles(settings.pptDirectory).size, hymns.size)
        hymns.forEach { hymn ->
            assertEquals(hymn.pptFile?.nameWithoutExtension, hymn.number)
            assertEquals(titles[hymn.number] ?: "", hymn.title)
            hymn.pianoFile?.let { assertTrue(it.belongsTo(hymn.number), "${it.name} is not ${hymn.number}'s piano") }
            hymn.vocalFile?.let { assertTrue(it.belongsTo(hymn.number), "${it.name} is not ${hymn.number}'s vocal") }
        }
    }

    @Test
    fun mp3MatchesTheLeadingDigitsWhenTheNumberCarriesText() {
        val piano = newDir()
        write(piano, "051.mp3")
        assertEquals("051.mp3", HymnLibrary.matchAudio(piano.path, "051(甲)")?.name)
        assertNull(HymnLibrary.matchAudio(piano.path, "052"))
    }

    @Test
    fun anExactNumberFileWinsOverItsPlainDigitSibling() {
        val piano = newDir()
        write(piano, "051(甲).mp3")
        write(piano, "051.mp3")
        assertEquals("051(甲).mp3", HymnLibrary.matchAudio(piano.path, "051(甲)")?.name)
    }

    @Test
    fun aTrackNamedNumberSpaceTitleMatchesByItsNumber() {
        val piano = newDir()
        write(piano, "001 Track.mp3")
        write(piano, "132(甲) Track.mp3")
        // The token before the first space is the number; the title after it is ignored.
        assertEquals("001 Track.mp3", HymnLibrary.matchAudio(piano.path, "001")?.name)
        assertEquals("132(甲) Track.mp3", HymnLibrary.matchAudio(piano.path, "132(甲)")?.name)
        assertNull(HymnLibrary.matchAudio(piano.path, "132(乙)"))
        assertNull(HymnLibrary.matchAudio(piano.path, "002"))
    }

    @Test
    fun scanMatchesTracksThatCarryTheirTitleAfterTheNumber() {
        val ppt = newDir()
        write(ppt, "051(甲).pptx")
        val piano = newDir()
        write(piano, "051(甲) Piano.mp3")
        val vocal = newDir()
        write(vocal, "051 Vocal.mp3")

        val hymn = HymnLibrary.scan(HymnSettings(ppt.path, piano.path, vocal.path, "")).single()
        // The piano's exact token matches; the vocal falls back to the number's leading digits.
        assertEquals("051(甲) Piano.mp3", hymn.pianoFile?.name)
        assertEquals("051 Vocal.mp3", hymn.vocalFile?.name)
    }

    @Test
    fun audioExtensionIsMatchedCaseInsensitively() {
        val vocal = newDir()
        write(vocal, "129.MP3")
        assertEquals("129.MP3", HymnLibrary.matchAudio(vocal.path, "129")?.name)
    }

    @Test
    fun numberTextWithNoDigitPrefixNeedsAnExactFile() {
        val piano = newDir()
        write(piano, "445A.mp3")
        assertNull(HymnLibrary.matchAudio(piano.path, "445(甲)"))
        assertNull(HymnLibrary.matchAudio(piano.path, "445"))
    }

    @Test
    fun blankOrMissingFoldersYieldAnEmptyLibrary() {
        assertTrue(HymnLibrary.scan(HymnSettings()).isEmpty())
        val empty = newDir()
        assertTrue(HymnLibrary.scan(HymnSettings(empty.path, empty.path, empty.path, empty.path)).isEmpty())
    }

    @Test
    fun aMissingOrUnreadableTitleFileLeavesTitlesBlank() {
        val ppt = newDir()
        write(ppt, "001.pptx")
        val noTitleDir = newDir()

        val blank = HymnLibrary.scan(HymnSettings(ppt.path, "", "", noTitleDir.path)).single()
        assertEquals("", blank.title)
        assertEquals("", HymnLibrary.scan(HymnSettings(ppt.path, "", "", "")).single().title)
    }

    @Test
    fun numberKeyReadsTheLeadingDigits() {
        assertEquals(51, HymnLibrary.numberKey("051(甲)"))
        assertEquals(51, HymnLibrary.numberKey("51"))
        assertEquals(Int.MAX_VALUE, HymnLibrary.numberKey("not-a-number"))
        assertEquals(2, HymnLibrary.numberKey("002"))
    }

    @Test
    fun onlyPptAndPptxFilesAreSlides() {
        val ppt = newDir()
        write(ppt, "001.pptx")
        write(ppt, "002.ppt")
        write(ppt, "003.pdf")
        write(ppt, "readme.txt")
        assertEquals(listOf("001", "002"), HymnLibrary.scan(HymnSettings(ppt.path, "", "", "")).map { it.number })
    }

    /** Whether [file]'s name is [number] itself, or — for a number carrying text — its leading digits. */
    private fun File.belongsTo(number: String): Boolean {
        val token = nameWithoutExtension.substringBefore(' ')
        return token == number || (number.any { !it.isDigit() } && token == number.takeWhile { it.isDigit() })
    }
}
