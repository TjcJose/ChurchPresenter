package org.churchpresenter.recording

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Where the recorder is in its cycle.
 *
 * [IDLE] is a fresh tab and [STOPPED] is a take that has ended and can be saved; the two differ
 * because [IDLE] has nothing to save.
 */
enum class RecordingPhase { IDLE, RECORDING, PAUSED, STOPPED }

/**
 * The Recording tab's state, held for the whole run rather than by the tab.
 *
 * Recording must survive the operator switching to another tab, so the state lives here, beside the
 * app, and the tab is only a view over it — the same reason `STTManager` and `QAManager` outlive
 * their tabs. The engine that actually captures the mixer will drive the same states.
 */
class RecordingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _phase = mutableStateOf(RecordingPhase.IDLE)
    val phase: State<RecordingPhase> = _phase

    private val _elapsedMillis = mutableStateOf(0L)
    val elapsedMillis: State<Long> = _elapsedMillis

    private var ticker: Job? = null

    /** Whether there is a finished take waiting to be saved. */
    val hasTakeToSave: Boolean get() = _phase.value == RecordingPhase.STOPPED

    /** Whether the recorder is running or held by [pause]. */
    val isActive: Boolean get() = _phase.value == RecordingPhase.RECORDING || _phase.value == RecordingPhase.PAUSED

    /** Clears the tab back to [RecordingPhase.IDLE], ready to record the next take. */
    fun startNew() {
        stopTicker()
        _elapsedMillis.value = 0L
        _phase.value = RecordingPhase.IDLE
    }

    /** Begins (or restarts) a take. A take that already ended starts over from zero. */
    fun record() {
        if (_phase.value != RecordingPhase.RECORDING && _phase.value != RecordingPhase.PAUSED) {
            _elapsedMillis.value = 0L
            _phase.value = RecordingPhase.RECORDING
            startTicker()
        }
    }

    /** Holds the take where it is; the elapsed time stops advancing. */
    fun pause() {
        if (_phase.value == RecordingPhase.RECORDING) {
            _phase.value = RecordingPhase.PAUSED
            stopTicker()
        }
    }

    /** Carries on from where [pause] left off. */
    fun resume() {
        if (_phase.value == RecordingPhase.PAUSED) {
            _phase.value = RecordingPhase.RECORDING
            startTicker()
        }
    }

    /** Ends the take, leaving it ready to save. */
    fun stop() {
        if (isActive) {
            stopTicker()
            _phase.value = RecordingPhase.STOPPED
        }
    }

    private fun startTicker() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (true) {
                delay(TICK_MILLIS)
                _elapsedMillis.value += TICK_MILLIS
            }
        }
    }

    private fun stopTicker() {
        ticker?.cancel()
        ticker = null
    }

    private companion object {
        /** How often the elapsed clock advances. Fine enough for a second-reading display. */
        const val TICK_MILLIS = 200L
    }
}
