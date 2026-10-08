package com.nautesh.challengeclock.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZoneId

/** The user's world-clock cities, stored as an ordered list of zone ids. */
class WorldClocks(private val prefs: SharedPreferences) {
    private val _zones = MutableStateFlow(
        prefs.getString(KEY, null)?.split(',')?.filter { it.isNotBlank() } ?: DEFAULTS,
    )
    val zones: StateFlow<List<String>> = _zones

    /** Adds [zone] at [at] (default: the end), e.g. back where it was when a removal is undone. */
    fun add(zone: String, at: Int = Int.MAX_VALUE) =
        save((_zones.value - zone).toMutableList().apply { add(at.coerceIn(0, size), zone) })
    fun remove(zone: String) = save(_zones.value - zone)

    private fun save(list: List<String>) {
        _zones.value = list
        prefs.edit().putString(KEY, list.joinToString(",")).apply()
    }

    companion object {
        private const val KEY = "zones"
        private val DEFAULTS = listOf("Europe/London", "America/New_York", "America/Los_Angeles", "Asia/Tokyo")

        private val regions = setOf("Africa", "America", "Antarctica", "Asia", "Atlantic", "Australia", "Europe", "Indian", "Pacific")

        // ponytail: region whitelist + no all-caps names drops most legacy aliases ("US/Alaska", "Australia/ACT");
        // ICU's canonical ids can't be used because they keep old names (Asia/Calcutta over Asia/Kolkata).
        val pickable: List<String> by lazy {
            ZoneId.getAvailableZoneIds()
                .filter { it.substringBefore('/') in regions && cityName(it).any(Char::isLowerCase) }
                .sortedBy { cityName(it) }
        }

        fun cityName(zone: String) = zone.substringAfterLast('/').replace('_', ' ')
        fun region(zone: String) = zone.substringBefore('/').replace('_', ' ')
    }
}
