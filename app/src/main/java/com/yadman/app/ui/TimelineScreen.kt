package com.yadman.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yadman.app.analysis.Chapter
import com.yadman.app.analysis.Granularity
import com.yadman.app.analysis.Jalali
import com.yadman.app.analysis.MonthActivity
import com.yadman.app.analysis.OnThisDay
import com.yadman.app.analysis.Silence
import com.yadman.app.analysis.Stats
import com.yadman.app.analysis.Timeline
import com.yadman.app.analysis.fa
import com.yadman.app.analysis.faGrouped
import com.yadman.app.data.Contact
import kotlin.math.roundToInt

private const val MAX_EVENTS_PER_CHAPTER = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    contact: Contact,
    timeline: Timeline?,
    analyzing: Boolean,
    error: String?,
    onBack: () -> Unit,
    onOpenChapter: (Chapter) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(contact.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                analyzing -> Loading("دارم پیام‌ها را می‌خوانم و فصل‌بندی می‌کنم…")
                timeline == null -> CenteredMessage(error ?: "پیامی با این شماره پیدا نشد.")
                else -> TimelineContent(contact, timeline, onOpenChapter)
            }
        }
    }
}

@Composable
private fun TimelineContent(contact: Contact, timeline: Timeline, onOpenChapter: (Chapter) -> Unit) {
    val other = contact.name ?: "او"
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { SummaryCard(timeline.stats, other) }
        item { ActivityCard(timeline.activity, other) }
        if (timeline.onThisDay.isNotEmpty()) {
            item { OnThisDayCard(timeline.onThisDay, other) }
        }
        item {
            Text(
                if (timeline.granularity == Granularity.SEASON) "فصل به فصل" else "ماه به ماه",
                Modifier.padding(top = 8.dp, bottom = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        items(timeline.entries) { entry ->
            when (entry) {
                is Chapter -> ChapterRow(entry, other, onOpen = { onOpenChapter(entry) })
                is Silence -> SilenceRow(entry)
            }
        }
    }
}

@Composable
private fun SummaryCard(s: Stats, other: String) {
    val myShare = (s.sent * 100.0 / s.total).roundToInt()
    val starter = when {
        s.iStartedPercent >= 55 -> "بیشتر تو (${fa(s.iStartedPercent)}٪)"
        s.iStartedPercent <= 45 -> "بیشتر $other (${fa(100 - s.iStartedPercent)}٪)"
        else -> "تقریباً برابر"
    }
    val cells = buildList {
        add("سهم تو از پیام‌ها" to "${fa(myShare)}٪")
        add("روزهایی که حرف زدید" to "${faGrouped(s.activeDays.toLong())} روز")
        add("طولانی‌ترین روزهای پشت‌سرهم" to "${fa(s.longestStreakDays)} روز")
        add("شلوغ‌ترین روز" to "${Fmt.date(s.busiestDay)} · ${fa(s.busiestDayCount)} پیام")
        add("طولانی‌ترین سکوت" to Fmt.duration(s.longestSilenceDays))
        add("کی سر صحبت را باز می‌کند" to starter)
        s.myMedianReplyMinutes?.let { add("معمولاً تو جواب می‌دهی در" to Fmt.minutes(it)) }
        s.theirMedianReplyMinutes?.let { add("معمولاً $other جواب می‌دهد در" to Fmt.minutes(it)) }
        add("ساعت محبوب" to "حدود ${fa(s.peakHour)}:۰۰")
        add("روز محبوب" to Jalali.weekdayName(s.peakWeekday))
    }

    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${Fmt.duration(s.spanDays)} گفتگو",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "از ${Fmt.date(s.firstDate)} تا ${Fmt.date(s.lastDate)} · ${faGrouped(s.total.toLong())} پیام",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            cells.chunked(2).forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { (label, value) -> StatCell(label, value, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            if (s.topEmojis.isNotEmpty()) {
                Text("ایموجی‌های همیشگی", style = MaterialTheme.typography.labelLarge)
                Text(s.topEmojis.joinToString("  ") { it.first }, fontSize = 22.sp)
            }
            if (s.topWords.isNotEmpty()) {
                Text("کلمه‌های همیشگی", style = MaterialTheme.typography.labelLarge)
                WordChips(s.topWords.map { it.first })
            }
            HorizontalDivider()
            Text("اولین پیام", style = MaterialTheme.typography.labelLarge)
            MessageBubble(s.first, other, showDate = true)
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActivityCard(activity: List<MonthActivity>, other: String) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ریتم گفتگو، ماه به ماه", style = MaterialTheme.typography.titleMedium)
            ActivityChart(activity)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LegendDot(MaterialTheme.colorScheme.primary, "پیام‌های تو")
                LegendDot(MaterialTheme.colorScheme.tertiary, "پیام‌های $other")
            }
        }
    }
}

@Composable
private fun OnThisDayCard(days: List<OnThisDay>, other: String) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("امروز، در سال‌های قبل", style = MaterialTheme.typography.titleMedium)
            days.forEach { day ->
                Text(
                    "${fa(day.yearsAgo)} سال پیش · ${Fmt.date(day.date)}",
                    style = MaterialTheme.typography.labelLarge,
                )
                day.messages.forEach { MessageBubble(it, other, showDate = false) }
            }
        }
    }
}

/** یک فصل روی خط زمان: خط عمودی و نقطه در حاشیه، کارت فصل کنارش. */
@Composable
private fun ChapterRow(chapter: Chapter, other: String, onOpen: () -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = if (chapter.isPeak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 12.dp.toPx() else 12.dp.toPx()
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                drawCircle(dotColor, radius = 7.dp.toPx(), center = Offset(x, 30.dp.toPx()))
            },
    ) {
        Spacer(Modifier.width(32.dp))
        Card(
            Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        chapter.title,
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (chapter.isPeak) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.tertiaryContainer) {
                            Text(
                                "🔥 اوج گفتگو",
                                Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
                Text(
                    "${Fmt.range(chapter.start, chapter.end)} · ${faGrouped(chapter.count.toLong())} پیام",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (chapter.keywords.isNotEmpty()) WordChips(chapter.keywords)

                chapter.events.take(MAX_EVENTS_PER_CHAPTER).forEach { event ->
                    Column {
                        Text(
                            "${event.type.emoji} ${event.type.label} · ${Fmt.date(event.message.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            "«${event.message.text.replace('\n', ' ')}»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                val hiddenEvents = chapter.events.size - MAX_EVENTS_PER_CHAPTER
                if (hiddenEvents > 0) {
                    Text(
                        "و ${fa(hiddenEvents)} رویداد دیگر",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                chapter.keyMessages.forEach { MessageBubble(it, other, showDate = true) }

                TextButton(onClick = onOpen) {
                    Text("همه‌ی ${faGrouped(chapter.count.toLong())} پیام این دوره")
                }
            }
        }
    }
}

@Composable
private fun SilenceRow(silence: Silence) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - 12.dp.toPx() else 12.dp.toPx()
                drawLine(
                    lineColor,
                    Offset(x, 0f),
                    Offset(x, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                )
            },
    ) {
        Spacer(Modifier.width(32.dp))
        Text(
            "⏸  ${Fmt.duration(silence.days)} سکوت",
            Modifier.padding(vertical = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
