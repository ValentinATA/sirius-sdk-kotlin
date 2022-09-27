package com.sirius.library.messaging

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.messages.*
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.*
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Pong
import com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages.AnswerMessage
import com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages.QuestionMessage
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.*
import com.sirius.library.agent.aries_rfc.feature_0211_mediator_coordination_protocol.*
import com.sirius.library.agent.consensus.simple.messages.*
import kotlin.reflect.KClass

class MessageFabric {
    companion object {


        fun restoreMessageInstance(kClass: KClass<out Message>, payload: String): Message?{
            for (pair in MessageUtil.MSG_REGISTRY2) {
                if (pair.first == kClass) {
                    return pair.second.invoke(payload)
                }
            }
            return null
        }

        fun registerAllMessagesClass(){
            MessageUtil.registerMessageClass(com.sirius.library.agent.aries_rfc.feature_0095_basic_message.Message::class, "basicmessage", "message"){
               com.sirius.library.agent.aries_rfc.feature_0095_basic_message.Message(it)
           }
            MessageUtil.registerMessageClass(Ping::class, Ping.PROTOCOL, "ping"){
                Ping(it)
            }
            MessageUtil.registerMessageClass(Pong::class, Pong.PROTOCOL, "ping_response"){
                Pong(it)
            }
            MessageUtil.registerMessageClass(Ack::class, Ack.PROTOCOL, "ack"){
                Ack(it)
            }

            MessageUtil.registerMessageClass(IssueCredentialMessage::class, BaseIssueCredentialMessage.PROTOCOL, "issue-credential"){
                IssueCredentialMessage(it)
            }

            MessageUtil.registerMessageClass(
                IssueProblemReport::class,
                BaseIssueCredentialMessage.PROTOCOL,
                "problem_report"
            ){
                IssueProblemReport(it)
            }

            MessageUtil.registerMessageClass(OfferCredentialMessage::class, BaseIssueCredentialMessage.PROTOCOL, "offer-credential"){
                OfferCredentialMessage(it)
            }
            MessageUtil.registerMessageClass(ProposeCredentialMessage::class, BaseIssueCredentialMessage.PROTOCOL, "propose-credential"){
                ProposeCredentialMessage(it)
            }

            MessageUtil.registerMessageClass(RequestCredentialMessage::class, BaseIssueCredentialMessage.PROTOCOL, "request-credential"){
                RequestCredentialMessage(it)
            }

            MessageUtil.registerMessageClass(PresentationMessage::class, BasePresentProofMessage.PROTOCOL, "presentation"){
                PresentationMessage(it)
            }

            MessageUtil.registerMessageClass(
                PresentProofProblemReport::class,
                BasePresentProofMessage.PROTOCOL,
                "problem_report"
            ){
                PresentProofProblemReport(it)
            }

            MessageUtil.registerMessageClass(RequestPresentationMessage::class, BasePresentProofMessage.PROTOCOL, "request-presentation"){
                RequestPresentationMessage(it)
            }

            MessageUtil.registerMessageClass(AnswerMessage::class, "questionanswer", "answer"){
                AnswerMessage(it)
            }

            MessageUtil.registerMessageClass(
                QuestionMessage::class,
                "questionanswer",
                "question"
            ){
                QuestionMessage(it)
            }

            MessageUtil.registerMessageClass(ConnProblemReport::class, ConnProtocolMessage.PROTOCOL, "problem_report"){
                ConnProblemReport(it)
            }

            MessageUtil.registerMessageClass(ConnRequest::class, ConnProtocolMessage.PROTOCOL, "request"){
                ConnRequest(it)
            }

            MessageUtil.registerMessageClass(ConnResponse::class, ConnProtocolMessage.PROTOCOL, "response"){
                ConnResponse(it)
            }

            MessageUtil.registerMessageClass(Invitation::class, ConnProtocolMessage.PROTOCOL, "invitation"){
                Invitation(it)
            }

            MessageUtil.registerMessageClass(KeylistUpdate::class, CoordinateMediationMessage.PROTOCOL, "keylist-update"){
                KeylistUpdate(it)
            }

            MessageUtil.registerMessageClass(KeylistUpdateResponse::class, CoordinateMediationMessage.PROTOCOL, "keylist-update-response"){
                KeylistUpdateResponse(it)
            }

            MessageUtil.registerMessageClass(MediateDeny::class, CoordinateMediationMessage.PROTOCOL, "mediate-deny"){
                MediateDeny(it)
            }
            MessageUtil.registerMessageClass(MediateGrant::class, CoordinateMediationMessage.PROTOCOL, "mediate-grant"){
                MediateGrant(it)
            }

            MessageUtil.registerMessageClass(MediateRequest::class, CoordinateMediationMessage.PROTOCOL, "mediate-request"){
                MediateRequest(it)
            }

            MessageUtil.registerMessageClass(
                BaseInitLedgerMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "initialize"
            ){
                BaseInitLedgerMessage(it)
            }

            MessageUtil.registerMessageClass(BaseTransactionsMessage::class, SimpleConsensusMessage.PROTOCOL, "stage"){
                BaseTransactionsMessage(it)
            }

            MessageUtil.registerMessageClass(
                CommitTransactionsMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "stage-commit"
            ){
                CommitTransactionsMessage(it)
            }

            MessageUtil.registerMessageClass(
                InitRequestLedgerMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "initialize-request"
            ){
                InitRequestLedgerMessage(it)
            }

            MessageUtil.registerMessageClass(
                InitResponseLedgerMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "initialize-response"
            ){
                InitResponseLedgerMessage(it)
            }

            MessageUtil.registerMessageClass(
                PostCommitTransactionsMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "stage-post-commit"
            ){
                PostCommitTransactionsMessage(it)
            }

            MessageUtil.registerMessageClass(
                PreCommitTransactionsMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "stage-pre-commit"
            ){
                PreCommitTransactionsMessage(it)
            }

            MessageUtil.registerMessageClass(
                ProposeTransactionsMessage::class,
                SimpleConsensusMessage.PROTOCOL,
                "stage-propose"
            ){
                ProposeTransactionsMessage(it)
            }

            MessageUtil.registerMessageClass(
                SimpleConsensusProblemReport::class,
                SimpleConsensusMessage.PROTOCOL,
                "problem_report"
            ){
                SimpleConsensusProblemReport(it)
            }

            MessageUtil.registerMessageClass(
                PresentationAck::class,
                BasePresentProofMessage.PROTOCOL,
                "ack"
            ){
                PresentationAck(it)
            }
        }
    }
}