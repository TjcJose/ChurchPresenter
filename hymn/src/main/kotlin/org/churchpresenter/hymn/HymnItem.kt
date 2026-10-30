package org.churchpresenter.hymn

import java.io.File

/**
 * One hymn of the library.
 *
 * [number] is the hymn's folder name — "129", "051(甲)" — and [title] is what `HymnTitle.txt` maps
 * it to, or "" when there is no entry. [imageFolder] is the folder of slide images named by that
 * number; it is null only while the images folder is missing or empty. [pianoFile]/[vocalFile]
 * are null when no mp3 matches the number.
 */
data class HymnItem(
    val number: String,
    val title: String,
    val imageFolder: File?,
    val pianoFile: File?,
    val vocalFile: File?,
) {
    val hasSlides: Boolean get() = imageFolder != null
    val hasPiano: Boolean get() = pianoFile != null
    val hasVocal: Boolean get() = vocalFile != null
}
