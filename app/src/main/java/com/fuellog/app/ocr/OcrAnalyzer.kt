package com.fuellog.app.ocr

import com.fuellog.app.util.ReceiptDateParser
import java.time.LocalDate
import kotlin.math.roundToLong

/** OCRテキストから給油データ・メーター値を抽出する。 */
object OcrAnalyzer {
    data class DashboardResult(
        val odometerKm: Double? = null,
        val tripAKm: Double? = null,
        val tripBKm: Double? = null,
        val vehicleFuelEconomyKmPerLiter: Double? = null
    )

    data class ReceiptResult(
        val fuelLiters: Double? = null,
        val costYen: Int? = null,
        val unitPrice: Double? = null,
        val receiptDate: LocalDate? = null
    )

    private val numberRegex = Regex("""(\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d+(?:\.\d+)?)""")

    fun parseReceipt(text: String, today: LocalDate = LocalDate.now()): ReceiptResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var liters = findNumberNear(lines, listOf("給油量", "数量", "リットル"), preferDecimal = true)
        val rawAmount = findNumberNear(lines, listOf("合計", "金額", "お買上", "お買い上げ", "¥", "円"), preferDecimal = false)
        var amount = rawAmount?.takeIf { it >= 500 }?.toInt()
        var unitPrice = findNumberNear(lines, listOf("単価", "税込単価"), preferDecimal = true)

        if (liters == null) {
            liters = allNumbers(text).filter { it.contains('.') }
                .mapNotNull { it.replace(",", "").toDoubleOrNull() }
                .firstOrNull { it in 5.0..99.99 }
        }
        if (amount == null) {
            amount = allNumbers(text).mapNotNull { it.replace(",", "").toDoubleOrNull() }
                .filter { it % 1.0 == 0.0 && it in 1000.0..100000.0 }.maxOrNull()?.toInt()
        }
        if (unitPrice == null && amount != null && liters != null && liters > 0) {
            unitPrice = amount / liters
        }
        return ReceiptResult(liters, amount, unitPrice, ReceiptDateParser.parse(text, today))
    }

    /** オドメーター(総走行距離): 1,000km以上の最大値を採用 */
    fun parseOdometer(text: String): Double? =
        allNumbers(text).mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .filter { it >= 1000.0 }.maxOrNull()

    /**
     * Dashboard OCR uses labels where possible so Trip A and Trip B are not confused.
     * Odometer is an integer display; Trip B can supply its decimal digit when its
     * four-digit integer part approximately matches the odometer's last four digits.
     */
    fun parseDashboard(text: String): DashboardResult {
        // 車載メーターの表示形式を優先する。NFKCで全角英数字・小数点を半角にそろえる。
        val normalizedText = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
        val lines = normalizedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        fun valueNear(labels: List<String>, predicate: (Double) -> Boolean): Double? {
            for (i in lines.indices) {
                val line = lines[i].uppercase()
                if (labels.any { line.contains(it.uppercase()) }) {
                    val candidates = (i..minOf(i + 2, lines.lastIndex))
                        .flatMap { allNumbers(lines[it]) }
                        .mapNotNull { it.replace(",", "").toDoubleOrNull() }
                    candidates.firstOrNull(predicate)?.let { return it }
                }
            }
            return null
        }

        fun formattedValueNear(
            labelPattern: Regex,
            valuePattern: Regex,
            convert: (String) -> Double?
        ): Double? {
            for (i in lines.indices) {
                if (labelPattern.containsMatchIn(lines[i])) {
                    val candidate = (i..minOf(i + 2, lines.lastIndex))
                        .asSequence()
                        .mapNotNull { lineIndex -> valuePattern.find(lines[lineIndex])?.value }
                        .mapNotNull(convert)
                        .firstOrNull()
                    if (candidate != null) return candidate
                }
            }
            return null
        }

        // ODO: 6桁の整数表示（例: 012345）。先頭ゼロも桁数判定の段階では保持する。
        val formattedOdometer = formattedValueNear(
            Regex("""(?i)ODO|オド|総走行"""),
            Regex("""(?<!\d)\d{6}(?!\d)""")
        ) { it.toDoubleOrNull() }
        val labeledOdometer = formattedOdometer
            // ODOラベルの近くにTrip Bの数値がある場合、そちらを誤採用しないよう6桁整数だけに限定する。
            ?: Regex("""(?<!\d)\d{6}(?![\d.])""")
                .findAll(normalizedText)
                .mapNotNull { it.value.toDoubleOrNull() }
                .maxOrNull()

        // Trip A/B: A/Bラベルに続く4桁整数+小数1桁（例: A 0000.0）。
        val tripAFormat = formattedValueNear(
            Regex("""(?i)(?:^\s*A(?!\s*\d{2}\.\d\s*KM\s*/\s*L)(?:\s|[:：]|$)|\bTRIP\s*-?\s*A\b|\bA\s*TRIP\b|トリップ\s*A)"""),
            Regex("""(?<!\d)\d{1,4}\.\d(?!\d)""")
        ) { it.toDoubleOrNull() }
        val tripBValuePattern = Regex("""(?<!\d)\d{1,4}\.\d(?!\d)""")
        val tripBFormat = formattedValueNear(
            Regex("""(?i)(?:^\s*B(?:\s|[:：]|$)|\bTRIP\s*-?\s*B\b|\bB\s*TRIP\b|トリップ\s*B)"""),
            tripBValuePattern
        ) { it.toDoubleOrNull() } ?: run {
            // OCRが「TRIP」と「B」を別行に分割したケースも扱う。
            var splitLabelValue: Double? = null
            for (i in lines.indices) {
                if (!Regex("""(?i)^\s*TRIP\s*$""").containsMatchIn(lines[i])) continue
                val bIndex = (i + 1..minOf(i + 2, lines.lastIndex)).firstOrNull { index ->
                    Regex("""(?i)^\s*B\s*[:：]?\s*$""").containsMatchIn(lines[index])
                } ?: continue
                splitLabelValue = (bIndex..minOf(bIndex + 2, lines.lastIndex))
                    .asSequence()
                    .mapNotNull { index -> tripBValuePattern.find(lines[index])?.value?.toDoubleOrNull() }
                    .firstOrNull()
                if (splitLabelValue != null) break
            }
            splitLabelValue
        }
        val tripA = tripAFormat ?: valueNear(
            listOf("TRIP A", "TRIP-A", "TRIPA", "トリップA", "トリップ Ａ", "A TRIP"),
            { it in 0.0..9999999.9 }
        )
        val tripB = tripBFormat ?: valueNear(
            listOf("TRIP B", "TRIP-B", "TRIPB", "トリップB", "トリップ Ｂ", "B TRIP"),
            { it in 0.0..9999999.9 }
        )

        // If A/B labels were found, do not mistake their long readings for the odometer.
        val odoRaw = (labeledOdometer ?: if (tripA == null && tripB == null) parseOdometer(normalizedText) else null)
            ?.toLong()?.toDouble()
        // 車載燃費: 00.0 km/L。00.0も形式上有効なので0.0以上を許可する。
        val vehicleEconomy = formattedValueNear(
            Regex("""(?i)AVG|平均燃費|燃費|KM\s*/\s*L"""),
            Regex("""(?<!\d)\d{2}\.\d(?!\d)""")
        ) { it.toDoubleOrNull() } ?: valueNear(
            listOf("AVG", "平均燃費", "燃費", "KM/L", "KM／L"),
            { it in 0.0..99.9 }
        )
        val mergedOdometer = odoRaw?.let { mergeOdometerWithTripBDecimal(it, tripB) }
        return DashboardResult(mergedOdometer, tripA, tripB, vehicleEconomy)
    }

    /**
     * ODOの下4桁とTrip Bの整数部4桁を比較し、小数部をODOに補完する。
     * OCR誤読を許容して、4桁中1桁までの不一致なら一致とみなす。
     * 条件を満たさない場合はODOの整数値をそのまま返す。
     */
    fun mergeOdometerWithTripBDecimal(odometerInteger: Double, tripB: Double?): Double {
        val odoWhole = odometerInteger.toLong()
        if (tripB == null || odoWhole < 1000L) return odoWhole.toDouble()

        // Trip BはODO小数部の補完専用。末尾4桁の照合で弾かず、読み取った小数第1位を必ず採用する。
        val decimal = kotlin.math.abs((tripB * 10.0).roundToLong() % 10L)
        return odoWhole.toDouble() + decimal / 10.0
    }

    /**
     * 旧APIとの互換性維持用。ダッシュボード解析ではTrip B版を使用する。
     */
    fun mergeOdometerWithTripADecimal(odometerInteger: Double, tripA: Double?): Double {
        val odoWhole = odometerInteger.toLong()
        if (tripA == null || odoWhole < 1000L) return odoWhole.toDouble()
        val tripWhole = tripA.toLong()
        if (odoWhole / 1000L != tripWhole / 1000L) return odoWhole.toDouble()
        val decimal = ((tripA * 10).toLong() % 10).coerceIn(0L, 9L)
        return odoWhole.toDouble() + decimal / 10.0
    }

    /** ROIで整数部と小数部を別撮りした場合の数字連結。小数は1桁に限定する。 */
    fun combineOdometer(integerText: String, decimalText: String): Double? {
        val integerDigits = integerText.filter(Char::isDigit)
        val decimalDigit = decimalText.firstOrNull(Char::isDigit) ?: return null
        if (integerDigits.isEmpty()) return null
        return "$integerDigits.$decimalDigit".toDoubleOrNull()
    }

    /** メーターB用。ラベルがOCRで欠落した場合も、4桁以下+小数1桁の表示だけを候補にする。 */
    fun parseTripB(text: String): Double? =
        Regex("""(?<!\d)\d{1,4}\.\d(?!\d)""")
            .find(text)?.value?.toDoubleOrNull()

    /** トリップメーター(区間距離): 小数点付きの数値を優先 */
    fun parseTrip(text: String): Double? {
        val tokens = allNumbers(text)
        val withDecimal = tokens.firstOrNull { s ->
            val v = s.replace(",", "").toDoubleOrNull()
            s.contains('.') && v != null && v in 1.0..9999.9
        }
        if (withDecimal != null) return withDecimal.replace(",", "").toDoubleOrNull()
        return tokens.mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .firstOrNull { it in 1.0..9999.0 }
    }

    private fun allNumbers(text: String): List<String> =
        numberRegex.findAll(text).map { it.value }.toList()

    private fun findNumberNear(lines: List<String>, keywords: List<String>, preferDecimal: Boolean): Double? {
        for (line in lines) {
            if (keywords.any { line.contains(it) }) {
                val nums = allNumbers(line).mapNotNull { it.replace(",", "").toDoubleOrNull() }
                if (preferDecimal) nums.firstOrNull { it % 1.0 != 0.0 }?.let { return it }
                nums.maxOrNull()?.let { return it }
            }
        }
        return null
    }
}