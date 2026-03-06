package com.kreation.vanity

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun RevealScreen(
    vm: GeneratorViewModel,
    onDone: () -> Unit,
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity

    // Block screenshots/screen recording (best-effort)
    LaunchedEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    val words = vm.ui.value.revealMnemonic

    if (words == null || words.size != 12) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Nothing to reveal.")
            Button(onClick = onDone) { Text("Back") }
        }
        return
    }

    // Verification: ask for 3 random positions
    val size = words.size
    val rnd = kotlin.random.Random(System.currentTimeMillis())
    val picks = (1..size).shuffled(rnd).take(3).sorted()
    val p1 = picks[0]
    val p2 = picks[1]
    val p3 = picks[2]

    var a1 by remember { mutableStateOf("") }
    var a2 by remember { mutableStateOf("") }
    var a3 by remember { mutableStateOf("") }

    val ok = a1.trim().lowercase() == words[p1 - 1] &&
        a2.trim().lowercase() == words[p2 - 1] &&
        a3.trim().lowercase() == words[p3 - 1]

    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Seed phrase",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Never share this seed phrase. We will never ask for it.\n" +
                        "If you lose it, you lose the wallet.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(words.chunked(3).joinToString("\n") { it.joinToString(" ") })
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verify you saved it", style = MaterialTheme.typography.titleMedium)
                Text("Enter word #$p1, #$p2, and #$p3")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = a1,
                        onValueChange = { a1 = it },
                        label = { Text("#$p1") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = a2,
                        onValueChange = { a2 = it },
                        label = { Text("#$p2") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = a3,
                        onValueChange = { a3 = it },
                        label = { Text("#$p3") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                Button(onClick = onDone, enabled = ok) {
                    Text("I saved it. Close reveal")
                }
            }
        }

        Button(onClick = onDone) { Text("Cancel (discard reveal)") }
    }
}
