package com.fuellog.app.ocr

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** ROI設定をSharedPreferencesへ保存する。座標は画像に対する相対比率。 */
class RoiStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ocr_roi", Context.MODE_PRIVATE)

    fun load(): List<NormalizedRoi> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val target = RoiTarget.entries.firstOrNull { it.name == item.optString("target") }
                        ?: continue
                    val left = item.optDouble("left", Double.NaN).toFloat()
                    val top = item.optDouble("top", Double.NaN).toFloat()
                    val right = item.optDouble("right", Double.NaN).toFloat()
                    val bottom = item.optDouble("bottom", Double.NaN).toFloat()
                    if (listOf(left, top, right, bottom).all { it.isFinite() } &&
                        left in 0f..1f && top in 0f..1f && right in 0f..1f &&
                        bottom in 0f..1f && right > left && bottom > top) {
                        add(NormalizedRoi(target, left, top, right, bottom))
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(rois: List<NormalizedRoi>) {
        val array = JSONArray()
        rois.forEach { roi ->
            array.put(JSONObject().apply {
                put("target", roi.target.name)
                put("left", roi.left.toDouble())
                put("top", roi.top.toDouble())
                put("right", roi.right.toDouble())
                put("bottom", roi.bottom.toDouble())
            })
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    companion object {
        private const val KEY = "regions_v1"
    }
}
