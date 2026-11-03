package com.yadman.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.yadman.app.analysis.MonthActivity
import com.yadman.app.analysis.fa

/**
 * ستون‌های ماهانه: پایین پیام‌های طرف مقابل، بالا پیام‌های خودت.
 * زمان مثل متن فارسی از راست به چپ جلو می‌رود و سال‌ها زیر فروردین هر سال نوشته می‌شوند.
 */
@Composable
fun ActivityChart(data: List<MonthActivity>, modifier: Modifier = Modifier) {
    val sentColor = MaterialTheme.colorScheme.primary
    val receivedColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val measurer = rememberTextMeasurer()
    val max = (data.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        if (data.isEmpty()) return@Canvas
        val labelArea = 20.dp.toPx()
        val chartHeight = size.height - labelArea
        val slot = size.width / data.size
        val gap = if (slot > 6.dp.toPx()) slot * 0.25f else 0f
        val rtl = layoutDirection == LayoutDirection.Rtl
        val labelRanges = ArrayList<ClosedFloatingPointRange<Float>>()

        data.forEachIndexed { i, month ->
            val left = if (rtl) size.width - (i + 1) * slot else i * slot
            val x = left + gap / 2
            val width = (slot - gap).coerceAtLeast(1f)
            val receivedHeight = chartHeight * month.received / max
            val sentHeight = chartHeight * month.sent / max
            drawRect(receivedColor, Offset(x, chartHeight - receivedHeight), Size(width, receivedHeight))
            drawRect(sentColor, Offset(x, chartHeight - receivedHeight - sentHeight), Size(width, sentHeight))

            if (month.month == 1 || i == 0) {
                val layout = measurer.measure(fa(month.year), labelStyle)
                val textWidth = layout.size.width.toFloat()
                val textX = (left + slot / 2 - textWidth / 2).coerceIn(0f, (size.width - textWidth).coerceAtLeast(0f))
                val range = (textX - 4f)..(textX + textWidth + 4f)
                if (labelRanges.none { it.start < range.endInclusive && range.start < it.endInclusive }) {
                    labelRanges.add(range)
                    drawText(layout, topLeft = Offset(textX, chartHeight + 4.dp.toPx()))
                }
            }
        }
        drawLine(axisColor, Offset(0f, chartHeight), Offset(size.width, chartHeight), strokeWidth = 1.dp.toPx())
    }
}
