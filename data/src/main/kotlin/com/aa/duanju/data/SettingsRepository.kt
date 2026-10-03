package com.aa.duanju.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.aa.duanju.domain.AfterDrama
import com.aa.duanju.domain.PlayOrder
import com.aa.duanju.domain.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "app_settings")

private val KEY_AUTO_PLAY = booleanPreferencesKey("auto_play")
private val KEY_PLAY_ORDER = stringPreferencesKey("play_order")
private val KEY_KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
private val KEY_AFTER_DRAMA = stringPreferencesKey("after_drama")

@Singleton
class PreferencesSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    override fun observeAutoPlay(): Flow<Boolean> =
        context.settingsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[KEY_AUTO_PLAY] ?: true }

    override suspend fun setAutoPlay(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[KEY_AUTO_PLAY] = enabled }
    }

    override fun observePlayOrder(): Flow<PlayOrder> =
        context.settingsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                when (prefs[KEY_PLAY_ORDER]) {
                    "shuffle" -> PlayOrder.SHUFFLE
                    else -> PlayOrder.SEQUENTIAL
                }
            }

    override suspend fun setPlayOrder(order: PlayOrder) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_PLAY_ORDER] = when (order) {
                PlayOrder.SHUFFLE -> "shuffle"
                PlayOrder.SEQUENTIAL -> "sequential"
            }
        }
    }

    override fun observeKeepScreenOn(): Flow<Boolean> =
        context.settingsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs -> prefs[KEY_KEEP_SCREEN_ON] ?: true }

    override suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[KEY_KEEP_SCREEN_ON] = enabled }
    }

    override fun observeAfterDrama(): Flow<AfterDrama> =
        context.settingsDataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                when (prefs[KEY_AFTER_DRAMA]) {
                    "stop" -> AfterDrama.STOP
                    else -> AfterDrama.AUTO_NEXT
                }
            }

    override suspend fun setAfterDrama(after: AfterDrama) {
        context.settingsDataStore.edit { prefs ->
            prefs[KEY_AFTER_DRAMA] = when (after) {
                AfterDrama.STOP -> "stop"
                AfterDrama.AUTO_NEXT -> "auto_next"
            }
        }
    }
}
