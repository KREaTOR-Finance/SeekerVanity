package com.kreation.vanity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
    vm: GeneratorViewModel,
    onReveal: (List<String>) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val mwa = com.kreation.vanity.mwa.LocalMwaEnv.current
    val walletButtonText = when {
        ui.connectingWallet -> "Connecting..."
        ui.walletConnected -> "Disconnect"
        else -> "Connect Wallet"
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AppHeader(
                action = {
                    Button(
                        onClick = {
                            if (mwa != null) {
                                if (ui.walletConnected) vm.disconnectWallet(mwa) else vm.connectWallet(mwa)
                            }
                        },
                        enabled = mwa != null && !ui.connectingWallet,
                    ) {
                        Text(walletButtonText)
                    }
                }
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Instructions & Helpers", style = MaterialTheme.typography.titleMedium)
                    Text("1) Connect wallet from the top-right button (ownership approval only).")
                    Text("2) Pick mode and pattern, then tap Start.")
                    Text("3) When a match is found, either reveal the seed phrase or wipe it. Wipe permanently removes the generated address and seed phrase from this app.")
                    Text("4) Verify the requested words to complete reveal.")
                    Text("Wallet status: ${if (ui.walletConnected) "Connected" else "Not connected"}")
                }
            }
        }

        if (ui.error != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Alert", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                        Text(ui.error!!)
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Seed phrase (12 words)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Current beta flow uses a 12-word BIP39 seed phrase for speed.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(6.dp))

                    Text("Mode", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.setMode(VanityMode.SUFFIX) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ui.mode == VanityMode.SUFFIX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) { Text("Suffix") }

                        Button(
                            onClick = { vm.setMode(VanityMode.SKR_PREFIX) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (ui.mode == VanityMode.SKR_PREFIX) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                            )
                        ) { Text("SKR Prefix") }
                    }

                    Text(
                        "Searching and reveal are free.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (ui.mode == VanityMode.SUFFIX) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Suffix (max 6)", style = MaterialTheme.typography.titleMedium)

                        OutlinedTextField(
                            value = ui.suffix,
                            onValueChange = { vm.setSuffix(it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                keyboardType = KeyboardType.Ascii
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ignore case")
                            Switch(checked = ui.ignoreCase, onCheckedChange = { vm.setIgnoreCase(it) })
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.setSuffix("RAVEN") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("RAVEN") }
                            Button(
                                onClick = { vm.setSuffix("SEEKER") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) { Text("SEEKER") }
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    Text("Attempts: ${ui.attempts}")
                    Text("Keys/sec: ${ui.keysPerSec}")

                    if (ui.found != null) {
                        Spacer(Modifier.height(6.dp))
                        Text("Found:", color = MaterialTheme.colorScheme.primary)
                        Text(ui.found!!.address)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { vm.revealFound(onReveal) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Reveal seed phrase") }

                            Button(
                                onClick = { vm.discardFoundAndContinue() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Wipe") }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { vm.start(ctx) }, enabled = !ui.running) { Text("Start") }
                        Button(onClick = { vm.stop() }, enabled = ui.running) { Text("Stop") }
                    }

                    Text(
                        "On-device only. We do not store secrets.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        item {
            PoweredByFooter()
        }
    }
}
