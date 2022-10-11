package com.sirius.library.agent.aries_rfc.feature_0037_present_proof.state_machines

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.messages.AttribTranslation
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.*
import com.sirius.library.agent.ledger.Ledger
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.wallet.abstract_wallet.model.CacheOptions
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolP2P
import com.sirius.library.utils.Date
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger

/**
 * Implementation of Verifier role for present-proof protocol
 *
 * See details: https://github.com/hyperledger/aries-rfcs/tree/master/features/0037-present-proof
 */
class Verifier : BaseVerifyStateMachine {
    lateinit var prover: Pairwise
    var log: Logger = Logger.getLogger(Verifier::class.simpleName)
    var poolname: String
    var requestedProof: JSONObject? = null
    var revealedAttrs: JSONObject? = null


    constructor(context: Context<*>, prover: Pairwise, ledger: Ledger?, timeToLive: Int) : super(context) {
        this.context = context
        this.prover = prover
        poolname = ledger?.name?:""
        timeToLiveSec = timeToLive
    }

    constructor(context: Context<*>, prover: Pairwise, ledger: Ledger?) : super(context) {
        this.context = context
        this.prover = prover
        poolname = ledger?.name?:""
    }

    class VerifyParams {
        /**
         * proof_request: Hyperledger Indy compatible proof-request
         */
        var proofRequest: JSONObject? = null

        /**
         * human readable attributes translations
         */
        var translation: List<AttribTranslation>? = null

        /**
         * human readable comment from Verifier to Prover
         */
        var comment: String? = null

        /**
         * locale, for example "en" or "ru"
         */
        var locale: String = BasePresentProofMessage.DEF_LOCALE

        /**
         * 0037 protocol version, for example 1.0 or 1.1
         */
        var protocolVersion: String? = null
        fun setProofRequest(proofRequest: JSONObject?): VerifyParams {
            this.proofRequest = proofRequest
            return this
        }

        fun setTranslation(translation: List<AttribTranslation>?): VerifyParams {
            this.translation = translation
            return this
        }

        fun setComment(comment: String?): VerifyParams {
            this.comment = comment
            return this
        }

        fun setLocale(locale: String): VerifyParams {
            this.locale = locale
            return this
        }

        fun setProtocolVersion(protoVersion: String?): VerifyParams {
            protocolVersion = protoVersion
            return this
        }
    }

    suspend fun verify(params: VerifyParams): Boolean {
        try {
            CoProtocolP2P(context, prover, protocols(), timeToLiveSec).also { coprotocol ->
                try {
                    // Step-1: Send proof request
                    val expiresTime: Date =
                        Date(Date().time + timeToLiveSec * 1000L)
                    val requestPresentationMessage: RequestPresentationMessage =
                        RequestPresentationMessage.builder().setProofRequest(params.proofRequest)
                            .setTranslation(params.translation).setComment(params.comment)
                            .setLocale(params.locale).setVersion(params.protocolVersion?:"1.0") //setExpiresTime(expiresTime).
                    .build()
                    requestPresentationMessage.setPleaseAck(true)
                    log.log(Logger.Level.INFO, "30% - Send request")
                    val (_, second) = coprotocol.sendAndWait(requestPresentationMessage)
                    if (second !is PresentationMessage) {
                        throw StateMachineTerminatedWithError(
                            RESPONSE_NOT_ACCEPTED,
                            "Unexpected @type: " + second?.getType()
                        )
                    }
                    log.log(Logger.Level.INFO, "60% - Presentation received")
                    // Step-2 Verify
                    val presentationMessage: PresentationMessage = second as PresentationMessage
                    var identifiers: JSONArray? = presentationMessage.proof().optJSONArray("identifiers")
                    if (identifiers == null) identifiers = JSONArray()
                    val schemas = JSONObject()
                    val credentialDefs = JSONObject()
                    val revRegDefs = JSONObject()
                    val revRegs = JSONObject()
                    val opts = CacheOptions()
                    for (o in identifiers) {
                        val identifier: JSONObject = o as JSONObject
                        val schemaId: String = identifier.optString("schema_id", "") ?:""
                        val credDefId: String = identifier.optString("cred_def_id", "") ?:""
                        val revRegId: String = identifier.optString("rev_reg_id", "") ?:""
                        if (!schemaId.isEmpty() && !schemas.has(schemaId)) {
                            schemas.put(
                                schemaId, JSONObject(
                                    context.getCaches().getSchema(poolname, prover?.me?.did, schemaId, opts)
                                )
                            )
                        }
                        if (!credDefId.isEmpty() && !credentialDefs.has(credDefId)) {
                            credentialDefs.put(
                                credDefId, JSONObject(
                                    context.getCaches().getCredDef(poolname, prover?.me?.did, credDefId, opts)
                                )
                            )
                        }
                    }
                    val success: Boolean = context.getAnonCredsi().verifierVerifyProof(
                        params.proofRequest, presentationMessage.proof(), schemas, credentialDefs, revRegDefs, revRegs
                    )
                    return if (success) {
                        requestedProof = presentationMessage.proof().getJSONObject("requested_proof")

                        revealedAttrs = revealAttrs(requestedProof, params.proofRequest)
                        val ack: PresentationAck = PresentationAck.builder().setStatus(Ack.Status.OK).build()

                        ack.setThreadId(if (presentationMessage.hasPleaseAck()) presentationMessage.getAckMessageId() else presentationMessage.getId())
                        log.log(Logger.Level.INFO, "100% - Verifying terminated successfully")
                        coprotocol.send(ack)
                        true
                    } else {
                        throw StateMachineTerminatedWithError(VERIFY_ERROR, "Verifying return false")
                    }
                } catch (ex: StateMachineTerminatedWithError) {
                    problemReport = PresentProofProblemReport.builder().setProblemCode(ex.problemCode)
                        .setExplain(ex.explain).build()
                    log.info("100% - Terminated with error. " + ex.problemCode.toString() + " " + ex.explain)
                    if (ex.isNotify) coprotocol.send(problemReport!!)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun revealAttrs(
        requestedProof: JSONObject?,
        proofRequest:JSONObject?
    ): JSONObject {
        val revAttrs: JSONObject = JSONObject()
        val selfAttestedAttrs: JSONObject? = requestedProof?.getJSONObject("self_attested_attrs")
        selfAttestedAttrs?.let {
            for (refId in selfAttestedAttrs.keySet()) {
                if (proofRequest?.getJSONObject("requested_attributes")?.has(refId)==true) {
                    if (proofRequest.getJSONObject("requested_attributes")?.getJSONObject(refId)?.has("name")==true) {
                        val attrName: String? =
                            proofRequest.getJSONObject("requested_attributes")?.getJSONObject(refId)?.optString("name")
                        attrName?.let {
                            revAttrs.put(attrName, selfAttestedAttrs?.get(refId))
                        }

                    }
                }
            }
        }

        val revealedAttrs: JSONObject? = requestedProof?.getJSONObject("revealed_attrs")
        revealedAttrs?.let {
            for (refId in revealedAttrs.keySet()) {
                if (proofRequest?.getJSONObject("requested_attributes")?.has(refId)==true) {
                    if (proofRequest.getJSONObject("requested_attributes")?.getJSONObject(refId)?.has("name")==true) {
                        val attrName: String? =
                            proofRequest.getJSONObject("requested_attributes")?.getJSONObject(refId)?.optString("name")
                        attrName?.let {
                            revAttrs.put(attrName, revealedAttrs?.getJSONObject(refId)?.optString("raw"))
                        }

                    }
                }
            }
        }
        return revAttrs
    }


}
