package com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.state_machines

import com.sirius.library.agent.Codec
import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.messages.*
import com.sirius.library.agent.ledger.CredentialDefinition
import com.sirius.library.agent.ledger.Schema
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolP2P
import com.sirius.library.utils.Date
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger

class Issuer(context: Context<*>, holder: Pairwise, timeToLiveSec: Int) : BaseIssuingStateMachine(context) {
    var log: Logger = Logger.getLogger("Issuer")
    var holder: Pairwise

    init {
        this.holder = holder
        this.timeToLiveSec = timeToLiveSec
    }

    class IssueParams {
        var values: JSONObject? = null
        var schema: Schema? = null
        var credDef: CredentialDefinition? = null
        var comment: String? = null
        var locale: String = BaseIssueCredentialMessage.DEF_LOCALE
        var preview: List<ProposedAttrib> = ArrayList<ProposedAttrib>()
        var translation: List<AttribTranslation> = ArrayList<AttribTranslation>()
        var credId: String? = null
        fun setValues(values: JSONObject?): IssueParams {
            this.values = values
            return this
        }

        fun setSchema(schema: Schema?): IssueParams {
            this.schema = schema
            return this
        }

        fun setCredDef(credDef: CredentialDefinition?): IssueParams {
            this.credDef = credDef
            return this
        }

        fun setComment(comment: String?): IssueParams {
            this.comment = comment
            return this
        }

        fun setLocale(locale: String): IssueParams {
            this.locale = locale
            return this
        }

        fun setPreview(preview: List<ProposedAttrib>): IssueParams {
            this.preview = preview
            return this
        }

        fun setTranslation(translation: List<AttribTranslation>): IssueParams {
            this.translation = translation
            return this
        }

        fun setCredId(credId: String?): IssueParams {
            this.credId = credId
            return this
        }
    }

    suspend fun issue(params: IssueParams): Boolean {
        if (params.values == null || params.schema == null || params.credDef == null || params.credId == null) throw RuntimeException(
            "Bad params"
        )
        try {
            CoProtocolP2P(context, holder, protocols(), timeToLiveSec).also { coprotocol ->
                try {
                    // Step-1: Send offer to holder
                    val expiresTime: Date =
                        Date(Date().time + timeToLiveSec * 1000L)
                    val offer: JSONObject? = context.getAnonCredsi().issuerCreateCredentialOffer(params.credDef?.id)
                    val offerMsg: OfferCredentialMessage =
                        OfferCredentialMessage.builder().setComment(params.comment).setLocale(params.locale)
                            .setOffer(offer).setCredDef(JSONObject(params.credDef?.getBodyi().toString()))
                            .setPreview(params.preview).setIssuerSchema(params.schema?.body)
                            .setTranslation(params.translation).build() //setExpiresTime(expiresTime).
                    log.log(Logger.Level.INFO, "20% - Send offer")
                    // Switch to await participant action
                    val (_, second) = coprotocol.sendAndWait(offerMsg)
                    if (second !is RequestCredentialMessage) {
                        throw StateMachineTerminatedWithError(
                            OFFER_PROCESSING_ERROR,
                            "Unexpected @type: " + second?.getType()
                        )
                    }

                    // Step-2: Create credential
                    val requestMsg: RequestCredentialMessage = second as RequestCredentialMessage
                    log.log(Logger.Level.INFO, "40% - Received credential request")
                    val encodedCredValues = JSONObject()
                    for (key in params.values?.keySet() ?: setOf()) {
                        val encCredVal = JSONObject()
                        encCredVal.put("raw", params.values?.get(key).toString())
                        encCredVal.put("encoded", Codec.encode(params.values?.get(key)))
                        encodedCredValues.put(key, encCredVal)
                    }
                    log.log(Logger.Level.INFO, "70% - Build credential with values")
                    val (cred) = context.getAnonCredsi().issuerCreateCredential(
                        offer, requestMsg.credRequest(), encodedCredValues
                    )

                    // Step-3: Issue and wait Ack
                    val issueMsg: IssueCredentialMessage =
                        IssueCredentialMessage.builder().setComment(params.comment).setLocale(params.locale)
                            .setCred(cred).setCredId(params.credId).build()
                    log.log(Logger.Level.INFO, "90% - Send Issue message")
                    val (_, second1) = coprotocol.sendAndWait(issueMsg)
                    if (second1 !is Ack) {
                        throw StateMachineTerminatedWithError(
                            ISSUE_PROCESSING_ERROR,
                            "Unexpected @type: " + second1?.getType()
                        )
                    }
                    log.log(Logger.Level.INFO, "100% - Issuing was terminated successfully")
                    return true
                } catch (ex: StateMachineTerminatedWithError) {
                    problemReport =
                        IssueProblemReport.builder().setProblemCode(ex.problemCode).setExplain(ex.explain)
                            .build()
                    log.info("100% - Terminated with error. " + ex.problemCode.toString() + " " + ex.explain)
                    if (ex.isNotify) coprotocol.send(problemReport!!)
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return false
    }

}
