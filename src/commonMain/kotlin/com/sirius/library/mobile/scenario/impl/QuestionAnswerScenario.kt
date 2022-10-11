package com.sirius.library.mobile.scenario.impl


import com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages.AnswerMessage
import com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages.QuestionMessage
import com.sirius.library.agent.aries_rfc.feature_0113_question_answer.messages.Recipes
import com.sirius.library.agent.listener.Event
import com.sirius.library.messaging.Message
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.scenario.*
import com.sirius.library.mobile.helpers.PairwiseHelper
import kotlin.reflect.KClass


abstract class QuestionAnswerScenario(val eventStorage : EventStorageAbstract) : BaseScenario(), EventActionAbstract {
    override fun initMessages(): List<KClass<out Message>> {
        return listOf(QuestionMessage::class, AnswerMessage::class)
    }


    override suspend fun start(event: Event): Pair<Boolean, String?> {
        val id = event.message()?.getId()
        if (event.message() is QuestionMessage) {
            val eventPair = EventTransform.eventToPair(event)
            eventStorage.eventStore(id?:"", eventPair, false)
        } else {
            val answerMessage = event.message() as AnswerMessage
            val idQuestion = answerMessage.getThreadId()
            val questionEvent = eventStorage.getEvent(idQuestion?:"")
            questionEvent?.let {
                eventStorage.eventStore(id?:"", questionEvent, true)
            }
        }
        return Pair(true, null)
    }

    override fun onScenarioStart(id: String) {

    }

    override fun onScenarioEnd(id: String,success: Boolean, error: String?) {

    }

    override suspend fun actionStart(action: EventAction, id: String, comment: String?, actionListener: EventActionListener?) {
        if (action == EventAction.accept) {
            accept(id, comment,actionListener)
        } else if (action == EventAction.cancel) {
            cancel(id, comment,actionListener)
        }
    }


    fun accept(id: String, comment: String?,actionListener: EventActionListener?) {
        val event = eventStorage.getEvent(id)
        val questionMessage = event?.second as? QuestionMessage
        val pairwise = PairwiseHelper.getPairwise(event?.first)
        if(questionMessage!=null && pairwise!=null){
            SiriusSDK.context?.let {
                Recipes.makeAnswer(
                    it,
                    comment,
                    questionMessage,
                    pairwise
                )
            }
        }
    }

    fun cancel(id: String, cause: String?,actionListener: EventActionListener?) {

    }
}