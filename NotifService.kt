package com.clawd.pet
import android.app.Notification
import android.service.notification.*

class NotifService : NotificationListenerService() {
    private var last = ""
    override fun onNotificationPosted(s: StatusBarNotification) {
        val n = s.notification; val e = n.extras
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        if (n.category == Notification.CATEGORY_TRANSPORT || e.containsKey("android.mediaSession")) {
            val t = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
            val full = if (text.isBlank()) t else "$t — $text"
            if (full != last) { last = full; Bus.music?.invoke(full) }
        } else if (n.category == Notification.CATEGORY_MESSAGE) Bus.msg?.invoke(text)
    }
}
