package org.churchpresenter.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.churchpresenter.sharedui.composables.ActionIconButton
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.cancel
import org.churchpresenter.strings.generated.resources.recording_discard_confirm
import org.churchpresenter.strings.generated.resources.recording_discard_confirm_title
import org.churchpresenter.strings.generated.resources.recording_new
import org.churchpresenter.strings.generated.resources.recording_pause
import org.churchpresenter.strings.generated.resources.recording_resume
import org.churchpresenter.strings.generated.resources.recording_start
import org.churchpresenter.strings.generated.resources.recording_status_idle
import org.churchpresenter.strings.generated.resources.recording_status_paused
import org.churchpresenter.strings.generated.resources.recording_status_recording
import org.churchpresenter.strings.generated.resources.recording_status_stopped
import org.churchpresenter.strings.generated.resources.recording_stop
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The **Recording** tab.
 *
 * At this stage it is the transport control: new, record, pause/resume and stop, over the elapsed
 * clock, with the state held by [RecordingManager] so a take survives leaving the tab. The mixer
 * picker, the level meter and the save-the-take panel (period, speaker, title, folder) still follow.
 */
@Composable
fun RecordingTab(
    modifier: Modifier = Modifier,
    manager: RecordingManager,
) {
    val phase = manager.phase.value
    val elapsed = manager.elapsedMillis.value
    var confirmNew by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = formatRecordingElapsed(elapsed),
            style = MaterialTheme.typography.displayMedium,
            color = if (phase == RecordingPhase.RECORDING) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(statusRes(phase)), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        RecordingTransportBar(
            phase = phase,
            onNew = { if (manager.isActive) confirmNew = true else manager.startNew() },
            onRecord = manager::record,
            onPauseToggle = { if (phase == RecordingPhase.PAUSED) manager.resume() else manager.pause() },
            onStop = manager::stop,
        )
    }

    if (confirmNew) {
        NewRecordingDialog(
            onConfirm = {
                confirmNew = false
                manager.startNew()
            },
            onDismiss = { confirmNew = false },
        )
    }
}

@Composable
private fun RecordingTransportBar(
    phase: RecordingPhase,
    onNew: () -> Unit,
    onRecord: () -> Unit,
    onPauseToggle: () -> Unit,
    onStop: () -> Unit,
) {
    val running = phase == RecordingPhase.RECORDING || phase == RecordingPhase.PAUSED
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        ActionIconButton(
            onClick = onNew,
            tooltipText = stringResource(Res.string.recording_new),
            icon = Icons.Filled.Add,
            buttonSize = 48.dp,
            iconSize = 22.dp,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ActionIconButton(
            onClick = onRecord,
            tooltipText = stringResource(Res.string.recording_start),
            icon = Icons.Filled.FiberManualRecord,
            enabled = !running,
            buttonSize = 48.dp,
            iconSize = 22.dp,
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
        ActionIconButton(
            onClick = onPauseToggle,
            tooltipText = stringResource(
                if (phase == RecordingPhase.PAUSED) Res.string.recording_resume else Res.string.recording_pause
            ),
            icon = if (phase == RecordingPhase.PAUSED) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            enabled = running,
            buttonSize = 48.dp,
            iconSize = 22.dp,
        )
        ActionIconButton(
            onClick = onStop,
            tooltipText = stringResource(Res.string.recording_stop),
            icon = Icons.Filled.Stop,
            enabled = running,
            buttonSize = 48.dp,
            iconSize = 22.dp,
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
        )
    }
}

@Composable
private fun NewRecordingDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.recording_discard_confirm_title)) },
        text = { Text(stringResource(Res.string.recording_discard_confirm)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(Res.string.recording_new)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}

private fun statusRes(phase: RecordingPhase): StringResource = when (phase) {
    RecordingPhase.IDLE -> Res.string.recording_status_idle
    RecordingPhase.RECORDING -> Res.string.recording_status_recording
    RecordingPhase.PAUSED -> Res.string.recording_status_paused
    RecordingPhase.STOPPED -> Res.string.recording_status_stopped
}
