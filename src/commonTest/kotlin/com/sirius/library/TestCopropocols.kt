package com.sirius.library

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.CloudAgent
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Pong
import com.sirius.library.agent.coprotocols.AbstractCloudCoProtocolTransport
import com.sirius.library.agent.coprotocols.PairwiseCoProtocolTransport
import com.sirius.library.agent.coprotocols.TheirEndpointCoProtocolTransport
import com.sirius.library.agent.coprotocols.ThreadBasedCoProtocolTransport
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.model.Entity
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.helpers.ConfTest
import com.sirius.library.helpers.ServerTestSuite
import com.sirius.library.hub.CloudContext
import com.sirius.library.hub.coprotocols.AbstractP2PCoProtocol
import com.sirius.library.hub.coprotocols.CoProtocolThreadedP2P
import com.sirius.library.hub.coprotocols.CoProtocolThreadedTheirs
import com.sirius.library.messaging.Message
import com.sirius.library.models.AgentParams
import com.sirius.library.utils.CompletableFutureKotlin
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.UUID
import kotlinx.coroutines.*
import kotlin.test.*

class TestCopropocols {
    lateinit  var confTest: ConfTest
    lateinit  var testSuite: ServerTestSuite
    lateinit  var msgLog: MutableList<Message>
    @BeforeTest
    fun configureTest() {
        confTest = ConfTest.newInstance()
        testSuite = confTest.suiteSingleton
        msgLog = ArrayList<Message>()
        val future = CompletableFutureKotlin<Boolean>()
        LibsodiumInitializer.initializeWithCallback {
            future.complete(true)
        }
        future.get(60)
    }

    suspend fun routine1(protocol: AbstractCloudCoProtocolTransport) {
        try {
            val firstReq = Message(JSONObject().put("@type", TEST_MSG_TYPES[0]).put("content", "Request1"))
            msgLog!!.add(firstReq)
            val (first, second) = protocol.sendAndWait(firstReq)
            assertTrue(first)
            assertNotNull(second)
            msgLog!!.add(second)
            val secondReq = Message(JSONObject().put("@type", TEST_MSG_TYPES[2]).put("content", "Request2"))
            val (first1, second1) = protocol.sendAndWait(secondReq)
            assertTrue(first1)
            assertNotNull(second1)
            msgLog!!.add(second1)
        } catch (ex: Exception) {
            ex.printStackTrace()
            assertTrue(false)
        }
    }

    suspend fun routine1OnHub(protocol: AbstractP2PCoProtocol) {
        try {
            val firstReq = Message(JSONObject().put("@type", TEST_MSG_TYPES[0]).put("content", "Request1"))
            msgLog!!.add(firstReq)
            val (first, second) = protocol.sendAndWait(firstReq)
            assertTrue(first)
            assertNotNull(second)
            msgLog!!.add(second)
            val secondReq = Message(JSONObject().put("@type", TEST_MSG_TYPES[2]).put("content", "Request2"))
            val (first1, second1) = protocol.sendAndWait(secondReq)
            assertTrue(first1)
            assertNotNull(second1)
            msgLog!!.add(second1)
        } catch (ex: Exception) {
            ex.printStackTrace()
            assertTrue(false)
        }
    }

    suspend   fun  routine2(protocol: AbstractCloudCoProtocolTransport) {
        try {
            delay(1000)
            val firstResp = Message(JSONObject().put("@type", TEST_MSG_TYPES[1]).put("content", "Response1"))
            val (first, second) = protocol.sendAndWait(firstResp)
            assertTrue(first)
            assertNotNull(second)
            msgLog!!.add(second!!)
            val endMsg = Message(JSONObject().put("@type", TEST_MSG_TYPES[3]).put("content", "End"))
            protocol.send(endMsg)
        } catch (ex:Exception) {
            assertTrue(false)
        }
    }

    suspend fun routine2OnHub(protocol: AbstractP2PCoProtocol) {
       try {
           delay(1000)
            val firstResp = Message(JSONObject().put("@type", TEST_MSG_TYPES[1]).put("content", "Response1"))
            val (first, second) = protocol.sendAndWait(firstResp)
            assertTrue(first)
            assertNotNull(second)
            msgLog!!.add(second)
            val endMsg = Message(JSONObject().put("@type", TEST_MSG_TYPES[3]).put("content", "End"))
            protocol.send(endMsg)
        } catch (ex: Exception) {
            assertTrue(false)
        }
    }

    fun checkMsgLog() {
        assertEquals(msgLog!!.size.toLong(), TEST_MSG_TYPES.size.toLong())
        for (i in TEST_MSG_TYPES.indices) {
            assertEquals(TEST_MSG_TYPES[i], msgLog!![i].getType())
        }
        assertEquals("Request1", msgLog!![0].getStringFromJSON("content"))
        assertEquals("Response1", msgLog!![1].getStringFromJSON("content"))
        assertEquals("Request2", msgLog!![2].getStringFromJSON("content"))
        assertEquals("End", msgLog!![3].getStringFromJSON("content"))
    }

    @Test
    fun testTheirEndpointProtocol() {
       val agent1params: AgentParams = testSuite.getAgentParams("agent1")
        val agent2params: AgentParams = testSuite.getAgentParams("agent2")
        val entity1: Entity = agent1params.entitiesList.get(0)
        val entity2: Entity = agent2params.entitiesList.get(0)
        val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        agent1.open()
        agent2.open()
        val agent1Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent1)
        val agent2Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent2)
        val their1 = TheirEndpoint(agent2Endpoint, entity2.verkey)
        val agent1Protocol: TheirEndpointCoProtocolTransport =
            agent1.spawn(entity1.verkey, their1) as TheirEndpointCoProtocolTransport
        val their2 = TheirEndpoint(agent1Endpoint, entity1.verkey)
        val agent2Protocol: TheirEndpointCoProtocolTransport =
            agent2.spawn(entity2.verkey, their2) as TheirEndpointCoProtocolTransport
        agent1Protocol.start(listOf("test_protocol"))
        agent2Protocol.start(listOf("test_protocol"))
        msgLog!!.clear()
        val job1 = GlobalScope.launch (Dispatchers.Default){
            routine1(agent1Protocol)
        }
       val job2 =  GlobalScope.launch (Dispatchers.Default){
           routine2(agent2Protocol)
        }
        runBlocking {
            job1.join()
            job2.join()
        }
        checkMsgLog()
        agent1Protocol.stop()
        agent2Protocol.stop()
        agent1.close()
        agent2.close()
    }

    @Test
    fun testPairwiseProtocol() {
       val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        agent1.open()
        agent2.open()
        val agent1Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent1)
        val agent2Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent2)
        val (first, second) = agent1.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
        val (first1, second1) = agent2.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
        agent1.getWalleti()?.did?.storeTheirDid(first1, second1)
        agent1.getWalleti()?.pairwise?.createPairwise(first1, first)
        agent2.getWalleti()?.did?.storeTheirDid(first, second)
        agent2.getWalleti()?.pairwise?.createPairwise(first, first1)
        val pairwise1 = Pairwise(
            Pairwise.Me(first, second),
            Pairwise.Their(first1, "Label-2", agent2Endpoint, second1)
        )
        val pairwise2 = Pairwise(
            Pairwise.Me(first1, second1),
            Pairwise.Their(first, "Label-1", agent1Endpoint, second)
        )
        val agent1Protocol: PairwiseCoProtocolTransport? = agent1.spawn(pairwise1)
        val agent2Protocol: PairwiseCoProtocolTransport?= agent2.spawn(pairwise2)
        agent1Protocol?.start(listOf("test_protocol"))
        agent2Protocol?.start(listOf("test_protocol"))
        msgLog!!.clear()

        val job1 = GlobalScope.launch (Dispatchers.Default){
            if (agent1Protocol != null) {
                routine1(agent1Protocol)
            }
        }
        val job2 =  GlobalScope.launch (Dispatchers.Default){
            if (agent2Protocol != null) {
                routine2(agent2Protocol)
            }
        }
        runBlocking {
            job1.join()
            job2.join()
        }

        checkMsgLog()
        agent1Protocol?.stop()
        agent2Protocol?.stop()
        agent1.close()
        agent2.close()
    }

    @Test
    fun testThreadBasedProtocol() {
        val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        agent1.open()
        agent2.open()
        val agent1Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent1)
        val agent2Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent2)
        val (first, second) = agent1.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
        val (first1, second1) = agent2.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
        agent1.getWalleti()?.did?.storeTheirDid(first1, second1)
        agent1.getWalleti()?.pairwise?.createPairwise(first1, first)
        agent2.getWalleti()?.did?.storeTheirDid(first, second)
        agent2.getWalleti()?.pairwise?.createPairwise(first, first1)
        val pairwise1 = Pairwise(
            Pairwise.Me(first, second),
            Pairwise.Their(first1, "Label-2", agent2Endpoint, second1)
        )
        val pairwise2 = Pairwise(
            Pairwise.Me(first1, second1),
            Pairwise.Their(first, "Label-1", agent1Endpoint, second)
        )
        val threadUi: String = UUID.randomUUID.toString()
        val agent1Protocol: ThreadBasedCoProtocolTransport? = agent1.spawn(threadUi, pairwise1)
        val agent2Protocol: ThreadBasedCoProtocolTransport? = agent2.spawn(threadUi, pairwise2)
        agent1Protocol?.start(listOf("test_protocol"))
        agent2Protocol?.start(listOf("test_protocol"))
        msgLog!!.clear()

        val job1 = GlobalScope.launch (Dispatchers.Default){
            if (agent1Protocol != null) {
                routine1(agent1Protocol)
            }
        }
        val job2 =  GlobalScope.launch (Dispatchers.Default){
            if (agent2Protocol != null) {
                routine2(agent2Protocol)
            }
        }
        runBlocking {
            job1.join()
            job2.join()
        }

        checkMsgLog()
        agent1Protocol?.stop()
        agent2Protocol?.stop()
        agent1.close()
        agent2.close()
    }

    @Test
    fun testThreadbasedProtocolOnHub() {
        val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        val agent1params: AgentParams = testSuite.getAgentParams("agent1")
        val agent2params: AgentParams = testSuite.getAgentParams("agent2")
        agent1.open()
        agent2.open()
        var pairwise1: Pairwise? = null
        var pairwise2: Pairwise? = null
        try {
            // Get endpoints
            val agent1Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent1)
            val agent2Endpoint: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent2)

            // Init pairwise list #1
            val (first, second) = agent1.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
            val (first1, second1) = agent2.getWalleti()?.did?.createAndStoreMyDid() ?:Pair("","")
            agent1.getWalleti()?.did?.storeTheirDid(first1, second1)
            agent1.getWalleti()?.pairwise?.createPairwise(first1, first)
            agent2.getWalleti()?.did?.storeTheirDid(first, second)
            agent2.getWalleti()?.pairwise?.createPairwise(first, first1)

            // Init pairwise list #2
            pairwise1 = Pairwise(
                Pairwise.Me(first, second),
                Pairwise.Their(first1, "Label-2", agent2Endpoint, second1)
            )
            pairwise2 = Pairwise(
                Pairwise.Me(first1, second1),
                Pairwise.Their(first, "Label-1", agent1Endpoint, second)
            )
        } finally {
            agent1.close()
            agent2.close()
        }
        val threadUi: String = UUID.randomUUID.toString()
        val finalPairwise: Pairwise? = pairwise1
        assertNotNull(finalPairwise)

        val job1 = GlobalScope.launch (Dispatchers.Default){
            CloudContext.builder().setServerUri(agent1params.serverAddress)
                .setP2p(agent1params.connection)
                .setCredentials(agent1params.credentials.encodeToByteArray())
                .build().also { context ->
                    val co1 = CoProtocolThreadedP2P(context, threadUi, finalPairwise)
                    routine1OnHub(co1)
                }
        }

        val finalPairwise2: Pairwise? = pairwise2
        assertNotNull(finalPairwise2)

        val job2 =  GlobalScope.launch (Dispatchers.Default){
            CloudContext.builder().setServerUri(agent2params.serverAddress)
                .setP2p(agent2params.connection)
                .setCredentials(agent2params.credentials.encodeToByteArray())
                .build().also { context ->
                    val co2 = CoProtocolThreadedP2P(context, threadUi, finalPairwise2)
                    routine2OnHub(co2)
                }
        }





        msgLog!!.clear()
        runBlocking {
            job1.join()
            job2.join()
        }

        checkMsgLog()
    }

    @Test
    fun testCoprotocolThreadedTheirsSend() {
       val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        val agent3: CloudAgent = confTest.getAgent("agent3")
        val agent1params: AgentParams = testSuite.getAgentParams("agent1")
        val agent2params: AgentParams = testSuite.getAgentParams("agent2")
        val agent3params: AgentParams = testSuite.getAgentParams("agent3")
        agent1.open()
        agent2.open()
        agent3.open()
        val pw1: Pairwise = confTest.getPairwise(agent1, agent2)
        val pw2: Pairwise = confTest.getPairwise(agent1, agent3)
        val threadId = "thread-id-" + UUID.randomUUID
        val rcvMessages: MutableList<Message> = mutableListOf()

        val sender = GlobalScope.launch(Dispatchers.Default) {
            CloudContext.builder().setServerUri(agent1params.serverAddress)
                .setCredentials(agent1params.credentials.encodeToByteArray())
                .setP2p(agent1params.connection).build().also { context ->
                    val msg: Ping = Ping.builder().setComment("Test Ping").build()
                    val co =
                        CoProtocolThreadedTheirs(context, threadId, listOf(pw1, pw2), null, 60)
                    co.send(msg)
                }
        }
        val reader1 = GlobalScope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
            try {
                CloudContext.builder().setServerUri(agent2params.serverAddress).setCredentials(
                    agent2params.credentials.encodeToByteArray()
                )
                    .setP2p(agent2params.connection).build().also { context ->
                        context.subscribe()?.one?.get(30)?.let {
                            rcvMessages.add(it)
                        }
                    }
            } catch (e:Exception) {
                e.printStackTrace()
            }
        }

        val reader2 = GlobalScope.launch(Dispatchers.Default, start = CoroutineStart.LAZY) {
            try {
                CloudContext.builder().setServerUri(agent3params.serverAddress).setCredentials(
                    agent3params.credentials.encodeToByteArray()
                )
                    .setP2p(agent3params.connection).build().also { context ->
                        context.subscribe()?.one?.get(30)?.let {
                            rcvMessages.add(it)
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

      //  runBlocking {
           // sender.join()

     //   }
        runBlocking {
            reader1.join()
            reader2.join()
        }
        assertEquals(2, rcvMessages.size.toLong())
    }

    @Test
    fun testCoprotocolThreadedTheirsSwitch() {
        val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        val agent3: CloudAgent = confTest.getAgent("agent3")
        val agent1params: AgentParams = testSuite.getAgentParams("agent1")
        val agent2params: AgentParams = testSuite.getAgentParams("agent2")
        val agent3params: AgentParams = testSuite.getAgentParams("agent3")
        agent1.open()
        agent2.open()
        agent3.open()
        val pw1: Pairwise = confTest.getPairwise(agent1, agent2)
        val pw2: Pairwise = confTest.getPairwise(agent1, agent3)
        val threadId = "thread-id-" + UUID.randomUUID
        val statuses: MutableList<CoProtocolThreadedTheirs.SendAndWaitResult> = ArrayList<CoProtocolThreadedTheirs.SendAndWaitResult>()

        val actor = GlobalScope.launch (Dispatchers.Default){
            CloudContext.builder().setServerUri(agent1params.serverAddress)
                .setCredentials(agent1params.credentials.encodeToByteArray())
                .setP2p(agent1params.connection).build().also { context ->
                    val msg: Ping = Ping.builder().setComment("Test Ping").build()
                    val co =
                        CoProtocolThreadedTheirs(context, threadId, listOf(pw1, pw2), null, 60)
                    statuses.addAll(co.sendAndWait(msg))
                }

        }

        val responder1 = GlobalScope.launch (Dispatchers.Default){
            try {
                CloudContext.builder().setServerUri(agent2params.serverAddress).setCredentials(
                    agent2params.credentials.encodeToByteArray()
                )
                    .setP2p(agent2params.connection).build().also { context ->
                        val event: Event? =
                            context?.subscribe()?.one?.get(30)
                        val threadId_: String? =
                            event?.message()?.getJSONOBJECTFromJSON("~thread")?.optString("thid")
                        val pong: Pong = Pong.builder().setPingId(threadId_).setComment("PONG").build()
                        assertNotNull(event?.getPairwisei())
                        context.sendTo(pong, event?.getPairwisei()!!)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val responder2 = GlobalScope.launch (Dispatchers.Default){
            try {
                CloudContext.builder().setServerUri(agent3params.serverAddress).setCredentials(
                    agent3params.credentials.encodeToByteArray()
                )
                    .setP2p(agent3params.connection).build().also { context ->
                        val event: Event? =
                            context.subscribe()?.one?.get(30)
                        val threadId_: String? =
                            event?.message()?.getJSONOBJECTFromJSON("~thread")?.optString("thid")
                        val pong: Pong = Pong.builder().setPingId(threadId_).setComment("PONG").build()
                        val pairwise = event?.getPairwisei()
                        assertNotNull(pairwise)
                        context.sendTo(pong, pairwise)
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        runBlocking {
            actor.join()
            responder1.join()
            responder2.join()
        }
        assertFalse(statuses.isEmpty())
        for (s in statuses) {
            assertTrue(s.success)
            assertEquals("PONG", s.message?.getMessageObjec()?.optString("comment"))
        }
    }

    companion object {
        val TEST_MSG_TYPES = arrayOf(
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/test_protocol/1.0/request-1",
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/test_protocol/1.0/response-1",
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/test_protocol/1.0/request-2",
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/test_protocol/1.0/response-2"
        )
    }
}
