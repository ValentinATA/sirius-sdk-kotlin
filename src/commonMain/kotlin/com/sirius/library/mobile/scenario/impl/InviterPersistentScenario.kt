package com.sirius.library.mobile.scenario.impl


import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnProblemReport
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Invitee
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Persistent0160
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.messaging.Message
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.scenario.*
import com.sirius.library.utils.SDK
import kotlin.reflect.KClass


abstract class InviterPersistentScenario(eventStorage: EventStorageAbstract) :
    InviterScenario(eventStorage) {


    override fun start(event: Event): Pair<Boolean, String?> {
        val connRequest = event.message() as ConnRequest
        val connectionKey = event.recipientVerkey ?: ""
        val endpoint = SiriusSDK.context?.endpointWithEmptyRoutingKeys
        SiriusSDK.context?.let {
            Persistent0160.receiveRequest(
                it,
                connRequest,
                connectionKey,
                endpoint
            )
        }
        return Pair(true, null)
    }
}