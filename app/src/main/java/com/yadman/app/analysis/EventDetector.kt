package com.yadman.app.analysis

/**
 * رویدادهای مهم زندگی را از روی عبارت‌های آشنا پیدا می‌کند («تولدت مبارک»، «تسلیت»، «قبول شدم»...).
 * هر عبارت باید از ابتدای یک کلمه شروع شود، ولی پسوند مجاز است («عروسی» ← «عروسیت»).
 * ترتیب قانون‌ها مهم است: اولین قانونی که بخورد برنده است، پس «تولدت مبارک باشه» تولد حساب می‌شود نه تبریک.
 */
object EventDetector {
    /** یک رویداد تکراری (مثلاً ده پیام تبریک عید در یک روز) فقط یک بار ثبت می‌شود. */
    private const val DEDUP_MS = 3L * 24 * 60 * 60 * 1000

    private val rawRules: List<Pair<EventType, List<String>>> = listOf(
        EventType.BIRTHDAY to listOf(
            "تولدت مبارک", "تولد مبارک", "تولدتون مبارک", "تولدت رو تبریک", "تولدتو تبریک",
            "happy birthday", "hbd", "tavalodet mobarak",
        ),
        EventType.CONDOLENCE to listOf(
            "تسلیت", "خدا رحمت", "خدا بیامرز", "خدابیامرز", "روحش شاد", "غم آخرت", "مرحوم",
            "فوت کرد", "فوت شد", "به رحمت خدا",
        ),
        EventType.NOWRUZ to listOf(
            "سال نو مبارک", "سال نوت مبارک", "عید مبارک", "عیدت مبارک", "عیدتون مبارک", "نوروز",
            "happy new year", "eyd mobarak",
        ),
        EventType.WEDDING to listOf(
            "عروسی", "نامزد", "خواستگاری", "عقدکنان", "عقد کنان", "جشن عقد", "ازدواج",
        ),
        EventType.BABY to listOf(
            "بچه دار شد", "بچه دار شدی", "به دنیا اومد", "به دنیا آمد", "زایمان", "دختردار", "پسردار",
        ),
        EventType.ACHIEVEMENT to listOf(
            "قبول شدم", "قبول شدی", "پذیرفته شدم", "دفاع کردم", "دفاعم", "فارغ التحصیل",
            "رتبم", "رتبه ام", "مدرکمو گرفتم",
        ),
        EventType.EMIGRATION to listOf(
            "مهاجرت", "ویزا", "ویزام", "اپلای", "سفارت", "اقامت", "پذیرش گرفتم",
        ),
        EventType.MILITARY to listOf(
            "سربازی", "پادگان", "کارت پایان خدمت", "پایان خدمت", "ترخیص شدم",
        ),
        EventType.JOB to listOf(
            "استخدام", "کار جدید", "مصاحبه کاری", "مصاحبه دارم", "اولین روز کار", "اخراج", "استعفا",
        ),
        EventType.HEALTH to listOf(
            "بیمارستان", "بستری", "جراحی", "اتاق عمل", "تصادف", "کرونا گرفتم", "کرونا گرفتی",
        ),
        EventType.MOVING to listOf(
            "اسباب کشی", "خونه جدید", "خانه جدید", "خونه گرفتم", "خونه عوض",
        ),
        EventType.TRAVEL to listOf(
            "فرودگاه", "پرواز", "بلیط", "بلیت", "سفر بخیر", "سفرت بخیر", "رفتم سفر", "میرم سفر",
            "میریم سفر", "سفرم",
        ),
        EventType.CONFLICT to listOf(
            "معذرت میخوام", "معذرت می خوام", "منو ببخش", "ازت ناراحتم", "ازت دلخورم", "دلخور", "قهر",
            "آشتی",
        ),
        EventType.CONGRATS to listOf(
            "تبریک", "مبارک باشه", "مبارکه", "مبارکت باشه", "congrat", "mobarak",
        ),
    )

    private val rules: List<Pair<EventType, List<Regex>>> = rawRules.map { (type, phrases) ->
        type to phrases.map { phrase ->
            Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(PersianText.normalize(phrase)))
        }
    }

    fun classify(text: String): EventType? {
        val normalized = PersianText.normalize(text)
        return rules.firstOrNull { (_, patterns) -> patterns.any { it.containsMatchIn(normalized) } }?.first
    }

    /** [messages] باید به ترتیب زمان باشند. */
    fun detect(messages: List<Message>): List<LifeEvent> {
        val lastSeen = HashMap<EventType, Long>()
        val out = ArrayList<LifeEvent>()
        for (message in messages) {
            val type = classify(message.text) ?: continue
            val previous = lastSeen[type]
            if (previous != null && message.timestamp - previous < DEDUP_MS) continue
            lastSeen[type] = message.timestamp
            out.add(LifeEvent(type, message))
        }
        return out
    }
}
