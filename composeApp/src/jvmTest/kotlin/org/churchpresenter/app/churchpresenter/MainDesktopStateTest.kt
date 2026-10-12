package org.churchpresenter.app.churchpresenter

import androidx.compose.runtime.mutableStateOf
import org.churchpresenter.core.models.schedule.ScheduleItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainDesktopStateTest {

    private fun state(crashFeedback: Boolean = false) = MainDesktopState(mutableStateOf(0), crashFeedback)

    @Test
    fun `no dialog is open at launch`() {
        val state = state()

        assertFalse(state.showAddLabelDialog)
        assertNull(state.editingLabelItem)
        assertFalse(state.showAddWebsiteDialog)
        assertFalse(state.showKonamiEasterEgg)
        assertFalse(state.showCrashFeedback)
    }

    @Test
    fun `the crash feedback prompt opens at launch only when asked for`() {
        assertTrue(state(crashFeedback = true).showCrashFeedback)
    }

    @Test
    fun `editing a label opens its dialog on that label, and closing forgets it`() {
        val state = state()
        val label = ScheduleItem.LabelItem(id = "1", text = "Welcome", textColor = "#FFF", backgroundColor = "#000")

        state.editingLabelItem = label
        state.showAddLabelDialog = true
        assertEquals(label, state.editingLabelItem)
        assertTrue(state.showAddLabelDialog)

        state.showAddLabelDialog = false
        state.editingLabelItem = null
        assertFalse(state.showAddLabelDialog)
        assertNull(state.editingLabelItem)
    }

    @Test
    fun `the website and easter egg dialogs open and close on their own flags`() {
        val state = state()

        state.showAddWebsiteDialog = true
        state.showKonamiEasterEgg = true
        assertTrue(state.showAddWebsiteDialog)
        assertTrue(state.showKonamiEasterEgg)

        state.showAddWebsiteDialog = false
        state.showKonamiEasterEgg = false
        assertFalse(state.showAddWebsiteDialog)
        assertFalse(state.showKonamiEasterEgg)
    }

    @Test
    fun `select routes a hymn row to the Hymn tab and a plain deck to the Presentation tab`() {
        val state = state()
        val deck = ScheduleItem.PresentationItem(
            id = "1", filePath = "/decks/sermon.pptx", fileName = "sermon",
            slideCount = 3, fileType = "pptx",
        )
        val hymn = ScheduleItem.PresentationItem(
            id = "2", filePath = "/hymns/001.pptx", fileName = "001",
            slideCount = 3, fileType = "pptx", hymnNumber = "001",
        )

        assertTrue(state.select(deck))
        assertEquals(deck, state.selectedPresentationItem)
        assertNull(state.selectedHymnItem)

        assertTrue(state.select(hymn))
        assertEquals(hymn, state.selectedHymnItem)
        // The plain deck the Presentation tab already holds is left alone.
        assertEquals(deck, state.selectedPresentationItem)
    }
}
