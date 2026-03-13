package com.kreation.vanity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SeedPhraseVerificationTest {
    private val words = listOf(
        "abandon", "ability", "able", "about", "above", "absent",
        "absorb", "abstract", "absurd", "abuse", "access", "accident"
    )

    @Test
    fun accepts_case_and_surrounding_whitespace() {
        val ok = SeedPhraseVerification.areRequestedWordsCorrect(
            words = words,
            positions = listOf(1, 5, 12),
            answers = listOf("  ABANDON ", "Above", " accident  ")
        )
        assertTrue(ok)
    }

    @Test
    fun rejects_wrong_word() {
        val ok = SeedPhraseVerification.areRequestedWordsCorrect(
            words = words,
            positions = listOf(2, 3, 4),
            answers = listOf("ability", "WRONG", "about")
        )
        assertFalse(ok)
    }

    @Test
    fun rejects_invalid_position() {
        val ok = SeedPhraseVerification.areRequestedWordsCorrect(
            words = words,
            positions = listOf(1, 13, 2),
            answers = listOf("abandon", "x", "ability")
        )
        assertFalse(ok)
    }

    @Test
    fun rejects_mismatched_answer_count() {
        val ok = SeedPhraseVerification.areRequestedWordsCorrect(
            words = words,
            positions = listOf(1, 2, 3),
            answers = listOf("abandon", "ability")
        )
        assertFalse(ok)
    }

    @Test
    fun rejects_duplicate_positions() {
        val ok = SeedPhraseVerification.areRequestedWordsCorrect(
            words = words,
            positions = listOf(1, 1, 3),
            answers = listOf("abandon", "abandon", "able")
        )
        assertFalse(ok)
    }

    @Test
    fun random_positions_are_sorted_unique_and_in_range() {
        val picks = SeedPhraseVerification.randomPositions(
            totalWords = 12,
            count = 3,
            random = Random(1234)
        )
        assertEquals(3, picks.size)
        assertEquals(picks.sorted(), picks)
        assertEquals(3, picks.toSet().size)
        assertTrue(picks.all { it in 1..12 })
    }
}
