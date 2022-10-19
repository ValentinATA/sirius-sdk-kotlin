package com.sirius.library

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.CloudAgent
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.model.Entity
import com.sirius.library.helpers.ConfTest
import com.sirius.library.helpers.ServerTestSuite
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageUtil
import com.sirius.library.models.AgentParams
import com.sirius.library.models.TrustPingMessageUnderTest
import com.sirius.library.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.test.*

class TestCloudAgent {
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

    //TODO do all tests
    @Test
    fun testAllAgentsPing() {

        val testSuite: ServerTestSuite = confTest.suiteSingleton
        println("get suiteSingleton testSuite=" + testSuite)
        val allAgentsList: MutableList<String> = ArrayList<String>()
        allAgentsList.add("agent1")
        allAgentsList.add("agent2")
        allAgentsList.add("agent3")
        allAgentsList.add("agent4")
        for (i in allAgentsList.indices) {
            val agentName = allAgentsList[i]
            val params: AgentParams = testSuite.getAgentParams(agentName)
            val agent = CloudAgent(
                params.serverAddress, StringUtils.stringToBytes(params.credentials, StringUtils.CODEC.US_ASCII),
                params.getConnectioni(), 10
            )
            agent.open()
            val isPinged: Boolean = agent.ping()
            assertTrue(isPinged)
            agent.close()
        }

    }

    @Test
    fun testAgentsWallet() {


        val testSuite: ServerTestSuite = confTest.suiteSingleton
        val params: AgentParams = testSuite.getAgentParams("agent1")

        val agent = CloudAgent(
            params.serverAddress, StringUtils.stringToBytes(params.credentials, StringUtils.CODEC.US_ASCII),
            params.getConnectioni(), 10
        )
        agent.open()
        //Check wallet calls is ok
        val didVerkey: Pair<String, String> = agent.getWalleti()?.did?.createAndStoreMyDid() ?: Pair("", "")
        assertNotNull(didVerkey)
        assertNotNull(didVerkey.first)
        assertNotNull(didVerkey.second)
        agent.close()

    }

    @Test

    fun testAgentsCommunications() {


        val testSuite: ServerTestSuite = confTest.suiteSingleton
        val agent1params: AgentParams = testSuite.getAgentParams("agent1")
        val agent2params: AgentParams = testSuite.getAgentParams("agent2")
        val entityList1: List<Entity> = agent1params.getEntitiesListi()
        val entityList2: List<Entity> = agent2params.getEntitiesListi()
        val entity1: Entity = entityList1[0]
        val entity2: Entity = entityList2[0]
        val codec = StringCodec()
        val agent1 = CloudAgent(
            agent1params.serverAddress,
            codec.fromASCIIStringToByteArray(agent1params.credentials),
            agent1params.getConnectioni(),
            10
        )
        val agent2 = CloudAgent(
            agent2params.serverAddress,
            codec.fromASCIIStringToByteArray(agent2params.credentials),
            agent2params.getConnectioni(),
            10
        )
        agent1.open()
        agent2.open()
        //Get endpoints
        var agent2Endpoint = ""
        for (e in agent2.getEndpointsi()) {
            if (e.routingKeys.size === 0) {
                agent2Endpoint = e.address
                break
            }
        }
        agent1.getWalleti()?.did?.storeTheirDid(entity2.did, entity2.verkey)
        val exist1 = agent1.getWalleti()?.pairwise?.isPairwiseExist(entity2.did) ?: false
        if (!exist1) {
            println("#1")
            agent1.getWalleti()?.pairwise?.createPairwise(entity2.did, entity1.did)
        }
        agent2.getWalleti()?.did?.storeTheirDid(entity1.did, entity1.verkey)
        val exist = agent2.getWalleti()?.pairwise?.isPairwiseExist(entity1.did) ?: false
        if (!exist) {
            println("#2")
            agent2.getWalleti()?.pairwise?.createPairwise(entity1.did, entity2.did)
        }
        //Prepare Message
        val trustPing = Message(
            JSONObject().put("@type", "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/trust_ping/1.0/ping")
                .put("@id", "trust-ping-message" + UUID.randomUUID.hashCode())
                .put("comment", "Hi. Are you listening?").put("response_requested", true)
        )
        val thierVerkeys: MutableList<String> = ArrayList<String>()
        thierVerkeys.add(entity2.verkey)
        val finalAgent2Endpoint = agent2Endpoint
        val agent2Listener: Listener = agent2.subscribe()
        val eventFeat = agent2Listener.one
        println("sendMess1=")
        GlobalScope.launch {
            agent1.sendMessage(trustPing, thierVerkeys, finalAgent2Endpoint, entity1.verkey, listOf())
        }
        val event: Event? = eventFeat?.get(10)
        println("event=" + event?.getMessageObjec())
        val message: JSONObject? = event?.getJSONOBJECTFromJSON("message")
        assertNotNull(message)
        val type: String? = message.getString("@type")
        assertEquals("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/trust_ping/1.0/ping", type)
        val id: String? = message.getString("@id")
        assertEquals(trustPing.getId(), id)
        agent1.close()
        agent2.close()

    }

    @Test

    fun testListenerRestoreMessage() {


        val agent1Params: AgentParams = confTest.suiteSingleton.getAgentParams("agent1")
        val agent2Params: AgentParams = confTest.suiteSingleton.getAgentParams("agent2")
        val agent1ParamsEntitiesList: List<Entity> = agent1Params.getEntitiesListi()
        val agent2ParamsEntitiesList: List<Entity> = agent2Params.getEntitiesListi()
        val entity1: Entity = agent1ParamsEntitiesList[0]
        val entity2: Entity = agent2ParamsEntitiesList[0]
        val codec = StringCodec()
        val agent1 = CloudAgent(
            agent1Params.serverAddress,
            codec.fromASCIIStringToByteArray(agent1Params.credentials),
            agent1Params.getConnectioni(),
            10
        )
        val agent2 = CloudAgent(
            agent2Params.serverAddress,
            codec.fromASCIIStringToByteArray(agent2Params.credentials),
            agent2Params.getConnectioni(),
            10
        )
        agent1.open()
        agent2.open()

        //GET endpoints
        var agent2Endpoint: String? = null
        for (i in 0 until agent2.getEndpointsi().size) {
            if (agent2.getEndpointsi().get(i).routingKeys.isEmpty()) {
                agent2Endpoint = agent2.getEndpointsi().get(i).address
            }
        }
        val agent2Listener: Listener = agent2.subscribe()

        //# Exchange Pairwise
        agent1.getWalleti()?.did?.storeTheirDid(entity2.did, entity2.verkey)
        val isExist1: Boolean = agent1.getWalleti()?.pairwise?.isPairwiseExist(entity2.did) ?: false
        if (!isExist1) {
            println("#1")
            agent1.getWalleti()?.pairwise?.createPairwise(entity2.did, entity1.did)
        }
        agent2.getWalleti()?.did?.storeTheirDid(entity1.did, entity1.verkey)
        val isExist2: Boolean = agent2.getWalleti()?.pairwise?.isPairwiseExist(entity1.did) ?: false
        if (!isExist2) {
            println("#2")
            agent2.getWalleti()?.pairwise?.createPairwise(entity1.did, entity2.did)
        }

        //Bind Message class to protocol
        MessageUtil.registerMessageClass(TrustPingMessageUnderTest::class, "trust_ping_test") {
            TrustPingMessageUnderTest(it)
        }
        //Prepare message
        val trust_ping = Message(
            JSONObject().put("@type", "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/trust_ping_test/1.0/ping")
                .put("@id", "trust-ping-message" + UUID.randomUUID.hashCode())
                .put("comment", "Hi. Are you listening?").put("response_requested", true)
        )
        val verkeyList: MutableList<String> = ArrayList<String>()
        verkeyList.add(entity2.verkey)
        val eventFeat = agent2Listener.one
        GlobalScope.launch {
            agent1.sendMessage(trust_ping, verkeyList, agent2Endpoint ?: "", entity1.verkey, listOf())
        }
        val event: Event? = eventFeat?.get(10)
        val message: JSONObject? = event?.getJSONOBJECTFromJSON("message")
        println("message=$message")
        // msg
        // assertTrue()
        // assert isinstance(msg, TrustPingMessageUnderTest), 'Unexpected msg type: ' + str(type(msg))
        agent1.close()
        agent2.close()

    }
}
