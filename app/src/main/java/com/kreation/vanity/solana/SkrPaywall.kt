package com.kreation.vanity.solana

import com.funkatronics.encoders.Base58
import com.solana.publickey.ProgramDerivedAddress
import com.solana.publickey.SolanaPublicKey
import com.solana.transaction.AccountMeta
import com.solana.transaction.Message
import com.solana.transaction.Transaction
import com.solana.transaction.TransactionInstruction
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * SKR paywall utilities.
 *
 * Implements a Solana-standard SPL token transfer using low-level instructions.
 * (Avoids relying on higher-level TokenProgram/AssociatedTokenProgram wrappers which
 * may not be present in every published artifact variant.)
 */
object SkrPaywall {
    const val SKR_MINT = "SKRbvo6Gf7GondiT3BbTfuRDPqLWei4j2Qy2NPGZhW3"
    const val TREASURY_OWNER = "2atxENqo9UY4eSHTRZnQoiaJuh7MeDn7wq61KB77hpKM"
    const val PRICE_UI = 250L

    // Programs
    private const val ATA_PROGRAM = "ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"
    private const val TOKEN_PROGRAM = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val TOKEN_2022_PROGRAM = "TokenzQdBNbLqP5VEhdkAS6EPFLC1PHnBqCXEpPxuEb"
    private const val SYSTEM_PROGRAM = "11111111111111111111111111111111"
    private const val RENT_SYSVAR = "SysvarRent111111111111111111111111111111111"

    data class Plan(
        val mint: SolanaPublicKey,
        val payer: SolanaPublicKey,
        val treasury: SolanaPublicKey,
        val payerAta: SolanaPublicKey,
        val treasuryAta: SolanaPublicKey,
        val decimals: Int,
        val amountBaseUnits: Long,
        val tokenProgramId: String,
    )

    suspend fun buildPlan(payer: SolanaPublicKey, amountUi: Long = PRICE_UI): Plan {
        val mintInfo = SolanaRpc.getMintInfo(SKR_MINT)
        val mint = SolanaPublicKey.from(SKR_MINT)
        val treasury = SolanaPublicKey.from(TREASURY_OWNER)

        if (mintInfo.tokenProgram == TOKEN_2022_PROGRAM) {
            throw IllegalStateException("SKR appears to be Token-2022. Token-2022 transfers not supported yet.")
        }

        val payerAta = associatedTokenAddress(owner = payer, mint = mint, tokenProgramId = SolanaPublicKey.from(TOKEN_PROGRAM))
        val treasuryAta = associatedTokenAddress(owner = treasury, mint = mint, tokenProgramId = SolanaPublicKey.from(TOKEN_PROGRAM))

        val amountBaseUnits = uiToBaseUnits(amountUi, mintInfo.decimals)

        return Plan(
            mint = mint,
            payer = payer,
            treasury = treasury,
            payerAta = payerAta,
            treasuryAta = treasuryAta,
            decimals = mintInfo.decimals,
            amountBaseUnits = amountBaseUnits,
            tokenProgramId = mintInfo.tokenProgram,
        )
    }

    suspend fun buildUnsignedPaymentTransaction(recentBlockhash: String, plan: Plan): ByteArray {
        val msg = Message.Builder()
            .setRecentBlockhash(recentBlockhash)
            .addInstruction(createAtaIdempotentIx(payer = plan.payer, ata = plan.payerAta, owner = plan.payer, mint = plan.mint))
            .addInstruction(createAtaIdempotentIx(payer = plan.payer, ata = plan.treasuryAta, owner = plan.treasury, mint = plan.mint))
            .addInstruction(transferCheckedIx(
                sourceAta = plan.payerAta,
                mint = plan.mint,
                destAta = plan.treasuryAta,
                owner = plan.payer,
                amount = plan.amountBaseUnits,
                decimals = plan.decimals,
            ))
            .build()

        val tx = Transaction(msg)
        return tx.serialize()
    }

    private fun createAtaIdempotentIx(payer: SolanaPublicKey, ata: SolanaPublicKey, owner: SolanaPublicKey, mint: SolanaPublicKey): TransactionInstruction {
        // Idempotent ATA create instruction uses discriminator 1
        val data = byteArrayOf(1)
        return TransactionInstruction(
            SolanaPublicKey.from(ATA_PROGRAM),
            listOf(
                AccountMeta(payer, isSigner = true, isWritable = true),
                AccountMeta(ata, isSigner = false, isWritable = true),
                AccountMeta(owner, isSigner = false, isWritable = false),
                AccountMeta(mint, isSigner = false, isWritable = false),
                AccountMeta(SolanaPublicKey.from(SYSTEM_PROGRAM), isSigner = false, isWritable = false),
                AccountMeta(SolanaPublicKey.from(TOKEN_PROGRAM), isSigner = false, isWritable = false),
                AccountMeta(SolanaPublicKey.from(RENT_SYSVAR), isSigner = false, isWritable = false),
            ),
            data
        )
    }

    private fun transferCheckedIx(
        sourceAta: SolanaPublicKey,
        mint: SolanaPublicKey,
        destAta: SolanaPublicKey,
        owner: SolanaPublicKey,
        amount: Long,
        decimals: Int,
    ): TransactionInstruction {
        // SPL Token transferChecked instruction index = 12
        val data = ByteArray(1 + 8 + 1)
        data[0] = 12
        ByteBuffer.wrap(data, 1, 8).order(ByteOrder.LITTLE_ENDIAN).putLong(amount)
        data[9] = decimals.toByte()

        return TransactionInstruction(
            SolanaPublicKey.from(TOKEN_PROGRAM),
            listOf(
                AccountMeta(sourceAta, isSigner = false, isWritable = true),
                AccountMeta(mint, isSigner = false, isWritable = false),
                AccountMeta(destAta, isSigner = false, isWritable = true),
                AccountMeta(owner, isSigner = true, isWritable = false),
            ),
            data
        )
    }

    private fun uiToBaseUnits(amountUi: Long, decimals: Int): Long {
        var v = amountUi
        repeat(decimals) { v = Math.multiplyExact(v, 10L) }
        return v
    }

    private suspend fun associatedTokenAddress(owner: SolanaPublicKey, mint: SolanaPublicKey, tokenProgramId: SolanaPublicKey): SolanaPublicKey {
        val pda = ProgramDerivedAddress.find(
            listOf(owner.bytes, tokenProgramId.bytes, mint.bytes),
            SolanaPublicKey.from(ATA_PROGRAM)
        ).getOrThrow()
        return SolanaPublicKey(pda.bytes)
    }

    fun base58(pubkey: SolanaPublicKey): String = Base58.encodeToString(pubkey.bytes)
}
