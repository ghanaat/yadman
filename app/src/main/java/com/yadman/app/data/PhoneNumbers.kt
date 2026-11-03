package com.yadman.app.data

import com.yadman.app.analysis.PersianText

object PhoneNumbers {
    /**
     * کلید یکسان برای یک فرستنده، تا +98912…، 0098912…، 0912… و «۰۹۱۲…» یک نفر حساب شوند.
     * برای شماره‌های بلند ۱۰ رقم آخر کافی است؛ فرستنده‌های متنی (مثل نام بانک) همان متن کوچک‌شده‌اند.
     */
    fun key(address: String?): String {
        if (address.isNullOrBlank()) return ""
        val latin = PersianText.toLatinDigits(address).trim()
        if (latin.any { it.isLetter() }) return latin.lowercase()
        val digits = latin.filter { it in '0'..'9' }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    /** بخشی از شماره که کاربر در جستجو تایپ کرده، به همان شکل کلید (بدون ۰، ۹۸ و ۰۰۹۸ اول). */
    fun searchKey(query: String): String {
        var digits = PersianText.toLatinDigits(query).filter { it in '0'..'9' }
        digits = digits.removePrefix("00")
        if (digits.startsWith("98") && digits.length > 4) digits = digits.removePrefix("98")
        return digits.trimStart('0')
    }
}
