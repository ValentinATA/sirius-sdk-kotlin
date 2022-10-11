package com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages

import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolThreadedP2P

object Recipes {
    suspend fun askAndWaitAnswer(context: Context<*>, question: QuestionMessage, to: Pairwise): AnswerMessage? {
        var ttlSec = 60
        if (question.expiresTime != null) {
            ttlSec = -1
                //TODO java.time.temporal.ChronoUnit.SECONDS.between(java.time.ZonedDateTime.now(), question.expiresTime)
                    .toInt()
            if (ttlSec < 0) ttlSec = 60
        }
        try {
            CoProtocolThreadedP2P(context, question.getId() ?:"", to, ttlSec).also { cp ->
                val (first, second) = cp.sendAndWait(question)
                if (first) {
                    if (second is AnswerMessage) {
                        return second
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    suspend fun makeAnswer(context: Context<*>, response: String?, question: QuestionMessage, to: Pairwise) {
        val answer = AnswerMessage.builder().setResponse(response).build()
        answer.setThreadId(question.getId())
        answer.setOutTime()
        context.sendTo(answer, to)
    }
}
