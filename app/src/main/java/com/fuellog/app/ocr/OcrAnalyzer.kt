package com.fuellog.app.ocr

/** OCRテキストから給油データ・メーター値を抽出する。 */
object OcrAnalyzer {

    data class ReceiptResult(
        val fuelLiters: Double? = null,
        val costYen: Int? = null,
        val unitPrice: Double? = null
    )

    private val numberRegex = Regex("""(\d{1,3}(?:,\d{3})+(?:\.\d+)?|\d+(?:\.\d+)?)""")

    fun parseReceipt(text: String): ReceiptResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var liters = findNumberNear(lines, listOf("給油量", "数量", "リットル"), preferDecimal = true)

        val rawAmount = findNumberNear(lines, listOf("合計", "金額", "お買上", "お買い上げ", "¥", "円"), preferDecimal = false)
        var amount = rawAmount?.takeIf { it >= 500 }?.toInt()

        var unitPrice = findNumberNear(lines, listOf("単価", "税込単価"), preferDecimal = true)

        if (liters == null) {
            liters = allNumbers(text)
                .filter { it.contains('.') }
                .mapNotNull { it.replace(",", "").toDoubleOrNull() }
                .firstOrNull { it in 5.0..99.9 }
        }
        if (amount == null) {
            amount = allNumbers(text)
                .mapNotNull { it.replace(",", "").toDoubleOrNull() }
                .filter { it % 1.0 == 0.0 && it in 1000.0..100000.0 }
                .maxOrNull()?.toInt()
        }
        if (unitPrice == null && amount != null && liters != null && liters > 0) {
            unitPrice = amount / liters
        }
        return ReceiptResult(liters, amount, unitPrice)
    }

    /** オドメーター(総走行距離): 1,000km以上の最大値を採用 */
    fun parseOdometer(text: String): Double? =
        allNumbers(text)
            .mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .filter { it >= 1000.0 }
            .maxOrNull()

    /** トリップメーター(区間距離): 小数点付きの数値を優先 */
    fun parseTrip(text: String): Double? {
        val tokens = allNumbers(text)
        val withDecimal = tokens.firstOrNull { s ->
            val v = s.replace(",", "").toDoubleOrNull()
            s.contains('.') && v != null && v in 1.0..9999.9
        }
        if (withDecimal != null) return withDecimal.replace(",", "").toDoubleOrNull()
        return tokens
            .mapNotNull { it.replace(",", "").toDoubleOrNull() }
            .firstOrNull { it in 1.0..9999.0 }
    }

    private fun allNumbers(text: String): List<String> =
        numberRegex.findAll(text).map { it.value }.toList()

    private fun findNumberNear(
        lines: List<String>,
        keywords: List<String>,
        preferDecimal: Boolean
    ): Double? {
        for (line in lines) {
            if (keywords.any { line.contains(it) }) {
                val nums = allNumbers(line).mapNotNull { it.replace(",", "").toDoubleOrNull() }
                if (preferDecimal) {
                    nums.firstOrNull { it % 1.0 != 0.0 }?.let { return it }
                }
                nums.maxOrNull()?.let { return it }
            }
        }
        return null
    }
}
