package com.example.browser

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RecentItem(
    val id: String = System.currentTimeMillis().toString(),
    val title: String,
    val subtitle: String,
    val type: RecentItemType,
    val timestamp: Long = System.currentTimeMillis(),
    val data: String = "" // URL or action ID
)

enum class RecentItemType {
    BROWSER_SITE,
    FEATURE_TOOL,
    SCREENSHOT,
    ORB_ACTION
}

class BrowserRecentStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("savior_recent_store", Context.MODE_PRIVATE)

    private val _recentItems = MutableStateFlow<List<RecentItem>>(loadRecentItems())
    val recentItems: StateFlow<List<RecentItem>> = _recentItems.asStateFlow()

    fun addRecentItem(item: RecentItem) {
        val current = _recentItems.value.toMutableList()
        // Deduplicate same data/URL if recent
        current.removeAll { it.data.isNotEmpty() && it.data == item.data }
        current.add(0, item)
        // Keep max 30 items
        val trimmed = if (current.size > 30) current.take(30) else current
        _recentItems.value = trimmed
        persistRecentItems(trimmed)
    }

    fun addWebsiteVisit(title: String, url: String) {
        if (url.isBlank()) return
        addRecentItem(
            RecentItem(
                title = if (title.isBlank()) url else title,
                subtitle = url,
                type = RecentItemType.BROWSER_SITE,
                data = url
            )
        )
    }

    fun addToolUsage(toolTitle: String, description: String, toolId: String) {
        addRecentItem(
            RecentItem(
                title = toolTitle,
                subtitle = description,
                type = RecentItemType.FEATURE_TOOL,
                data = toolId
            )
        )
    }

    fun clearAll() {
        _recentItems.value = emptyList()
        prefs.edit().remove(KEY_ITEMS_JSON).apply()
    }

    private fun loadRecentItems(): List<RecentItem> {
        val raw = prefs.getString(KEY_ITEMS_JSON, null) ?: return defaultInitialRecents()
        return try {
            val array = JSONArray(raw)
            val list = mutableListOf<RecentItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RecentItem(
                        id = obj.optString("id", i.toString()),
                        title = obj.optString("title", "Item"),
                        subtitle = obj.optString("subtitle", ""),
                        type = try {
                            RecentItemType.valueOf(obj.optString("type", RecentItemType.FEATURE_TOOL.name))
                        } catch (e: Exception) {
                            RecentItemType.FEATURE_TOOL
                        },
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        data = obj.optString("data", "")
                    )
                )
            }
            if (list.isEmpty()) defaultInitialRecents() else list
        } catch (e: Exception) {
            defaultInitialRecents()
        }
    }

    private fun persistRecentItems(items: List<RecentItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("subtitle", item.subtitle)
                put("type", item.type.name)
                put("timestamp", item.timestamp)
                put("data", item.data)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ITEMS_JSON, array.toString()).apply()
    }

    private fun defaultInitialRecents(): List<RecentItem> {
        return listOf(
            RecentItem(
                title = "YouTube",
                subtitle = "https://m.youtube.com",
                type = RecentItemType.BROWSER_SITE,
                data = "https://m.youtube.com"
            ),
            RecentItem(
                title = "Background Play",
                subtitle = "Media multitasking environment",
                type = RecentItemType.FEATURE_TOOL,
                data = "background_play"
            ),
            RecentItem(
                title = "Volume & Assistive Orb",
                subtitle = "Floating overlay controls",
                type = RecentItemType.FEATURE_TOOL,
                data = "volume_orb"
            )
        )
    }

    companion object {
        private const val KEY_ITEMS_JSON = "recent_items_json"

        @Volatile
        private var instance: BrowserRecentStore? = null

        fun getInstance(context: Context): BrowserRecentStore {
            return instance ?: synchronized(this) {
                instance ?: BrowserRecentStore(context.applicationContext).also { instance = it }
            }
        }
    }
}
