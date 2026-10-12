package org.churchpresenter.hymn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.churchpresenter.icons.generated.resources.Res as IconRes
import org.churchpresenter.icons.generated.resources.ic_microphone
import org.churchpresenter.icons.generated.resources.ic_music_note
import org.churchpresenter.icons.generated.resources.ic_pause
import org.churchpresenter.icons.generated.resources.ic_play
import org.churchpresenter.icons.generated.resources.ic_stop
import org.churchpresenter.icons.generated.resources.ic_volume_off
import org.churchpresenter.icons.generated.resources.ic_volume_up
import org.churchpresenter.sharedui.composables.SlimSlider
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.hymn_piano
import org.churchpresenter.strings.generated.resources.hymn_vocal
import org.churchpresenter.strings.generated.resources.media_mute
import org.churchpresenter.strings.generated.resources.media_unmute
import org.churchpresenter.strings.generated.resources.pause
import org.churchpresenter.strings.generated.resources.play
import org.churchpresenter.strings.generated.resources.stop
import org.churchpresenter.theme.components.KeyIconButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val VOLUME_SLIDER_WIDTH = 140.dp

/**
 * The player bar that sits above the slides while a hymn's piano or vocal track is loaded: what is
 * playing, the Play/Pause/Stop transport, a mute toggle and volume, and the seek bar — the Media
 * tab's basics in the Hymn tab's own shape.
 */
@Composable
internal fun HymnPlayerBar(viewModel: HymnViewModel, modifier: Modifier = Modifier) {
    val current = viewModel.audioState ?: return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            HymnTransportRow(viewModel, current.kind)
            HymnSeekRow(viewModel)
        }
        HorizontalDivider()
    }
}

/** What is playing, and the transport: Play, Pause, Stop, mute and volume. */
@Composable
private fun HymnTransportRow(viewModel: HymnViewModel, kind: HymnAudioKind) {
    val kindLabel = stringResource(if (kind == HymnAudioKind.PIANO) Res.string.hymn_piano else Res.string.hymn_vocal)
    val hymn = viewModel.audioHymn
    val heading = listOfNotNull(kindLabel, hymn?.number, hymn?.title?.takeIf { it.isNotBlank() })
        .joinToString(" · ")
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(
                if (kind == HymnAudioKind.PIANO) IconRes.drawable.ic_music_note else IconRes.drawable.ic_microphone
            ),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = heading,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        KeyIconButton(onClick = { viewModel.playAudio() }, enabled = !viewModel.audioPlaying) {
            Icon(painterResource(IconRes.drawable.ic_play), contentDescription = stringResource(Res.string.play))
        }
        KeyIconButton(onClick = { viewModel.pauseAudio() }, enabled = viewModel.audioPlaying) {
            Icon(painterResource(IconRes.drawable.ic_pause), contentDescription = stringResource(Res.string.pause))
        }
        KeyIconButton(onClick = { viewModel.stopAudio() }) {
            Icon(painterResource(IconRes.drawable.ic_stop), contentDescription = stringResource(Res.string.stop))
        }
        KeyIconButton(onClick = { viewModel.toggleAudioMute() }) {
            Icon(
                painter = painterResource(
                    if (viewModel.audioMuted) IconRes.drawable.ic_volume_off else IconRes.drawable.ic_volume_up
                ),
                contentDescription = stringResource(
                    if (viewModel.audioMuted) Res.string.media_unmute else Res.string.media_mute
                ),
            )
        }
        HymnVolumeSlider(viewModel)
    }
}

/** The mute-aware volume slider and its percentage label. */
@Composable
private fun HymnVolumeSlider(viewModel: HymnViewModel) {
    val shown = if (viewModel.audioMuted) 0f else viewModel.audioVolume
    SlimSlider(
        value = shown,
        onValueChange = { viewModel.setVolume(it) },
        valueRange = 0f..1f,
        modifier = Modifier.width(VOLUME_SLIDER_WIDTH),
        trailingLabel = "${(shown * 100).roundToInt()}%",
    )
}

/** Elapsed time, the seek bar, and the track's length. */
@Composable
private fun HymnSeekRow(viewModel: HymnViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = formatHymnTime(viewModel.audioPosition),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SlimSlider(
            value = viewModel.audioPosition.toFloat(),
            onValueChange = { viewModel.seekAudioTo(it.toLong()) },
            valueRange = 0f..viewModel.audioDuration.coerceAtLeast(1L).toFloat(),
            enabled = viewModel.audioDuration > 0,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatHymnTime(viewModel.audioDuration),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Milliseconds as m:ss, clamped at zero. */
internal fun formatHymnTime(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60L
