package com.sirius.library.helpers

import com.sirius.library.agent.CloudAgent
import com.sirius.library.agent.model.Entity
import com.sirius.library.base.JsonMessage
import com.sirius.library.encryption.P2PConnection
import com.sirius.library.hub.CloudContext
import com.sirius.library.hub.Context
import com.sirius.library.models.AgentParams
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.StringUtils
import com.sirius.library.utils.System
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import kotlin.time.ExperimentalTime


class ServerTestSuite {
    var SETUP_TIMEOUT = 60
    var serverAddress: String
    var url: String
    var metadata: String?
    var testSuitePath: String?
    var testSuiteExistsLocally = false
    fun getAgentParams(name: String): AgentParams {
        if (metadata == null || metadata!!.isEmpty()) {
            throw RuntimeException("TestSuite is not running...")
        }
        val agentObject = JsonMessage(metadata!!)
        val agent: JSONObject? = agentObject.getJSONOBJECTFromJSON(name)
        if (agent == null || agent.isEmpty()) {
            throw RuntimeException("TestSuite does not have agent with name $name")
        }
        val p2pObject: JSONObject? = agent.getJSONObject("p2p")
        val credentials: String? = agent.getString("credentials")
        val entitiesObject: JSONObject? = agent.getJSONObject("entities")
        val entityList: MutableList<Entity> = ArrayList<Entity>()
        if (entitiesObject != null) {
            val keys: Set<String> = entitiesObject.keySet()
            for (key in keys) {
                val entityObject: JSONObject? = entitiesObject.getJSONObject(key)
                val seed: String? = entityObject?.getString("seed")
                val verkey: String? = entityObject?.getString("verkey")
                val did: String? = entityObject?.getString("did")
                if(seed!=null &&verkey!=null &&did!=null) {
                    entityList.add(Entity(key, seed, verkey, did))
                }
            }
        }
        val smartContractObject: JSONObject? = p2pObject?.getJSONObject("smart_contract")
        val agentP2pObject: JSONObject? = p2pObject?.getJSONObject("agent")
        val myVerKey: String? = smartContractObject?.getString("verkey")
        val mySecretKey: String? = smartContractObject?.getString("secret_key")
        val theirVerkey: String? = agentP2pObject?.getString("verkey")
        val connection = P2PConnection(myVerKey ?:"", mySecretKey?:"", theirVerkey?:"")
        return AgentParams(serverAddress, credentials?:"", connection, entityList)

    }

    @ExperimentalTime
    fun ensureIsAlive() {

       /* CoroutineScope(Dispatchers.Default).launch{
            val time = measureTime {
                println("The answer is ${concurrentSum()}")
            }
            println("Completed in $time ms")
           // concurrentSum()
        }*/

            val (first, second) = httpGet(url)
            if (first) {
                metadata = second
            } else {
                if (testSuiteExistsLocally) {
                    runSuiteLocally()
                }
            }

    }


    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    fun runSuiteLocally() {}
      fun httpGet(url: String?): Pair<Boolean, String?> {
          println("httpGet url="+url)
          val helpers = com.sirius.library.helpers.HttpClient()
          return helpers.get(url?:"")
    /*      CoroutineScope(Dispatchers.Default).launch {
              val client = HttpClient()
              println("httpGet client="+client)
              val response: HttpResponse = client.request(url?:"") {
                  // Configure request parameters exposed by HttpRequestBuilder
              }
              println("httpGet response="+response)
              val byteArrayBody: ByteArray = response.receive()
              val string = byteArrayBody.decodeToString()
              println("httpGet string="+string)
              client.close()

          }*/

//         return Pair(false,  "byteArrayBody.decodeToString()")


      //  return Pair(false, "e.message")
    }

    fun getContext(agentName: String): Context<*> {
        val agent = getAgentParams(agentName)
        return CloudContext.builder().setServerUri(agent.serverAddress)
            .setCredentials(StringUtils.stringToBytes(agent.credentials, StringUtils.CODEC.UTF_8))
            .setP2p(agent.connection).build()
    }



    companion object {
        fun newInstance(): ServerTestSuite {
            return ServerTestSuite()
        }

        fun getFirstEndpointAddressWIthEmptyRoutingKeys(agent: CloudAgent): String {
            for (e in agent.getEndpointsi()) {
                if (e.routingKeys.size === 0) {
                    return e.address
                }
            }
            return ""
        }
    }

    init {
        serverAddress = ConfTest.singletonInstance.test_suite_baseurl ?:""
        url = "$serverAddress/test_suite"
        metadata = null
        testSuitePath = System.getenv("TEST_SUITE")
        if (testSuitePath == null) {
            testSuiteExistsLocally = false
        } else {
            //testSuiteExistsLocally = System.path.isfile(test_suite_path) and 'localhost' in self.__server_address
        }
    }
}

