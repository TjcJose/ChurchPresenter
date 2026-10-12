package org.churchpresenter.hymn

import java.io.File

/**
 * One hymn of the library.
 *
 * [number] is the PPT file's name without its extension — "129", "051(甲)" — and [title] is what
 * `HymnTitle.txt` maps it to, or "" when there is no entry. [pptFile] is null only while the PPT
 * folder is missing or empty; [pianoFile]/[vocalFile] are null when no mp3 matches the number.
 */
data class HymnItem(
    val number: String,
    val title: String,
    val pptFile: File?,
    val pianoFile: File?,
    val vocalFile: File?,
) {
    val hasSlides: Boolean get() = pptFile != null
    val hasPiano: Boolean get() = pianoFile != null
    val hasVocal: Boolean get() = vocalFile != null
}
