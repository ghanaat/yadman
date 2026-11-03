package com.yadman.app.ui

import com.yadman.app.analysis.Jalali
import com.yadman.app.analysis.PersianText
import com.yadman.app.analysis.fa
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

/** نمایش تاریخ‌ها (شمسی)، ساعت‌ها و مدت‌ها با رقم فارسی. */
object Fmt {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun localDate(timestamp: Long): LocalDate = Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    fun date(d: LocalDate): String {
        val j = Jalali.of(d)
        return "${fa(j.day)} ${Jalali.monthName(j.month)} ${fa(j.year)}"
    }

    fun date(timestamp: Long): String = date(localDate(timestamp))

    fun dayWithWeekday(d: LocalDate): String = "${Jalali.weekdayName(d.dayOfWeek)} ${date(d)}"

    fun time(timestamp: Long): String {
        val t = Instant.ofEpochMilli(timestamp).atZone(zone)
        return PersianText.toPersianDigits(String.format(Locale.US, "%02d:%02d", t.hour, t.minute))
    }

    fun dateTime(timestamp: Long): String = "${date(timestamp)}، ${time(timestamp)}"

    fun range(from: LocalDate, to: LocalDate): String =
        if (from == to) date(from) else "${date(from)} تا ${date(to)}"

    fun years(fromTimestamp: Long, toTimestamp: Long): String {
        val a = Jalali.of(localDate(fromTimestamp)).year
        val b = Jalali.of(localDate(toTimestamp)).year
        return if (a == b) fa(a) else "${fa(a)} تا ${fa(b)}"
    }

    fun duration(days: Long): String {
        val years = days / 365
        val months = (days % 365) / 30
        return when {
            years > 0 && months > 0 -> "${fa(years)} سال و ${fa(months)} ماه"
            years > 0 -> "${fa(years)} سال"
            months > 0 -> "${fa(months)} ماه"
            else -> "${fa(days)} روز"
        }
    }

    fun minutes(m: Double): String = when {
        m < 1 -> "کمتر از یک دقیقه"
        m < 60 -> "${fa(m.roundToInt())} دقیقه"
        m < 24 * 60 -> "${fa((m / 60).roundToInt())} ساعت"
        else -> "${fa((m / (24 * 60)).roundToInt())} روز"
    }
}
