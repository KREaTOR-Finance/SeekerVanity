package com.kreation.vanity

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private const val PREFS = "vanity_prefs"
private const val KEY_ACCEPTED = "accepted_disclaimer"

fun hasAcceptedDisclaimer(context: Context): Boolean =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ACCEPTED, false)

fun setAcceptedDisclaimer(context: Context, accepted: Boolean) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_ACCEPTED, accepted)
        .apply()
}

@Composable
fun WelcomeScreen(
    onContinue: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Vanity",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Read this first", style = MaterialTheme.typography.titleMedium)
                Text("• Wallets are generated on your phone only. We do not store your seed phrase.")
                Text("• If you lose your seed phrase, you lose the wallet.")
                Text("• Never share your seed phrase. We will never ask for it.")
                Text("• Search is free. Pay 250 SKR to reveal a found wallet’s seed phrase.")
                Text("• If you tap Wipe, that wallet is gone forever.")

                Text("\nBrought to you by @buidlerlabs LLC", style = MaterialTheme.typography.bodySmall)
            }
        }

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
        Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text("Exit")
        }
    }
}
