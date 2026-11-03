package com.yadman.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yadman.app.analysis.Chapter
import com.yadman.app.analysis.faGrouped
import com.yadman.app.data.Contact
import java.time.LocalDate

/** همه‌ی پیام‌های یک فصل، روز به روز. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(contact: Contact, chapter: Chapter, onBack: () -> Unit) {
    val other = contact.name ?: "او"
    val days = remember(chapter) { chapter.messages.groupBy { Fmt.localDate(it.timestamp) }.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(chapter.title)
                        Text(
                            "${faGrouped(chapter.count.toLong())} پیام با ${contact.displayName}",
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = { BackButton(onBack) },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        ) {
            days.forEach { (day, messages) ->
                item(key = "day-$day") { DayHeader(day) }
                items(messages) { MessageBubble(it, other, showDate = false) }
            }
        }
    }
}

@Composable
private fun DayHeader(day: LocalDate) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(
                Fmt.dayWithWeekday(day),
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
