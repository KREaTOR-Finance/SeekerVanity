package com.kreation.vanity

import com.kreation.vanity.crypto.Bip39
import org.junit.Assert.assertEquals
import org.junit.Test

class Bip39Test {

    private val wordlist = (0 until 2048).map { "w$it" }

    @Test
    fun entropy128_produces12Words() {
        val ent = ByteArray(16) { 0 }
        val words = Bip39.entropyToMnemonic(ent, wordlist)
        assertEquals(12, words.size)
    }

    @Test
    fun entropy256_produces24Words() {
        val ent = ByteArray(32) { 0 }
        val words = Bip39.entropyToMnemonic(ent, wordlist)
        assertEquals(24, words.size)
    }
}
