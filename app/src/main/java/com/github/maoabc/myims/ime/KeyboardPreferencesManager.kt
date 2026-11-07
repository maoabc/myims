package com.github.maoabc.myims.ime

import android.content.Context
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
    const val TRADITIONALIZATION = "traditionalization"
    const val ASCII_MODE = "ascii_mode"
    const val EMOJI = "emoji"
}

data class KeyboardOptions(
    val asciiPunct: Boolean = false,
    val fullShape: Boolean = false,
    val simplified: Boolean = true,
    val emoji: Boolean = true
)

object KeyboardPreferencesManager {
    private var instance: DataStore<Preferences>? = null

    private object Keys {
        val ASCII_PUNCT = booleanPreferencesKey(RimeOptions.ASCII_PUNCT)
        val FULL_SHAPE = booleanPreferencesKey(RimeOptions.FULL_SHAPE)
        val SIMPLIFIED = booleanPreferencesKey(RimeOptions.TRADITIONALIZATION)
        val EMOJI = booleanPreferencesKey(RimeOptions.EMOJI)
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

    suspend fun loadOptions(context: Context): KeyboardOptions {
        val prefs = getDataStore(context).data.first()
        return KeyboardOptions(
            asciiPunct = prefs[Keys.ASCII_PUNCT] ?: false,
            fullShape = prefs[Keys.FULL_SHAPE] ?: false,
            simplified = prefs[Keys.SIMPLIFIED] ?: true,
            emoji = prefs[Keys.EMOJI] ?: true
        )
    }

    fun applyToSession(session: RimeSession, options: KeyboardOptions) {
        session.setOption(RimeOptions.ASCII_PUNCT, options.asciiPunct)
        session.setOption(RimeOptions.TRADITIONALIZATION, options.simplified)
        session.setOption(RimeOptions.FULL_SHAPE, options.fullShape)
        session.setOption(RimeOptions.EMOJI, options.emoji)
    }

    suspend fun toggleOption(
        context: Context,
        session: RimeSession,
        option: String,
        currentValue: Boolean
    ): Boolean {
        val next = !currentValue
        session.setOption(option, next)

        val key = when (option) {
            RimeOptions.ASCII_PUNCT -> Keys.ASCII_PUNCT
            RimeOptions.FULL_SHAPE -> Keys.FULL_SHAPE
            RimeOptions.TRADITIONALIZATION -> Keys.SIMPLIFIED
            RimeOptions.EMOJI -> Keys.EMOJI
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
