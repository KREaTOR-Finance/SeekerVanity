package com.kreation.vanity.solana

import com.funkatronics.encoders.Base58
import com.kreation.vanity.util.SafeLog
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.Message
import com.solana.transaction.Transaction

/**
 * SKR paywall utilities.
 *
 * Implements a Solana-standard SPL token transfer using web3-core primitives.
 */
object SkrPaywall {
    const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
    const val TREASURY_OWNER = "2atxENqo9UY4eSHTRZnQoiaJuh7MeDn7wq61KB77hpKM"
    const val PRICE_UI = 250L

    // Token-2022 program id (for detection); current implementation supports legacy SPL Token program only.
    private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"

    data class Plan(
        val mint: SolanaPublicKey,
        val payer: SolanaPublicKey,
        val treasury: SolanaPublicKey,
        val payerAta: SolanaPublicKey,
        val treasuryAta: SolanaPublicKey,
        val decimals: Int,
        val amountBaseUnits: Long,
        val tokenProgramId: SolanaPublicKey,
    )

    suspend fun buildPlan(payer: SolanaPublicKey, amountUi: Long = PRICE_UI): Plan {
        val mintInfo = SolanaRpc.getMintInfo(SKR_MINT)
        val mint = SolanaPublicKey.from(SKR_MINT)
        val treasury = SolanaPublicKey.from(TREASURY_OWNER)

        val tokenProgramId = SolanaPublicKey.from(mintInfo.tokenProgram)
        val decimals = mintInfo.decimals

        // Enforce legacy Token Program for now
        if (mintInfo.tokenProgram == TOKEN_2022_PROGRAM) {
            throw IllegalStateException("SKR appears to be Token-2022. Token-2022 transfers not supported yet.")
        }

        val payerAta = associatedTokenAddress(owner = payer, mint = mint, tokenProgramId = TokenProgram.PROGRAM_ID)
        val treasuryAta = associatedTokenAddress(owner = treasury, mint = mint, tokenProgramId = TokenProgram.PROGRAM_ID)

        val amountBaseUnits = uiToBaseUnits(amountUi, decimals)

        return Plan(
            mint = mint,
            payer = payer,
            treasury = treasury,
            payerAta = payerAta,
            treasuryAta = treasuryAta,
            decimals = decimals,
            amountBaseUnits = amountBaseUnits,
            tokenProgramId = tokenProgramId,
        )
    }

    suspend fun buildUnsignedPaymentTransaction(recentBlockhash: String, plan: Plan): ByteArray {
        val msg = Message.Builder()
            .addFeePayer(plan.payer)
            .setRecentBlockhash(recentBlockhash)
            // Create ATAs safely (idempotent)
            .addInstruction(
                AssociatedTokenProgram.createIdempotent(
                    payer = plan.payer,
                    associatedAccount = plan.payerAta,
                    owner = plan.payer,
                    mint = plan.mint,
                    programId = TokenProgram.PROGRAM_ID,
                )
            )
            .addInstruction(
                AssociatedTokenProgram.createIdempotent(
                    payer = plan.payer,
                    associatedAccount = plan.treasuryAta,
                    owner = plan.treasury,
                    mint = plan.mint,
                    programId = TokenProgram.PROGRAM_ID,
                )
            )
            .addInstruction(
                TokenProgram.transferChecked(
                    from = plan.payerAta,
                    to = plan.treasuryAta,
                    amount = plan.amountBaseUnits,
                    decimals = plan.decimals.toByte(),
                    owner = plan.payer,
                    mint = plan.mint,
                )
            )
            .build()

        // Unsigned transaction has zeroed signatures; wallet will sign.
        val tx = Transaction(msg)
        return tx.serialize()
    }

    private fun uiToBaseUnits(amountUi: Long, decimals: Int): Long {
        var v = amountUi
        repeat(decimals) { v = Math.multiplyExact(v, 10L) }
        return v
    }

    private suspend fun associatedTokenAddress(owner: SolanaPublicKey, mint: SolanaPublicKey, tokenProgramId: SolanaPublicKey): SolanaPublicKey {
        val pda = ProgramDerivedAddress.find(
            listOf(owner.bytes, tokenProgramId.bytes, mint.bytes),
            AssociatedTokenProgram.PROGRAM_ID
        ).getOrThrow()
        return SolanaPublicKey(pda.bytes)
    }

    fun base58(pubkey: SolanaPublicKey): String = Base58.encodeToString(pubkey.bytes)
}
