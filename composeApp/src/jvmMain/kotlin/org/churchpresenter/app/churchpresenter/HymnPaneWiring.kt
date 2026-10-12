package org.churchpresenter.app.churchpresenter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.churchpresenter.hymn.HymnTab
import org.churchpresenter.media.composables.isVlcAvailable

/*
 * The Hymn tab as the main screen composes it: the hymnbook's own pane, wired to the Schedule and
 * to the presentation output the way the Presentation tab is.
 */

@Composable
internal fun MainDesktopScope.HymnTabPane() {
    HymnTab(
        modifier = Modifier.fillMaxSize(),
        appSettings = appSettings,
        viewModel = hymnViewModel,
        slidesViewModel = hymnPresentationViewModel,
        presenterManager = presenterManager.slidesOutput,
        onAddToSchedule = { filePath, fileName, slideCount, fileType, hymnNumber ->
            currentScheduleActions.addPresentation(filePath, fileName, slideCount, fileType, hymnNumber)
        },
        vlcAvailable = isVlcAvailable,
        selectedHymnItem = state.selectedHymnItem,
        selectedHymnItemVersion = state.selectedHymnItemVersion,
    )
}
