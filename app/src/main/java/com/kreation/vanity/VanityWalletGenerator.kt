package com.kreation.vanity

import android.content.Context
import com.kreation.vanity.crypto.Bip39
import com.kreation.vanity.crypto.SolanaKeys

object VanityWalletGenerator {

    @Volatile
    private var wordlist: List<String>? = null

    private fun getWordlist(context: Context): List<String> {
        val existing = wordlist
        if (existing != null) return existing
        val loaded = Bip39.loadWordlist(context)
        wordlist = loaded
        return loaded
    }

    data class Wallet(
        val mnemonicWords: List<String>,
        val address: String,
    )

    /**
     * Generates a mnemonic (12 or 24 words), derives Solana address, returns both.
     *
     * NOTE: This does PBKDF2 (2048 rounds) per attempt; vanity search time varies by device.
     */
    fun generateMnemonicSolanaWallet(context: Context, wordCount: Int = 12): Wallet {
        // Keep 12 words for the current beta flow.
        val wl = getWordlist(context)
        val entropy = Bip39.generateEntropy(128)
        val mnemonic = Bip39.entropyToMnemonic(entropy, wl)
        val derived = SolanaKeys.fromMnemonic(mnemonic)
        return Wallet(mnemonicWords = mnemonic, address = derived.address)
    }
}
