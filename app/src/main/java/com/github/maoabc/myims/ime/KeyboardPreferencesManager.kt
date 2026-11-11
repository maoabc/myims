package com.github.maoabc.myims.ime

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.github.maoabc.librime.RimeSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import java.io.File

object RimeOptions {
    const val FULL_SHAPE = "full_shape"
    const val ASCII_PUNCT = "ascii_punct"
    const val TRADITIONALIZATION = "traditional"
    const val ASCII_MODE = "ascii_mode"
    const val EMOJI = "emoji"
    const val VIBRATION = "vibration_enabled"
}

data class KeyboardOptions(
    val asciiPunct: Boolean = false,
    val fullShape: Boolean = false,
    val tradition: Boolean = true,
    val emoji: Boolean = true,
    val vibrationEnabled: Boolean = true
)

object KeyboardPreferencesManager {
    private var instance: DataStore<Preferences>? = null

    var options by mutableStateOf(KeyboardOptions())
        private set

    private object Keys {
        val ASCII_PUNCT = booleanPreferencesKey(RimeOptions.ASCII_PUNCT)
        val FULL_SHAPE = booleanPreferencesKey(RimeOptions.FULL_SHAPE)
        val TRADITION = booleanPreferencesKey(RimeOptions.TRADITIONALIZATION)
        val EMOJI = booleanPreferencesKey(RimeOptions.EMOJI)
        val VIBRATION = booleanPreferencesKey(RimeOptions.VIBRATION)
    }

    private fun getDataStore(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext
        return instance ?: synchronized(this) {
            instance ?: run {
                // Direct Boot compatible: use device protected storage context
                val directBootContext = appContext.createDeviceProtectedStorageContext()
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
                    produceFile = {
                        File(
                            directBootContext.filesDir,
                            "datastore/keyboard_preferences.preferences_pb"
                        )
                    }
                ).also { instance = it }
            }
        }
    }

    suspend fun loadOptions(context: Context): KeyboardOptions? {
        try {
            val prefs = getDataStore(context).data.first()
            options = KeyboardOptions(
                asciiPunct = prefs[Keys.ASCII_PUNCT] ?: false,
                fullShape = prefs[Keys.FULL_SHAPE] ?: false,
                tradition = prefs[Keys.TRADITION] ?: true,
                emoji = prefs[Keys.EMOJI] ?: true,
                vibrationEnabled = prefs[Keys.VIBRATION] ?: true
            )
        } catch (_: Exception) {
            return null
        }
        return options
    }

    fun applyToSession(session: RimeSession, options: KeyboardOptions) {
        session.setOption(RimeOptions.ASCII_PUNCT, options.asciiPunct)
        session.setOption(RimeOptions.TRADITIONALIZATION, options.tradition)
        session.setOption(RimeOptions.FULL_SHAPE, options.fullShape)
        session.setOption(RimeOptions.EMOJI, options.emoji)
    }

    fun getOptionsFromSession(session: RimeSession): KeyboardOptions {
        return KeyboardOptions(
            asciiPunct = session.getOption(RimeOptions.ASCII_PUNCT),
            fullShape = session.getOption(RimeOptions.FULL_SHAPE),
            emoji = session.getOption(RimeOptions.EMOJI),
            tradition = session.getOption(RimeOptions.TRADITIONALIZATION),
            vibrationEnabled = options.vibrationEnabled
        )
    }

    suspend fun toggleOption(
        context: Context,
        session: RimeSession?,
        option: String,
        currentValue: Boolean
    ): Boolean {
        val next = !currentValue
        if (session != null && option != RimeOptions.VIBRATION) {
            session.setOption(option, next)
        }

        options = when (option) {
            RimeOptions.ASCII_PUNCT -> options.copy(asciiPunct = next)
            RimeOptions.FULL_SHAPE -> options.copy(fullShape = next)
            RimeOptions.TRADITIONALIZATION -> options.copy(tradition = next)
            RimeOptions.EMOJI -> options.copy(emoji = next)
            RimeOptions.VIBRATION -> options.copy(vibrationEnabled = next)
            else -> options
        }

        val key = when (option) {
            RimeOptions.ASCII_PUNCT -> Keys.ASCII_PUNCT
            RimeOptions.FULL_SHAPE -> Keys.FULL_SHAPE
            RimeOptions.TRADITIONALIZATION -> Keys.TRADITION
            RimeOptions.EMOJI -> Keys.EMOJI
            RimeOptions.VIBRATION -> Keys.VIBRATION
            else -> null
        }

        if (key != null) {
            getDataStore(context).edit { prefs ->
                prefs[key] = next
            }
        }
        return next
    }
}
