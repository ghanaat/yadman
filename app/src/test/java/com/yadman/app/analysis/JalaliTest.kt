package com.yadman.app.analysis

import org.junit.Assert.assertEquals
import org.junit.Test

class JalaliTest {
    @Test
    fun nowruzDates() {
        assertEquals(JalaliDate(1403, 1, 1), Jalali.fromGregorian(2024, 3, 20))
        assertEquals(JalaliDate(1404, 1, 1), Jalali.fromGregorian(2025, 3, 21))
        assertEquals(JalaliDate(1405, 1, 1), Jalali.fromGregorian(2026, 3, 21))
    }

    @Test
    fun knownDates() {
        assertEquals(JalaliDate(1357, 11, 22), Jalali.fromGregorian(1979, 2, 11))
        assertEquals(JalaliDate(1402, 12, 29), Jalali.fromGregorian(2024, 3, 19))
        assertEquals(JalaliDate(1405, 8, 1), Jalali.fromGregorian(2026, 10, 23))
        assertEquals(JalaliDate(1399, 12, 30), Jalali.fromGregorian(2021, 3, 20))
    }
}
