package com.kreation.vanity.solana

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object SolanaRpc {
    private const val MAINNET = "https://api.mainnet-beta.solana.com"

    data class MintInfo(
        val decimals: Int,
        val tokenProgram: String,
    )

    fun getLatestBlockhash(): String {
        val req = jsonrpc("getLatestBlockhash", JSONArray().put(JSONObject().put("commitment", "confirmed")))
        val res = post(req)
        return res.getJSONObject("result").getJSONObject("value").getString("blockhash")
    }

    fun getMintInfo(mint: String): MintInfo {
        val config = JSONObject()
            .put("encoding", "base64")
            .put("commitment", "confirmed")
        val params = JSONArray().put(mint).put(config)
        val req = jsonrpc("getAccountInfo", params)
        val res = post(req)
        val value = res.getJSONObject("result").optJSONObject("value")
            ?: throw IllegalStateException("Mint account not found")

        val ownerProgram = value.getString("owner")
        val dataArr = value.getJSONArray("data")
        val dataB64 = dataArr.getString(0)
        val bytes = Base64.decode(dataB64, Base64.DEFAULT)

        // SPL Mint layout decimals byte is at offset 44
        if (bytes.size < 45) throw IllegalStateException("Mint data too short")
        val decimals = bytes[44].toInt() and 0xFF

        return MintInfo(decimals = decimals, tokenProgram = ownerProgram)
    }

    fun confirmSignature(signatureBase58: String, maxTries: Int = 30, sleepMs: Long = 1000): Boolean {
        // Poll getSignatureStatuses
        repeat(maxTries) {
            val params = JSONArray()
                .put(JSONArray().put(signatureBase58))
                .put(JSONObject().put("searchTransactionHistory", true))
            val req = jsonrpc("getSignatureStatuses", params)
            val res = post(req)
            val statusArr = res.getJSONObject("result").getJSONArray("value")
            val status = statusArr.optJSONObject(0)
            if (status != null) {
                val err = status.opt("err")
                if (err != null && err != JSONObject.NULL) return false
                val confirmationStatus = status.optString("confirmationStatus", "")
                if (confirmationStatus == "confirmed" || confirmationStatus == "finalized") return true
            }
            try { Thread.sleep(sleepMs) } catch (_: InterruptedException) { return false }
        }
        return false
    }

    fun accountExists(pubkey: String): Boolean {
        val config = JSONObject().put("encoding", "base64").put("commitment", "confirmed")
        val params = JSONArray().put(pubkey).put(config)
        val req = jsonrpc("getAccountInfo", params)
        val res = post(req)
        val value = res.getJSONObject("result").opt("value")
        return value != null && value != JSONObject.NULL
    }

    private fun jsonrpc(method: String, params: JSONArray): JSONObject {
        return JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", method)
            .put("params", params)
    }

    private fun post(body: JSONObject): JSONObject {
        val conn = (URL(MAINNET).openConnection() as HttpURLConnection)
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        conn.outputStream.use { os ->
            os.write(body.toString().toByteArray(Charsets.UTF_8))
        }
        val code = conn.responseCode
        val input = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = BufferedReader(InputStreamReader(input)).readText()
        val json = JSONObject(text)
        if (json.has("error")) {
            throw IllegalStateException("RPC error: ${json.getJSONObject("error")}")
        }
        return json
    }
}
