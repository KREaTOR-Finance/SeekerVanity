package com.kreation.vanity.crypto

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.macs.HMac
import org.bouncycastle.crypto.params.KeyParameter

/**
 * Minimal SLIP-0010 hardened derivation for Ed25519.
 *
 * Master: I = HMAC-SHA512(key="ed25519 seed", data=seed)
 * Child hardened: data = 0x00 || k || ser32(i)
 */
object Slip10Ed25519 {

    data class Node(val key: ByteArray, val chainCode: ByteArray)

    fun master(seed: ByteArray): Node {
        val i = hmacSha512("ed25519 seed".toByteArray(Charsets.UTF_8), seed)
        return Node(
            key = i.copyOfRange(0, 32),
            chainCode = i.copyOfRange(32, 64)
        )
    }

    fun deriveHardened(node: Node, index: Int): Node {
        require(index >= 0) { "index must be non-negative" }
        val hardened = index or 0x80000000.toInt()
        val data = ByteArray(1 + 32 + 4)
        data[0] = 0
        System.arraycopy(node.key, 0, data, 1, 32)
        // big-endian ser32
        data[33] = ((hardened ushr 24) and 0xFF).toByte()
        data[34] = ((hardened ushr 16) and 0xFF).toByte()
        data[35] = ((hardened ushr 8) and 0xFF).toByte()
        data[36] = (hardened and 0xFF).toByte()

        val i = hmacSha512(node.chainCode, data)
        return Node(
            key = i.copyOfRange(0, 32),
            chainCode = i.copyOfRange(32, 64)
        )
    }

    fun derivePathHardened(master: Node, path: IntArray): Node {
        var n = master
        for (p in path) {
            n = deriveHardened(n, p)
        }
        return n
    }

    private fun hmacSha512(key: ByteArray, data: ByteArray): ByteArray {
        val hmac = HMac(SHA512Digest())
        hmac.init(KeyParameter(key))
        hmac.update(data, 0, data.size)
        val out = ByteArray(64)
        hmac.doFinal(out, 0)
        return out
    }
}
