package org.churchpresenter.hymn

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import org.churchpresenter.core.models.schedule.ScheduleItem
import org.churchpresenter.settings.AppSettings
import org.churchpresenter.slides.SlidesOutput
import org.churchpresenter.slides.viewmodel.PresentationViewModel

/** The list column's share of the tab's width, against the slide pane's. */
internal const val HYMN_LIST_WEIGHT = 0.9f
internal const val HYMN_SLIDES_WEIGHT = 1.6f

/**
 * The hymn slide grid's target cell width, in dp — the same width the Presentation tab's
 * `GridCells.Adaptive(200.dp)` lands on at full width. This pane is narrower, so its grid counts
 * columns against this width instead of letting `Adaptive` hand it a few oversized cells: a 4:3
 * deck's 148dp-tall cell would otherwise widen past the deck and `ContentScale.Fit` would pad it
 * with black down each side, while the same deck shows edge to edge in Presentation.
 */
internal const val HYMN_CELL_WIDTH = 200f

/** How often the player bar re-reads the player's position while a track is loaded. */
private const val AUDIO_POLL_MS = 200L

/**
 * The Hymn tab: the church's hymnbook as files.
 *
 * The left pane lists every hymn — its number, its title from `HymnTitle.txt`, and per-row the
 * Add to Schedule, Piano and Vocal buttons — like the Songs tab. The right pane is a hymn's slides
 * in exactly the Presentation tab's shape: a thumbnail grid whose slides go live by double-click
 * or the Go Live button, over the same [SlidesOutput] the Presentation tab airs on
 * (`Presenting.PRESENTATION`).
 *
 * The slide machinery is `:slides`' own [PresentationViewModel] taken whole; this tab only drives
 * it with the selected hymn's PPT file. Audio plays one hymn mp3 at a time through
 * [HymnAudioPlayer], which never touches vlcj directly. Both view models are handed in — the main
 * screen owns them, so what is selected and what is playing survive a switch to another tab.
 */
@Composable
fun HymnTab(
    appSettings: AppSettings,
    viewModel: HymnViewModel,
    slidesViewModel: PresentationViewModel,
    presenterManager: SlidesOutput? = null,
    onAddToSchedule: (
        (filePath: String, fileName: String, slideCount: Int, fileType: String, hymnNumber: String) -> Unit
    )? = null,
    vlcAvailable: Boolean = true,
    selectedHymnItem: ScheduleItem? = null,
    selectedHymnItemVersion: Int = 0,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(appSettings.hymnSettings) {
        viewModel.configure(appSettings.hymnSettings)
    }
    // A Schedule row the Hymn tab put there: bring the hymn it names back on screen, whatever tab
    // the operator had open. Held by the ViewModel until the library has scanned it in.
    LaunchedEffect(selectedHymnItem, selectedHymnItemVersion) {
        val item = selectedHymnItem as? ScheduleItem.PresentationItem ?: return@LaunchedEffect
        viewModel.selectByPptPath(item.filePath)
    }
    LaunchedEffect(appSettings.projectionSettings.audioOutputDeviceId) {
        viewModel.setAudioOutputDevice(appSettings.projectionSettings.audioOutputDeviceId)
    }
    // The bar reads the player on a timer: VLC reports position and length through no event of its
    // own, and polling only while a hymn is on keeps the tab idle otherwise.
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUDIO_POLL_MS)
            viewModel.syncAudio()
        }
    }
    Row(modifier = modifier.fillMaxSize().testTag("hymn_root")) {
        HymnListPane(
            viewModel = viewModel,
            vlcAvailable = vlcAvailable,
            onAddToSchedule = { hymn ->
                hymn.pptFile?.let { file ->
                    onAddToSchedule?.invoke(
                        file.absolutePath,
                        file.nameWithoutExtension,
                        slideCountFor(slidesViewModel, hymn),
                        file.extension.lowercase(),
                        hymn.number,
                    )
                }
            },
            modifier = Modifier.fillMaxHeight().weight(HYMN_LIST_WEIGHT),
        )
        HymnSlidePane(
            hymn = viewModel.selectedHymn,
            slidesViewModel = slidesViewModel,
            presenterManager = presenterManager,
            viewModel = viewModel,
            modifier = Modifier.fillMaxHeight().weight(HYMN_SLIDES_WEIGHT),
        )
    }
}

/** How many slides an Add to Schedule knows for [hymn]: the loaded count only when it is the one on screen. */
private fun slideCountFor(slidesViewModel: PresentationViewModel, hymn: HymnItem): Int =
    if (slidesViewModel.selectedPresentation?.absolutePath == hymn.pptFile?.absolutePath) {
        slidesViewModel.slideFiles.size
    } else {
        0
    }
