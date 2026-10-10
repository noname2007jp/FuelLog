package com.fuellog.app.util

import java.time.LocalDate
import java.time.chrono.JapaneseDate
import java.time.chrono.JapaneseEra
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

enum class DateOrigin(val label: String) {
    RECEIPT("レシート"),
    EXIF("写真Exif"),
    TODAY("今日"),
    MANUAL("手動修正")
}

/** レシートのOCRテキストから日付を抽出する。 */
object ReceiptDateParser {
    private val western = Regex("""(?<!\d)(\d{2,4})\s*[/.-]\s*(\d{1,2})\s*[/.-]\s*(\d{1,2})(?!\d)""")
    private val japanese = Regex("""(令和|平成|昭和|R|H|S)\s*([元\d]{1,3})\s*年?\s*[./年-]?\s*(\d{1,2})\s*[./月-]?\s*(\d{1,2})\s*日?""", RegexOption.IGNORE_CASE)
    private val yearKanji = Regex("""(\d{4})\s*年\s*(\d{1,2})\s*月\s*(\d{1,2})\s*日""")

    fun parse(text: String, today: LocalDate = LocalDate.now()): LocalDate? {
        val normalized = text
            .replace('０', '0').replace('１', '1').replace('２', '2').replace('３', '3')
            .replace('４', '4').replace('５', '5').replace('６', '6').replace('７', '7')
            .replace('８', '8').replace('９', '9')
            .replace('／', '/').replace('－', '-').replace('．', '.')
        val candidates = mutableListOf<LocalDate>()

        western.findAll(normalized).forEach { m ->
            val rawYear = m.groupValues[1].toIntOrNull() ?: return@forEach
            val year = normalizeYear(rawYear)
            makeDate(year, m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let(candidates::add)
        }
        yearKanji.findAll(normalized).forEach { m ->
            makeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let(candidates::add)
        }
        japanese.findAll(normalized).forEach { m ->
            val era = m.groupValues[1].uppercase(Locale.ROOT)
            val eraYearText = m.groupValues[2]
            val eraYear = if (eraYearText == "元") 1 else eraYearText.toIntOrNull() ?: return@forEach
            val base = when (era) {
                "令和", "R" -> 2018
                "平成", "H" -> 1988
                "昭和", "S" -> 1925
                else -> return@forEach
            }
            makeDate(base + eraYear, m.groupValues[3].toInt(), m.groupValues[4].toInt())?.let(candidates::add)
        }
        return candidates.firstOrNull { it.year >= 1971 && !it.isAfter(today) }
    }

    private fun normalizeYear(year: Int): Int = when {
        year >= 100 -> year
        year <= 79 -> 2000 + year
        else -> 1900 + year
    }

    private fun makeDate(year: Int, month: Int, day: Int): LocalDate? =
        try { LocalDate.of(year, month, day) } catch (_: Exception) { null }
}
