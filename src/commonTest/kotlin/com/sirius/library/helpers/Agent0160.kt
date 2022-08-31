/*
package com.sirius.library.helpers

import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Persistent0160.acceptInvitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Persistent0160.receive
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.hub.Context


class Agent0160(context: Context<*>, var nickname: String) {
    var loop = false
    var context: Context<*>
    var pairwises: ReplaySubject<Pairwise> = ReplaySubject.create()
    fun start() {
        if (!loop) {
            loop = true
            java.lang.Thread(java.lang.Runnable { routine() }).start()
        }
    }

    fun createInvitation(): Invitation {
        val connectionKeyBase58: String? = context.crypto.createKey()
        return Invitation.builder().setLabel("Inviter")
            .setEndpoint(context.endpointAddressWithEmptyRoutingKeys)
            .setRecipientKeys(listOfNotNull(connectionKeyBase58)).build()
    }

    fun acceptInvitation(invitation: Invitation?) {
        acceptInvitation(context, invitation!!, nickname)
    }

    fun getPairwises(): Observable<Pairwise> {
        return pairwises
    }

    protected fun routine() {
        val listener: Listener? = context.subscribe()
       val event =  listener?.one?.get(30)
        println(
            nickname + " received message " + event?.getMessageObjec()
                .toString() + " from " + event?.senderVerkey
        )
        event?.let {
            val pw: Pairwise? = receive(context, event)
            if (pw!=null) {
                context.pairwiseList.ensureExists(pw)
                pairwises.onNext(pw)
            }
        }

    }

    init {
        this.context = context
    }
}
*/
