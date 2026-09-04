package com.pocketpdf.data

import android.content.Context
import com.pocketpdf.model.HistoryItem
import com.pocketpdf.model.HistoryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("pdf_toolbox_history", Context.MODE_PRIVATE)
    private val _historyItems = MutableStateFlow<List<HistoryItem>>(emptyList())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val rawJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<HistoryItem>()

        runCatching {
            val jsonArray = JSONArray(rawJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        filePath = obj.getString("filePath"),
                        type = HistoryType.valueOf(obj.optString("type", HistoryType.COMPRESSED_PDF.name)),
                        originalSizeBytes = obj.optLong("originalSizeBytes", 0L),
                        resultSizeBytes = obj.optLong("resultSizeBytes", 0L),
                        pageCount = obj.optInt("pageCount", 1),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }
        _historyItems.value = list.sortedByDescending { it.timestamp }
    }

    suspend fun addItem(item: HistoryItem) = withContext(Dispatchers.IO) {
        val current = _historyItems.value.toMutableList()
        current.removeAll { it.id == item.id || it.filePath == item.filePath }
        current.add(0, item)
        saveList(current)
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val current = _historyItems.value.filterNot { it.id == id }
        saveList(current)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        saveList(emptyList())
    }

    private fun saveList(items: List<HistoryItem>) {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("filePath", item.filePath)
                put("type", item.type.name)
                put("originalSizeBytes", item.originalSizeBytes)
                put("resultSizeBytes", item.resultSizeBytes)
                put("pageCount", item.pageCount)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
        _historyItems.value = items
    }

    companion object {
        private const val KEY_HISTORY = "history_items_json"
    }
}
