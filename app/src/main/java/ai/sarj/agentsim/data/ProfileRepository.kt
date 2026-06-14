package ai.sarj.agentsim.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ai.sarj.agentsim.model.DailyObjective
import ai.sarj.agentsim.model.ObjectiveKind
import ai.sarj.agentsim.model.PlayerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.profileStore: DataStore<Preferences> by preferencesDataStore(name = "player_profile")

/** Loads/saves the persistent PlayerProfile via Jetpack DataStore (offline, no backend). */
class ProfileRepository(context: Context) {

    private val store = context.applicationContext.profileStore

    private object K {
        val TOKENS = intPreferencesKey("tokens")
        val XP = longPreferencesKey("xp")
        val STREAK = intPreferencesKey("streak")
        val LAST_DAY = longPreferencesKey("last_day")
        val UPGRADE_LEVELS = stringPreferencesKey("upgrade_levels")   // "id:level|id:level|..."
        val NAME = stringPreferencesKey("name")
        val THEME = intPreferencesKey("theme")
        val MUTED = booleanPreferencesKey("muted")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val SEEN_INTENTS = stringSetPreferencesKey("seen_intents")
        val FIRST_WIN_DAY = longPreferencesKey("first_win_day")
        val BEST_ENDLESS = intPreferencesKey("best_endless")
        val LIFETIME = intPreferencesKey("lifetime_served")
        val DAILY_DAY = longPreferencesKey("daily_day")
        val DAILY = stringPreferencesKey("daily")   // "KIND:progress:claimed|KIND:progress:claimed|..."
    }

    private fun encodeLevels(m: Map<String, Int>): String =
        m.entries.joinToString("|") { "${it.key}:${it.value}" }

    private fun decodeLevels(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split("|").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size != 2) return@mapNotNull null
            val lvl = bits[1].toIntOrNull() ?: return@mapNotNull null
            bits[0] to lvl
        }.toMap()
    }

    private fun encodeDaily(daily: List<DailyObjective>): String =
        daily.joinToString("|") { "${it.kind.name}:${it.progress}:${if (it.claimed) 1 else 0}" }

    private fun decodeDaily(raw: String?): List<DailyObjective> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val bits = part.split(":")
            if (bits.size != 3) return@mapNotNull null
            val kind = runCatching { ObjectiveKind.valueOf(bits[0]) }.getOrNull() ?: return@mapNotNull null
            DailyObjective(kind, bits[1].toIntOrNull() ?: 0, bits[2] == "1")
        }
    }

    val profile: Flow<PlayerProfile> = store.data.map { p ->
        PlayerProfile(
            tokens = p[K.TOKENS] ?: 0,
            xp = p[K.XP] ?: 0L,
            streakDays = p[K.STREAK] ?: 0,
            lastPlayedEpochDay = p[K.LAST_DAY] ?: 0L,
            upgradeLevels = decodeLevels(p[K.UPGRADE_LEVELS]),
            agentName = p[K.NAME] ?: "",
            themeId = p[K.THEME] ?: 0,
            muted = p[K.MUTED] ?: false,
            tutorialSeen = p[K.TUTORIAL_SEEN] ?: false,
            seenIntents = p[K.SEEN_INTENTS] ?: emptySet(),
            firstWinDay = p[K.FIRST_WIN_DAY] ?: -1L,
            bestEndless = p[K.BEST_ENDLESS] ?: 0,
            lifetimeServed = p[K.LIFETIME] ?: 0,
            dailyDay = p[K.DAILY_DAY] ?: -1L,
            daily = decodeDaily(p[K.DAILY])
        )
    }

    suspend fun save(profile: PlayerProfile) {
        store.edit { p ->
            p[K.TOKENS] = profile.tokens
            p[K.XP] = profile.xp
            p[K.STREAK] = profile.streakDays
            p[K.LAST_DAY] = profile.lastPlayedEpochDay
            p[K.UPGRADE_LEVELS] = encodeLevels(profile.upgradeLevels)
            p[K.NAME] = profile.agentName
            p[K.THEME] = profile.themeId
            p[K.MUTED] = profile.muted
            p[K.TUTORIAL_SEEN] = profile.tutorialSeen
            p[K.SEEN_INTENTS] = profile.seenIntents
            p[K.FIRST_WIN_DAY] = profile.firstWinDay
            p[K.BEST_ENDLESS] = profile.bestEndless
            p[K.LIFETIME] = profile.lifetimeServed
            p[K.DAILY_DAY] = profile.dailyDay
            p[K.DAILY] = encodeDaily(profile.daily)
        }
    }
}
