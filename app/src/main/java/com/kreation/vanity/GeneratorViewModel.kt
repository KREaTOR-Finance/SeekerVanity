package com.kreation.vanity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.funkatronics.encoders.Base58
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
    // Beta default: 12-word flow for speed and compatibility.
    val wordCount: Int = 12,
    val running: Boolean = false,
    val attempts: Long = 0,
    val keysPerSec: Long = 0,
    val found: FoundWallet? = null,
    val error: String? = null,
    val revealMnemonic: List<String>? = null,
    val connectedWalletAddress: String? = null,
    val connectingWallet: Boolean = false,
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

    // Keep 12 words for the current beta flow; retained as a stub for future expansion.
    fun setWordCount(v: Int) {
        _ui.value = _ui.value.copy(wordCount = 12)
    }

    fun start(context: Context) {
        if (_ui.value.running) return

        val mode = _ui.value.mode

        // Clear any prior found result at start of a run
        _ui.value = _ui.value.copy(running = true, error = null, found = null, revealMnemonic = null)
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
                val winner = java.util.concurrent.atomic.AtomicReference<FoundWallet?>(null)
                val workerCount = max(1, Runtime.getRuntime().availableProcessors() - 1)

                val workers = (0 until workerCount).map {
                    launch {
                        while (!foundFlag.get() && kotlinx.coroutines.currentCoroutineContext().isActive) {
                            val wallet = VanityWalletGenerator.generateMnemonicSolanaWallet(context)
                            attempts.incrementAndGet()

                            if (matcher(wallet.address)) {
                                val found = FoundWallet(address = wallet.address, mnemonic24 = wallet.mnemonicWords)
                                if (foundFlag.compareAndSet(false, true)) {
                                    winner.set(found)
                                    com.kreation.vanity.util.SafeLog.i("MATCH_FOUND address=${com.kreation.vanity.util.SafeLog.redact(found.address)}")
                                }
                                break
                            }
                        }
                    }
                }

                // Wait for workers to finish
                workers.forEach { it.join() }

                // Commit winner to UI state exactly once
                val w = winner.get()
                if (w != null) {
                    _ui.value = _ui.value.copy(found = w, running = false)
                } else {
                    _ui.value = _ui.value.copy(running = false)
                }

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
            // No-op for now; UI can call start() again.
        }
    }

    fun setRevealMnemonic(words: List<String>) {
        _ui.value = _ui.value.copy(revealMnemonic = words)
    }

    fun clearRevealMnemonic() {
        _ui.value = _ui.value.copy(revealMnemonic = null)
    }

    fun wipeAfterRevealComplete() {
        _ui.value = _ui.value.copy(found = null, revealMnemonic = null)
    }

    fun connectWallet(mwa: com.kreation.vanity.mwa.MwaEnv) {
        if (_ui.value.connectingWallet) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(connectingWallet = true, error = null)
            try {
                when (val result = mwa.adapter.connect(mwa.sender)) {
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                        val account = result.authResult.accounts.firstOrNull()?.publicKey
                        val address = account?.let { Base58.encodeToString(it) }
                        _ui.value = _ui.value.copy(
                            connectedWalletAddress = address,
                            error = if (address == null) "Wallet connected, but no account was returned." else null
                        )
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                        _ui.value = _ui.value.copy(error = "No MWA-compatible wallet found on device.")
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                        _ui.value = _ui.value.copy(error = "Wallet connect failed: ${result.message}")
                    }
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(error = "Wallet connect error: ${t.message ?: t}")
            } finally {
                _ui.value = _ui.value.copy(connectingWallet = false)
            }
        }
    }

    fun disconnectWallet(mwa: com.kreation.vanity.mwa.MwaEnv) {
        if (_ui.value.connectingWallet) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(connectingWallet = true, error = null)
            try {
                when (val result = mwa.adapter.disconnect(mwa.sender)) {
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Success -> {
                        _ui.value = _ui.value.copy(connectedWalletAddress = null)
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.NoWalletFound -> {
                        _ui.value = _ui.value.copy(error = "No MWA-compatible wallet found on device.")
                    }
                    is com.solana.mobilewalletadapter.clientlib.TransactionResult.Failure -> {
                        _ui.value = _ui.value.copy(error = "Wallet disconnect failed: ${result.message}")
                    }
                }
            } catch (t: Throwable) {
                _ui.value = _ui.value.copy(error = "Wallet disconnect error: ${t.message ?: t}")
            } finally {
                _ui.value = _ui.value.copy(connectingWallet = false)
            }
        }
    }

    fun revealFound(onReveal: (List<String>) -> Unit) {
        val found = _ui.value.found
        if (found == null) {
            _ui.value = _ui.value.copy(error = "No found wallet to reveal.")
            return
        }
        prepareReveal()
        onReveal(found.mnemonic24)
    }
}
