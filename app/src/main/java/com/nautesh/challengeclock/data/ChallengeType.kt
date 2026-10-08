package com.nautesh.challengeclock.data

import kotlin.random.Random

enum class ChallengeType(val id: String) {
    MATH("math"),
    MEMORY("memory"),
    SHAPES("shapes"),
    RETYPE("retype"),
    SHAKE("shake"),
    ;

    companion object {
        fun parse(csv: String): List<ChallengeType> =
            csv.split(',').mapNotNull { id -> entries.firstOrNull { it.id == id } }
    }
}

fun List<ChallengeType>.toCsv() = joinToString(",") { it.id }

val Alarm.challengeTypes: List<ChallengeType> get() = ChallengeType.parse(challenges)

/** The challenges to solve for this ring: shuffled if asked, cut to the chosen count (0 = all). */
fun Alarm.challengePlan(random: Random = Random.Default): List<ChallengeType> {
    val all = challengeTypes.let { if (shuffleChallenges) it.shuffled(random) else it }
    return if (challengeCount in 1 until all.size) all.take(challengeCount) else all
}

/** Retype answers match regardless of case and spacing. */
fun sameWords(typed: String, target: String): Boolean {
    fun norm(s: String) = s.trim().lowercase().split(Regex("\\s+")).joinToString(" ")
    return norm(typed) == norm(target)
}
