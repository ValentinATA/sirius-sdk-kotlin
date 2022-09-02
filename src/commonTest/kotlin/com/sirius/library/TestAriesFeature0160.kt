package com.sirius.library

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Invitee
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise

import com.sirius.library.helpers.ConfTest
import com.sirius.library.helpers.ServerTestSuite
import com.sirius.library.hub.CloudContext
import com.sirius.library.hub.Context
import com.sirius.library.models.AgentParams
import com.sirius.library.utils.CompletableFutureKotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.test.*


class TestAriesFeature0160 {
    lateinit var confTest: ConfTest
    @BeforeTest
    fun configureTest() {
        confTest = ConfTest.newInstance()
        val future = CompletableFutureKotlin<Boolean>()
        LibsodiumInitializer.initializeWithCallback {
            future.complete(true)
        }
        future.get(60)
    }

    @Test
    fun testEstablishConnection() {
        val testSuite: ServerTestSuite = confTest.suiteSingleton
        val inviter: AgentParams = testSuite.getAgentParams("agent1")
        val invitee: AgentParams = testSuite.getAgentParams("agent2")

        // Get endpoints
        var connectionKey: String? = null
        var invitation: Invitation? = null
        CloudContext.builder().setServerUri(inviter.serverAddress)
            .setCredentials(inviter.credentials.encodeToByteArray())
            .setP2p(inviter.connection).build().also { context ->
                val inviterEndpointAddress: String = context.endpointAddressWithEmptyRoutingKeys
                connectionKey = context.crypto.createKey()
                invitation = Invitation.builder().setLabel("Inviter").setEndpoint(inviterEndpointAddress)
                    .setRecipientKeys(listOfNotNull(connectionKey)).build()
            }

        // Init Me
        var inviterMe: Pairwise.Me? = null
        CloudContext.builder().setServerUri(inviter.serverAddress)
            .setCredentials(inviter.credentials.encodeToByteArray())
            .setP2p(inviter.connection).build().also { context ->
                val (first, second) = context.getDidi().createAndStoreMyDid()
                inviterMe = Pairwise.Me(first, second)
            }
        var inviteeMe: Pairwise.Me? = null
        CloudContext.builder().setServerUri(invitee.serverAddress)
            .setCredentials(invitee.credentials.encodeToByteArray())
            .setP2p(invitee.connection).build().also { context ->
                val (first, second) = context.getDidi().createAndStoreMyDid()
                inviteeMe = Pairwise.Me(first, second)
            }
        val finalConnectionKey = connectionKey
        val finalInviterMe: Pairwise.Me? = inviterMe
        assertNotNull(finalConnectionKey)
        assertNotNull(finalInviterMe)
        GlobalScope.launch(Dispatchers.Default){
            CloudContext.builder().setServerUri(inviter.serverAddress)
                .setCredentials(inviter.credentials.encodeToByteArray())
                .setP2p(inviter.connection).build().also { context ->
                    runInviter(
                        context,
                        finalConnectionKey,
                        finalInviterMe
                    )
                }
        }

        val finalInvitation: Invitation? = invitation
        val finalInviteeMe: Pairwise.Me? = inviteeMe

        assertNotNull(finalInvitation)
        assertNotNull(finalInviteeMe)
        GlobalScope.launch(Dispatchers.Default){
            CloudContext.builder().setServerUri(invitee.serverAddress)
                .setCredentials(invitee.credentials.encodeToByteArray())
                .setP2p(invitee.connection).build().also { context ->
                    runInvitee(
                        context,
                        finalInvitation,
                        "Invitee",
                        finalInviteeMe
                    )
                }
        }

    }

    /*@Test
    fun testPersistent0160() {
        val testSuite = confTest.suiteSingleton
        val inviterContext: Context<*> = testSuite.getContext("agent1")
        val inviteeContext: Context<*> = testSuite.getContext("agent2")
        val inviter = Agent0160(inviterContext, "Inviter")
        val invitee = Agent0160(inviteeContext, "Invitee")
        inviter.start()
        invitee.start()
        val invitation = inviter.createInvitation()
        invitee.acceptInvitation(invitation)
        val inviteePw: Pairwise =
            invitee.getPairwises().timeout(30, TimeUnit.SECONDS).blockingFirst()
        val inviterPw: Pairwise =
            inviter.getPairwises().timeout(30, TimeUnit.SECONDS).blockingFirst()
        assertEquals(inviteePw.their.did, inviterPw.me.did)
        assertEquals(inviterPw.their.did, inviteePw.me.did)
        assertEquals(inviteePw.their.verkey, inviterPw.me.verkey)
        assertEquals(inviterPw.their.verkey, inviteePw.me.verkey)
        assertEquals(
            inviteePw.their.endpointAddress,
            inviterContext.endpointAddressWithEmptyRoutingKeys
        )
        assertEquals(
            inviterPw.their.endpointAddress,
            inviteeContext.endpointAddressWithEmptyRoutingKeys
        )
    }*/

    companion object {
        fun runInviter(
            context: Context<*>, expectedConnectionKey: String,
            me: Pairwise.Me
        ) {
            try {
                val myEndpoint: Endpoint? = context.endpointWithEmptyRoutingKeys
                val listener: Listener? = context.subscribe()
                val event: Event? = listener?.one?.get(30)
                if (expectedConnectionKey == event?.recipientVerkey) {
                    if (event?.message() is ConnRequest) {
                        val request: ConnRequest = event?.message() as ConnRequest
                        assertNotNull( myEndpoint)
                        val machine = Inviter(context, me, expectedConnectionKey, myEndpoint)
                        val pairwise: Pairwise? = machine.createConnection(request)
                        assertNotNull( pairwise)
                        context.getPairwiseListi().ensureExists(pairwise)
                    } else {
                        fail("Wrong request message type")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                fail()
            }
        }

        fun runInvitee(
            context: Context<*>, invitation: Invitation,
            myLabel: String?, me: Pairwise.Me
        ) {
            val myEndpoint: Endpoint? = context.endpointWithEmptyRoutingKeys
            // Create and start machine
            if(myEndpoint==null){
                fail()
            }
            val machine = Invitee(context, me, myEndpoint)
            val pairwise: Pairwise? = machine.createConnection(invitation, myLabel)
            assertNotEquals(null, pairwise)
            pairwise?.let {
                context.getPairwiseListi().ensureExists(pairwise)
            }
        }
    }
}
