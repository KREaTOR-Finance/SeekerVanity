package com.kreation.vanity.util

import android.util.Log

/**
 * Logging helper with a hard rule: never log secrets.
 *
 * - Do not log mnemonics, private keys, or raw transaction bytes.
 * - Public addresses and tx signatures are OK.
 */
object SafeLog {
    private const val TAG = "Vanity"

    fun d(msg: String) = Log.d(TAG, msg)
    fun i(msg: String) = Log.i(TAG, msg)
    fun w(msg: String, t: Throwable? = null) = if (t != null) Log.w(TAG, msg, t) else Log.w(TAG, msg)
    fun e(msg: String, t: Throwable? = null) = if (t != null) Log.e(TAG, msg, t) else Log.e(TAG, msg)

    fun redact(s: String?): String {
        if (s == null) return "<null>"
        // Best-effort: if it looks like a mnemonic (contains spaces), redact fully.
        if (s.contains(' ')) return "<redacted>"
        // Long blobs: partially redact
        return if (s.length > 16) s.take(6) + "…" + s.takeLast(4) else s
    }
}
