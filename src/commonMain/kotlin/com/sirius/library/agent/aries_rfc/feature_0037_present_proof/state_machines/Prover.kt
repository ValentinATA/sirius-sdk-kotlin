package com.sirius.library.agent.aries_rfc.feature_0037_present_proof.state_machines

import com.sirius.library.agent.aries_rfc.SchemasNonSecretStorage
import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.PresentProofProblemReport
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.PresentationMessage
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.RequestPresentationMessage
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.wallet.abstract_wallet.model.CacheOptions
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolP2P
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger

class Prover(context: Context<*>, var verifier: Pairwise, var masterSecretId: String?, var poolName: String?) :
    BaseVerifyStateMachine(context) {
    //var poolName: String?
   // var masterSecretId: String?
    var log: Logger = Logger.getLogger("Prover")

    constructor(context: Context<*>, verifier: Pairwise, masterSecretId: String?) : this(
        context,
        verifier,
        masterSecretId,
        null
    ) {
    }

    suspend fun prove(request: RequestPresentationMessage, selfAttestedAttributes :JSONObject = JSONObject()): Boolean {
        try {
            CoProtocolP2P(context, verifier, protocols(), timeToLiveSec).also { coprotocol ->
                try {
                    // Step-1: Process proof-request
                    log.log(Logger.Level.INFO, "10% - Received proof request")
                    try {
                        request.validate()
                    } catch (e: SiriusValidationError) {
                        throw StateMachineTerminatedWithError(REQUEST_NOT_ACCEPTED, e.message ?:"")
                    }
                    val credInfoRes =
                        extractCredentialsInfo(request.proofRequest() ?: JSONObject(), poolName,selfAttestedAttributes)

                    // Step-2: Build proof
                    val proof: JSONObject? = context.getAnonCredsi().proverCreateProof(
                        request.proofRequest(), credInfoRes.credInfos, masterSecretId, credInfoRes.schemas,
                        credInfoRes.credentialDefs, credInfoRes.revStates
                    )
                    log.logLongText("proforJSON = "+proof?.toString())
                    // Step-3: Send proof and wait Ack to check success from Verifier side
                    val presentationMessage: PresentationMessage = PresentationMessage.builder()
                        .setProof(proof)
                        .setVersion(request.getVersion()?: "1.0")
                        .build()
                    log.logLongText("presentationMessage = "+presentationMessage.serialize())
                    presentationMessage.setPleaseAck(true)
                    if (request.hasPleaseAck()) {
                        presentationMessage.setThreadId(request.getAckMessageId())
                    } else {
                        presentationMessage.setThreadId(request.getId())
                    }

                    // Step-3: Wait ACK
                    log.log(Logger.Level.INFO, "50% - Send presentation")

                    // Switch to await participant action
                    val (_, second) = coprotocol.sendAndWait(presentationMessage)
                    return if (second is Ack) {
                        log.log(Logger.Level.INFO, "100% - Verify OK!")
                        true
                    } else if (second is PresentProofProblemReport) {
                        log.log(Logger.Level.INFO, "100% - Verify ERROR!")
                        false
                    } else {
                        throw StateMachineTerminatedWithError(
                            RESPONSE_FOR_UNKNOWN_REQUEST,
                            "Unexpected response @type:" + second?.getType()?.toString()
                        )
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

    internal class ExtractCredentialsInfoResult {
        var credInfos: JSONObject = JSONObject()
        var schemas: JSONObject = JSONObject()
        var credentialDefs: JSONObject = JSONObject()
        var revStates: JSONObject = JSONObject()

        override fun toString(): String {
            return "{credInfos=$credInfos schemas=$schemas credentialDefs=$credentialDefs revStates=$revStates}"
        }
    }




    private fun extractCredentialsInfo(proofRequest: JSONObject, poolName: String?, selfAttestedAttrs: JSONObject = JSONObject()):
            ExtractCredentialsInfoResult {
        val proofResponse: JSONObject? = context.getAnonCredsi().proverSearchCredentialsForProofReq(proofRequest, 1)
        val res = ExtractCredentialsInfoResult()
        val opts = CacheOptions()
        res.credInfos.put("self_attested_attributes", JSONObject())
        res.credInfos.put("requested_attributes", JSONObject())
        res.credInfos.put("requested_predicates", JSONObject())
        val requestedAttributesWithNoRestrictions: JSONObject? = JSONObject()
        if (proofResponse == null) {
            return res
        }
        var requestedAttributes: JSONObject? = proofRequest.getJSONObject("requested_attributes")
        requestedAttributes?.let {
            for (referentId in requestedAttributes!!.keySet()) {
                val data: JSONObject? = requestedAttributes?.getJSONObject(referentId)
                val hasRestrictions: Boolean = data?.has("restrictions") ?: false
                if (!hasRestrictions) {
                    if (data?.has("names")==true) {
                        requestedAttributesWithNoRestrictions?.put(referentId, data.get("names"))
                    }
                    if (data?.has("name")==true) {
                        requestedAttributesWithNoRestrictions?.put(referentId, JSONArray().put(data.get("name")))
                    }
                }
            }
        }

        requestedAttributes = proofResponse.getJSONObject("requested_attributes")
        val allInfos: MutableList<JSONObject> =ArrayList<JSONObject>()
        requestedAttributes?.let {
            for (referentId in requestedAttributes!!.keySet()) {
                if (requestedAttributesWithNoRestrictions?.has(referentId)==true) {
                    val attrNames: JSONArray? = requestedAttributesWithNoRestrictions.getJSONArray(referentId)
                    attrNames?.let {
                        for (o in attrNames) {
                            val attrName = o as String
                            if (selfAttestedAttrs?.has(attrName)==true) {
                                res.credInfos.getJSONObject("self_attested_attributes")!!
                                    .put(referentId, selfAttestedAttrs.get(attrName))
                            } else {
                                res.credInfos.getJSONObject("self_attested_attributes")!!.put(referentId, "")
                            }
                        }
                    }

                }
                val credInfos: JSONArray? = requestedAttributes.getJSONArray(referentId)
                val credInfo: JSONObject? = credInfos?.getJSONObject(0)?.getJSONObject("cred_info")
                val info:JSONObject = JSONObject()
                info.put("cred_id", credInfo?.getString("referent"))
                info.put("revealed", true)
                res.credInfos.getJSONObject("requested_attributes")!!.put(referentId, info)
                credInfo?.let {
                    allInfos.add(credInfo)
                }

            }
        }

        val requestedPredicates: JSONObject? = proofResponse.getJSONObject("requested_predicates")
        requestedPredicates?.let {
            for (referentId in requestedPredicates.keySet()) {
                val predicates: JSONArray? = requestedPredicates.getJSONArray(referentId)
                if (predicates?.isEmpty()==false) {
                    val predInfo:JSONObject? = predicates?.getJSONObject(0)?.getJSONObject("cred_info")
                    val info: JSONObject = JSONObject()
                    info.put("cred_id", predInfo?.getString("referent"))
                    res.credInfos.getJSONObject("requested_predicates")!!.put(referentId, info)
                    predInfo?.let {
                        allInfos.add(predInfo)
                    }

                }
            }
        }

        for (credInfo in allInfos) {
            val schemaId: String? = credInfo.getString("schema_id")
            val credDefId: String? = credInfo.getString("cred_def_id")
            var schema: JSONObject? = null
            if (poolName != null) {
                schema = JSONObject(
                    context.getCaches().getSchema(poolName, verifier.me.did, schemaId, opts)
                )
            } else {
                schema = SchemasNonSecretStorage.getCredSchemaNonSecret(context.nonSecrets, schemaId)
            }
            schemaId?.let {
                res.schemas.put(schemaId, schema)
            }

            var credDef: JSONObject? = null
            if (poolName != null) {
                credDef = JSONObject(
                    context.getCaches().getCredDef(poolName, verifier.me.did, credDefId, opts)
                )
            } else {
                credDef = SchemasNonSecretStorage.getCredDefNonSecret(context.nonSecrets, credDefId)
            }
            credDefId?.let {
                res.credentialDefs.put(credDefId, credDef)
            }
        }
        return res
    }
   // init {
      //  this.context = context
      //  this.verifier = verifier
      //  this.poolName = poolName
      //  this.masterSecretId = masterSecretId
  //  }
}
