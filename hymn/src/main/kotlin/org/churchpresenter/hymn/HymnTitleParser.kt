package org.churchpresenter.hymn

/**
 * Turns a `HymnTitle.txt` into the number → title map the library resolves every hymn against.
 *
 * The file in the wild is a JavaScript array literal —
 *
 * ```
 * const groups = [
 *     '00 Title Zero',
 *     '001 Title One',
 *     ...
 * ];
 * ```
 *
 * — which is why the parser is built around quoted strings first and reads the bare line format
 * (`001 Title` per line) as a fallback: it must understand the file the church already
 * owns without being rewritten by hand.
 *
 * Each entry is `number title`, split on the *first* run of whitespace only, because a title may
 * contain commas and other punctuation. A number may carry text (`051(甲)`). Entries with no title
 * are dropped; when a number repeats (the real file spells `00` and `000` differently), the later
 * entry wins.
 */
object HymnTitleParser {

    private val QUOTED = Regex("'([^']*)'")
    private val FIRST_SPACE = Regex("\\s")

    fun parse(text: String): Map<String, String> =
        quotedEntries(text).ifEmpty { lineEntries(text) }
            .mapNotNull { splitEntry(it) }
            .toMap(LinkedHashMap())

    /** `number title` split on the first whitespace run, or null when either half is missing. */
    private fun splitEntry(entry: String): Pair<String, String>? {
        val split = FIRST_SPACE.find(entry) ?: return null
        val number = entry.substring(0, split.range.first).trim()
        val title = entry.substring(split.range.last + 1).trim()
        return if (number.isEmpty() || title.isEmpty()) null else number to title
    }

    /** The `'…'` strings of the JS array literal. */
    internal fun quotedEntries(text: String): List<String> =
        QUOTED.findAll(text).map { it.groupValues[1] }.toList()

    /** One entry per line, skipping the JS punctuation and comments. */
    internal fun lineEntries(text: String): List<String> = text.lineSequence()
        .map { it.trim() }
        .filter { line ->
            line.isNotEmpty() &&
                !line.startsWith("const") &&
                !line.startsWith("//") && !line.startsWith("#") &&
                !line.startsWith("[") && !line.startsWith("]") &&
                !line.startsWith(";") &&
                line.contains(FIRST_SPACE)
        }
        .toList()
}
