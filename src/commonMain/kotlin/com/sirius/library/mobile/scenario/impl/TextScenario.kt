package com.sirius.library.mobile.scenario.impl


import com.sirius.library.agent.listener.Event
import com.sirius.library.messaging.Message
import com.sirius.library.mobile.scenario.BaseScenario
import com.sirius.library.mobile.scenario.EventStorageAbstract
import com.sirius.library.mobile.scenario.EventTransform
import kotlin.reflect.KClass

abstract class TextScenario(val eventStorage: EventStorageAbstract) : BaseScenario() {

    override fun initMessages(): List<KClass<out Message>> {
        return listOf(com.sirius.library.agent.aries_rfc.feature_0095_basic_message.Message::class)
    }



    override fun onScenarioEnd(id: String,success: Boolean, error: String?) {

    }

    override fun onScenarioStart(id: String) {

    }

    override suspend fun start(event: Event): Pair<Boolean, String?> {
        val eventPair = EventTransform.eventToPair(event)
        eventStorage.eventStore(eventPair.second?.getId()?:"", eventPair, false)
        return Pair(true, null)
    }

}