package com.kreation.vanity.crypto

import com.funkatronics.encoders.Base58
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters

/**
 * Derive a Solana address from a BIP39 mnemonic using SLIP-0010 ed25519.
 *
 * Default path used by many Solana wallets: m/44'/501'/0'/0'
 */
object SolanaKeys {

    private val SOLANA_DEFAULT_HARDENED_PATH = intArrayOf(44, 501, 0, 0)

    data class WalletFromMnemonic(
        val mnemonicWords: List<String>,
        val address: String,
        val publicKey: ByteArray,
        val privateKeySeed: ByteArray, // 32-byte ed25519 seed (do not persist)
    )

    fun fromMnemonic(mnemonicWords: List<String>, passphrase: String = ""): WalletFromMnemonic {
        val seed = Bip39.mnemonicToSeed(mnemonicWords, passphrase)
        val master = Slip10Ed25519.master(seed)
        val node = Slip10Ed25519.derivePathHardened(master, SOLANA_DEFAULT_HARDENED_PATH)

        val priv = Ed25519PrivateKeyParameters(node.key, 0)
        val pub = priv.generatePublicKey().encoded
        val addr = Base58.encodeToString(pub)

        return WalletFromMnemonic(
            mnemonicWords = mnemonicWords,
            address = addr,
            publicKey = pub,
            privateKeySeed = node.key
        )
    }
}
