package com.fuellog.app.util

import java.util.Locale

object Formatters {
    fun fmt1(v: Double): String = String.format(Locale.US, "%.1f", v)
    fun fmt0(v: Double): String = String.format(Locale.US, "%,.0f", v)
    /** CSV用(桁区切りなし) */
    fun fmt0plain(v: Double): String = String.format(Locale.US, "%.0f", v)
    fun money(v: Int): String = String.format(Locale.US, "%,d", v)
    /** 入力欄表示用: 整数なら小数点を付けない */
    fun editable(v: Double): String =
        if (v % 1.0 == 0.0) v.toLong().toString() else fmt1(v)
}
