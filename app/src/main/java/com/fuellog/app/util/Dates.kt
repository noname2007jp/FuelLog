package com.fuellog.app.util

import java.time.LocalDate

fun String.toLocalDateOrNull(): LocalDate? =
    try {
        LocalDate.parse(this)
    } catch (e: Exception) {
        null
    }
