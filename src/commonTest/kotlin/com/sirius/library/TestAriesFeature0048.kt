package com.sirius.library

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.CloudAgent
import com.sirius.library.agent.aries_rfc.feature_0048_trust_ping.Ping
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.helpers.ConfTest
import com.sirius.library.helpers.ServerTestSuite
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageUtil
import com.sirius.library.utils.CompletableFutureKotlin
import com.sirius.library.utils.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAriesFeature0048 {
  lateinit  var confTest: ConfTest
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
       val agent1: CloudAgent = confTest.getAgent("agent1")
        val agent2: CloudAgent = confTest.getAgent("agent2")
        val agent3: CloudAgent = confTest.getAgent("agent3")
        agent1.open()
        agent2.open()
        agent3.open()
        val (first, second) = agent1.getWalleti()?.did?.createAndStoreMyDid() ?: Pair("","")
        val (first1, second1) = agent2.getWalleti()?.did?.createAndStoreMyDid() ?: Pair("","")
        val endpointAddress2: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent2)
        val endpointAddress3: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(agent3)
        agent1.getWalleti()?.did?.storeTheirDid(first1, second1)
        agent1.getWalleti()?.pairwise?.createPairwise(first1, first)
        agent2.getWalleti()?.did?.storeTheirDid(first, second)
        agent2.getWalleti()?.pairwise?.createPairwise(first, first1)
        var to = Pairwise(
            Pairwise.Me(first, second),
            Pairwise.Their(first1, "Agent2", endpointAddress2, second1)
        )
        val listener2: Listener = agent2.subscribe()
        val ping: Ping = Ping.builder().setComment("testMsg").setResponseRequested(false).build()
        val feature2= listener2.one
        GlobalScope.async {
            agent1.sendTo(ping, to)
        }


        // Check OK
        val event: Event? = feature2?.get(10)
        val recv: JSONObject? = event?.getJSONOBJECTFromJSON("message")
        val (first2, second2) = MessageUtil.restoreMessageInstance(recv.toString())
        assertTrue(first2)
        assertTrue(second2 is Ping)
        assertEquals((second2 as Ping).comment, ping.comment)

        // Check Error
        val thrown = false
        to = Pairwise(
            Pairwise.Me(first, second),
            Pairwise.Their(first1, "Agent3", endpointAddress3, second1)
        )
        GlobalScope.async {
            agent1.sendTo(ping, to)
        }
    }
}
