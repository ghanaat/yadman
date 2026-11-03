package com.yadman.app.analysis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TimelineAnalyzerTest {
    private val zone = ZoneId.of("Asia/Tehran")

    private fun at(y: Int, m: Int, d: Int, h: Int, min: Int = 0) =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun msg(y: Int, m: Int, d: Int, h: Int, fromMe: Boolean, text: String, min: Int = 0) =
        Message(at(y, m, d, h, min), fromMe, text)

    /** دو دوره‌ی متفاوت با یک سکوت طولانی وسطشان: کنکور در ۲۰۲۰، سربازی در ۲۰۲۲. */
    private val conversation = buildList {
        add(msg(2020, 6, 1, 10, true, "سلام، فردا کنکور داری؟ استرس دارم"))
        add(msg(2020, 6, 1, 10, false, "آره کنکور دارم، تست ریاضی رو خراب کردم", min = 5))
        add(msg(2020, 6, 2, 21, true, "کنکور چطور بود؟ تست ها سخت بود؟"))
        add(msg(2020, 6, 2, 22, false, "کنکور خیلی سخت بود ولی ریاضی خوب شد"))
        add(msg(2020, 6, 20, 9, false, "تولدت مبارک رفیق قدیمی 🎂🎂"))
        add(msg(2020, 6, 21, 18, true, "مرسی 😂 کتابخونه میای فردا؟"))
        add(msg(2020, 6, 22, 18, false, "آره کتابخونه میام، ساعت ۴"))
        // ۲ سال سکوت
        add(msg(2022, 8, 1, 12, false, "سلام، دارم میرم سربازی، پادگان کرمان افتادم"))
        add(msg(2022, 8, 1, 12, true, "ای بابا پادگان کرمان؟ کی اعزامی؟", min = 3))
        add(msg(2022, 8, 3, 20, false, "پادگان سخته، مرخصی گرفتم میام ببینمت"))
        add(msg(2022, 8, 4, 20, true, "مرخصی اومدی خبر بده"))
        add(msg(2022, 8, 5, 20, true, "   "))
    }

    @Test
    fun emptyInputGivesNoTimeline() {
        assertNull(TimelineAnalyzer(zone).analyze(emptyList()))
        assertNull(TimelineAnalyzer(zone).analyze(listOf(msg(2020, 1, 1, 1, true, "  "))))
    }

    @Test
    fun buildsChaptersSilenceAndStats() {
        val timeline = TimelineAnalyzer(zone, today = LocalDate.of(2026, 6, 10)).analyze(conversation.shuffled())
        assertNotNull(timeline)
        timeline!!

        // دوره‌ی بیش از ۱۸ ماه ← فصل‌بندی سه‌ماهه؛ خرداد ۹۹ و مرداد ۱۴۰۱.
        assertEquals(Granularity.SEASON, timeline.granularity)
        val chapters = timeline.chapters
        assertEquals(listOf("بهار ۱۳۹۹", "تابستان ۱۳۹۹", "تابستان ۱۴۰۱"), chapters.map { it.title })
        assertTrue(timeline.entries.any { it is Silence })

        val spring = chapters.first()
        assertEquals("کنکور", spring.keywords.first())
        assertTrue(spring.keyMessages.isNotEmpty())

        val military = chapters.last()
        assertTrue("پادگان" in military.keywords)
        assertTrue(military.events.any { it.type == EventType.MILITARY })

        assertTrue(timeline.events.any { it.type == EventType.BIRTHDAY })

        val s = timeline.stats
        assertEquals(11, s.total)
        assertEquals(5, s.sent)
        assertEquals(LocalDate.of(2020, 6, 1), s.firstDate)
        assertEquals(LocalDate.of(2022, 8, 4), s.lastDate)
        assertEquals(LocalDate.of(2020, 6, 1), s.busiestDay)
        assertEquals(3, s.longestStreakDays) // ۲۰ تا ۲۲ ژوئن
        assertTrue(s.longestSilenceDays > 700)
        // جواب‌ها فقط وقتی حساب می‌شوند که زیر ۶ ساعت باشند: او ۵ و ۶۰ دقیقه، تو ۳ دقیقه.
        assertEquals(32.5, s.theirMedianReplyMinutes!!, 0.001)
        assertEquals(3.0, s.myMedianReplyMinutes!!, 0.001)
        assertEquals("🎂", s.topEmojis.first().first)

        // ماه‌های بی‌پیام هم در نمودار هستند (خرداد ۹۹ تا مرداد ۱۴۰۱).
        assertEquals(27, timeline.activity.size)
        assertEquals(timeline.activity.sumOf { it.total }, s.total)
    }

    @Test
    fun shortRelationshipsAreMonthly() {
        val messages = listOf(
            msg(2024, 1, 5, 10, true, "سلام خوبی؟ پروژه رو تموم کردی؟"),
            msg(2024, 2, 5, 10, false, "آره پروژه تموم شد بالاخره"),
            msg(2024, 3, 5, 10, true, "عالی! ارائه کی هست؟"),
        )
        val timeline = TimelineAnalyzer(zone).analyze(messages)!!
        assertEquals(Granularity.MONTH, timeline.granularity)
        assertEquals(listOf("دی ۱۴۰۲", "بهمن ۱۴۰۲", "اسفند ۱۴۰۲"), timeline.chapters.map { it.title })
        assertEquals(1, timeline.chapters.count { it.isPeak })
    }

    @Test
    fun onThisDayFindsSameJalaliDateInEarlierYears() {
        // ۱۱ خرداد ۱۳۹۹ = ۳۱ می ۲۰۲۰؛ ۱۱ خرداد ۱۴۰۵ = ۱ ژوئن ۲۰۲۶.
        val messages = listOf(msg(2020, 5, 31, 10, true, "یادته امروز رفتیم کوه؟"))
        val timeline = TimelineAnalyzer(zone, today = LocalDate.of(2026, 6, 1)).analyze(messages)!!
        assertEquals(1, timeline.onThisDay.size)
        assertEquals(6, timeline.onThisDay.first().yearsAgo)
    }
}
