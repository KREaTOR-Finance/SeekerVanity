package com.kreation.vanity.mwa

import androidx.compose.runtime.staticCompositionLocalOf
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter

data class MwaEnv(
    val sender: ActivityResultSender,
    val adapter: MobileWalletAdapter,
)

val LocalMwaEnv = staticCompositionLocalOf<MwaEnv?> { null }
