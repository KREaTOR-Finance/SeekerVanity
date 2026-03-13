package com.kreation.vanity

import java.util.Locale
import kotlin.random.Random

object SeedPhraseVerification {
    fun randomPositions(totalWords: Int, count: Int = 3, random: Random = Random.Default): List<Int> {
        require(totalWords > 0) { "totalWords must be > 0" }
        require(count in 1..totalWords) { "count must be in 1..totalWords" }
        return (1..totalWords).shuffled(random).take(count).sorted()
    }

    fun areRequestedWordsCorrect(
        words: List<String>,
        positions: List<Int>,
        answers: List<String>,
    ): Boolean {
        if (positions.isEmpty()) return false
        if (positions.size != answers.size) return false
        if (positions.toSet().size != positions.size) return false

        for (i in positions.indices) {
            val pos = positions[i]
            val expected = words.getOrNull(pos - 1) ?: return false
            val actual = answers[i]
            if (normalize(actual) != normalize(expected)) return false
        }
        return true
    }

    private fun normalize(word: String): String = word.trim().lowercase(Locale.ROOT)
}
