package com.example.livess

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class LiveSsRepository private constructor(private val context: Context) {

    private val prefs = context.getSharedPreferences("savior_live_ss_store", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow<List<LiveSsItem>>(emptyList())
    val items: StateFlow<List<LiveSsItem>> = _items.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val jsonStr = prefs.getString(KEY_ITEMS, null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<LiveSsItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val stillUriStr = obj.optString("stillUri", "")
                if (stillUriStr.isEmpty()) continue
                val videoUriStr = obj.optString("videoUri", "")
                val modeName = obj.optString("mode", "")
                val mode = LiveSsMode.values().firstOrNull { it.name == modeName } ?: LiveSsMode.LIVE_SCREENSHOT
                list.add(
                    LiveSsItem(
                        id = obj.getString("id"),
                        title = obj.optString("title", obj.getString("id")),
                        stillUri = Uri.parse(stillUriStr),
                        videoUri = if (videoUriStr.isNotEmpty()) Uri.parse(videoUriStr) else null,
                        createdAt = obj.getLong("createdAt"),
                        durationSeconds = obj.optDouble("durationSeconds", 0.0).toFloat(),
                        mode = mode,
                        width = obj.optInt("width", 1080),
                        height = obj.optInt("height", 1920)
                    )
                )
            }
            _items.value = list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveItems(list: List<LiveSsItem>) {
        try {
            val arr = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("stillUri", item.stillUri.toString())
                    put("videoUri", item.videoUri?.toString() ?: "")
                    put("createdAt", item.createdAt)
                    put("durationSeconds", item.durationSeconds.toDouble())
                    put("mode", item.mode.name)
                    put("width", item.width)
                    put("height", item.height)
                }
                arr.put(obj)
            }
            prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
            _items.value = list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addItem(item: LiveSsItem) {
        val current = _items.value.toMutableList()
        current.removeAll { it.id == item.id }
        current.add(0, item)
        saveItems(current)
    }

    fun deleteItem(id: String) {
        val current = _items.value.toMutableList()
        val target = current.firstOrNull { it.id == id }
        if (target != null) {
            current.remove(target)
            saveItems(current)
            try {
                context.contentResolver.delete(target.stillUri, null, null)
                target.videoUri?.let { context.contentResolver.delete(it, null, null) }
            } catch (e: Exception) {
                // Ignore failure if already removed
            }
        }
    }

    fun clearAll() {
        val current = _items.value
        for (item in current) {
            try {
                context.contentResolver.delete(item.stillUri, null, null)
                item.videoUri?.let { context.contentResolver.delete(it, null, null) }
            } catch (e: Exception) {
                // Ignore
            }
        }
        prefs.edit().remove(KEY_ITEMS).apply()
        _items.value = emptyList()
    }

    companion object {
        private const val KEY_ITEMS = "live_ss_items_json"

        @Volatile
        private var instance: LiveSsRepository? = null

        fun getInstance(context: Context): LiveSsRepository {
            return instance ?: synchronized(this) {
                instance ?: LiveSsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
