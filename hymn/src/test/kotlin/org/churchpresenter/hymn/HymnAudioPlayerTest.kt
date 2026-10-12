package org.churchpresenter.hymn

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HymnAudioPlayerTest {

    private class FakePlayer : HymnMediaPlayer {
        val played = mutableListOf<String>()
        var stopped = 0
        var released = 0
        var pauses = 0
        var resumes = 0
        val seeks = mutableListOf<Long>()
        val volumes = mutableListOf<Int>()
        val mutes = mutableListOf<Boolean>()
        var device: String? = null
        var finished: (() -> Unit)? = null
        override fun play(file: File) {
            played += file.name
        }

        override fun pause() {
            pauses++
        }

        override fun resume() {
            resumes++
        }

        override fun stop() {
            stopped++
        }

        override fun setTime(millis: Long) {
            seeks += millis
        }

        override fun time(): Long = 0L

        override fun length(): Long = 0L

        override fun isPlaying(): Boolean = false

        override fun setVolume(percent: Int) {
            volumes += percent
        }

        override fun setMute(mute: Boolean) {
            mutes += mute
        }

        override fun setAudioOutputDevice(id: String) {
            device = id
        }

        override fun setOnFinished(listener: () -> Unit) {
            finished = listener
        }

        override fun release() {
            released++
        }
    }

    private fun pianoHymn(fileName: String = "051.mp3") = HymnItem(
        number = "051(甲)",
        title = "Title A",
        pptFile = File("051(甲).pptx"),
        pianoFile = File(fileName),
        vocalFile = File("051 vocal.mp3"),
    )

    @Test
    fun startingATrackLoadsItAtTheAskedVolumeAndDevice() {
        val fake = FakePlayer()
        val audio = HymnAudioPlayer { fake }

        val started = audio.start(HymnAudioKind.PIANO, pianoHymn(), volume = 0.5f, audioDeviceId = "dev")

        assertEquals(HymnAudioPlayer.Current(HymnAudioKind.PIANO, "051(甲)", File("051.mp3")), started)
        assertEquals(listOf("051.mp3"), fake.played)
        assertEquals(listOf(50), fake.volumes)
        assertEquals(listOf(false), fake.mutes)
        assertEquals("dev", fake.device)
    }

    @Test
    fun switchingTracksReusesOnePlayer() {
        val fake = FakePlayer()
        var opens = 0
        val audio = HymnAudioPlayer { opens++; fake }
        val hymn = pianoHymn()

        audio.start(HymnAudioKind.PIANO, hymn, 1f, "")
        val switched = audio.start(HymnAudioKind.VOCAL, hymn, 1f, "")

        assertEquals(HymnAudioPlayer.Current(HymnAudioKind.VOCAL, "051(甲)", File("051 vocal.mp3")), switched)
        assertEquals(listOf("051.mp3", "051 vocal.mp3"), fake.played)
        assertEquals(1, opens)
    }

    @Test
    fun aTrackWithNoFilePlaysNothing() {
        val fake = FakePlayer()
        val audio = HymnAudioPlayer(openPlayer = { fake })
        val hymn = HymnItem("129", "Title", File("129.pptx"), pianoFile = null, vocalFile = null)

        assertNull(audio.start(HymnAudioKind.PIANO, hymn, 1f, ""))
        assertNull(audio.start(HymnAudioKind.VOCAL, hymn, 1f, ""))
        assertTrue(fake.played.isEmpty())
    }

    @Test
    fun anUnopenableVlcMakesNothingPlayButKeepsThePressSilent() {
        val audio = HymnAudioPlayer(openPlayer = { null })

        assertNull(audio.start(HymnAudioKind.PIANO, pianoHymn(), 1f, ""))
    }

    @Test
    fun theTransportReachesThePlayer() {
        val fake = FakePlayer()
        val audio = HymnAudioPlayer { fake }
        audio.start(HymnAudioKind.PIANO, pianoHymn(), 1f, "")

        audio.pause()
        audio.resume()
        audio.seekTo(1234)
        audio.setVolume(0.25f)
        audio.setMute(true)

        assertEquals(1, fake.pauses)
        assertEquals(1, fake.resumes)
        assertEquals(listOf(1234L), fake.seeks)
        assertEquals(listOf(100, 25), fake.volumes)
        assertEquals(listOf(false, true), fake.mutes)
    }

    @Test
    fun stopClearsWhatIsLoaded() {
        val fake = FakePlayer()
        val audio = HymnAudioPlayer { fake }
        audio.start(HymnAudioKind.PIANO, pianoHymn(), 1f, "")

        audio.stop()

        assertEquals(1, fake.stopped)
        assertNull(audio.current)
    }

    @Test
    fun disposeReleasesThePlayerAndForgetsIt() {
        val fake = FakePlayer()
        val audio = HymnAudioPlayer { fake }
        audio.start(HymnAudioKind.PIANO, pianoHymn(), 1f, "")

        audio.dispose()

        assertEquals(1, fake.released)
        assertNull(audio.current)
    }
}
