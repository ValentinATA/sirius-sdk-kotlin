package com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines

import com.sirius.library.agent.wallet.abstract_wallet.model.RetrieveRecordOptions
import com.sirius.library.hub.Context
import com.sirius.library.utils.JSONObject


internal object PairwiseNonSecretStorage {
    private const val NON_SECRET_PERSISTENT_0160_PW = "NON_SECRET_PERSISTENT_0160_PW"
    fun optValueByConnectionKey(
        context: Context<*>,
        connectionKeyBase58: String?
    ): JSONObject? {
        try {
            val query = JSONObject()
            query.put("connectionKey", connectionKeyBase58)
            val opts = RetrieveRecordOptions(false, true, false)
            val (first, second) = context.nonSecrets
                .walletSearch(NON_SECRET_PERSISTENT_0160_PW, query.toString(), opts, 1)
            return if (second !== 1) null else
                JSONObject(
                    JSONObject(
                        first.get(0)
                    ).optString("value")
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun optConnectionKeyByTheirVerkey(context: Context<*>, theirVerkey: String?): String? {
        try {
            val query = JSONObject()
            query.put("theirVk", theirVerkey)
            val opts = RetrieveRecordOptions(false, false, true)
            val (first, second) = context.nonSecrets
                .walletSearch(NON_SECRET_PERSISTENT_0160_PW, query.toString(), opts, 1)
            val firstF = first
            val secondF = second
            if (firstF.isNotEmpty()) {
                val jsonObject = JSONObject(first.get(0))
                val tagsJsonObject = jsonObject.optJSONObject("tags")
                return tagsJsonObject?.optString("connectionKey")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun write(context: Context<*>, connectionKeyBase58: String?, pairwise: JSONObject) {
        var theirVk = ""
        if (pairwise.has("their")) {
            theirVk = pairwise.optJSONObject("their")?.optString("verkey") ?: ""
        }
        val tags: JSONObject =
            JSONObject().put("connectionKey", connectionKeyBase58).put("theirVk", theirVk)
        if (optValueByConnectionKey(context, connectionKeyBase58) != null) {
            context.nonSecrets.updateWalletRecordValue(
                NON_SECRET_PERSISTENT_0160_PW,
                connectionKeyBase58,
                pairwise.toString()
            )
            context.nonSecrets.updateWalletRecordTags(
                NON_SECRET_PERSISTENT_0160_PW,
                connectionKeyBase58,
                tags.toString()
            )
        } else {
            context.nonSecrets.addWalletRecord(
                NON_SECRET_PERSISTENT_0160_PW,
                connectionKeyBase58,
                pairwise.toString(),
                tags.toString()
            )
        }
    }

    fun remove(context: Context<*>, connectionKeyBase58: String?) {
        if (optValueByConnectionKey(context, connectionKeyBase58) != null) {
            context.nonSecrets
                .deleteWalletRecord(NON_SECRET_PERSISTENT_0160_PW, connectionKeyBase58)
        }
    }
}
