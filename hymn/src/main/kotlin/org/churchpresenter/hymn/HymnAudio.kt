package org.churchpresenter.hymn

import org.churchpresenter.diagnostics.CrashReporter
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import java.io.File

/** Which of the two backing tracks a hymn is playing. */
enum class HymnAudioKind { PIANO, VOCAL }

/** The mp3 backing [kind] of this hymn, or null when it has none. */
internal fun HymnItem.audioFileFor(kind: HymnAudioKind): File? = when (kind) {
    HymnAudioKind.PIANO -> pianoFile
    HymnAudioKind.VOCAL -> vocalFile
}

/**
 * An opened VLC media player playing at most one hymn mp3, with the transport the tab's player bar
 * drives.
 *
 * This is the small surface the tab needs; the vlcj types never leave [openHymnMediaPlayer], and no
 * test ever touches libvlc.
 */
@Suppress("TooManyFunctions") // One function per player call the bar drives; the count is the transport's.
interface HymnMediaPlayer {
    fun play(file: File)
    fun pause()
    fun resume()
    fun stop()
    fun setTime(millis: Long)
    fun time(): Long
    fun length(): Long
    fun isPlaying(): Boolean
    fun setVolume(percent: Int)
    fun setMute(mute: Boolean)
    fun setAudioOutputDevice(id: String)
    fun setOnFinished(listener: () -> Unit)
    fun release()
}

/**
 * Opens the headless vlcj player the hymn mp3s play through, or null when VLC cannot be started.
 *
 * `:media` keeps its own availability checks and players; this module plays audio only, so it has
 * its own one-step seam instead of reaching into `:media` — [HymnAudioPlayer] takes the factory,
 * and the tab's viewmodel is handed the player, so no test ever touches libvlc.
 */
internal fun openHymnMediaPlayer(): HymnMediaPlayer? {
    val factory = try {
        MediaPlayerFactory("--no-video", "--quiet")
    } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
        CrashReporter.reportException(t, "Hymn: VLC MediaPlayerFactory init failed")
        return null
    }
    val mediaPlayer: MediaPlayer = try {
        factory.mediaPlayers().newMediaPlayer()
    } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
        CrashReporter.reportException(t, "Hymn: VLC media player init failed")
        factory.release()
        return null
    }
    return object : HymnMediaPlayer {
        override fun play(file: File) {
            mediaPlayer.media().play(file.absolutePath)
        }

        override fun pause() {
            mediaPlayer.controls().setPause(true)
        }

        override fun resume() {
            mediaPlayer.controls().setPause(false)
        }

        override fun stop() {
            mediaPlayer.controls().stop()
        }

        override fun setTime(millis: Long) {
            mediaPlayer.controls().setTime(millis)
        }

        override fun time(): Long = mediaPlayer.status().time().coerceAtLeast(0L)

        override fun length(): Long = mediaPlayer.status().length().coerceAtLeast(0L)

        override fun isPlaying(): Boolean = mediaPlayer.status().isPlaying

        override fun setVolume(percent: Int) {
            mediaPlayer.audio().setVolume(percent)
        }

        override fun setMute(mute: Boolean) {
            mediaPlayer.audio().setMute(mute)
        }

        override fun setAudioOutputDevice(id: String) {
            // A blank id is the system default, which VLC already uses — leave it alone.
            if (id.isNotBlank()) mediaPlayer.audio().setOutputDevice(null, id)
        }

        override fun setOnFinished(listener: () -> Unit) {
            mediaPlayer.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
                override fun finished(mediaPlayer: MediaPlayer) = listener()
            })
        }

        override fun release() {
            mediaPlayer.release()
            factory.release()
        }
    }
}

/**
 * The one hymn-audio player the tab owns: piano and vocal share it, so starting either ends the
 * other, and it exposes the transport (pause/resume/seek/volume) the tab's player bar drives.
 *
 * [Current] is what is loaded — the tab's viewmodel exposes it as state; [onFinished] fires when a
 * track reaches its end so the tab can clear what it shows.
 */
@Suppress("TooManyFunctions") // The transport's own verbs: start, pause, resume, stop, seek, volume, position.
class HymnAudioPlayer(
    private val openPlayer: () -> HymnMediaPlayer? = ::openHymnMediaPlayer,
) {
    /** What is on the player right now, or null when nothing is. */
    data class Current(val kind: HymnAudioKind, val hymnNumber: String, val file: File)

    private var player: HymnMediaPlayer? = null

    var current: Current? = null
        private set

    /** Called when the loaded track finishes; set by the tab's viewmodel. */
    var onFinished: (() -> Unit)? = null

    private fun opened(): HymnMediaPlayer? =
        player ?: openPlayer()?.also { opened ->
            player = opened
            opened.setOnFinished { onFinished?.invoke() }
        }

    /**
     * Starts [kind] of [hymn] from the top, at [volume] (0..1) on [audioDeviceId] (blank = system
     * default). Returns the new [Current], or null when the hymn has no such file or VLC is missing.
     */
    fun start(kind: HymnAudioKind, hymn: HymnItem, volume: Float, audioDeviceId: String): Current? {
        val file = hymn.audioFileFor(kind) ?: return null
        val opened = opened() ?: return null
        opened.setAudioOutputDevice(audioDeviceId)
        opened.setVolume((volume.coerceIn(0f, 1f) * VOLUME_PERCENT_SCALE).toInt())
        opened.setMute(false)
        opened.play(file)
        val started = Current(kind, hymn.number, file)
        current = started
        return started
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.resume()
    }

    fun seekTo(millis: Long) {
        player?.setTime(millis)
    }

    /** Sets the volume, 0..1; a positive value unmutes. */
    fun setVolume(volume: Float) {
        player?.setVolume((volume.coerceIn(0f, 1f) * VOLUME_PERCENT_SCALE).toInt())
    }

    fun setMute(mute: Boolean) {
        player?.setMute(mute)
    }

    /** The player's position in milliseconds, 0 when nothing is loaded. */
    fun position(): Long = player?.time() ?: 0L

    /** The loaded track's length in milliseconds, 0 until VLC reports it. */
    fun duration(): Long = player?.length() ?: 0L

    /** Whether sound is coming out right now. */
    fun isPlaying(): Boolean = player?.isPlaying() == true

    fun stop() {
        player?.stop()
        current = null
    }

    fun dispose() {
        onFinished = null
        player?.release()
        player = null
        current = null
    }

    private companion object {
        const val VOLUME_PERCENT_SCALE = 100
    }
}
