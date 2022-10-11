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
import kotlin.reflect.KClass


abstract class InviteeScenario(val eventStorage: EventStorageAbstract) : BaseScenario(), EventActionAbstract {


    override fun initMessages(): List<KClass<out Message>> {
        return listOf(Invitation::class)
    }


    override suspend fun actionStart(action: EventAction, id: String, comment: String?, actionListener: EventActionListener?) {
        if (action == EventAction.accept) {
            accept(id, comment, actionListener)
        } else if (action == EventAction.cancel) {
            cancel(id, comment, actionListener)
        }
    }

    fun cancel(id: String, cause: String?, actionListener: EventActionListener?) {
        actionListener?.onActionStart(EventAction.cancel, id, cause)
        val event = eventStorage.getEvent(id)
        //TODO send problem report
        event?.let {
            eventStorage.eventStore(id, event, false)
        }
        actionListener?.onActionEnd(EventAction.cancel, id, null, false, cause)
    }

    open suspend fun accept(id: String, comment: String?, actionListener: EventActionListener?) {
        actionListener?.onActionStart(EventAction.accept, id, comment)
        val event = eventStorage.getEvent(id)
        val invitation = event?.second as? Invitation
        val didVerkey = SiriusSDK.context?.did?.createAndStoreMyDid()
        var myDid = didVerkey?.first
        var myConnectionKey = didVerkey?.second
        val me = Pairwise.Me(myDid, myConnectionKey)
        var pairwise: Pairwise? = null
        var error: String? = null
        if (invitation != null) {
            val machine = SiriusSDK.context?.let {
                Invitee(
                    it,
                    me,
                    it.endpointWithEmptyRoutingKeys ?: Endpoint("")
                )
            }
            pairwise =
                machine?.createConnection(invitation, SiriusSDK.label)
            pairwise?.let {
                SiriusSDK.context?.pairwiseList?.ensureExists(it)
            }

            if (pairwise == null) {
                val problemReport: ConnProblemReport? = machine?.problemReport
                problemReport?.let {
                    error = problemReport.explain
                }
            }
            event?.let {
                eventStorage.eventStore(
                    invitation?.getId() ?: "",
                    Pair(pairwise?.their?.did, event.second),
                    pairwise != null
                )
            }
        }else{
            error = "Invitation is empty"
        }

        actionListener?.onActionEnd(EventAction.accept, id, comment, pairwise != null, error)
    }


    override suspend fun start(event: Event): Pair<Boolean, String?> {
        val eventPair = EventTransform.eventToPair(event)
        val id = eventPair.second?.getId()
        eventStorage.eventStore(id?:"", eventPair, false)
        return Pair(true, null)
    }


}