package com.yadman.app.analysis

import java.time.DayOfWeek
import java.time.LocalDate

data class JalaliDate(val year: Int, val month: Int, val day: Int)

object Jalali {
    private val monthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )
    private val seasonNames = listOf("بهار", "تابستان", "پاییز", "زمستان")
    private val gregorianDaysBeforeMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    fun monthName(month: Int): String = monthNames[month - 1]

    fun seasonName(month: Int): String = seasonNames[(month - 1) / 3]

    fun weekdayName(day: DayOfWeek): String = when (day) {
        DayOfWeek.SATURDAY -> "شنبه"
        DayOfWeek.SUNDAY -> "یکشنبه"
        DayOfWeek.MONDAY -> "دوشنبه"
        DayOfWeek.TUESDAY -> "سه‌شنبه"
        DayOfWeek.WEDNESDAY -> "چهارشنبه"
        DayOfWeek.THURSDAY -> "پنجشنبه"
        DayOfWeek.FRIDAY -> "جمعه"
    }

    fun of(date: LocalDate): JalaliDate = fromGregorian(date.year, date.monthValue, date.dayOfMonth)

    /** الگوریتم حسابی رایج (چرخه‌ی ۳۳ ساله) برای تبدیل میلادی به شمسی. */
    fun fromGregorian(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 +
            gd + gregorianDaysBeforeMonth[gm - 1]
        var jy = -1595 + 33 * (days / 12053)
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        return if (days < 186) {
            JalaliDate(jy, 1 + days / 31, 1 + days % 31)
        } else {
            JalaliDate(jy, 7 + (days - 186) / 30, 1 + (days - 186) % 30)
        }
    }
}
