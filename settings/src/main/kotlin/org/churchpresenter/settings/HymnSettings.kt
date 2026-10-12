package org.churchpresenter.settings

import kotlinx.serialization.Serializable

/**
 * The four folders the Hymn tab reads from.
 *
 * - [pptDirectory] holds the hymn slides as PPT/PPTX files, one per hymn, named by hymn number
 *   ("129.pptx", "051(甲).pptx"); the number may carry text.
 * - [pianoDirectory] and [vocalDirectory] hold the piano and vocal mp3 backing tracks, named by
 *   the same hymn number.
 * - [hymnTitleDirectory] holds `HymnTitle.txt`, the number → title map the tab resolves each
 *   file name against; a hymn with no entry there keeps an empty title.
 *
 * Blank means the folder is not set yet and the tab shows an empty list.
 */
@Serializable
data class HymnSettings(
    val pptDirectory: String = "",
    val pianoDirectory: String = "",
    val vocalDirectory: String = "",
    val hymnTitleDirectory: String = "",
)
