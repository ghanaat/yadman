package com.yadman.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.yadman.app.analysis.Message

data class Contact(
    val key: String,
    val address: String,
    val name: String?,
    val sent: Int,
    val received: Int,
    val firstTimestamp: Long,
    val lastTimestamp: Long,
) {
    val total: Int get() = sent + received

    /** گفتگوی دوطرفه؛ پیامک‌های بانک و سرویس‌ها فقط دریافتی‌اند. */
    val isTwoWay: Boolean get() = sent > 0 && received > 0

    val displayName: String get() = name ?: address
}

class SmsRepository(private val context: Context) {

    fun hasSmsPermission(): Boolean = granted(Manifest.permission.READ_SMS)

    fun loadContacts(): List<Contact> {
        class Acc(val address: String) {
            var sent = 0
            var received = 0
            var first = Long.MAX_VALUE
            var last = Long.MIN_VALUE
        }

        val byKey = HashMap<String, Acc>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.TYPE, Telephony.Sms.DATE),
            null, null, null,
        )?.use { c ->
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (c.moveToNext()) {
                val type = c.getInt(iType)
                if (type != Telephony.Sms.MESSAGE_TYPE_INBOX && type != Telephony.Sms.MESSAGE_TYPE_SENT) continue
                val address = c.getString(iAddress) ?: continue
                val key = PhoneNumbers.key(address)
                if (key.isEmpty()) continue
                val acc = byKey.getOrPut(key) { Acc(address) }
                if (type == Telephony.Sms.MESSAGE_TYPE_SENT) acc.sent++ else acc.received++
                val date = c.getLong(iDate)
                if (date < acc.first) acc.first = date
                if (date > acc.last) acc.last = date
            }
        }

        val names = loadContactNames()
        return byKey
            .map { (key, a) -> Contact(key, a.address, names[key], a.sent, a.received, a.first, a.last) }
            .sortedByDescending { it.total }
    }

    /** همه‌ی پیامک‌های ارسالی و دریافتی با یک نفر، فارغ از اینکه شماره با چه قالبی ذخیره شده. */
    fun loadMessages(contactKey: String): List<Message> {
        val out = ArrayList<Message>()
        context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.TYPE),
            null, null, null,
        )?.use { c ->
            val iAddress = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val iBody = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val iDate = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val iType = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (c.moveToNext()) {
                val type = c.getInt(iType)
                if (type != Telephony.Sms.MESSAGE_TYPE_INBOX && type != Telephony.Sms.MESSAGE_TYPE_SENT) continue
                if (PhoneNumbers.key(c.getString(iAddress)) != contactKey) continue
                out.add(
                    Message(
                        timestamp = c.getLong(iDate),
                        fromMe = type == Telephony.Sms.MESSAGE_TYPE_SENT,
                        text = c.getString(iBody).orEmpty(),
                    ),
                )
            }
        }
        return out
    }

    /** اسم مخاطب‌ها فقط وقتی کاربر اجازه‌ی مخاطبین را هم داده باشد؛ بدون آن فقط شماره نشان می‌دهیم. */
    private fun loadContactNames(): Map<String, String> {
        if (!granted(Manifest.permission.READ_CONTACTS)) return emptyMap()
        val names = HashMap<String, String>()
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.DISPLAY_NAME),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val number = c.getString(0) ?: continue
                val name = c.getString(1) ?: continue
                names.putIfAbsent(PhoneNumbers.key(number), name)
            }
        }
        return names
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
