package com.sirius.library.hub

import com.sirius.library.agent.BaseSender
import com.sirius.library.agent.MobileAgent
import com.sirius.library.agent.MobileContextConnection
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Invitee
import com.sirius.library.agent.aries_rfc.feature_0211_mediator_coordination_protocol.KeylistUpdate
import com.sirius.library.agent.aries_rfc.feature_0211_mediator_coordination_protocol.KeylistUpdateResponse
import com.sirius.library.agent.aries_rfc.feature_0211_mediator_coordination_protocol.MediateGrant
import com.sirius.library.agent.aries_rfc.feature_0211_mediator_coordination_protocol.MediateRequest
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.agent.wallet.abstract_wallet.model.RetrieveRecordOptions
import com.sirius.library.agent.wallet.impl.mobile.PoolMobile
import com.sirius.library.hub.coprotocols.CoProtocolP2PAnon
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import kotlin.jvm.JvmOverloads


class MobileContext(config: MobileHub.Config) : Context<MobileHub>(MobileHub(config)) {
    var mediatorPw: Pairwise? = null
    var timeToLiveSec = 60
    override var currentHub: MobileHub
        get() = super.currentHub as MobileHub
        set(currentHub) {
            super.currentHub = currentHub
        }

    class MobileContextBuilder {
        var config: MobileHub.Config = MobileHub.Config()
        fun setWalletConfig(walletConfig: JSONObject): MobileContextBuilder {
            config.walletConfig = walletConfig
            return this
        }

        fun setIndyEndpoint(indyEndpoint: String): MobileContextBuilder {
            config.indyEndpoint = indyEndpoint
            return this
        }



        fun setWalletCredentials(walletCredentials: JSONObject): MobileContextBuilder {
            config.walletCredentials = walletCredentials
            return this
        }

        fun setSender(sender: BaseSender): MobileContextBuilder {
            config.sender = sender
            return this
        }

        fun setMediatorInvitation(invitation: Invitation): MobileContextBuilder {
            config.mediatorInvitation = invitation
            return this
        }

        fun build(): MobileContext {
            return MobileContext(config)
        }
    }

    @JvmOverloads
    fun connectToMediator(label: String?, connections: List<MobileContextConnection>? = null) {
        val invitation: Invitation? = (currentHub.config as MobileHub.Config).mediatorInvitation
        val mediatorDid = getMediatorDid(invitation?.recipientKeys()?.firstOrNull() ?: "")
        if (mediatorDid == null) {
            val (first, second) = did.createAndStoreMyDid()
            val me = Pairwise.Me(first, second)
            val endpoint = Endpoint("ws://")
            val invitee = Invitee(this, me, endpoint)
            var connectionArray: JSONArray? = null
            if (connections != null) {
                for (connection in connections) {
                    connectionArray = JSONArray()
                    val fcmServiceObject: JSONObject = JSONObject()
                        .put("id", "did:peer:" + me.did.toString() + ";indy").put("type", connection.type)
                        .put("priority", connection.priority).put(
                            "recipientKeys",
                            JSONArray(connection.recipientKeys)
                        ).put("serviceEndpoint", connection.serviceEndpoint)
                    connectionArray.put(fcmServiceObject)
                }
            }
            if (invitation != null) {
                val pw: Pairwise? = invitee.createConnection(invitation, label, null, connectionArray)
                if (pw != null) {
                    pairwiseList.ensureExists(pw)
                    mediatorPw = pw
                }
            }
            println("mediatorPw="+mediatorPw)
            askForMediation()
        } else {
            mediatorPw = pairwiseList.loadForDid(mediatorDid)
            val endpoint = getMyMediatorEndpoint(invitation?.recipientKeys()?.get(0) ?: "")
            if (endpoint != null) {
                endpoints?.add(endpoint)
            }
        }
        if (mediatorPw != null) {
            val services: JSONArray = mediatorPw?.their?.didDoc?.optJSONArray("service") ?: JSONArray()
            var mediatorService: JSONObject = JSONObject()
            for (o in services) {
                val service: JSONObject = o as JSONObject
                if (service.optString("type") == "MediatorService") {
                    mediatorService = service
                    break
                }
            }
            val myWsEndpoint: String? = mediatorService.optString("serviceEndpoint")
            (currentHub.agentConnectionLazy as MobileAgent).connect(myWsEndpoint)
        }
    }

    private fun getMediatorDid(mediatorRecipientKey: String): String? {
        val recordStr: String? = nonSecrets.getWalletRecord(
            MEDIATOR_ENDPOINTS,
            mediatorRecipientKey,
            RetrieveRecordOptions(false, true, false)
        )
        if (recordStr != null && !recordStr.isEmpty()) {
            val r: JSONObject = JSONObject(recordStr)
            val v: JSONObject = JSONObject(r.opt("value").toString())
            if (v.has("their_did")) {
                return v.optString("their_did")
            }
        }
        return null
    }

    private fun getMyMediatorEndpoint(mediatorRecipientKey: String): Endpoint? {
        val recordStr: String? = nonSecrets.getWalletRecord(
            MEDIATOR_ENDPOINTS,
            mediatorRecipientKey,
            RetrieveRecordOptions(false, true, false)
        )
        if (recordStr != null && !recordStr.isEmpty()) {
            val r: JSONObject = JSONObject(recordStr)
            val v: JSONObject = JSONObject(r.opt("value").toString())
            if (v.has("endpoint_address")) {
                return Endpoint(v.optString("endpoint_address") ?: "")
            }
        }
        return null
    }

    private fun saveMediatorInfo(mediatorRecipientKey: String, theirDid: String, endpoint: Endpoint) {
        nonSecrets.addWalletRecord(
            MEDIATOR_ENDPOINTS,
            mediatorRecipientKey,
            JSONObject().put("their_did", theirDid).put("endpoint_address", endpoint.address).toString()
        )
    }

    fun askForMediation(): Boolean {
        println("askForMediation START")
        try {
            val cp = CoProtocolP2PAnon(
                this,
                mediatorPw?.me?.verkey ?: "",
                mediatorPw?.their ?: TheirEndpoint("", "", null),
                listOf(),
                timeToLiveSec
            )
            val request: MediateRequest = MediateRequest.builder().build()
            val (first, second) = cp.sendAndWait(request)
            if (first) {
                if (second is MediateGrant) {
                    val grant: MediateGrant = second as MediateGrant
                    val endpoint = Endpoint(grant.endpointAddress?:"", grant.routingKeys)
                    endpoints?.add(endpoint)
                    val invitation: Invitation? = (currentHub.config as MobileHub.Config).mediatorInvitation
                    saveMediatorInfo(invitation?.recipientKeys()?.get(0)?:"", mediatorPw?.their?.did ?:"", endpoint)
                    return true
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun addMediatorKeys(keys: List<String>): Boolean {
        try {
            val cp = CoProtocolP2PAnon(
                this,
                mediatorPw?.me?.verkey ?: "",
                mediatorPw?.their ?: TheirEndpoint("", "", null),
                listOf(),
                timeToLiveSec
            )

            val keylistUpdate: KeylistUpdate = KeylistUpdate.builder().addKeys(keys).build()
            val (first, second) = cp.sendAndWait(keylistUpdate)
            if (first) {
                if (second is KeylistUpdateResponse) {
                    return true
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun addMediatorKey(key: String?): Boolean {
        return addMediatorKeys(listOfNotNull(key))
    }

    companion object {
        const val MEDIATOR_ENDPOINTS = "MEDIATOR_ENDPOINTS"
        const val PROTOCOL_VERSION = 2
        fun addPool(name: String?, txnPath: String?) {
            PoolMobile.setProtocolVersion(PROTOCOL_VERSION)
            PoolMobile.registerPool(name, txnPath )
        }

        fun builder(): MobileContextBuilder {
            return MobileContextBuilder()
        }
    }
}

