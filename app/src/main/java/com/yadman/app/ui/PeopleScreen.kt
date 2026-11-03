package com.yadman.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yadman.app.analysis.faGrouped
import com.yadman.app.data.Contact
import com.yadman.app.data.PhoneNumbers

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(state: UiState, onOpen: (Contact) -> Unit, onRetry: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var showAll by rememberSaveable { mutableStateOf(false) }
    val contacts = state.contacts.orEmpty()
    val visible = remember(contacts, query, showAll) { filterContacts(contacts, query, showAll) }

    Scaffold(topBar = { TopAppBar(title = { Text("خط زمان با کی؟") }) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("اسم یا شماره") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showAll = !showAll }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "نمایش فرستنده‌های یک‌طرفه (بانک، سرویس‌ها)",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = showAll, onCheckedChange = { showAll = it })
            }

            val error = state.error
            when {
                state.contacts == null && state.loadingContacts -> Loading("دارم پیامک‌ها را مرور می‌کنم…")
                state.contacts == null && error != null -> CenteredMessage(error, "دوباره", onRetry)
                visible.isEmpty() -> CenteredMessage(
                    if (query.isBlank()) "گفتگوی پیامکی دوطرفه‌ای پیدا نشد." else "کسی با این اسم یا شماره پیدا نشد.",
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(visible, key = { it.key }) { contact ->
                        ContactRow(contact, onClick = { onOpen(contact) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Avatar(contact.displayName) },
        headlineContent = {
            Text(contact.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                "${faGrouped(contact.total.toLong())} پیام · ${Fmt.years(contact.firstTimestamp, contact.lastTimestamp)}",
            )
        },
    )
}

private fun filterContacts(all: List<Contact>, query: String, showAll: Boolean): List<Contact> {
    val q = query.trim()
    if (q.isEmpty()) return all.filter { showAll || it.isTwoWay }
    val digits = PhoneNumbers.searchKey(q)
    return all.filter { c ->
        c.name?.contains(q, ignoreCase = true) == true ||
            c.address.contains(q, ignoreCase = true) ||
            (digits.length >= 3 && c.key.contains(digits))
    }
}
