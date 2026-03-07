package com.kreation.vanity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onPayments: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    var showInfo by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Vanity",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(onClick = { showInfo = !showInfo }) {
                    Text(if (showInfo) "Hide info" else "Info")
                }
            }
        }

        if (showInfo) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("About", style = MaterialTheme.typography.titleMedium)
                        Text("Brought to you by @buidlerlabs LLC", style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "SeekerMigrate helps onboard app developers to the SKR ecosystem and Solana Mobile dApp Store. " +
                                "It offers a devkit of common tools to help developers quickly migrate their app to SKR and Solana.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("SeekerMigrate.com — Coming soon", style = MaterialTheme.typography.bodyMedium)
                        Text("Telegram: https://t.me/SeekerMigrate", style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Security: on-device only, we do not store secrets. You can wipe found wallets irreversibly.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Payments: Pay & Reveal sends 250 SKR on mainnet to the project treasury.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
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
                        "Seed Vault oriented MVP uses a 12-word BIP39 seed phrase for speed.",
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
                        "Payment is per reveal (250 SKR). Searching is free.",
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

                        val mwa = com.kreation.vanity.mwa.LocalMwaEnv.current

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (mwa != null) {
                                        vm.payAndReveal(mwa) {
                                            onReveal(ui.found!!.mnemonic24)
                                        }
                                    } else {
                                        vm.stop()
                                    }
                                },
                                enabled = !ui.awaitingRevealPayment,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (ui.awaitingRevealPayment) "Paying…" else "Pay & Reveal (250 SKR)") }

                            Button(
                                onClick = { vm.discardFoundAndContinue() },
                                enabled = !ui.awaitingRevealPayment,
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
    }
}
