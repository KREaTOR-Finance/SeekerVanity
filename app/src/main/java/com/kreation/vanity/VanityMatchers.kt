package com.kreation.vanity

object VanityMatchers {
    fun endsWith(address: String, suffix: String, ignoreCase: Boolean): Boolean {
        if (suffix.isEmpty()) return false
        if (suffix.length > address.length) return false
        return address.endsWith(suffix, ignoreCase = ignoreCase)
    }

    fun startsWith(address: String, prefix: String, ignoreCase: Boolean): Boolean {
        if (prefix.isEmpty()) return false
        if (prefix.length > address.length) return false
        return address.startsWith(prefix, ignoreCase = ignoreCase)
    }
}
