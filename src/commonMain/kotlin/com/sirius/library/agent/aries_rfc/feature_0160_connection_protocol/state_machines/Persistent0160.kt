package com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProtocolMessage.ExtractTheirInfoRes
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnResponse
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.PairwiseNonSecretStorage.optConnectionKeyByTheirVerkey
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.PairwiseNonSecretStorage.optValueByConnectionKey
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.PairwiseNonSecretStorage.remove
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.PairwiseNonSecretStorage.write
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.hub.Context
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject


object Persistent0160 {
    suspend fun receive(context: Context<*>, event: Event): Pairwise? {
        if (event.message() is ConnRequest) {
            receiveRequest(
                context,
                event.message() as ConnRequest,
                event.recipientVerkey ?: "",
                context.endpointWithEmptyRoutingKeys
            )
        } else return if (event.message() is ConnResponse) {
            receiveResponse(context, event.message() as ConnResponse)
        } else {
            receiveAck(context, event)
        }
        return null
    }

    suspend fun acceptInvitation(context: Context<*>, invitation: Invitation, myLabel: String?) {
        val (first, second) = context.did.createAndStoreMyDid()
        val request = ConnRequest.builder().setLabel(myLabel).setDid(first).setVerkey(second)
            .setEndpoint(context.endpointAddressWithEmptyRoutingKeys).setDocUri(
                invitation.getDocUri()!!
            ).build()
        val connectionKey = invitation.recipientKeys()[0]
        request.setPleaseAck(true)
        request.messageObj.put("recipient_verkey", connectionKey)
        val pairwise: JSONObject = JSONObject().put(
            "me", JSONObject().put("did", first).put(
                "verkey",
                second
            ).put("did_doc", request.didDoc())
        ).put(
            "their", JSONObject()
                .put("label", invitation.label())
        )
        write(context, connectionKey, pairwise)
        context.sendMessage(
            request, listOf(connectionKey), invitation.endpoint(),
            second, listOf()
        )
    }

    suspend fun receiveRequest(
        context: Context<*>,
        request: ConnRequest,
        connectionKeyBase58: String,
        myEndpoint: Endpoint?
    ): ConnResponse? {
        val theirInfo: ExtractTheirInfoRes
        theirInfo = try {
            request.extractTheirInfo()
        } catch (e: SiriusInvalidMessage) {
            e.printStackTrace()
            return null
        }
        val (first, second) = context.did.createAndStoreMyDid()
        val response = ConnResponse.builder().setDid(first).setVerkey(second)
            .setEndpoint(myEndpoint?.address).setDocUri(
                request.getDocUri()!!
            ).build()
        if (request.hasPleaseAck()) {
            response.setThreadId(request.getAckMessageId())
        } else {
            response.setThreadId(request.getId())
        }

        response.setPleaseAck(true)
        val myDidDoc = response.didDoc()
        val theirDidDoc: JSONObject = request.didDoc()!!.payload
        response.signConnection(context.crypto, connectionKeyBase58)
        context.sendMessage(
            response, listOf(theirInfo.verkey), theirInfo.endpoint,
            second, theirInfo.routingKeys
        )
        val pairwise: JSONObject = JSONObject().put(
            "me", JSONObject().put("did", first).put(
                "verkey",
                second
            ).put("did_doc", myDidDoc!!.payload)
        ).put(
            "their",
            JSONObject().put("did", theirInfo.did).put("verkey", theirInfo.verkey)
                .put("label", request.label).put(
                    "endpoint",
                    JSONObject().put("address", theirInfo.endpoint)
                        .put("routing_keys", theirInfo.routingKeys)
                ).put("did_doc", theirDidDoc)
        )
        write(context, connectionKeyBase58, pairwise)
        return response
    }

    suspend fun receiveResponse(context: Context<*>, response: ConnResponse): Pairwise? {
        if (!response.verifyConnection(context.crypto)) return null
        val connectionKey =
            response.messageObj.getJSONObject("connection~sig")!!.optString("signer")
        if (optValueByConnectionKey(context, connectionKey)!!.isEmpty()) return null
        var theirInfo: ExtractTheirInfoRes? = null
        theirInfo = try {
            response.extractTheirInfo()
        } catch (e: SiriusInvalidMessage) {
            return null
        }
        context.did.storeTheirDid(theirInfo?.did, theirInfo?.verkey)
        val myVk: String? = optValueByConnectionKey(context, connectionKey)?.optJSONObject("me")
            ?.optString("verkey")
        if (response.hasPleaseAck()) {
            val ack = Ack.builder().setStatus(Ack.Status.OK).build()
            ack.setThreadId(response.getAckMessageId())
            context.sendMessage(
                ack,
                listOfNotNull(theirInfo?.verkey),
                theirInfo?.endpoint,
                myVk,
                theirInfo?.routingKeys
            )
        } else {
            val ping: Ping =
                Ping.builder().setComment("Connection established").setResponseRequested(false)
                    .build()
            context.sendMessage(
                ping,
                listOfNotNull(theirInfo?.verkey),
                theirInfo?.endpoint,
                myVk,
                theirInfo?.routingKeys
            )
        }
        val their: JSONObject =
            JSONObject().put("did", theirInfo?.did).put("verkey", theirInfo?.verkey).put(
                "label",
                optValueByConnectionKey(context, connectionKey)?.optJSONObject("their")
                    ?.optString("label")
            ).put(
                "endpoint",
                JSONObject().put("address", theirInfo?.endpoint)
                    .put("routing_keys", theirInfo?.routingKeys)
            ).put("did_doc", response.didDoc()!!.payload)
        val jsonPw: JSONObject? = optValueByConnectionKey(context, connectionKey)
        jsonPw?.put("their", their)
        val pairwise: Pairwise = createPairwiseObject(jsonPw)
        remove(context, connectionKey)
        return pairwise
    }

    fun receiveAck(context: Context<*>, event: Event): Pairwise? {
        val senderVk: String? = event.senderVerkey
        val connectionKey: String? = optConnectionKeyByTheirVerkey(context, senderVk)
        if (connectionKey.isNullOrEmpty()) return null
        val jsonPw: JSONObject = optValueByConnectionKey(context, connectionKey) ?: return null
        val pairwise: Pairwise = createPairwiseObject(jsonPw)
        remove(context, connectionKey)
        return pairwise
    }

    private fun createPairwiseObject(o: JSONObject?): Pairwise {
        val me = Pairwise.Me(
            o?.optJSONObject("me")?.optString("did"),
            o?.optJSONObject("me")?.optString("verkey"),
            o?.optJSONObject("me")?.optJSONObject("did_doc")
        )
        val routingKeys: MutableList<String> = ArrayList()
        val kor = o?.optJSONObject("their")?.optJSONObject("endpoint")
            ?.optJSONArray("routing_keys") ?: JSONArray()
        for (k in kor) routingKeys.add(k as String)
        val their = Pairwise.Their(
            o?.optJSONObject("their")?.optString("did"),
            o?.optJSONObject("their")?.optString("label"),
            o?.optJSONObject("their")?.optJSONObject("endpoint")?.optString("address"),
            o?.optJSONObject("their")?.optString("verkey"),
            routingKeys
        )
        return Pairwise(me, their, o)
    }
}
