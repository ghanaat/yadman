package com.yadman.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun YadmanApp(vm: AppViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshPermission() }
    BackHandler(enabled = state.selected != null) { vm.back() }

    val selected = state.selected
    val chapter = state.openChapter
    when {
        !state.hasSmsPermission -> PermissionScreen(onResult = vm::refreshPermission)
        selected == null -> PeopleScreen(state, onOpen = vm::open, onRetry = vm::loadContacts)
        chapter != null -> MessagesScreen(selected, chapter, onBack = { vm.back() })
        else -> TimelineScreen(
            contact = selected,
            timeline = state.timeline,
            analyzing = state.analyzing,
            error = state.error,
            onBack = { vm.back() },
            onOpenChapter = vm::openChapter,
        )
    }
}

@Composable
private fun PermissionScreen(onResult: () -> Unit) {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        onResult()
    }

    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🕰️", fontSize = 56.sp)
            Text("یادمان", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "پیامک‌هایت با یک نفر را می‌خوانم و از سال‌ها گفتگو یک خط زمان می‌سازم: " +
                    "فصل‌ها، حرف‌های هر دوره، رویدادها و روزهایی که شاید یادت رفته باشد.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(
                    "🔒 این اپ اصلاً اجازه‌ی اینترنت ندارد. پیام‌ها فقط روی همین گوشی خوانده و تحلیل می‌شوند " +
                        "و جایی ذخیره یا فرستاده نمی‌شوند.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = {
                    launcher.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.READ_CONTACTS))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("اجازه‌ی خواندن پیامک‌ها")
            }
            Text(
                "اجازه‌ی مخاطبین اختیاری است و فقط برای نشان دادن اسم به‌جای شماره است.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (asked) {
                Text(
                    "اگر پنجره‌ی اجازه باز نشد یا آن را رد کردی، از تنظیمات اپ دسترسی «پیامک» را روشن کن. " +
                        "روی گوشی‌های شیائومی ممکن است «خواندن پیامک‌های اعلان» هم لازم باشد.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }) {
                    Text("باز کردن تنظیمات اپ")
                }
            }
        }
    }
}
