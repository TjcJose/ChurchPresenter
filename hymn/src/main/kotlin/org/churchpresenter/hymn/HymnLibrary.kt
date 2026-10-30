package org.churchpresenter.hymn

import org.churchpresenter.settings.HymnSettings
import java.io.File

/**
 * Scans the four hymn folders into the library: one [HymnItem] per hymn's image folder, its title
 * resolved from `HymnTitle.txt`, its piano and vocal mp3s matched by number.
 *
 * The matching rule lives here and here only: a track file's *name* is its number — the token
 * before the first space, since tracks keep their titles ("001 Title.mp3" belongs to "001"). A
 * track is the file named exactly by that number, or — when the number carries text ("051(甲)") —
 * the file named by its leading digits ("051"), because a physical CD rip often keeps one
 * plain-number file for the siblings. `HymnTitle.txt` sits in its own folder; a missing or
 * unreadable file leaves every title blank rather than failing the scan.
 *
 * A hymn's slides are the image files in its folder, taken in file-name order — the same order the
 * Pictures tab loads a folder's images in.
 */
object HymnLibrary {

    const val TITLE_FILE_NAME = "HymnTitle.txt"

    /** The image extensions a hymn's slide folder is scanned for. */
    val IMAGE_EXTENSIONS: Set<String> = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif")

    private const val AUDIO_EXTENSION = "mp3"

    fun scan(settings: HymnSettings): List<HymnItem> {
        val titles = readTitleMap(settings.hymnTitleDirectory)
        return hymnFolders(settings.imagesDirectory)
            .map { folder ->
                val number = folder.name
                HymnItem(
                    number = number,
                    title = titles[number] ?: "",
                    imageFolder = folder,
                    pianoFile = matchAudio(settings.pianoDirectory, number),
                    vocalFile = matchAudio(settings.vocalDirectory, number),
                )
            }
            .sortedWith(compareBy({ numberKey(it.number) }, { it.number }))
    }

    fun readTitleMap(titleDirectory: String): Map<String, String> {
        if (titleDirectory.isBlank()) return emptyMap()
        val file = File(titleDirectory, TITLE_FILE_NAME)
        if (!file.isFile) return emptyMap()
        val text = try {
            file.readText()
        } catch (_: Exception) {
            return emptyMap()
        }
        return HymnTitleParser.parse(text)
    }

    /** The hymn folders in [imagesDirectory] (one per hymn, named by number), or empty when there
     *  is no usable folder. A folder with no images is skipped — it carries no slides. */
    internal fun hymnFolders(imagesDirectory: String): List<File> =
        listFiles(imagesDirectory) { it.isDirectory && it.hasImages() }

    /** Whether [this] folder holds at least one image a hymn's slides can be made from. */
    private fun File.hasImages(): Boolean =
        (listFiles { f -> f.isFile && f.extension.lowercase() in IMAGE_EXTENSIONS } ?: emptyArray()).any()

    /**
     * The mp3 matching [number] in [directory], or null when none does: the file whose name starts
     * with the number — the token before the first space ("001 Title.mp3" is the track for
     * "001") — first, then, only for a number that carries text, the file named by its leading
     * digits.
     */
    internal fun matchAudio(directory: String, number: String): File? {
        val byToken = listFiles(directory) { it.isFile && it.extension.equals(AUDIO_EXTENSION, ignoreCase = true) }
            .associateBy { it.nameWithoutExtension.substringBefore(' ') }
        byToken[number]?.let { return it }
        return if (number.any { !it.isDigit() }) byToken[number.takeWhile { it.isDigit() }] else null
    }

    /** The leading digits of the number, or a large value for a name with no leading digits. */
    internal fun numberKey(number: String): Int = number.takeWhile { it.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE

    private fun listFiles(directory: String, predicate: (File) -> Boolean): List<File> {
        if (directory.isBlank()) return emptyList()
        val dir = File(directory)
        if (!dir.isDirectory) return emptyList()
        return (dir.listFiles { f -> predicate(f) } ?: emptyArray()).toList()
    }
}
