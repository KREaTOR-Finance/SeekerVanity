package com.kreation.vanity

import android.content.Context
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

data class FoundWallet(
    val address: String,
    val mnemonic24: List<String>,
    val createdAtMs: Long = System.currentTimeMillis(),
)

data class GeneratorUiState(
    val suffix: String = "RAVEN",
    val ignoreCase: Boolean = true,
    val mode: VanityMode = VanityMode.SUFFIX,
    // Locked: Seed Vault oriented (12-word only for MVP speed)
    val wordCount: Int = 12,
    val running: Boolean = false,
    val attempts: Long = 0,
    val keysPerSec: Long = 0,
    val found: FoundWallet? = null,
    val error: String? = null,
    val revealMnemonic: List<String>? = null,
    val awaitingRevealPayment: Boolean = false,
)

enum class VanityMode { SUFFIX, SKR_PREFIX }

class GeneratorViewModel : ViewModel() {

    private val _ui = MutableStateFlow(GeneratorUiState())
    val ui: StateFlow<GeneratorUiState> = _ui.asStateFlow()

    private var job: Job? = null
    private val attempts = AtomicLong(0)

    fun setSuffix(v: String) {
        _ui.value = _ui.value.copy(suffix = v.take(6))
    }

    fun setIgnoreCase(v: Boolean) {
        _ui.value = _ui.value.copy(ignoreCase = v)
    }

    fun setMode(mode: VanityMode) {
        _ui.value = _ui.value.copy(mode = mode, error = null)
    }

    // Locked to 12 words for MVP (Seed Vault compatible; faster). Kept as a stub for future expansion.
    fun setWordCount(v: Int) {
        _ui.value = _ui.value.copy(wordCount = 12)
    }

    fun start(context: Context) {
        if (_ui.value.running) return

        // Payment is per reveal (250 SKR). Searching is free.
        val mode = _ui.value.mode

        _ui.value = _ui.value.copy(running = true, error = null)
        attempts.set(0)

        job = viewModelScope.launch(Dispatchers.Default) {
            val statsJob = launch {
                var last = 0L
                while (true) {
                    delay(1000)
                    val now = attempts.get()
                    val kps = max(0, now - last)
                    last = now
                    _ui.value = _ui.value.copy(attempts = now, keysPerSec = kps)
                }
            }

            try {
                val suffix = _ui.value.suffix
                val ignoreCase = _ui.value.ignoreCase

                val matcher: (String) -> Boolean = when (mode) {
                    VanityMode.SUFFIX -> { addr ->
                        VanityMatchers.endsWith(addr, suffix, ignoreCase)
                    }
                    VanityMode.SKR_PREFIX -> { addr ->
                        VanityMatchers.startsWith(addr, "SKR", ignoreCase = false)
                    }
                }

                val foundFlag = java.util.concurrent.atomic.AtomicBoolean(false)
                val workerCount = max(1, Runtime.getRuntime().availableProcessors() - 1)

                val workers = (0 until workerCount).map {
                    launch {
                        while (!foundFlag.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
                            val wallet = VanityWalletGenerator.generateMnemonicSolanaWallet(context, _ui.value.wordCount)
                            attempts.incrementAndGet()
                            if (matcher(wallet.address)) {
                                if (foundFlag.compareAndSet(false, true)) {
                                    val found = FoundWallet(address = wallet.address, mnemonic24 = wallet.mnemonicWords)
                                    _ui.value = _ui.value.copy(found = found, running = false)
                                }
                                break
                            }
                        }
                    }
                }

                // Wait for any worker to find a match
                workers.forEach { it.join() }

            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(error = (t.message ?: t.toString()), running = false)
            } finally {
                statsJob.cancel()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _ui.value = _ui.value.copy(running = false)
    }

    fun prepareReveal() {
        // Only sets the mnemonic into reveal state; payment gating happens externally.
        val found = _ui.value.found ?: return
        _ui.value = _ui.value.copy(revealMnemonic = found.mnemonic24)
    }

    fun discardFoundAndContinue() {
        // Wipe everything (address + secret) and keep searching.
        _ui.value = _ui.value.copy(found = null, revealMnemonic = null, error = null)
        startPendingSearchIfNeeded()
    }

    private fun startPendingSearchIfNeeded() {
        // If user discarded while generator was stopped, restart search automatically.
        if (!_ui.value.running) {
            // No-op for now; UI can call start() again. Keeping explicit to avoid auto-charging surprises.
        }
    }

    fun setRevealMnemonic(words: List<String>) {
        _ui.value = _ui.value.copy(revealMnemonic = words)
    }

    fun setAwaitingRevealPayment(v: Boolean) {
        _ui.value = _ui.value.copy(awaitingRevealPayment = v)
    }

    fun clearRevealMnemonic() {
        _ui.value = _ui.value.copy(revealMnemonic = null)
    }

    fun wipeAfterRevealComplete() {
        // After a successful paid reveal, wipe everything.
        _ui.value = _ui.value.copy(found = null, revealMnemonic = null)
    }

    /**
     * Step 3 placeholder.
     *
     * We will implement MWA + SPL token transfer of 250 SKR to the treasury.
     * To do that we still need the **SKR mint address** and (optionally) token program (Token-2022 vs SPL Token).
     */
    fun payAndReveal(activity: ComponentActivity, onPaid: () -> Unit) {
        val found = _ui.value.found
        if (found == null) {
            _ui.value = _ui.value.copy(error = "No found wallet to reveal.")
            return
        }

        viewModelScope.launch {
            setAwaitingRevealPayment(true)
            com.kreation.vanity.util.SafeLog.i("Pay&Reveal: starting MWA transact")
            try {
                val walletAdapter = com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter(
                    connectionIdentity = com.solana.mobilewalletadapter.clientlib.ConnectionIdentity(
                        identityUri = Uri.parse("https://kreation.studio"),
                        iconUri = Uri.parse("favicon.ico"),
                        identityName = "Vanity"
                    )
                )

                val sender = com.solana.mobilewalletadapter.clientlib.ActivityResultSender(activity)

                // TEMP: Payment wiring is being refactored to match the exact web3-solana API types.
                // For now, just connect to an MWA wallet and report success.
                val result = walletAdapter.connect(sender)

                when (result) {
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                        com.kreation.vanity.util.SafeLog.i("Pay&Reveal: connected (payment step pending)")
                        _ui.value = _ui.value.copy(error = "Connected wallet. SKR payment transaction wiring is pending.")
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                        _ui.value = _ui.value.copy(error = "No MWA-compatible wallet found on device.")
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                        _ui.value = _ui.value.copy(error = "Wallet connect failed: ${result.e.message}")
                    }
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(error = "Pay & reveal error: ${t.message ?: t}")
            } finally {
                setAwaitingRevealPayment(false)
            }
        }
    }
}
