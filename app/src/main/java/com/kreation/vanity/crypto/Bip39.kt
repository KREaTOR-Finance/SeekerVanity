package com.kreation.vanity.crypto

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Minimal BIP39 implementation (English wordlist).
 *
 * - Entropy: 256 bits => 24 words
 * - Seed: PBKDF2-HMAC-SHA512(mnemonic, "mnemonic"+passphrase, 2048, 64)
 */
object Bip39 {

    private const val WORDLIST_ASSET = "bip39_english.txt"
    private const val PBKDF2_ROUNDS = 2048
    private const val SEED_BYTES = 64

    private val rng = SecureRandom()

    @Volatile
    private var cachedWordlist: List<String>? = null

    fun loadWordlist(context: Context): List<String> {
        val existing = cachedWordlist
        if (existing != null) return existing

        val words = context.assets.open(WORDLIST_ASSET).use { input ->
            BufferedReader(InputStreamReader(input)).readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        require(words.size == 2048) { "BIP39 wordlist must contain 2048 words; got ${words.size}" }
        cachedWordlist = words
        return words
    }

    fun generateEntropy(numBits: Int): ByteArray {
        require(numBits == 128 || numBits == 256) { "Supported entropy: 128 or 256" }
        val b = ByteArray(numBits / 8)
        rng.nextBytes(b)
        return b
    }

    fun entropyToMnemonic(entropy: ByteArray, wordlist: List<String>): List<String> {
        require(entropy.size == 16 || entropy.size == 32) { "Expected 16 bytes (12 words) or 32 bytes (24 words)" }

        val entBits = entropy.size * 8
        val checksumBits = entBits / 32
        val totalBits = entBits + checksumBits
        val sha = MessageDigest.getInstance("SHA-256").digest(entropy)

        val bits = BooleanArray(totalBits)
        var bitIndex = 0

        for (byte in entropy) {
            val v = byte.toInt() and 0xFF
            for (i in 7 downTo 0) {
                bits[bitIndex++] = ((v shr i) and 1) == 1
            }
        }

        // Append checksumBits from sha[0..]
        var csRemaining = checksumBits
        var shaIndex = 0
        while (csRemaining > 0) {
            val v = sha[shaIndex].toInt() and 0xFF
            val take = minOf(8, csRemaining)
            for (i in 7 downTo (8 - take)) {
                bits[bitIndex++] = ((v shr i) and 1) == 1
            }
            csRemaining -= take
            shaIndex++
        }

        val wordCount = totalBits / 11
        val out = ArrayList<String>(wordCount)
        for (w in 0 until wordCount) {
            var idx = 0
            for (i in 0 until 11) {
                idx = (idx shl 1) or (if (bits[w * 11 + i]) 1 else 0)
            }
            out.add(wordlist[idx])
        }
        return out
    }

    fun mnemonicToSeed(mnemonicWords: List<String>, passphrase: String = ""): ByteArray {
        val mnemonic = mnemonicWords.joinToString(" ")
        val salt = "mnemonic" + passphrase
        val spec = PBEKeySpec(mnemonic.toCharArray(), salt.toByteArray(Charsets.UTF_8), PBKDF2_ROUNDS, SEED_BYTES * 8)
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
        return skf.generateSecret(spec).encoded
    }
}
