package com.yadman.app.analysis

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * کل گفتگو با یک نفر را به خط زمان تبدیل می‌کند: آمار کلی، فعالیت ماه‌به‌ماه، فصل‌ها با
 * کلمه‌های کلیدی و پیام‌های مهم، دوره‌های سکوت، رویدادها و «امروز در سال‌های قبل».
 * همه‌چیز آفلاین و بدون مدل زبانی است.
 */
class TimelineAnalyzer(
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: LocalDate = LocalDate.now(zone),
) {
    private class Item(
        val message: Message,
        val date: LocalDate,
        val jalali: JalaliDate,
        val tokens: List<String>,
    )

    fun analyze(input: List<Message>): Timeline? {
        val items = input
            .filter { it.text.isNotBlank() }
            .sortedBy { it.timestamp }
            .map { m ->
                val date = Instant.ofEpochMilli(m.timestamp).atZone(zone).toLocalDate()
                Item(m, date, Jalali.of(date), PersianText.tokens(m.text))
            }
        if (items.isEmpty()) return null

        val events = EventDetector.detect(items.map { it.message })
        val granularity = chooseGranularity(items)
        val chapters = buildChapters(items, granularity, events)
        return Timeline(
            stats = buildStats(items),
            activity = buildActivity(items),
            granularity = granularity,
            entries = withSilences(chapters),
            events = events,
            onThisDay = onThisDay(items),
        )
    }

    private fun monthIndex(j: JalaliDate) = j.year * 12 + j.month - 1

    private fun chooseGranularity(items: List<Item>): Granularity {
        val months = monthIndex(items.last().jalali) - monthIndex(items.first().jalali) + 1
        return if (months <= MAX_MONTHS_FOR_MONTHLY) Granularity.MONTH else Granularity.SEASON
    }

    private fun periodKey(j: JalaliDate, g: Granularity) = when (g) {
        Granularity.MONTH -> monthIndex(j)
        Granularity.SEASON -> j.year * 4 + (j.month - 1) / 3
    }

    private fun periodTitle(j: JalaliDate, g: Granularity) = when (g) {
        Granularity.MONTH -> "${Jalali.monthName(j.month)} ${fa(j.year)}"
        Granularity.SEASON -> "${Jalali.seasonName(j.month)} ${fa(j.year)}"
    }

    private fun buildChapters(items: List<Item>, g: Granularity, events: List<LifeEvent>): List<Chapter> {
        val groups = items.groupBy { periodKey(it.jalali, g) }.values.toList()

        val counts = groups.map { group ->
            val c = HashMap<String, Int>()
            for (item in group) for (t in item.tokens) c[t] = (c[t] ?: 0) + 1
            c
        }
        val documentFrequency = HashMap<String, Int>()
        for (c in counts) for (t in c.keys) documentFrequency[t] = (documentFrequency[t] ?: 0) + 1
        val n = groups.size.toDouble()

        val chapters = groups.mapIndexed { i, group ->
            // TF-IDF: کلمه‌هایی که در این دوره زیاد آمده‌اند ولی در بقیه‌ی دوره‌ها کم.
            val total = counts[i].values.sum().coerceAtLeast(1)
            val minCount = if (total >= 60) 2 else 1
            val ranked = counts[i].entries
                .filter { it.value >= minCount }
                .map { (word, count) ->
                    val idf = ln((n + 1.0) / (documentFrequency.getValue(word) + 1.0)) + 1.0
                    word to (count.toDouble() / total) * idf
                }
                .sortedWith(compareByDescending<Pair<String, Double>> { it.second }.thenByDescending { it.first.length })

            val first = group.first()
            val last = group.last()
            val busiest = group.groupingBy { it.date }.eachCount().maxBy { it.value }
            Chapter(
                title = periodTitle(first.jalali, g),
                start = first.date,
                end = last.date,
                messages = group.map { it.message },
                sentCount = group.count { it.message.fromMe },
                keywords = ranked.take(KEYWORDS_PER_CHAPTER).map { it.first },
                keyMessages = pickKeyMessages(group, ranked.take(15).toMap()),
                events = events.filter { it.message.timestamp in first.message.timestamp..last.message.timestamp },
                busiestDay = busiest.key,
                busiestDayCount = busiest.value,
                isPeak = false,
            )
        }

        if (chapters.size < 3) return chapters
        val peak = chapters.maxBy { it.count }
        return chapters.map { if (it === peak) it.copy(isPeak = true) else it }
    }

    /** پیام‌هایی که بیشترین کلمه‌های مهم دوره را دارند؛ پیام‌های یک رگبار ده‌دقیقه‌ای فقط یک نماینده دارند. */
    private fun pickKeyMessages(group: List<Item>, weights: Map<String, Double>): List<Message> {
        val wanted = when {
            group.size >= 20 -> 3
            group.size >= 5 -> 2
            else -> 1
        }
        val scored = group
            .filter { it.message.text.length in 12..400 && it.tokens.isNotEmpty() }
            .map { item ->
                val score = item.tokens.toSet().sumOf { weights[it] ?: 0.0 } / sqrt(max(item.tokens.size, 4).toDouble())
                item to score
            }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }

        val chosen = ArrayList<Item>()
        for ((item, _) in scored) {
            if (chosen.size >= wanted) break
            val tooClose = chosen.any {
                it.message.text == item.message.text ||
                    kotlin.math.abs(it.message.timestamp - item.message.timestamp) < BURST_MS
            }
            if (!tooClose) chosen.add(item)
        }
        if (chosen.isEmpty()) {
            chosen += group.sortedByDescending { it.message.text.length }.take(wanted)
        }
        return chosen.sortedBy { it.message.timestamp }.map { it.message }
    }

    private fun withSilences(chapters: List<Chapter>): List<TimelineEntry> {
        val out = ArrayList<TimelineEntry>()
        chapters.forEachIndexed { i, chapter ->
            if (i > 0) {
                val previous = chapters[i - 1]
                val days = ChronoUnit.DAYS.between(previous.end, chapter.start)
                if (days >= SILENCE_DAYS) out.add(Silence(previous.end, chapter.start, days))
            }
            out.add(chapter)
        }
        return out
    }

    private fun buildActivity(items: List<Item>): List<MonthActivity> {
        val byMonth = items.groupBy { monthIndex(it.jalali) }
        val from = monthIndex(items.first().jalali)
        val to = monthIndex(items.last().jalali)
        return (from..to).map { index ->
            val month = byMonth[index].orEmpty()
            val sent = month.count { it.message.fromMe }
            MonthActivity(index / 12, index % 12 + 1, sent, month.size - sent)
        }
    }

    private fun buildStats(items: List<Item>): Stats {
        val first = items.first()
        val last = items.last()
        val days = items.map { it.date }.distinct()

        var longestStreak = 1
        var streak = 1
        for (i in 1 until days.size) {
            streak = if (ChronoUnit.DAYS.between(days[i - 1], days[i]) == 1L) streak + 1 else 1
            longestStreak = max(longestStreak, streak)
        }

        val busiest = items.groupingBy { it.date }.eachCount().maxBy { it.value }

        var longestSilence = 0L
        val myReplies = ArrayList<Double>()
        val theirReplies = ArrayList<Double>()
        var conversations = 1
        var iStarted = if (first.message.fromMe) 1 else 0
        for (i in 1 until items.size) {
            val a = items[i - 1]
            val b = items[i]
            val gap = b.message.timestamp - a.message.timestamp
            longestSilence = max(longestSilence, ChronoUnit.DAYS.between(a.date, b.date))
            if (gap >= NEW_CONVERSATION_MS) {
                conversations++
                if (b.message.fromMe) iStarted++
            } else if (a.message.fromMe != b.message.fromMe && gap <= MAX_REPLY_MS) {
                (if (b.message.fromMe) myReplies else theirReplies).add(gap / 60_000.0)
            }
        }

        val hours = IntArray(24)
        val weekdays = HashMap<DayOfWeek, Int>()
        val emojis = HashMap<String, Int>()
        val words = HashMap<String, Int>()
        for (item in items) {
            val time = Instant.ofEpochMilli(item.message.timestamp).atZone(zone)
            hours[time.hour]++
            weekdays[time.dayOfWeek] = (weekdays[time.dayOfWeek] ?: 0) + 1
            for (e in PersianText.emojis(item.message.text)) emojis[e] = (emojis[e] ?: 0) + 1
            for (t in item.tokens) words[t] = (words[t] ?: 0) + 1
        }

        return Stats(
            total = items.size,
            sent = items.count { it.message.fromMe },
            first = first.message,
            last = last.message,
            firstDate = first.date,
            lastDate = last.date,
            spanDays = ChronoUnit.DAYS.between(first.date, last.date) + 1,
            activeDays = days.size,
            longestStreakDays = longestStreak,
            busiestDay = busiest.key,
            busiestDayCount = busiest.value,
            longestSilenceDays = longestSilence,
            myMedianReplyMinutes = median(myReplies),
            theirMedianReplyMinutes = median(theirReplies),
            conversations = conversations,
            iStartedPercent = (iStarted * 100.0 / conversations).roundToInt(),
            peakHour = hours.indices.maxBy { hours[it] },
            peakWeekday = weekdays.maxBy { it.value }.key,
            topEmojis = emojis.entries.sortedByDescending { it.value }.take(8).map { it.key to it.value },
            topWords = words.entries.sortedByDescending { it.value }.take(12).map { it.key to it.value },
        )
    }

    private fun onThisDay(items: List<Item>): List<OnThisDay> {
        val now = Jalali.of(today)
        return items
            .filter { it.jalali.month == now.month && it.jalali.day == now.day && it.jalali.year < now.year }
            .groupBy { it.jalali.year }
            .map { (year, group) -> OnThisDay(now.year - year, group.first().date, group.take(4).map { it.message }) }
            .sortedBy { it.yearsAgo }
    }

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    companion object {
        const val MAX_MONTHS_FOR_MONTHLY = 18
        const val SILENCE_DAYS = 45L
        const val KEYWORDS_PER_CHAPTER = 6
        private const val BURST_MS = 10L * 60 * 1000
        private const val NEW_CONVERSATION_MS = 6L * 60 * 60 * 1000
        private const val MAX_REPLY_MS = 12L * 60 * 60 * 1000
    }
}
