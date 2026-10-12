package org.churchpresenter.hymn

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.churchpresenter.core.models.presentation.PresentationLoadError
import org.churchpresenter.core.models.schedule.ScheduleItem
import org.churchpresenter.icons.generated.resources.Res as IconRes
import org.churchpresenter.icons.generated.resources.ic_go_live
import org.churchpresenter.icons.generated.resources.ic_skip_next
import org.churchpresenter.icons.generated.resources.ic_skip_previous
import org.churchpresenter.sharedui.composables.LocalWentLive
import org.churchpresenter.sharedui.composables.bibleListCard
import org.churchpresenter.sharedui.composables.handleGoLiveKey
import org.churchpresenter.sharedui.models.Presenting
import org.churchpresenter.sharedui.utils.LocalShortcuts
import org.churchpresenter.slides.SlidesOutput
import org.churchpresenter.slides.tabs.PresentationLiveSlideEffect
import org.churchpresenter.slides.tabs.SlideThumbnail
import org.churchpresenter.slides.tabs.presentationGoesBack
import org.churchpresenter.slides.tabs.presentationGoesForward
import org.churchpresenter.slides.viewmodel.PresentationViewModel
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.go_live
import org.churchpresenter.strings.generated.resources.hymn_no_ppt
import org.churchpresenter.strings.generated.resources.hymn_select_hymn
import org.churchpresenter.strings.generated.resources.hymn_slide_count
import org.churchpresenter.strings.generated.resources.loading_slides
import org.churchpresenter.strings.generated.resources.presentation_error_empty_document
import org.churchpresenter.strings.generated.resources.presentation_error_library_missing
import org.churchpresenter.strings.generated.resources.presentation_error_password_protected
import org.churchpresenter.strings.generated.resources.presentation_error_render_failed
import org.churchpresenter.theme.components.KeyIconButton
import org.churchpresenter.theme.components.RaisedButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.io.File

/**
 * The tab's right pane: the selected hymn's slides in exactly the Presentation tab's shape — a
 * thumbnail grid over :slides' [PresentationViewModel], whose slides go live by double-click or
 * the Go Live button through [SlidesOutput] under `Presenting.PRESENTATION`. With the pane focused
 * the Go Live key (Enter) also puts the selected slide up, and the previous/next keys — the arrow
 * keys, or a clicker's Page Up/Down — page it, exactly like the Presentation tab.
 *
 * [viewModel] carries the audio transport; while a track is loaded its player bar sits above the
 * slides.
 */
@Composable
internal fun HymnSlidePane(
    hymn: HymnItem?,
    slidesViewModel: PresentationViewModel,
    presenterManager: SlidesOutput?,
    viewModel: HymnViewModel,
    modifier: Modifier = Modifier,
) {
    val slides = hymn?.takeIf { it.pptFile != null }
    val scope = rememberCoroutineScope()
    val wentLive = LocalWentLive.current
    val shortcuts = LocalShortcuts.current
    val focusRequester = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    val canGoLive = presenterManager != null && slidesViewModel.slideFiles.isNotEmpty()

    // Take the keyboard once a hymn's slides are on screen, like the Presentation tab after its
    // load, so the arrow keys page and Enter goes live without a click on the grid first.
    LaunchedEffect(slides?.pptFile?.absolutePath, slidesViewModel.slideFiles.isNotEmpty()) {
        if (slidesViewModel.slideFiles.isNotEmpty()) focusRequester.requestFocus()
    }

    // Once the hymn is on a screen, the selection drives the projection, exactly as it does in the
    // Presentation tab: the arrow keys (and the toolbar's Previous/Next) then page the projected
    // slide, not merely the thumbnails. Starting the projection is still the toolbar's Go Live.
    PresentationLiveSlideEffect(slidesViewModel, presenterManager)

    Column(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            // A press anywhere in the pane hands the pane the keyboard, as a click on a thumbnail
            // would otherwise leave it on nothing: after that, Enter goes live and the arrows page.
            // The left pane's search field is a separate branch, so it keeps the keyboard when typed in.
            .pointerInput(focusRequester) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) focusRequester.requestFocus()
                    }
                }
            }
            .onFocusChanged { rootFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                // One page per press: Compose delivers a KeyDown and a KeyUp, and the chord
                // matcher looks at neither by itself, so the KeyUp would page a second time.
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                shortcuts.handleGoLiveKey(event, rootFocused, canGoLive) {
                    if (presenterManager != null) {
                        hymnGoLiveAtSlide(
                            slidesViewModel,
                            presenterManager,
                            slidesViewModel.selectedSlideIndex,
                            wentLive = wentLive,
                            scope = scope,
                        )
                    }
                } || when {
                    presentationGoesBack(shortcuts, event) -> {
                        goStep(slidesViewModel, presenterManager, forward = false)
                        true
                    }
                    presentationGoesForward(shortcuts, event) -> {
                        goStep(slidesViewModel, presenterManager, forward = true)
                        true
                    }
                    else -> false
                }
            },
    ) {
        HymnPlayerBar(viewModel, Modifier.fillMaxWidth())
        if (slides != null) {
            val file = slides.pptFile!!
            LaunchedEffect(file.absolutePath) {
                slidesViewModel.loadPresentationByPath(file.absolutePath)
            }
            HymnSlideToolbar(
                hymn = slides,
                slidesViewModel = slidesViewModel,
                presenterManager = presenterManager,
                scope = scope,
                wentLive = wentLive,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HymnSlideBody(
            hymn = hymn,
            slides = slides,
            slidesViewModel = slidesViewModel,
            onShowSlide = { index ->
                if (presenterManager != null) {
                    hymnGoLiveAtSlide(
                        slidesViewModel,
                        presenterManager,
                        index,
                        wentLive = wentLive,
                        scope = scope,
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * What the pane shows under the toolbar: the empty states, the loading/error status, or the grid —
 * one branch per state of [slidesViewModel] against the selected [hymn].
 */
@Composable
private fun HymnSlideBody(
    hymn: HymnItem?,
    slides: HymnItem?,
    slidesViewModel: PresentationViewModel,
    onShowSlide: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        // No hymn selected, or the selected hymn has no PPT file.
        slides == null -> HymnEmptyState(
            stringResource(if (hymn == null) Res.string.hymn_select_hymn else Res.string.hymn_no_ppt),
            modifier,
        )
        // A different hymn's deck is still selected — a switch just happened and the new
        // one hasn't started loading — so keep the loading state rather than flash the
        // previous hymn's slides.
        slidesViewModel.selectedPresentation?.absolutePath != slides.pptFile?.absolutePath ->
            HymnSlideStatus(slidesViewModel, modifier)
        // Slides land one at a time while a PPT renders, so the grid appears and grows as
        // each one arrives — the same incremental reveal the Presentation tab has, which
        // keeps a many-page file from looking stuck.
        slidesViewModel.slideFiles.isNotEmpty() -> HymnSlideGrid(
            slidesViewModel = slidesViewModel,
            onShowSlide = onShowSlide,
            modifier = modifier,
        )
        // The deck matches but nothing has rendered yet (still loading), or it failed with
        // no slides: the spinner or the error, as the Presentation tab shows them.
        else -> HymnSlideStatus(slidesViewModel, modifier)
    }
}

/** The controls above the grid: the hymn's name, Previous/Next, and Go Live. */
@Composable
private fun HymnSlideToolbar(
    hymn: HymnItem,
    slidesViewModel: PresentationViewModel,
    presenterManager: SlidesOutput?,
    scope: CoroutineScope,
    wentLive: (ScheduleItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = slidesViewModel.slideFiles.size
    val canNavigate = total > 0
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = listOfNotNull(hymn.number, hymn.title.takeIf { it.isNotBlank() }).joinToString(" · "),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        KeyIconButton(onClick = { goStep(slidesViewModel, presenterManager, forward = false) }, enabled = canNavigate) {
            Icon(painterResource(IconRes.drawable.ic_skip_previous), contentDescription = null)
        }
        Text(
            text = stringResource(
                Res.string.hymn_slide_count,
                if (canNavigate) slidesViewModel.selectedSlideIndex + 1 else 0,
                total,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        KeyIconButton(onClick = { goStep(slidesViewModel, presenterManager, forward = true) }, enabled = canNavigate) {
            Icon(painterResource(IconRes.drawable.ic_skip_next), contentDescription = null)
        }
        Spacer(Modifier.width(10.dp))
        RaisedButton(
            onClick = {
                if (presenterManager != null) {
                    hymnGoLiveAtSlide(
                        slidesViewModel,
                        presenterManager,
                        slidesViewModel.selectedSlideIndex,
                        wentLive = wentLive,
                        scope = scope,
                    )
                }
            },
            enabled = canNavigate && presenterManager != null,
        ) {
            Icon(
                painterResource(IconRes.drawable.ic_go_live),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.go_live))
        }
    }
}

/** Next/Previous: first the live deck's build steps, exactly like the Presentation tab's. */
private fun goStep(slidesViewModel: PresentationViewModel, presenterManager: SlidesOutput?, forward: Boolean) {
    val deck = slidesViewModel.deck
    val stepped = deck != null && presenterManager
        ?.let {
            if (forward) it.advancePresentationStep(deck, slidesViewModel.selectedSlideIndex)
            else it.rewindPresentationStep(deck, slidesViewModel.selectedSlideIndex)
        } == true
    if (!stepped) {
        if (forward) slidesViewModel.nextSlide() else slidesViewModel.previousSlide()
    }
}

/** The hymn's slides while they load, or the reason they could not be read — the
 *  Presentation tab's own loading and error states. */
@Composable
private fun HymnSlideStatus(slidesViewModel: PresentationViewModel, modifier: Modifier = Modifier) {
    val loadError = slidesViewModel.loadError
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (!slidesViewModel.isLoading && loadError != null) {
            Text(
                text = stringResource(
                    when (loadError) {
                        PresentationLoadError.PASSWORD_PROTECTED -> Res.string.presentation_error_password_protected
                        PresentationLoadError.EMPTY_DOCUMENT -> Res.string.presentation_error_empty_document
                        PresentationLoadError.LIBRARY_MISSING -> Res.string.presentation_error_library_missing
                        PresentationLoadError.RENDER_FAILED -> Res.string.presentation_error_render_failed
                    }
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                Text(
                    stringResource(Res.string.loading_slides),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

/** The thumbnail grid, one cell per slide of the selected hymn. */
@Composable
private fun HymnSlideGrid(
    slidesViewModel: PresentationViewModel,
    onShowSlide: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
            .bibleListCard()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 18.dp),
    ) {
        itemsIndexed(slidesViewModel.slideFiles, key = { _, file -> file.absolutePath }) { index, slideFile ->
            HymnSlideCell(slidesViewModel, index, slideFile, onShowSlide)
        }
    }
}

@Composable
private fun HymnSlideCell(
    slidesViewModel: PresentationViewModel,
    index: Int,
    slideFile: File,
    onShowSlide: (Int) -> Unit,
) {
    // Decode off the composition thread, like the Presentation tab's cells; a missing or corrupt
    // file just stays a blank thumbnail instead of crashing the grid.
    val bitmap by produceState<ImageBitmap?>(initialValue = null, slideFile) {
        value = withContext(Dispatchers.IO) { decodeSlide(slideFile) }
    }
    SlideThumbnail(
        slide = bitmap,
        slideNumber = index + 1,
        buildCount = slidesViewModel.deck?.slides?.getOrNull(index)?.timeline?.stepCount ?: 0,
        isSelected = slidesViewModel.selectedSlideIndex == index,
        isHidden = index in slidesViewModel.hiddenSlides,
        onToggleHidden = { slidesViewModel.toggleSlideHidden(index) },
        onClick = { slidesViewModel.selectSlide(index) },
        onDoubleClick = { onShowSlide(index) },
    )
}

/** Puts slide [index] of the selected hymn live, over the presentation path. */
private fun hymnGoLiveAtSlide(
    slidesViewModel: PresentationViewModel,
    presenterManager: SlidesOutput,
    index: Int,
    wentLive: (ScheduleItem) -> Unit,
    scope: CoroutineScope,
) {
    scope.launch {
        val current = slidesViewModel.slideFiles.getOrNull(index)?.let { file ->
            withContext(Dispatchers.IO) { decodeSlide(file) }
        }
        val next = slidesViewModel.nextShownSlideIndex(index)
            ?.let { slidesViewModel.slideFiles.getOrNull(it) }
            ?.let { file -> withContext(Dispatchers.IO) { decodeSlide(file) } }
        presenterManager.setSelectedSlide(current)
        presenterManager.setLiveSlide(slidesViewModel.selectedPresentation?.name, index)
        presenterManager.setNextSlide(next)
        presenterManager.setPresenterNotes(slidesViewModel.slideNotes.getOrElse(index) { "" })
    }
    presenterManager.setPresentingMode(Presenting.PRESENTATION)
    slidesViewModel.deck?.let { presenterManager.presentationShowSlide(it, index) }
    presenterManager.setShowPresenterWindow(true)
    slidesViewModel.selectedPresentation?.let { file ->
        wentLive(hymnPresentationRow(file, slidesViewModel.slideFiles.size))
    }
}

/** The open deck as the schedule row that identifies it — the same shape Add to Schedule builds. */
internal fun hymnPresentationRow(file: File, slideCount: Int): ScheduleItem.PresentationItem =
    ScheduleItem.PresentationItem(
        id = java.util.UUID.randomUUID().toString(),
        filePath = file.absolutePath,
        fileName = file.nameWithoutExtension,
        slideCount = slideCount,
        fileType = file.extension.lowercase(),
        hymnNumber = file.nameWithoutExtension,
    )

private fun decodeSlide(file: File): ImageBitmap? = try {
    org.jetbrains.skia.Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
} catch (_: Exception) {
    null
}
