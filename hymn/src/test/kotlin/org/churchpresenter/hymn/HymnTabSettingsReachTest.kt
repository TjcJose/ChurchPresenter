package org.churchpresenter.hymn

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.runComposeUiTest
import org.churchpresenter.settings.AppSettings
import org.churchpresenter.settings.HymnSettings
import org.churchpresenter.slides.viewmodel.PresentationViewModel
import java.io.File
import kotlin.test.Test

/**
 * The Hymn tab must show the folders in force the moment it is on screen -- both when they were
 * already set before it opened and when they change while it is open. Regression: `settings` used
 * to be a plain field, so the "set the folders" branch (checked before the observable hymn list)
 * never invalidated and the list stayed hidden until an unrelated recomposition.
 */
@OptIn(ExperimentalTestApi::class)
class HymnTabSettingsReachTest {

    private fun libraryFor(settings: HymnSettings): List<HymnItem> =
        if (settings.imagesDirectory.isBlank()) {
            emptyList()
        } else {
            listOf(HymnItem("001", "Amazing Grace", File("001"), null, null))
        }

    @Test
    fun showsFoldersAlreadySetBeforeItOpens() = runComposeUiTest {
        val viewModel = HymnViewModel(library = ::libraryFor)
        setContent {
            MaterialTheme {
                HymnTab(
                    appSettings = AppSettings(hymnSettings = HymnSettings(imagesDirectory = "C:/hymns")),
                    presenterManager = null,
                    viewModel = viewModel,
                    slidesViewModel = PresentationViewModel(),
                )
            }
        }
        waitForIdle()
        onAllNodesWithText("001").assertCountEquals(1)
    }

    @Test
    fun showsFoldersSetWhileItIsOpen() = runComposeUiTest {
        val settingsState = mutableStateOf(HymnSettings())
        val viewModel = HymnViewModel(library = ::libraryFor)
        setContent {
            MaterialTheme {
                HymnTab(
                    appSettings = AppSettings(hymnSettings = settingsState.value),
                    presenterManager = null,
                    viewModel = viewModel,
                    slidesViewModel = PresentationViewModel(),
                )
            }
        }
        waitForIdle()
        onAllNodesWithText("001").assertCountEquals(0)

        settingsState.value = HymnSettings(imagesDirectory = "C:/hymns")
        waitForIdle()
        onAllNodesWithText("001").assertCountEquals(1)
    }
}
