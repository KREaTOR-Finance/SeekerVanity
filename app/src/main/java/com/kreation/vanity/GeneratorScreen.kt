package com.kreation.vanity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
    onPayments: () -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val ctx = androidx.compose.ui.platform.LocalContext.current

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Vanity",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        if (ui.error != null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Alert", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
                    Text(ui.error!!)
                }
            }
        }

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

        if (ui.mode == VanityMode.SUFFIX) {
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

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text("Attempts: ${ui.attempts}")
                Text("Keys/sec: ${ui.keysPerSec}")

                if (ui.found != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("Found:", color = MaterialTheme.colorScheme.primary)
                    Text(ui.found!!.address)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val act = (ctx as? androidx.activity.ComponentActivity)
                                if (act != null) {
                                    vm.payAndReveal(act) {
                                        onReveal(ui.found!!.mnemonic24)
                                    }
                                }
                            },
                            enabled = !ui.awaitingRevealPayment
                        ) { Text(if (ui.awaitingRevealPayment) "Paying…" else "Pay & Reveal (250 SKR)") }

                        Button(
                            onClick = { vm.discardFoundAndContinue() },
                            enabled = !ui.awaitingRevealPayment
                        ) { Text("Wipe & Try Again") }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.start(ctx) }, enabled = !ui.running) { Text("Start") }
                    Button(onClick = { vm.stop() }, enabled = ui.running) { Text("Stop") }
                }

                Text(
                    "On-device only. We do not store secrets.\n" +
                        "Note: 24-word generation is CPU-heavy; vanity search time varies.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
