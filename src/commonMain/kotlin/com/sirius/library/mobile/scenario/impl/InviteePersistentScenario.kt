package com.sirius.library.mobile.scenario.impl


import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProblemReport
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Invitee
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Persistent0160
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.messaging.Message
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.scenario.*
import com.sirius.library.utils.SDK
import kotlin.reflect.KClass


abstract class InviteePersistentScenario(eventStorage: EventStorageAbstract) :
    InviteeScenario(eventStorage) {


    override suspend fun accept(id: String, comment: String?, actionListener: EventActionListener?) {
        actionListener?.onActionStart(EventAction.accept, id, comment)
        val event = eventStorage.getEvent(id)
        val invitation = event?.second as? Invitation
        var error: String? = null
        if (invitation != null) {
            SiriusSDK.context?.let { Persistent0160.acceptInvitation(it,invitation, SiriusSDK.label) }
        }else{
            error = "Invitation is empty"
        }
        actionListener?.onActionEnd(EventAction.accept, id, comment, true, error)
    }


    override suspend fun start(event: Event): Pair<Boolean, String?> {
        val eventPair = EventTransform.eventToPair(event)
        val id = eventPair.second?.getId()
        eventStorage.eventStore(id?:"", eventPair, false)
        return Pair(true, null)
    }


}