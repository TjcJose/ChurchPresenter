package org.churchpresenter.hymn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.churchpresenter.icons.generated.resources.Res as IconRes
import org.churchpresenter.icons.generated.resources.ic_microphone
import org.churchpresenter.icons.generated.resources.ic_music_note
import org.churchpresenter.icons.generated.resources.ic_playlist_add
import org.churchpresenter.icons.generated.resources.ic_search
import org.churchpresenter.settings.utils.Constants
import org.churchpresenter.sharedui.composables.BibleListRowShape
import org.churchpresenter.sharedui.composables.bibleRowColors
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.contains
import org.churchpresenter.strings.generated.resources.exact_match
import org.churchpresenter.strings.generated.resources.filter
import org.churchpresenter.strings.generated.resources.hymn_no_folder
import org.churchpresenter.strings.generated.resources.hymn_number
import org.churchpresenter.strings.generated.resources.hymn_piano
import org.churchpresenter.strings.generated.resources.hymn_vocal
import org.churchpresenter.strings.generated.resources.media_vlc_install
import org.churchpresenter.strings.generated.resources.no_files_detected
import org.churchpresenter.strings.generated.resources.no_results_found
import org.churchpresenter.strings.generated.resources.search
import org.churchpresenter.strings.generated.resources.search_hymns
import org.churchpresenter.strings.generated.resources.starts_with
import org.churchpresenter.strings.generated.resources.title
import org.churchpresenter.theme.AppShape
import org.churchpresenter.theme.components.DropdownSelector
import org.churchpresenter.theme.components.KeyIconButton
import org.churchpresenter.theme.elevationPalette
import org.churchpresenter.theme.hoverTint
import org.churchpresenter.theme.raisedHover
import org.churchpresenter.theme.sunken
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val NUMBER_WIDTH = 76.dp
private val ACTIONS_WIDTH = 124.dp

/**
 * The tab's left pane: the search bar (input, filter, query button), the VLC-missing warning, the
 * Number/Title/actions column header, and the list of hymns — one row per hymn with its Add to
 * Schedule, Piano and Vocal buttons.
 */
@Composable
internal fun HymnListPane(
    viewModel: HymnViewModel,
    vlcAvailable: Boolean,
    onAddToSchedule: (HymnItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        HymnSearchBar(viewModel, Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
        if (!vlcAvailable) {
            Text(
                text = stringResource(Res.string.media_vlc_install),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            )
        }
        HymnTableHeader(Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
        HorizontalDivider(Modifier.padding(horizontal = 14.dp))
        when {
            !viewModel.hasFolder ->
                HymnEmptyState(stringResource(Res.string.hymn_no_folder), Modifier.weight(1f))
            viewModel.hymns.isEmpty() -> HymnEmptyState(
                message = if (viewModel.searchQuery.isBlank()) {
                    stringResource(Res.string.no_files_detected)
                } else {
                    stringResource(Res.string.no_results_found, viewModel.searchQuery)
                },
                modifier = Modifier.weight(1f),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                items(viewModel.hymns, key = { it.number }) { hymn ->
                    HymnRow(
                        hymn = hymn,
                        viewModel = viewModel,
                        vlcAvailable = vlcAvailable,
                        onAddToSchedule = { onAddToSchedule(hymn) },
                    )
                }
            }
        }
    }
}

/**
 * The search bar, modelled on the Songs tab's: an input that filters as it is typed, a filter
 * choice over how the input is matched, and a query button that re-reads the folders from disk.
 */
@Composable
private fun HymnSearchBar(viewModel: HymnViewModel, modifier: Modifier = Modifier) {
    val containsText = stringResource(Res.string.contains)
    val startsWithText = stringResource(Res.string.starts_with)
    val exactMatchText = stringResource(Res.string.exact_match)
    val displayByKey = mapOf(
        Constants.CONTAINS to containsText,
        Constants.STARTS_WITH to startsWithText,
        Constants.EXACT_MATCH to exactMatchText,
    )
    val keyByDisplay = displayByKey.entries.associate { (key, display) -> display to key }

    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
        // Wraps to a second line when the pane is too narrow for all three at once.
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        HymnSearchField(viewModel, Modifier.weight(1f))
        DropdownSelector(
            label = stringResource(Res.string.filter),
            items = displayByKey.values.toList(),
            selected = displayByKey[viewModel.filterType] ?: containsText,
            onSelectedChange = { viewModel.filterType = keyByDisplay[it] ?: Constants.CONTAINS },
        )
        HymnSearchButton(onClick = { viewModel.rescan() })
    }
}

/** The input: a sunken well at the dropdowns' height, so the three sit on one line. */
@Composable
private fun HymnSearchField(viewModel: HymnViewModel, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .widthIn(min = 120.dp)
            .height(42.dp)
            .sunken(AppShape(8.dp), elevationPalette())
            .hoverTint(AppShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(IconRes.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.padding(start = 11.dp).size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
        )
        Box(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            BasicTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (viewModel.searchQuery.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.search_hymns),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                },
            )
        }
    }
}

/**
 * The query button: re-scans the hymn folders, so an image folder or track dropped in while the
 * tab was open turns up without leaving the tab.
 */
@Composable
private fun HymnSearchButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(42.dp)
            .raisedHover(AppShape(8.dp), elevationPalette().accent, elevationPalette())
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(IconRes.drawable.ic_search),
            contentDescription = stringResource(Res.string.search),
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun HymnTableHeader(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.hymn_number),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(NUMBER_WIDTH),
        )
        Text(
            text = stringResource(Res.string.title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        Spacer(Modifier.width(ACTIONS_WIDTH))
    }
}

@Composable
private fun HymnRow(
    hymn: HymnItem,
    viewModel: HymnViewModel,
    vlcAvailable: Boolean,
    onAddToSchedule: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = viewModel.selectedNumber == hymn.number
    val pianoActive = viewModel.audioState?.let {
        it.hymnNumber == hymn.number && it.kind == HymnAudioKind.PIANO
    } == true
    val vocalActive = viewModel.audioState?.let {
        it.hymnNumber == hymn.number && it.kind == HymnAudioKind.VOCAL
    } == true
    // The selected row uses the same primary tint and ink as Songs/Bible rows, so it reads at the
    // same depth across the app; the primaryContainer wash it used was too pale to see.
    val rowColors = bibleRowColors(selected, hovered = false)
    val textColor = if (selected) rowColors.ink else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(BibleListRowShape)
            .background(rowColors.background)
            .clickable { viewModel.select(hymn) }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = hymn.number,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = textColor,
            modifier = Modifier.width(NUMBER_WIDTH),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = hymn.title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (hymn.title.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else textColor,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(2.dp))
        Row(modifier = Modifier.width(ACTIONS_WIDTH), horizontalArrangement = Arrangement.End) {
            KeyIconButton(onClick = onAddToSchedule, enabled = hymn.hasSlides) {
                Icon(
                    painterResource(IconRes.drawable.ic_playlist_add),
                    contentDescription = null,
                    tint = LocalContentColor.current,
                )
            }
            HymnAudioButton(
                active = pianoActive,
                enabled = hymn.hasPiano && vlcAvailable,
                label = stringResource(Res.string.hymn_piano),
                kind = HymnAudioKind.PIANO,
                onClick = { viewModel.playPiano(hymn) },
            )
            HymnAudioButton(
                active = vocalActive,
                enabled = hymn.hasVocal && vlcAvailable,
                label = stringResource(Res.string.hymn_vocal),
                kind = HymnAudioKind.VOCAL,
                onClick = { viewModel.playVocal(hymn) },
            )
        }
    }
}

/**
 * The Piano/Vocal button: a music note for the piano, a microphone for the vocal, tinted with the
 * accent while its track is the one loaded on the player. The transport (play/pause/stop) lives in
 * the player bar; pressing this one plays that track, or pauses/resumes it when it is already on.
 */
@Composable
private fun HymnAudioButton(
    active: Boolean,
    enabled: Boolean,
    label: String,
    kind: HymnAudioKind,
    onClick: () -> Unit,
) {
    KeyIconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painterResource(
                if (kind == HymnAudioKind.PIANO) IconRes.drawable.ic_music_note else IconRes.drawable.ic_microphone
            ),
            contentDescription = label,
            tint = if (active) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        )
    }
}

/** A centered, muted message for an empty pane. */
@Composable
internal fun HymnEmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
