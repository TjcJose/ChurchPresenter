package org.churchpresenter.appsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.churchpresenter.profiles.FileManager
import org.churchpresenter.profiles.SettingsCard
import org.churchpresenter.settings.AppSettings
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.browse_directory
import org.churchpresenter.strings.generated.resources.hymn_piano_directory
import org.churchpresenter.strings.generated.resources.hymn_images_directory
import org.churchpresenter.strings.generated.resources.hymn_settings
import org.churchpresenter.strings.generated.resources.hymn_settings_hint
import org.churchpresenter.strings.generated.resources.hymn_title_directory
import org.churchpresenter.strings.generated.resources.hymn_vocal_directory
import org.churchpresenter.strings.generated.resources.no_directory_selected
import org.churchpresenter.strings.generated.resources.shortcut_unbound
import org.churchpresenter.strings.generated.resources.system_storage_status_linked
import org.churchpresenter.strings.generated.resources.system_storage_status_not_found
import org.churchpresenter.strings.generated.resources.system_storage_status_read_only
import org.churchpresenter.theme.AppShape
import org.churchpresenter.theme.components.RaisedButton
import org.churchpresenter.theme.semantic
import org.jetbrains.compose.resources.stringResource

/** One of the hymn's four folders: its label, where it points, and how to change it. */
private class HymnDirectory(val label: String, val path: String, val onPathChange: (String) -> Unit)

private val HYMN_NAME_COLUMN_WIDTH = 168.dp
private val HYMN_CONTROL_HEIGHT = 34.dp

/**
 * The Hymn tab's folders: where its slide images are, where the piano and vocal mp3s are, and
 * where the `HymnTitle.txt` that names the hymns is. Each is its own directory row, like the
 * storage card's.
 */
@Composable
internal fun SystemHymnCard(
    settings: AppSettings,
    onSettingsChange: ((AppSettings) -> AppSettings) -> Unit,
    fileManager: FileManager,
) {
    val hymn = settings.hymnSettings
    val directories = listOf(
        HymnDirectory(stringResource(Res.string.hymn_images_directory), hymn.imagesDirectory) { dir ->
            onSettingsChange { s -> s.copy(hymnSettings = s.hymnSettings.copy(imagesDirectory = dir)) }
        },
        HymnDirectory(stringResource(Res.string.hymn_piano_directory), hymn.pianoDirectory) { dir ->
            onSettingsChange { s -> s.copy(hymnSettings = s.hymnSettings.copy(pianoDirectory = dir)) }
        },
        HymnDirectory(stringResource(Res.string.hymn_vocal_directory), hymn.vocalDirectory) { dir ->
            onSettingsChange { s -> s.copy(hymnSettings = s.hymnSettings.copy(vocalDirectory = dir)) }
        },
        HymnDirectory(stringResource(Res.string.hymn_title_directory), hymn.hymnTitleDirectory) { dir ->
            onSettingsChange { s -> s.copy(hymnSettings = s.hymnSettings.copy(hymnTitleDirectory = dir)) }
        },
    )
    SettingsCard(
        title = stringResource(Res.string.hymn_settings),
        subtitle = stringResource(Res.string.hymn_settings_hint),
    ) {
        directories.forEachIndexed { index, directory ->
            HymnDirectoryRow(directory, fileManager)
            if (index < directories.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun HymnDirectoryRow(directory: HymnDirectory, fileManager: FileManager) {
    val scope = rememberCoroutineScope()
    val status = rememberDirStatus(directory.path)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(HYMN_NAME_COLUMN_WIDTH)) {
            Text(
                text = directory.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status.label(),
                style = MaterialTheme.typography.labelSmall,
                color = status.color(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        HymnPathField(directory.path, Modifier.weight(1f))
        RaisedButton(
            onClick = {
                scope.launch {
                    fileManager.chooseDirectory(currentDirectory = directory.path)?.let(directory.onPathChange)
                }
            },
            modifier = Modifier.height(HYMN_CONTROL_HEIGHT),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            shape = AppShape(8.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
        ) {
            Text(text = stringResource(Res.string.browse_directory), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun DirStatus.label(): String = when (this) {
    DirStatus.NOT_SET -> stringResource(Res.string.shortcut_unbound)
    DirStatus.CHECKING -> "…"
    DirStatus.WRITABLE -> stringResource(Res.string.system_storage_status_linked)
    DirStatus.READ_ONLY -> stringResource(Res.string.system_storage_status_read_only)
    DirStatus.NOT_FOUND, DirStatus.INVALID -> stringResource(Res.string.system_storage_status_not_found)
}

@Composable
private fun DirStatus.color() = when (this) {
    DirStatus.WRITABLE -> MaterialTheme.semantic.success
    DirStatus.READ_ONLY -> MaterialTheme.semantic.warning
    DirStatus.NOT_FOUND, DirStatus.INVALID -> MaterialTheme.colorScheme.error
    DirStatus.NOT_SET, DirStatus.CHECKING -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun HymnPathField(path: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(HYMN_CONTROL_HEIGHT)
            .clip(AppShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShape(8.dp))
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = path.ifEmpty { stringResource(Res.string.no_directory_selected) },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = if (path.isEmpty()) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
