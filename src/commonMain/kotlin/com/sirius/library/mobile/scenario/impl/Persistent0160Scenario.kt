package com.sirius.library.mobile.scenario.impl

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProblemReport
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnResponse
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Persistent0160
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.messaging.Message
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.scenario.*
import kotlin.reflect.KClass

abstract class Persistent0160Scenario(val eventStorage: EventStorageAbstract) : BaseScenario(),
    EventActionAbstract {

    override fun initMessages(): List<KClass<out Message>> {
        return listOf(Invitation::class, ConnRequest::class, ConnResponse::class, Ack::class)
    }


    suspend fun accept(id: String, comment: String?, actionListener: EventActionListener?) {
        actionListener?.onActionStart(EventAction.accept, id, comment)
        val event = eventStorage.getEvent(id)
        var error: String? = null
        if (event?.second is Invitation) {
            val invitation = event?.second as? Invitation
            if (invitation != null) {
                SiriusSDK.context?.let {
                    Persistent0160.acceptInvitation(
                        it,
                        invitation,
                        SiriusSDK.label
                    )
                }
            } else {
                error = "Invitation is empty"
            }
        } else if (event?.second is ConnRequest) {
            val connRequest = event.second as ConnRequest
            val connectionKey = connRequest.messageObj.optString("recipient_verkey")?:""
            SiriusSDK.context?.let {
                val endpoint = it.endpointWithEmptyRoutingKeys
              val connResponse =   Persistent0160.receiveRequest(
                    it,
                    connRequest,
                    connectionKey,
                    endpoint
                )
                connResponse?.getId()?.let { it1 -> eventStorage.eventStore(it1,Pair(connRequest.theirDid(),connResponse),false) }
            }
        }
        actionListener?.onActionEnd(EventAction.accept, id, comment, true, error)
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


    override suspend fun start(event: Event): Pair<Boolean, String?> {
        val message = event.message()
        if (message is ConnResponse) {

            val eventPair = EventTransform.eventToPair(event)
            val id = eventPair.second?.getId()
            eventStorage.eventStore(id ?: "", eventPair, false)

            val pairwise = SiriusSDK.context?.let { Persistent0160.receiveResponse(it, message) }
         /*   if (pairwise == null) {
                val problemReport: ConnProblemReport? = machine?.problemReport
                problemReport?.let {
                    error = problemReport.explain
                }
            }*/



            if (pairwise != null) {
                pairwise?.let {
                    SiriusSDK.context?.pairwiseList?.ensureExists(it)
                }
                return Pair(true, pairwise.toJSONObject().toString())
            }
            return Pair(false, null)
        } else if (message is Ack) {
            val pairwise = SiriusSDK.context?.let {
                Persistent0160.receiveAck(it, event)
            }
            if (pairwise != null) {
                pairwise?.let {
                    SiriusSDK.context?.pairwiseList?.ensureExists(it)
                }
                event?.let {
                    eventStorage.eventStore(
                        message?.getId() ?: "",
                        Pair(pairwise?.their?.did, message),
                        pairwise != null
                    )
                }
                return Pair(true, pairwise.toJSONObject().toString())
            }
            return Pair(false, null)
        } else if (message is Invitation) {
            val eventPair = EventTransform.eventToPair(event)
            val id = eventPair.second?.getId()
            eventStorage.eventStore(id ?: "", eventPair, false)
            return Pair(true, null)
        } else {
            val eventPair = EventTransform.eventToPair(event)
            val id = eventPair.second?.getId()
            eventStorage.eventStore(id ?: "", eventPair, false)

            return Pair(true, null)
        }
    }


    override suspend fun actionStart(
        action: EventAction,
        id: String,
        comment: String?,
        actionListener: EventActionListener?
    ) {
        if (action == EventAction.accept) {
            accept(id, comment, actionListener)
        } else if (action == EventAction.cancel) {
            cancel(id, comment, actionListener)
        }
    }
}