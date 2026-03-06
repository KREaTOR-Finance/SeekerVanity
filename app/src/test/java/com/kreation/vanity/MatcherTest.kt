package com.kreation.vanity

import org.junit.Assert.*
import org.junit.Test

class MatcherTest {
    @Test
    fun suffixIgnoreCaseWorks() {
        assertTrue(VanityMatchers.endsWith("AbCdEf", "def", ignoreCase = true))
        assertFalse(VanityMatchers.endsWith("AbCdEf", "def", ignoreCase = false))
    }

    @Test
    fun prefixCaseSensitiveWorks() {
        assertTrue(VanityMatchers.startsWith("SKRxyz", "SKR", ignoreCase = false))
        assertFalse(VanityMatchers.startsWith("skrxyz", "SKR", ignoreCase = false))
    }
}
