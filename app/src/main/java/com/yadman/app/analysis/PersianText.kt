package com.yadman.app.analysis

import java.util.Locale

/** عدد با رقم‌های فارسی. */
fun fa(n: Number): String = PersianText.toPersianDigits(n.toString())

/** عدد با رقم‌های فارسی و جداکننده‌ی هزارگان. */
fun faGrouped(n: Long): String =
    PersianText.toPersianDigits(String.format(Locale.US, "%,d", n)).replace(',', '٬')

object PersianText {
    private val urlRegex = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    private val repeatedRegex = Regex("""(.)\1{2,}""")
    private val spaceRegex = Regex("""\s+""")

    /**
     * یکسان‌سازی متن: ی و ک عربی، رقم‌های فارسی و عربی، اعراب، نیم‌فاصله، کشیده،
     * حروف لاتین کوچک، و حروف تکراری («سلاااام» ← «سلام»).
     */
    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                'ي', 'ى' -> sb.append('ی')
                'ك' -> sb.append('ک')
                'ة', 'ۀ' -> sb.append('ه')
                'أ', 'إ', 'ٱ' -> sb.append('ا')
                'ؤ' -> sb.append('و')
                in '۰'..'۹' -> sb.append('0' + (ch - '۰'))
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                '‌', '‍', '‎', '‏', 'ـ' -> Unit
                in 'ً'..'ٟ', 'ٰ' -> Unit
                else -> sb.append(ch.lowercaseChar())
            }
        }
        val collapsed = repeatedRegex.replace(sb, "\$1")
        return spaceRegex.replace(collapsed, " ").trim()
    }

    /** کلمه‌های معنادار یک پیام: بدون لینک، عدد خالی، کلمه‌های یک‌حرفی و کلمه‌های پرکاربرد بی‌معنا. */
    fun tokens(text: String): List<String> {
        val clean = urlRegex.replace(normalize(text), " ")
        val out = ArrayList<String>()
        val current = StringBuilder()
        fun flush() {
            if (current.length >= 2) {
                val word = current.toString()
                if (!word.all { it.isDigit() } && word !in Stopwords.words) out.add(word)
            }
            current.setLength(0)
        }
        for (ch in clean) {
            if (ch.isLetterOrDigit()) current.append(ch) else flush()
        }
        flush()
        return out
    }

    fun emojis(text: String): List<String> {
        val out = ArrayList<String>()
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val isPictograph = cp in 0x1F300..0x1FAFF && cp !in 0x1F3FB..0x1F3FF
            val isSymbol = cp in 0x2600..0x27BF
            if (isPictograph || isSymbol) {
                val emoji = StringBuilder().appendCodePoint(cp)
                // نمادهای قدیمی بدون این نویسه به‌صورت متن سیاه‌وسفید نمایش داده می‌شوند.
                if (isSymbol) emoji.append('️')
                out.add(emoji.toString())
            }
            i += Character.charCount(cp)
        }
        return out
    }

    fun toLatinDigits(s: String): String = buildString(s.length) {
        for (c in s) {
            append(
                when (c) {
                    in '۰'..'۹' -> '0' + (c - '۰')
                    in '٠'..'٩' -> '0' + (c - '٠')
                    else -> c
                },
            )
        }
    }

    fun toPersianDigits(s: String): String = buildString(s.length) {
        for (c in s) append(if (c in '0'..'9') '۰' + (c - '0') else c)
    }
}
