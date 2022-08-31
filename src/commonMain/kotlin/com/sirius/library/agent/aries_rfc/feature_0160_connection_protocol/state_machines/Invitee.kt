package com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines

import com.sirius.library.agent.aries_rfc.DidDoc
import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.*
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolP2PAnon
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger
import com.sirius.library.utils.System

class Invitee(context: Context<*>, me: Pairwise.Me, myEndpoint: Endpoint) : BaseConnectionStateMachine(context, me, myEndpoint) {
    var log: Logger = Logger.getLogger(Invitee::class.simpleName)
    fun createConnection(
        invitation: Invitation,
        myLabel: String?,
        didDoc: DidDoc?,
        additionalConnectiion: JSONArray?
    ): Pairwise? {
        // Validate invitation
        log.info("0% - Invitation validate")
        try {
            invitation.validate()
        } catch (e: SiriusValidationError) {
            e.printStackTrace()
            log.info("100% - Terminated with error")
            return null
        }
        log.info("20% - Invitation validation OK")
        val docUri = invitation.getDocUri()
        // Extract Inviter connection_key
        val connectionKey = invitation.recipientKeys()[0]
        println("connectionKey="+connectionKey)
        println("invitation.endpoint()="+invitation.endpoint())
        println("me.verkey="+me.verkey)
        val inviterEndpoint = TheirEndpoint(invitation.endpoint(), connectionKey)

        // Allocate transport channel between self and theirs by verkeys factor
        println("me.verkey="+me.verkey +"inviterEndpoint= "+inviterEndpoint)
        try {
            CoProtocolP2PAnon(context, me.verkey ?:"", inviterEndpoint, protocols(), timeToLiveSec).also { cp ->
                try {
                    val requestBuidler =
                        ConnRequest.builder().setLabel(myLabel)!!.setDid(me.did)!!
                            .setVerkey(me.verkey)!!.setEndpoint(myEndpoint.address)!!
                            .setDocUri(docUri!!)!!
                            .setDidDocExtra(didDoc?.getPayloadi())!!
                    if (additionalConnectiion != null) {
                        for (i in 0 until additionalConnectiion.length()) {
                            val connectionObject: JSONObject? = additionalConnectiion.optJSONObject(i)
                            requestBuidler.addConnectionService(connectionObject)
                        }
                    }
                    val request = requestBuidler.build()
                    log.info("30% - Step-1: send connection request to request+" + request.serialize())
                    log.info("30% - Step-1: send connection request to Inviter")
                    val (first, second) = cp.sendAndWait(request)
                    return if (first) {
                        if (second is ConnResponse) {
                            // Step 2: process connection response from Inviter
                            log.info("40% - Step-2: process connection response from Inviter")
                            val response = second as ConnResponse
                            try {
                                response.validate()
                            } catch (e: SiriusValidationError) {
                                e.printStackTrace()
                                throw StateMachineTerminatedWithError(RESPONSE_NOT_ACCEPTED, e.message ?: "")
                            }
                            val success = response.verifyConnection(context.crypto)
                            if (success && response.getMessageObjec().getJSONObject("connection~sig")!!
                                    .optString("signer").equals(connectionKey)
                            ) {
                                // Step 3: extract Inviter info and store did
                                log.info("70% - Step-3: extract Inviter info and store DID")
                                val theirInfo: ConnProtocolMessage.ExtractTheirInfoRes = response.extractTheirInfo()
                                context.getDidi().storeTheirDid(theirInfo.did, theirInfo.verkey)

                                // Step 4: Send ack to Inviter
                                if (response.hasPleaseAck()) {
                                    val ack = Ack.builder().setStatus(Ack.Status.OK)!!.build()
                                    ack.setThreadId(response.getAckMessageId())
                                    cp.send(ack)
                                    log.info("90% - Step-4: Send ack to Inviter")
                                } else {
                                    val ping =
                                        Ping.builder().setComment("Connection established").setResponseRequested(false)
                                            .build()
                                    cp.send(ping)
                                    log.info("90% - Step-4: Send ping to Inviter")
                                }

                                // Step 5: Make Pairwise instance
                                val their = Pairwise.Their(
                                    theirInfo.did,
                                    invitation.label(), theirInfo.endpoint, theirInfo.verkey, theirInfo.routingKeys
                                )
                                val myDidDoc: JSONObject? = request.didDoc()?.getPayloadi()
                                val theirDidDoc: JSONObject? = response.didDoc()?.getPayloadi()
                                val metadata: JSONObject? = JSONObject().put(
                                    "me",
                                    JSONObject().put("did", me.did)
                                        .put("verkey", me.verkey).put("did_doc", myDidDoc).put(
                                            "their",
                                            JSONObject().put("did", theirInfo.did)
                                                .put("verkey", theirInfo.verkey)
                                                .put("label", invitation.label()).put(
                                                    "endpoint",
                                                    JSONObject().put("address", theirInfo.endpoint)
                                                        .put("routing_keys", theirInfo.routingKeys)
                                                ).put("did_doc", theirDidDoc)
                                        )
                                )
                                val pairwise = Pairwise(me, their, metadata)
                                pairwise.me.setDidDoci(myDidDoc)
                                pairwise.their.setDidDoci(theirDidDoc)
                                log.info("100% - Pairwise established")
                                pairwise
                            } else {
                                throw StateMachineTerminatedWithError(
                                    RESPONSE_NOT_ACCEPTED,
                                    "Invalid connection response signature for connection_key: $connectionKey"
                                )
                            }
                        } else if (second is ConnProblemReport) {
                            problemReport = second
                            log.info("100% - Terminated with error. " + problemReport!!.getMessageObjec())
                            null
                        } else {
                            throw StateMachineTerminatedWithError(
                                RESPONSE_NOT_ACCEPTED,
                                "Unexpected message from Inviter: " + second?.getType()
                            )
                        }
                    } else {
                        throw StateMachineTerminatedWithError(
                            RESPONSE_PROCESSING_ERROR,
                            "Response awaiting was terminated by timeout",
                            false
                        )
                    }
                } catch (e: StateMachineTerminatedWithError) {
                    problemReport =
                        ConnProblemReport.builder().setProblemCode(e.problemCode).setExplain(e.explain)
                            .build()
                    if (e.isNotify) {
                        cp.send(problemReport!!)
                        log.info("100% - Terminated with error. " + e.message ?:"")
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return null
    }

    fun createConnection(invitation: Invitation, myLabel: String?, didDoc: DidDoc?): Pairwise? {
        return createConnection(invitation, myLabel, didDoc, null)
    }

    fun createConnection(invitation: Invitation, myLabel: String?): Pairwise? {
        return createConnection(invitation, myLabel, null, null)
    }

}
