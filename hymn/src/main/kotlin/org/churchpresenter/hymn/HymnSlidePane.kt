package org.churchpresenter.hymn

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.churchpresenter.core.models.presentation.PresentationLoadError
import org.churchpresenter.core.models.schedule.ScheduleItem
import org.churchpresenter.icons.generated.resources.Res as IconRes
import org.churchpresenter.icons.generated.resources.ic_skip_next
import org.churchpresenter.icons.generated.resources.ic_skip_previous
import org.churchpresenter.sharedui.composables.GoLiveButton
import org.churchpresenter.sharedui.composables.HIDDEN_TILE_ALPHA
import org.churchpresenter.sharedui.composables.HiddenBadge
import org.churchpresenter.sharedui.composables.LocalWentLive
import org.churchpresenter.sharedui.composables.SlideshowHideToggle
import org.churchpresenter.sharedui.composables.bibleListCard
import org.churchpresenter.sharedui.composables.handleGoLiveKey
import org.churchpresenter.sharedui.composables.hoverLift
import org.churchpresenter.sharedui.models.Presenting
import org.churchpresenter.sharedui.utils.LocalShortcuts
import org.churchpresenter.slides.SlidesOutput
import org.churchpresenter.slides.tabs.PresentationLiveSlideEffect
import org.churchpresenter.slides.tabs.PresentationNextSlideEffect
import org.churchpresenter.slides.tabs.presentationGoesBack
import org.churchpresenter.slides.tabs.presentationGoesForward
import org.churchpresenter.slides.utils.PictureDecoder
import org.churchpresenter.slides.viewmodel.PresentationViewModel
import org.churchpresenter.strings.generated.resources.Res
import org.churchpresenter.strings.generated.resources.go_live
import org.churchpresenter.strings.generated.resources.hymn_no_slides
import org.churchpresenter.strings.generated.resources.hymn_select_hymn
import org.churchpresenter.strings.generated.resources.hymn_slide_count
import org.churchpresenter.strings.generated.resources.loading
import org.churchpresenter.strings.generated.resources.loading_slides
import org.churchpresenter.strings.generated.resources.picture_thumbnail_unreadable
import org.churchpresenter.strings.generated.resources.presentation_error_empty_document
import org.churchpresenter.strings.generated.resources.presentation_error_library_missing
import org.churchpresenter.strings.generated.resources.presentation_error_password_protected
import org.churchpresenter.strings.generated.resources.presentation_error_render_failed
import org.churchpresenter.strings.generated.resources.slide_number
import org.churchpresenter.theme.AppShape
import org.churchpresenter.theme.components.KeyIconButton
import org.churchpresenter.theme.keyboardFocusRing
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
    val slides = hymn?.takeIf { it.imageFolder != null }
    val scope = rememberCoroutineScope()
    val wentLive = LocalWentLive.current
    val shortcuts = LocalShortcuts.current
    val focusRequester = remember { FocusRequester() }
    var rootFocused by remember { mutableStateOf(false) }
    val canGoLive = presenterManager != null && slidesViewModel.slideFiles.isNotEmpty()

    // Take the keyboard once a hymn's slides are on screen, like the Presentation tab after its
    // load, so the arrow keys page and Enter goes live without a click on the grid first.
    LaunchedEffect(slides?.imageFolder?.absolutePath, slidesViewModel.slideFiles.isNotEmpty()) {
        if (slidesViewModel.slideFiles.isNotEmpty()) focusRequester.requestFocus()
    }

    HymnLiveSlideEffects(slidesViewModel, presenterManager)

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
            // The whole pane takes the arrow keys; Tab landing on it is shown by a ring just inside
            // it, as the Presentation tab does for its body.
            .keyboardFocusRing(RectangleShape, inset = true)
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
            val folder = slides.imageFolder!!
            LaunchedEffect(folder.absolutePath) {
                slidesViewModel.loadImageFolder(folder)
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
        // No hymn selected, or the selected hymn has no slide folder.
        slides == null -> HymnEmptyState(
            stringResource(if (hymn == null) Res.string.hymn_select_hymn else Res.string.hymn_no_slides),
            modifier,
        )
        // A different hymn's deck is still selected — a switch just happened and the new
        // one hasn't started loading — so keep the loading state rather than flash the
        // previous hymn's slides.
        slidesViewModel.selectedPresentation?.absolutePath != slides.imageFolder?.absolutePath ->
            HymnSlideStatus(slidesViewModel, modifier)
        // The folder's images are the slides; once they are loaded the grid shows them, growing
        // in place exactly like the Presentation tab's thumbnails.
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
        // The Presentation tab's own button, so the hymn's reads and answers exactly like a deck's:
        // a TV-icon key whose tooltip carries the Go Live key ("Go Live (Enter)"), following a
        // rebinding and dropping out when the action is unbound. `handleGoLiveKey` above already
        // answers that key while this pane has focus.
        GoLiveButton(
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
            tooltipText = stringResource(Res.string.go_live),
            enabled = canNavigate && presenterManager != null,
            showsShortcut = true,
        )
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

/**
 * How many columns the hymn slide grid fits into [availableWidth].
 *
 * Picks the count whose resulting cell width is closest to [HYMN_CELL_WIDTH] — the width the
 * Presentation tab's `GridCells.Adaptive(HYMN_CELL_WIDTH)` lands on at full width. The nearest,
 * rather than `Adaptive`'s floor, is the point: this pane is only ~2/3 of the tab, so its floor is
 * far coarser, and `Adaptive` there hands it a few cells nearly twice the target. A 4:3 deck's
 * 148dp-tall thumbnail is then wider than the deck and `ContentScale.Fit` pads it with black down
 * each side, while the same deck shows edge to edge in Presentation.
 *
 * At least one column is always returned: a pane narrower than one cell still shows the slide, just
 * narrower than the target, exactly as `Adaptive` would.
 */
internal fun hymnGridColumns(availableWidth: Dp): Int {
    if (availableWidth <= 0.dp) return 1
    val gap = HYMN_GRID_GAP.value
    val width = availableWidth.value
    // `n` cells plus their gaps span exactly `width` when `n = (width + gap) / (HYMN_CELL_WIDTH +
    // gap)`, so the ideal count is that fraction and the cells that can really be drawn sit at the
    // whole counts either side of it. Of those, the one whose resulting cell is nearest the target
    // wins -- the smaller cell on a tie, so an error never widens a cell into a 4:3 deck's bars.
    val ideal = (width + gap) / (HYMN_CELL_WIDTH + gap)
    val floor = ideal.toInt()
    return listOf(floor + 1, floor)
        .filter { it >= 1 }
        .minBy { columns -> abs((width - (columns - 1) * gap) / columns - HYMN_CELL_WIDTH) }
}

/** The slide grid's inter-cell gap, shared by the column maths and the grid itself. */
internal val HYMN_GRID_GAP = 14.dp

/** The thumbnail grid, one cell per slide of the selected hymn. */
@Composable
private fun HymnSlideGrid(
    slidesViewModel: PresentationViewModel,
    onShowSlide: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = modifier
        .fillMaxWidth()
        .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
        .bibleListCard()
        .padding(horizontal = 16.dp)
    // The Presentation tab's grid spans the whole tab, so `GridCells.Adaptive(200.dp)` there hands it
    // many cells ~200dp wide. This pane is only ~2/3 of the tab, and `Adaptive`'s floor on that width
    // is far coarser: it would give a few cells of 230-290dp, wider than a 4:3 slide's 197.3dp, so
    // every thumbnail of a 4:3 hymn is letterboxed with black down each side while the same deck
    // shows edge to edge in Presentation. `hymnGridColumns` picks the count against that same 200dp
    // target instead of flooring it. See `HYMN_CELL_WIDTH`.
    BoxWithConstraints(modifier = card) {
        val columns = hymnGridColumns(maxWidth)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(HYMN_GRID_GAP),
            verticalArrangement = Arrangement.spacedBy(HYMN_GRID_GAP),
            contentPadding = PaddingValues(vertical = 18.dp),
        ) {
            itemsIndexed(slidesViewModel.slideFiles, key = { _, file -> file.absolutePath }) { index, slideFile ->
                HymnSlideCell(slidesViewModel, index, slideFile, onShowSlide)
            }
        }
    }
}

/** Grid tile size — the largest side a thumbnail is decoded to, matching the Pictures tab. */
private const val HYMN_THUMBNAIL_MAX_DIMENSION = 400

/** One hymn slide's thumbnail, in the shape the Pictures tab's tiles take. */
private sealed interface HymnTileState {
    object Loading : HymnTileState
    data class Ready(val bitmap: ImageBitmap) : HymnTileState
    object Failed : HymnTileState
}

@Composable
private fun HymnSlideCell(
    slidesViewModel: PresentationViewModel,
    index: Int,
    slideFile: File,
    onShowSlide: (Int) -> Unit,
) {
    // Decode off the composition thread, downscaled to a grid tile the way the Pictures tab does —
    // never the display's resolution. A file that cannot be read is shown as "unreadable" rather
    // than a perpetual "loading", the way PictureTileImage does.
    val state by produceState<HymnTileState>(HymnTileState.Loading, slideFile) {
        value = withContext(Dispatchers.IO) {
            PictureDecoder
                .decodeScaledOrNull(slideFile, HYMN_THUMBNAIL_MAX_DIMENSION, HYMN_THUMBNAIL_MAX_DIMENSION)
                ?.toComposeImageBitmap()
                ?.let { HymnTileState.Ready(it) }
                ?: HymnTileState.Failed
        }
    }
    HymnPictureTile(
        state = state,
        slideNumber = index + 1,
        isSelected = slidesViewModel.selectedSlideIndex == index,
        isHidden = index in slidesViewModel.hiddenSlides,
        onToggleHidden = { slidesViewModel.toggleSlideHidden(index) },
        onClick = { slidesViewModel.selectSlide(index) },
        onDoubleClick = { onShowSlide(index) },
    )
}

/** A slide tile drawn like the Pictures tab's: cropped to fill, with a loading/unreadable state. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HymnPictureTile(
    state: HymnTileState,
    slideNumber: Int,
    isSelected: Boolean,
    isHidden: Boolean,
    onToggleHidden: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit,
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .hoverLift(AppShape(8.dp))
            .clip(AppShape(8.dp))
            .border(2.dp, borderColor, AppShape(8.dp))
            .combinedClickable(onClick = onClick, onDoubleClick = onDoubleClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                is HymnTileState.Ready -> Image(
                    bitmap = state.bitmap,
                    contentDescription = stringResource(Res.string.slide_number, slideNumber),
                    modifier = Modifier.fillMaxSize().alpha(if (isHidden) HIDDEN_TILE_ALPHA else 1f),
                    contentScale = ContentScale.Crop,
                )
                HymnTileState.Failed -> Text(
                    text = stringResource(Res.string.picture_thumbnail_unreadable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                HymnTileState.Loading -> Text(
                    text = stringResource(Res.string.loading),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isHidden) HiddenBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.slide_number, slideNumber),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                },
                maxLines = 1,
            )
            SlideshowHideToggle(hidden = isHidden, position = slideNumber - 1, onToggle = onToggleHidden)
        }
    }
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
        val nextIndex = slidesViewModel.nextShownSlideIndex(index)
        val next = nextIndex
            ?.let { slidesViewModel.slideFiles.getOrNull(it) }
            ?.let { file -> withContext(Dispatchers.IO) { decodeSlide(file) } }
        presenterManager.setSelectedSlide(current)
        presenterManager.setLiveSlide(slidesViewModel.selectedPresentation?.name, index)
        if (next != null) {
            presenterManager.setNextSlide(next)
        } else {
            presenterManager.setNextSlide(null)
        }
        presenterManager.setPresenterNotes(slidesViewModel.slideNotes.getOrElse(index) { "" })
    }
    presenterManager.setPresentingMode(Presenting.PRESENTATION)
    slidesViewModel.deck?.let { presenterManager.presentationShowSlide(it, index) }
    presenterManager.setShowPresenterWindow(true)
    slidesViewModel.selectedPresentation?.let { file ->
        wentLive(hymnPresentationRow(file, slidesViewModel.slideFiles.size))
    }
}

private fun decodeSlide(file: File): ImageBitmap? = runCatching {
    PictureDecoder.decode(file).toComposeImageBitmap()
}.getOrNull()

@Composable
private fun HymnLiveSlideEffects(
    slidesViewModel: PresentationViewModel,
    presenterManager: SlidesOutput?,
) {
    // Once the hymn is on a screen, the selection drives the projection, exactly as it does in the
    // Presentation tab: the arrow keys (and the toolbar's Previous/Next) then page the projected
    // slide, not merely the thumbnails. Starting the projection is still the toolbar's Go Live.
    PresentationLiveSlideEffect(slidesViewModel, presenterManager)
    // Hiding a slide is offered here too, so the stage monitor's "next" follows it.
    PresentationNextSlideEffect(slidesViewModel, presenterManager)
}
