package com.nautesh.challengeclock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ChallengePlanTest {
    private val three = Alarm(hour = 7, minute = 0, challenges = "math,memory,shake")

    @Test fun noChallenges() = assertEquals(emptyList<ChallengeType>(), Alarm(hour = 7, minute = 0).challengePlan())

    @Test fun zeroCountMeansAllInOrder() =
        assertEquals(listOf(ChallengeType.MATH, ChallengeType.MEMORY, ChallengeType.SHAKE), three.challengePlan())

    @Test fun countCutsTheList() =
        assertEquals(listOf(ChallengeType.MATH, ChallengeType.MEMORY), three.copy(challengeCount = 2).challengePlan())

    @Test fun shuffleKeepsSameSet() {
        val plan = three.copy(shuffleChallenges = true).challengePlan(Random(42))
        assertEquals(three.challengeTypes.toSet(), plan.toSet())
    }

    @Test fun unknownIdsIgnored() = assertEquals(listOf(ChallengeType.MATH), ChallengeType.parse("math,,bogus"))

    @Test fun retypeIgnoresCaseAndSpacing() {
        assertTrue(sameWords("  Amber   river ", "amber river"))
        assertFalse(sameWords("amber rive", "amber river"))
    }
}
