package org.churchpresenter.hymn

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.churchpresenter.settings.HymnSettings
import org.churchpresenter.settings.utils.Constants

/** The volume the bar starts at, matching the Media tab. */
private const val DEFAULT_AUDIO_VOLUME = 1f

/**
 * The Hymn tab's state: the scanned library, the search over number and title, the selected hymn,
 * and the audio transport the player bar drives.
 *
 * The scan is a function parameter so a test feeds it a fabricated library without touching the
 * disk ([HymnLibrary.scan] is the production one); the audio player is injected the same way.
 */
@Suppress("TooManyFunctions") // The tab's one state holder: library, search, selection and the audio transport.
class HymnViewModel(
    private val audio: HymnAudioPlayer = HymnAudioPlayer(),
    private val library: (HymnSettings) -> List<HymnItem> = HymnLibrary::scan,
) {
    private val all = mutableStateListOf<HymnItem>()

    // Compose state, not a plain field: the pane's first branch tests `hasFolder` alone, so a change
    // here has to invalidate the composition even before anything has been scanned into `all`.
    private var settings by mutableStateOf(HymnSettings())

    var searchQuery by mutableStateOf("")

    /** How [searchQuery] is matched against a hymn: one of [Constants]' CONTAINS / STARTS_WITH / EXACT_MATCH. */
    var filterType: String by mutableStateOf(Constants.CONTAINS)

    var selectedNumber: String? by mutableStateOf(null)

    /** The folder a Schedule row asked for, kept until the library has scanned it in. */
    private var pendingFolderPath: String? = null

    /** The track loaded on the player, or null — read by the row buttons and the player bar. */
    var audioState by mutableStateOf<HymnAudioPlayer.Current?>(null)
        private set

    /** Whether the loaded track is sounding right now (as opposed to paused). */
    var audioPlaying by mutableStateOf(false)
        private set

    /** Where the loaded track is, in milliseconds. */
    var audioPosition by mutableStateOf(0L)
        private set

    /** How long the loaded track runs, in milliseconds; 0 until the player reports it. */
    var audioDuration by mutableStateOf(0L)
        private set

    var audioVolume by mutableStateOf(DEFAULT_AUDIO_VOLUME)
        private set

    var audioMuted by mutableStateOf(false)
        private set

    /** The output device the player opens audio on; blank is the system default. */
    private var audioDeviceId: String = ""

    init {
        audio.onFinished = { onAudioFinished() }
    }

    /** The hymns matching [searchQuery] under [filterType] — over number and title, case-insensitively. */
    val hymns: List<HymnItem>
        get() {
            val query = searchQuery.trim()
            if (query.isEmpty()) return all
            return all.filter { hymn -> hymn.matches(query, filterType) }
        }

    /** The selected hymn, from the full library so searching never hides the selection. */
    val selectedHymn: HymnItem? get() = all.firstOrNull { it.number == selectedNumber }

    /** The hymn whose track is loaded — what the player bar titles itself with. */
    val audioHymn: HymnItem? get() = all.firstOrNull { it.number == audioState?.hymnNumber }

    /** Whether the images folder is set at all — the tab's "set the folders" empty state. */
    val hasFolder: Boolean get() = settings.imagesDirectory.isNotBlank()

    fun configure(newSettings: HymnSettings) {
        if (newSettings == settings) return
        settings = newSettings
        rescan()
    }

    fun rescan() {
        val keep = selectedNumber
        all.clear()
        all.addAll(library(settings))
        selectedNumber = keep?.takeIf { number -> all.any { it.number == number } }
        resolvePendingSelection()
    }

    fun select(hymn: HymnItem) {
        selectedNumber = hymn.number
    }

    /**
     * Selects the hymn whose image folder is [path] — the Schedule's way in, from a row the Hymn
     * tab put there. Held until the library knows it, so it lands whether or not the scan has run
     * yet.
     */
    fun selectByFolderPath(path: String) {
        pendingFolderPath = path
        resolvePendingSelection()
    }

    private fun resolvePendingSelection() {
        val path = pendingFolderPath ?: return
        val match = all.firstOrNull { it.imageFolder?.absolutePath == path } ?: return
        selectedNumber = match.number
        pendingFolderPath = null
    }

    /** The folder VLC opens output on; the bar's presses reach it on the next start. */
    fun setAudioOutputDevice(id: String) {
        audioDeviceId = id
    }

    /** The Piano button: selects the hymn and plays its piano track, or pauses/resumes it when it is already on. */
    fun playPiano(hymn: HymnItem) = play(HymnAudioKind.PIANO, hymn)

    /** The Vocal button: selects the hymn and plays its vocal track, or pauses/resumes it when it is already on. */
    fun playVocal(hymn: HymnItem) = play(HymnAudioKind.VOCAL, hymn)

    private fun play(kind: HymnAudioKind, hymn: HymnItem) {
        select(hymn)
        val active = audioState
        if (active != null && active.kind == kind && active.hymnNumber == hymn.number) {
            toggleAudioPlayPause()
            return
        }
        val started = audio.start(kind, hymn, audioVolume, audioDeviceId)
        audioState = started
        audioPlaying = started != null
        audioPosition = 0
        audioDuration = 0
    }

    /** Pauses the loaded track, or resumes it — the row buttons' toggle. */
    fun toggleAudioPlayPause() {
        if (audioState == null) return
        if (audioPlaying) pauseAudio() else playAudio()
    }

    fun playAudio() {
        if (audioState == null || audioPlaying) return
        audio.resume()
        audioPlaying = true
    }

    fun pauseAudio() {
        if (!audioPlaying) return
        audio.pause()
        audioPlaying = false
    }

    fun stopAudio() {
        audio.stop()
        audioState = null
        audioPlaying = false
        audioPosition = 0
        audioDuration = 0
    }

    fun seekAudioTo(millis: Long) {
        if (audioState == null) return
        val end = audioDuration.takeIf { it > 0 } ?: millis
        val clamped = millis.coerceIn(0L, end)
        audio.seekTo(clamped)
        audioPosition = clamped
    }

    fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        audioVolume = v
        if (v > 0f && audioMuted) audioMuted = false
        audio.setMute(audioMuted)
        audio.setVolume(v)
    }

    fun toggleAudioMute() {
        audioMuted = !audioMuted
        audio.setMute(audioMuted)
    }

    /** One poll, from the tab: keeps the bar's position, length and playing flag in step with the player. */
    fun syncAudio() {
        if (audioState == null) return
        audioPosition = audio.position()
        val length = audio.duration()
        if (length > 0) audioDuration = length
        audioPlaying = audio.isPlaying()
    }

    // VLC raises this on one of its own threads; the state write is posted to the event thread.
    private fun onAudioFinished() {
        javax.swing.SwingUtilities.invokeLater {
            audioState = null
            audioPlaying = false
            audioPosition = 0
            audioDuration = 0
        }
    }

    fun dispose() {
        audio.onFinished = null
        audio.dispose()
    }
}

/** Whether [query] matches this hymn under [filterType], over its number and title. */
private fun HymnItem.matches(query: String, filterType: String): Boolean {
    val fields = listOf(number, title)
    return when (filterType) {
        Constants.STARTS_WITH -> fields.any { it.startsWith(query, ignoreCase = true) }
        Constants.EXACT_MATCH -> fields.any { it.equals(query, ignoreCase = true) }
        else -> fields.any { it.contains(query, ignoreCase = true) }
    }
}
