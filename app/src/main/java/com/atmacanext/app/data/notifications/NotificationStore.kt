package com.atmacanext.app.data.notifications

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class AccountNotification(
    val id: String,
    val username: String,
    val message: String,
    val createdAt: Long,
    val read: Boolean = false,
)

internal object NotificationFeed {
    fun add(existing: List<AccountNotification>, event: AccountNotification): List<AccountNotification> =
        if (existing.any { it.id == event.id }) existing else (listOf(event) + existing).take(200)
    fun readAll(existing: List<AccountNotification>): List<AccountNotification> = existing.map { it.copy(read = true) }
}

/** In-app notification inbox. Separate from logs, with durable read state and event deduplication. */
class NotificationStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("account_notifications_v1", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow(load())
    val notifications = mutable.asStateFlow()

    @Synchronized fun add(event: AccountNotification): Boolean = save(NotificationFeed.add(mutable.value, event))
    @Synchronized fun markAllRead(): Boolean = save(NotificationFeed.readAll(mutable.value))

    private fun load(): List<AccountNotification> = try {
        val json = JSONArray(prefs.getString("items", "[]"))
        (0 until json.length()).map { i -> json.getJSONObject(i).let {
            AccountNotification(it.getString("id"), it.getString("username"), it.getString("message"),
                it.getLong("createdAt"), it.optBoolean("read", false))
        } }.take(200)
    } catch (_: Exception) { emptyList() }

    private fun save(events: List<AccountNotification>): Boolean {
        if (events == mutable.value) return true
        val json = JSONArray()
        events.forEach { event -> json.put(JSONObject().put("id", event.id).put("username", event.username)
            .put("message", event.message).put("createdAt", event.createdAt).put("read", event.read)) }
        val saved = prefs.edit().putString("items", json.toString()).commit()
        // Keep the event visible even if Android reports a storage write failure.
        mutable.value = events
        return saved
    }
}
