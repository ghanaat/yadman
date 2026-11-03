package com.yadman.app.analysis

import java.time.DayOfWeek
import java.time.LocalDate

data class Message(
    val timestamp: Long,
    val fromMe: Boolean,
    val text: String,
)

/** فصل‌ها ماهانه‌اند اگر کل رابطه کوتاه باشد، وگرنه سه‌ماهه (بهار، تابستان، ...). */
enum class Granularity { MONTH, SEASON }

data class MonthActivity(
    val year: Int,
    val month: Int,
    val sent: Int,
    val received: Int,
) {
    val total: Int get() = sent + received
}

sealed interface TimelineEntry

data class Chapter(
    val title: String,
    val start: LocalDate,
    val end: LocalDate,
    val messages: List<Message>,
    val sentCount: Int,
    val keywords: List<String>,
    val keyMessages: List<Message>,
    val events: List<LifeEvent>,
    val busiestDay: LocalDate,
    val busiestDayCount: Int,
    val isPeak: Boolean,
) : TimelineEntry {
    val count: Int get() = messages.size
}

data class Silence(
    val from: LocalDate,
    val to: LocalDate,
    val days: Long,
) : TimelineEntry

enum class EventType(val emoji: String, val label: String) {
    BIRTHDAY("🎂", "تولد"),
    CONDOLENCE("🕯️", "تسلیت"),
    NOWRUZ("🌱", "عید"),
    WEDDING("💍", "عروسی و نامزدی"),
    BABY("👶", "بچه"),
    ACHIEVEMENT("🎓", "قبولی و موفقیت"),
    EMIGRATION("🌍", "مهاجرت"),
    MILITARY("🪖", "سربازی"),
    JOB("💼", "کار"),
    HEALTH("🏥", "بیماری و بیمارستان"),
    MOVING("🏠", "اسباب‌کشی"),
    TRAVEL("✈️", "سفر"),
    CONFLICT("🤝", "دلخوری و آشتی"),
    CONGRATS("🎉", "تبریک"),
}

data class LifeEvent(
    val type: EventType,
    val message: Message,
)

data class OnThisDay(
    val yearsAgo: Int,
    val date: LocalDate,
    val messages: List<Message>,
)

data class Stats(
    val total: Int,
    val sent: Int,
    val first: Message,
    val last: Message,
    val firstDate: LocalDate,
    val lastDate: LocalDate,
    val spanDays: Long,
    val activeDays: Int,
    val longestStreakDays: Int,
    val busiestDay: LocalDate,
    val busiestDayCount: Int,
    val longestSilenceDays: Long,
    val myMedianReplyMinutes: Double?,
    val theirMedianReplyMinutes: Double?,
    val conversations: Int,
    val iStartedPercent: Int,
    val peakHour: Int,
    val peakWeekday: DayOfWeek,
    val topEmojis: List<Pair<String, Int>>,
    val topWords: List<Pair<String, Int>>,
) {
    val received: Int get() = total - sent
}

data class Timeline(
    val stats: Stats,
    val activity: List<MonthActivity>,
    val granularity: Granularity,
    val entries: List<TimelineEntry>,
    val events: List<LifeEvent>,
    val onThisDay: List<OnThisDay>,
) {
    val chapters: List<Chapter> get() = entries.filterIsInstance<Chapter>()
}
