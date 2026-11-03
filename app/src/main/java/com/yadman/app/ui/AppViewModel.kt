package com.yadman.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yadman.app.analysis.Chapter
import com.yadman.app.analysis.Timeline
import com.yadman.app.analysis.TimelineAnalyzer
import com.yadman.app.data.Contact
import com.yadman.app.data.SmsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val hasSmsPermission: Boolean = false,
    val loadingContacts: Boolean = false,
    val contacts: List<Contact>? = null,
    val selected: Contact? = null,
    val analyzing: Boolean = false,
    val timeline: Timeline? = null,
    val openChapter: Chapter? = null,
    val error: String? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = SmsRepository(app)
    private val _state = MutableStateFlow(UiState(hasSmsPermission = repository.hasSmsPermission()))
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var analysisJob: Job? = null

    init {
        if (_state.value.hasSmsPermission) loadContacts()
    }

    /** بعد از برگشتن از پنجره‌ی اجازه یا تنظیمات گوشی صدا زده می‌شود. */
    fun refreshPermission() {
        val granted = repository.hasSmsPermission()
        val wasGranted = _state.value.hasSmsPermission
        _state.update { it.copy(hasSmsPermission = granted) }
        if (granted && !wasGranted) loadContacts()
    }

    fun loadContacts() {
        _state.update { it.copy(loadingContacts = true, error = null) }
        viewModelScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) { repository.loadContacts() }
                _state.update { it.copy(loadingContacts = false, contacts = contacts) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(loadingContacts = false, error = describe(e)) }
            }
        }
    }

    fun open(contact: Contact) {
        analysisJob?.cancel()
        _state.update { it.copy(selected = contact, timeline = null, openChapter = null, analyzing = true, error = null) }
        analysisJob = viewModelScope.launch {
            try {
                val messages = withContext(Dispatchers.IO) { repository.loadMessages(contact.key) }
                val timeline = withContext(Dispatchers.Default) { TimelineAnalyzer().analyze(messages) }
                _state.update { it.copy(analyzing = false, timeline = timeline) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(analyzing = false, error = describe(e)) }
            }
        }
    }

    fun openChapter(chapter: Chapter) = _state.update { it.copy(openChapter = chapter) }

    /** یک صفحه به عقب؛ اگر در صفحه‌ی اول باشیم false برمی‌گرداند. */
    fun back(): Boolean {
        val s = _state.value
        when {
            s.openChapter != null -> _state.update { it.copy(openChapter = null) }
            s.selected != null -> {
                analysisJob?.cancel()
                _state.update { it.copy(selected = null, timeline = null, analyzing = false, error = null) }
            }
            else -> return false
        }
        return true
    }

    private fun describe(e: Throwable): String = when (e) {
        is SecurityException ->
            "گوشی اجازه‌ی خواندن پیامک‌ها را نداد. در تنظیمات اپ، دسترسی «پیامک» را فعال کن " +
                "(روی شیائومی گزینه‌ی «خواندن پیامک‌های اعلان» را هم روشن کن)."
        else -> "خطا در خواندن پیامک‌ها: ${e.message ?: e.javaClass.simpleName}"
    }
}
