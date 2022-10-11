package com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProblemReport
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProtocolMessage
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnResponse
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolP2PAnon
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger

class Inviter(context: Context<*>, me: Pairwise.Me, connectionKey: String, myEndpoint: Endpoint) :
    BaseConnectionStateMachine(context,me, myEndpoint) {
    var log: Logger = Logger.getLogger(Inviter::class.simpleName)
    var connectionKey: String
    suspend fun createConnection(request: ConnRequest, didDoc: JSONObject?): Pairwise? {
        // Validate request
        log.info("0% - Validate request")
        try {
            request.validate()
        } catch (e: SiriusValidationError) {
            e.printStackTrace()
            log.info("100% - Terminated with error")
            return null
        }
        log.info("20% - Request validation OK")

        // Step 1: Extract their info from connection request
        log.info("40% - Step-1: Extract their info from connection request")
        val docUri = request.getDocUri()
        val theirInfo: ConnProtocolMessage.ExtractTheirInfoRes
        theirInfo = try {
            request.extractTheirInfo()
        } catch (siriusInvalidMessage: SiriusInvalidMessage) {
            siriusInvalidMessage.printStackTrace()
            log.info("100% - Terminated with error")
            return null
        }
        val inviteeEndpoint = TheirEndpoint(
            theirInfo.endpoint,
            theirInfo.verkey, theirInfo.routingKeys
        )
        CoProtocolP2PAnon(context, me.verkey ?:"", inviteeEndpoint, protocols(), timeToLiveSec).also { cp ->
            try {
                // Step 2: build connection response
                val response =
                    ConnResponse.builder().setDid(me.did)!!.setVerkey(me.verkey)!!
                        .setEndpoint(myEndpoint.address)!!.setDocUri(docUri!!)!!.setDidDocExtra(didDoc)!!
                        .build()
                if (request.hasPleaseAck()) {
                    response.setThreadId(request.getAckMessageId())
                }
                val myDidDoc = response.didDoc()
                response.signConnection(context.crypto, connectionKey)
                log.info("80% - Step-2: Connection response")
                val (first, second) = cp.sendAndWait(response)
                return if (first) {
                    if (second is Ack || second is Ping) {
                        // Step 3: store their did
                        log.info("90% - Step-3: Ack received, store their DID")
                        context.getDidi().storeTheirDid(theirInfo.did, theirInfo.verkey)
                        // Step 4: create pairwise
                        val their = Pairwise.Their(
                            theirInfo.did,
                            request.label,
                            theirInfo.endpoint,
                            theirInfo.verkey,
                            theirInfo.routingKeys
                        )
                        val theirDidDoc: JSONObject? = request.didDoc()?.getPayloadi()
                        val metadata: JSONObject = JSONObject().put(
                            "me",
                            JSONObject().put("did", me.did).put("verkey", me.verkey)
                                .put("did_doc", myDidDoc?.getPayloadi())
                        ).put(
                            "their",
                            JSONObject().put("did", theirInfo.did).put("verkey", theirInfo.verkey)
                                .put("label", request.label).put(
                                    "endpoint",
                                    JSONObject().put("address", theirInfo.endpoint)
                                        .put("routing_keys", theirInfo.routingKeys)
                                ).put("did_doc", theirDidDoc)
                        )
                        val pairwise = Pairwise(me, their, metadata)
                        pairwise.me.setDidDoci(myDidDoc?.getPayloadi())
                        pairwise.their.setDidDoci(theirDidDoc)
                        log.info("100% - Pairwise established")
                        pairwise
                    } else if (second is ConnProblemReport) {
                        problemReport = second
                        log.info("100% - Terminated with error. " + problemReport!!.getMessageObjec().toString())
                        null
                    } else {
                        throw StateMachineTerminatedWithError(
                            REQUEST_PROCESSING_ERROR,
                            "Expect for connection response ack. Unexpected message type" + second?.getType()
                        )
                    }
                } else {
                    throw StateMachineTerminatedWithError(
                        REQUEST_PROCESSING_ERROR,
                        "Response ack awaiting was terminated by timeout", false
                    )
                }
            } catch (e: StateMachineTerminatedWithError) {
                problemReport =
                    ConnProblemReport.builder().setProblemCode(e.problemCode).setExplain(e.explain).build()
                if (e.isNotify) {
                    cp.send(problemReport!!)
                    log.info("100% - Terminated with error. " + e.message ?:"")
                }
            } catch (siriusPendingOperation: Exception) {
                siriusPendingOperation.printStackTrace()
                log.info("100% - Terminated with error")
                return null
            }
        }
        return null
    }

    suspend fun createConnection(request: ConnRequest): Pairwise? {
        return createConnection(request, null)
    }

    init {
        this.connectionKey = connectionKey
    }
}
