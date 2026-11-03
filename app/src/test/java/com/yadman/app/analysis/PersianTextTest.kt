package com.yadman.app.analysis

import com.yadman.app.data.PhoneNumbers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PersianTextTest {
    @Test
    fun normalizesArabicLettersDigitsAndHalfSpaces() {
        assertEquals("میخوام بیام 123", PersianText.normalize("مي‌خوام  بيام ۱۲۳"))
        assertEquals("سلام", PersianText.normalize("سلاااااام"))
        assertEquals("کتاب", PersianText.normalize("كتاب"))
    }

    @Test
    fun tokensDropStopwordsLinksAndNumbers() {
        val tokens = PersianText.tokens("سلام! فردا کنکور دارم، استرس دارم 😅 https://example.com 1400")
        assertEquals(listOf("کنکور", "استرس"), tokens)
    }

    @Test
    fun emojisAreExtracted() {
        assertEquals(listOf("😂", "😂", "❤️"), PersianText.emojis("خخخ 😂😂 ❤️"))
    }

    @Test
    fun persianDigits() {
        assertEquals("۱۴۰۵", fa(1405))
        assertEquals("۱۲٬۳۴۵", faGrouped(12345))
    }

    @Test
    fun phoneKeysMatchAcrossFormats() {
        val key = PhoneNumbers.key("+989121234567")
        assertEquals(key, PhoneNumbers.key("09121234567"))
        assertEquals(key, PhoneNumbers.key("00989121234567"))
        assertEquals(key, PhoneNumbers.key("۰۹۱۲ ۱۲۳ ۴۵۶۷"))
        assertEquals("bank mellat", PhoneNumbers.key(" Bank Mellat "))
        assertEquals("912123", PhoneNumbers.searchKey("+98912123"))
        assertEquals("912123", PhoneNumbers.searchKey("۰۹۱۲۱۲۳"))
    }

    @Test
    fun eventsAreClassified() {
        assertEquals(EventType.BIRTHDAY, EventDetector.classify("تولدت مبارک باشه رفیق 🎂"))
        assertEquals(EventType.CONDOLENCE, EventDetector.classify("واقعا متاسفم، تسلیت میگم"))
        assertEquals(EventType.ACHIEVEMENT, EventDetector.classify("قبول شدمممم!!!"))
        assertEquals(EventType.WEDDING, EventDetector.classify("عروسیت کیه؟"))
        assertEquals(EventType.CONGRATS, EventDetector.classify("مبارکه داداش"))
        assertNull(EventDetector.classify("فردا ساعت ۵ همو ببینیم"))
        // «سفره» نباید سفر حساب شود.
        assertNull(EventDetector.classify("سفره رو بنداز"))
    }

    @Test
    fun stopwordsAreNormalized() {
        assertTrue("میخوام" in Stopwords.words)
        assertFalse("کنکور" in Stopwords.words)
    }
}
