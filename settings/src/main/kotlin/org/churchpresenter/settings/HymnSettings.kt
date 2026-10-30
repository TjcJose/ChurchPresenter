package org.churchpresenter.settings

import kotlinx.serialization.Serializable

/**
 * The four folders the Hymn tab reads from.
 *
 * - [imagesDirectory] holds the hymn slides as image files, one folder per hymn named by hymn
 *   number ("129/", "051(甲)/"); each folder's images are that hymn's slides, in file-name order.
 *   The number may carry text.
 * - [pianoDirectory] and [vocalDirectory] hold the piano and vocal mp3 backing tracks, named by
 *   the same hymn number.
 * - [hymnTitleDirectory] holds `HymnTitle.txt`, the number → title map the tab resolves each
 *   folder name against; a hymn with no entry there keeps an empty title.
 *
 * Blank means the folder is not set yet and the tab shows an empty list.
 */
@Serializable
data class HymnSettings(
    val imagesDirectory: String = "",
    val pianoDirectory: String = "",
    val vocalDirectory: String = "",
    val hymnTitleDirectory: String = "",
)
