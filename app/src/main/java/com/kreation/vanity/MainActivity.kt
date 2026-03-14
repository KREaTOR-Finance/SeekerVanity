package com.kreation.vanity

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.kreation.vanity.mwa.LocalMwaEnv
import com.kreation.vanity.mwa.MwaEnv
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide()

        // IMPORTANT: ActivityResultSender must be created/registered before STARTED.
        val sender = ActivityResultSender(this)
        val walletAdapter = MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://kreation.studio"),
                iconUri = Uri.parse("https://kreation.studio/favicon.ico"),
                identityName = "Vanity"
            )
        )

        setContent {
            CompositionLocalProvider(LocalMwaEnv provides MwaEnv(sender = sender, adapter = walletAdapter)) {
                VanityTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        AppNav()
                    }
                }
            }
        }
    }
}
