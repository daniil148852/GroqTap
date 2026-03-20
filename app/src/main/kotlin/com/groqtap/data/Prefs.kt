package com.groqtap.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("groqtap_prefs")

class Prefs(private val context: Context) {

    private object Keys {
        val API_KEY      = stringPreferencesKey("api_key")
        val MODEL_ID     = stringPreferencesKey("model_id")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val WIDGET_ENABLED = booleanPreferencesKey("widget_enabled")
        val TEMPERATURE  = stringPreferencesKey("temperature")
    }

    val apiKey: Flow<String> = context.dataStore.data
        .map { it[Keys.API_KEY] ?: "" }

    val modelId: Flow<String> = context.dataStore.data
        .map { it[Keys.MODEL_ID] ?: GroqModel.LLAMA_3_3_70B.id }

    val systemPrompt: Flow<String> = context.dataStore.data
        .map { it[Keys.SYSTEM_PROMPT] ?: GroqApi.DEFAULT_SYSTEM_PROMPT }

    val widgetEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[Keys.WIDGET_ENABLED] ?: false }

    val temperature: Flow<Float> = context.dataStore.data
        .map { it[Keys.TEMPERATURE]?.toFloatOrNull() ?: 0.7f }

    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.API_KEY] = key }
    suspend fun setModelId(id: String) = context.dataStore.edit { it[Keys.MODEL_ID] = id }
    suspend fun setSystemPrompt(p: String) = context.dataStore.edit { it[Keys.SYSTEM_PROMPT] = p }
    suspend fun setWidgetEnabled(v: Boolean) = context.dataStore.edit { it[Keys.WIDGET_ENABLED] = v }
    suspend fun setTemperature(v: Float) = context.dataStore.edit { it[Keys.TEMPERATURE] = v.toString() }
}
