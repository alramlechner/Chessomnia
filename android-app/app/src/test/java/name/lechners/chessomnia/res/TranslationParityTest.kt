package name.lechners.chessomnia.res

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Guards English and its translations (German, Spanish) against drifting apart.
 *
 * This exists because nothing else catches it. `MissingTranslation` and
 * `ExtraTranslation` are lint checks that only run as part of
 * `lintVitalRelease`, and `app/build.gradle.kts` sets
 * `lint { checkReleaseBuilds = false }` — so on a release build they never run
 * at all. A pure-JVM test does not care about that setting and cannot be
 * skipped by accident.
 *
 * What it deliberately does *not* check: that every English string has a
 * translated one. Two keys are marked `translatable="false"` (the app name and
 * an empty placeholder) and correctly have no translation; the test derives
 * that set from the attribute rather than hard-coding the two names, so
 * marking a third key untranslatable does not require touching this file.
 */
class TranslationParityTest {

    private val res = File("src/main/res")

    private data class Strings(
        val translatable: Map<String, String>,
        val untranslatable: Set<String>,
        val plurals: Map<String, Set<String>>,
    )

    private fun parse(dir: String): Strings {
        val file = File(res, "$dir/strings.xml")
        assertTrue("missing $file", file.isFile)
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val strings = mutableMapOf<String, String>()
        val untranslatable = mutableSetOf<String>()
        val plurals = mutableMapOf<String, Set<String>>()

        val children = doc.documentElement.childNodes
        for (i in 0 until children.length) {
            val el = children.item(i) as? Element ?: continue
            val name = el.getAttribute("name")
            when (el.tagName) {
                "string" ->
                    if (el.getAttribute("translatable") == "false") untranslatable += name
                    else strings[name] = el.textContent
                "plurals" -> {
                    val items = el.getElementsByTagName("item")
                    plurals[name] = (0 until items.length)
                        .map { (items.item(it) as Element).getAttribute("quantity") }
                        .toSet()
                }
            }
        }
        return Strings(strings, untranslatable, plurals)
    }

    /** `%s`, `%d`, `%1$s` … — the set a string requires its caller to supply. */
    private fun placeholders(value: String): List<String> =
        Regex("%(?:\\d+\\\$)?[a-zA-Z]").findAll(value).map { it.value }.sorted().toList()

    private val en by lazy { parse("values") }
    private val translations by lazy { mapOf("de" to parse("values-de"), "es" to parse("values-es")) }

    @Test
    fun everyTranslatableStringHasACounterpartInEachTranslation() {
        translations.forEach { (locale, t) ->
            assertEquals(
                "$locale is missing translations",
                emptySet<String>(),
                en.translatable.keys - t.translatable.keys,
            )
        }
    }

    @Test
    fun noTranslationHasStringsEnglishDoesNotHave() {
        // Catches a rename applied to only one of the files: the old key
        // lingers in the translation and the new one silently falls back to
        // English.
        translations.forEach { (locale, t) ->
            assertEquals(
                "$locale has strings absent from English",
                emptySet<String>(),
                t.translatable.keys - en.translatable.keys,
            )
        }
    }

    @Test
    fun untranslatableStringsAreNotTranslated() {
        translations.forEach { (locale, t) ->
            assertEquals(
                "these are marked translatable=false but a $locale version exists",
                emptySet<String>(),
                en.untranslatable intersect t.translatable.keys,
            )
        }
    }

    @Test
    fun placeholdersMatch() {
        // A translated string with a placeholder the English one does not have
        // throws IllegalFormatException at runtime — on a device in that
        // language only, which is exactly the kind of bug that ships.
        translations.forEach { (locale, t) ->
            val mismatched = en.translatable.keys.intersect(t.translatable.keys).filter {
                placeholders(en.translatable.getValue(it)) != placeholders(t.translatable.getValue(it))
            }
            assertEquals("$locale: format placeholders differ", emptyList<String>(), mismatched)
        }
    }

    @Test
    fun pluralsHaveTheSameQuantities() {
        translations.forEach { (locale, t) ->
            assertEquals("plurals missing in $locale", en.plurals.keys, t.plurals.keys)
            val mismatched = en.plurals.keys.filter { en.plurals[it] != t.plurals[it] }
            assertEquals("$locale: plural quantities differ", emptyList<String>(), mismatched)
        }
        // English, German and Spanish all pluralise the same way here, so every
        // language needs exactly one/other.
        en.plurals.forEach { (name, quantities) ->
            assertEquals("$name: unexpected quantities", setOf("one", "other"), quantities)
        }
    }

    @Test
    fun allLicenceFilesExist() {
        // The licences screen renders res/raw/licenses.txt verbatim. Without a
        // translated variant, a user taps a button in their language and lands
        // on an English page — which was the case until this test was written.
        (listOf("raw") + translations.keys.map { "raw-$it" }).forEach {
            val path = "$it/licenses.txt"
            assertTrue("missing $path", File(res, path).isFile)
        }
    }
}
