package org.churchpresenter.hymn

import org.churchpresenter.settings.HymnSettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HymnViewModelTest {

    private class FakePlayer : HymnMediaPlayer {
        val played = mutableListOf<String>()
        var stopped = 0
        var released = 0
        override fun play(file: File) {
            played += file.name
        }

        override fun pause() = Unit

        override fun resume() = Unit

        override fun stop() {
            stopped++
        }

        override fun setTime(millis: Long) = Unit

        override fun time(): Long = 0L

        override fun length(): Long = 0L

        override fun isPlaying(): Boolean = false

        override fun setVolume(percent: Int) = Unit

        override fun setMute(mute: Boolean) = Unit

        override fun setAudioOutputDevice(id: String) = Unit

        override fun setOnFinished(listener: () -> Unit) = Unit

        override fun release() {
            released++
        }
    }

    private fun hymn(number: String, title: String = "", piano: String? = null, vocal: String? = null) =
        HymnItem(number, title, File(number), piano?.let { File(it) }, vocal?.let { File(it) })

    @Test
    fun configureScansAndRescansOnlyOnChange() {
        var scans = 0
        val viewModel = HymnViewModel(library = { scans++; listOf(hymn("001", "Alpha")) })

        viewModel.configure(HymnSettings(imagesDirectory = "a"))
        assertEquals(1, scans)
        viewModel.configure(HymnSettings(imagesDirectory = "a"))
        assertEquals(1, scans)
        viewModel.configure(HymnSettings(imagesDirectory = "b"))
        assertEquals(2, scans)
    }

    @Test
    fun searchFiltersByNumberOrTitleCaseInsensitively() {
        val hymns = listOf(hymn("001", "Alpha"), hymn("129", "Bravo"), hymn("051(甲)", "Charlie"))
        val viewModel = HymnViewModel(library = { hymns })
        viewModel.configure(HymnSettings(imagesDirectory = "x"))
        assertEquals(3, viewModel.hymns.size)

        viewModel.searchQuery = "12"
        assertEquals(listOf("129"), viewModel.hymns.map { it.number })
        viewModel.searchQuery = "bravo"
        assertEquals(listOf("129"), viewModel.hymns.map { it.number })
        viewModel.searchQuery = "char"
        assertEquals(listOf("051(甲)"), viewModel.hymns.map { it.number })
        viewModel.searchQuery = "   "
        assertEquals(3, viewModel.hymns.size)
    }

    @Test
    fun selectionStaysThroughASearch() {
        val hymns = listOf(hymn("001"), hymn("129"))
        val viewModel = HymnViewModel(library = { hymns })
        viewModel.configure(HymnSettings(imagesDirectory = "x"))

        viewModel.select(hymns[1])
        viewModel.searchQuery = "001"
        assertEquals("129", viewModel.selectedHymn?.number)
    }

    @Test
    fun selectionDropsWhenTheHymnDisappearsOnRescan() {
        val hymns = mutableListOf(hymn("001"))
        val viewModel = HymnViewModel(library = { hymns.toList() })
        viewModel.configure(HymnSettings(imagesDirectory = "x"))
        viewModel.select(hymns[0])

        hymns.clear()
        viewModel.rescan()
        assertNull(viewModel.selectedHymn)
    }

    @Test
    fun hasFolderReflectsTheImagesDirectory() {
        val viewModel = HymnViewModel(library = { emptyList() })
        assertFalse(viewModel.hasFolder)
        viewModel.configure(HymnSettings(imagesDirectory = "x"))
        assertTrue(viewModel.hasFolder)
    }

    @Test
    fun playingTracksMovesTheAudioStateAndStops() {
        val fake = FakePlayer()
        val viewModel = HymnViewModel(
            audio = HymnAudioPlayer { fake },
            library = { listOf(hymn("001", piano = "001 p.mp3", vocal = "001 v.mp3")) },
        )
        viewModel.configure(HymnSettings(imagesDirectory = "x"))
        val only = viewModel.hymns.single()

        viewModel.playPiano(only)
        assertEquals(HymnAudioKind.PIANO, viewModel.audioState?.kind)
        viewModel.playVocal(only)
        assertEquals(HymnAudioKind.VOCAL, viewModel.audioState?.kind)
        viewModel.stopAudio()
        assertNull(viewModel.audioState)
        assertEquals(listOf("001 p.mp3", "001 v.mp3"), fake.played)
    }

    @Test
    fun disposeReleasesTheAudioPlayer() {
        val fake = FakePlayer()
        val viewModel = HymnViewModel(
            audio = HymnAudioPlayer { fake },
            library = { listOf(hymn("001", piano = "p.mp3")) },
        )
        viewModel.configure(HymnSettings(imagesDirectory = "x"))
        viewModel.playPiano(viewModel.hymns.single())

        viewModel.dispose()
        assertEquals(1, fake.released)
    }
}
