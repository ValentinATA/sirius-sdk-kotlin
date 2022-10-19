package com.sirius.library.helpers

import com.sirius.library.agent.CloudAgent
import com.sirius.library.agent.microledgers.AbstractMicroledger
import com.sirius.library.agent.model.Entity
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.encryption.Custom
import com.sirius.library.encryption.P2PConnection
import com.sirius.library.errors.sirius_exceptions.SiriusCryptoError
import com.sirius.library.models.AgentParams
import com.sirius.library.models.P2PModel
import com.sirius.library.rpc.AddressedTunnel
import com.sirius.library.utils.*
import kotlin.time.ExperimentalTime

class ConfTest {
    var test_suite_baseurl: String? =null
    var test_suite_overlay_address: String? = null
    var old_agent_address: String? = null
    var old_agent_overlay_address: String? = null
    var old_agent_root: String? = null
    var custom: Custom = Custom
    fun configureTestEnv() {
        test_suite_baseurl = System.getenv("TEST_SUITE_BASE_URL")
        if (test_suite_baseurl == null || test_suite_baseurl!!.isEmpty()) {
            test_suite_baseurl = "http://localhost"
        }
        test_suite_overlay_address = "http://10.0.0.90"
        old_agent_address = System.getenv("INDY_AGENT_BASE_URL")
        if (old_agent_address == null || old_agent_address!!.isEmpty()) {
            old_agent_address = "http://127.0.0.1:88"
        }
        old_agent_overlay_address = "http://10.0.0.52:8888"
        val jsonObject: JSONObject = JSONObject()
        jsonObject.put("username", "root")
        jsonObject.put("password", "root")
        old_agent_root = jsonObject.toString()
    }

    fun createP2P(): Pair<P2PModel, P2PModel>? {
        try {
            val codec = StringCodec()
            val keysAgent: KeyPair =
                custom.createKeypair(codec.fromASCIIStringToByteArray("000000000000000000000000000AGENT"))
            val keysSdk: KeyPair =
                custom.createKeypair(codec.fromASCIIStringToByteArray("00000000000000000000000000000SDK"))
            val agent = P2PConnection(
                StringUtils.bytesToBase58String(keysAgent.getPublicKey().asBytes),
                StringUtils.bytesToBase58String(keysAgent.getSecretKey().asBytes),
                StringUtils.bytesToBase58String(keysSdk.getPublicKey().asBytes)
            )
            val smartContract = P2PConnection(
                StringUtils.bytesToBase58String(keysSdk.getPublicKey().asBytes),
                StringUtils.bytesToBase58String(keysSdk.getSecretKey().asBytes),
                StringUtils.bytesToBase58String(keysAgent.getPublicKey().asBytes)
            )
            val downstream = InMemoryChannel()
            val upstream = InMemoryChannel()
            val agentTunnel = AddressedTunnel("memory://agent->sdk", upstream, downstream, agent)
            val sdkTunnel = AddressedTunnel("memory://sdk->agent", downstream, upstream, smartContract)
            val agentModel = P2PModel(agent, agentTunnel)
            val sdkModel = P2PModel(smartContract, sdkTunnel)
            return Pair(agentModel, sdkModel)
        } catch (siriusCryptoError: SiriusCryptoError) {
            siriusCryptoError.printStackTrace()
        }
        return null
    }

    @OptIn(ExperimentalTime::class)
    val suiteSingleton: ServerTestSuite
        get() {
            println("get suiteSingleton")
            val serverTestSuite: ServerTestSuite = ServerTestSuite.newInstance()
            println("get suiteSingleton ensureIsAlive")
            serverTestSuite.ensureIsAlive()
            println("get suiteSingleton ensureIsAlive END")
            return serverTestSuite
        }
    val indyAgentSingleton: IndyAgent
        get() = IndyAgent()

    fun getAgent(name: String): CloudAgent {
        val params: AgentParams = suiteSingleton.getAgentParams(name)
        val codec = StringCodec()
        return CloudAgent(
            params.serverAddress, codec.fromASCIIStringToByteArray(params.credentials),
            params.getConnectioni(), 60, null, name
        )
    }

    fun testSuite(): ServerTestSuite {
        return suiteSingleton
    }

    fun indyAgent(): IndyAgent {
        return indyAgentSingleton
    }

    fun agent1(): CloudAgent {
        return getAgent("agent1")
    }

    fun agent2(): CloudAgent {
        return getAgent("agent2")
    }

    fun agent3(): CloudAgent {
        return getAgent("agent3")
    }

    fun agent4(): CloudAgent {
        return getAgent("agent4")
    }

    fun A(): CloudAgent {
        return getAgent("agent1")
    }

    fun B(): CloudAgent {
        return getAgent("agent2")
    }

    fun C(): CloudAgent {
        return getAgent("agent3")
    }

    fun D(): CloudAgent {
        return getAgent("agent4")
    }

    fun ledgerName(): String {
        return "Ledger-" +UUID.randomUUID.toString()
    }

    fun defaultNetwork(): String {
        return "default"
    }

    fun getPairwise(me: CloudAgent, their: CloudAgent): Pairwise {
        val suite: ServerTestSuite = suiteSingleton
        val myParams: AgentParams = suite.getAgentParams(me.name?:"")
        val theirParams: AgentParams = suite.getAgentParams(their.name?:"")
        val myEntity: Entity = myParams.getEntitiesListi().get(0)
        val theirEntity: Entity = theirParams.getEntitiesListi().get(0)
        val myEndpointAddress: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(me)
        val theirEndpointAddress: String = ServerTestSuite.getFirstEndpointAddressWIthEmptyRoutingKeys(their)
        run {
            var pairwise: Pairwise? = me.getPairwiseListi()?.loadForDid(theirEntity.did)
            val isFilled = pairwise != null && pairwise.getMetadatai() != null
            if (!isFilled) {
                val me_ = Pairwise.Me(myEntity.did, myEntity.verkey)
                val their_ = Pairwise.Their(
                    theirEntity.did,
                    theirEntity.label,
                    theirEndpointAddress,
                    theirEntity.verkey,
                    ArrayList<String>()
                )
                val metadata: JSONObject = JSONObject().put(
                    "me",
                    JSONObject().put("did", myEntity.did).put("verkey", myEntity.verkey)
                ).put(
                    "their",
                    JSONObject().put("did", theirEntity.did).put("verkey", theirEntity.verkey)
                        .put("label", theirEntity.label).put(
                            "endpoint",
                            JSONObject().put("address", theirEndpointAddress)
                                .put("routing_keys",JSONArray())
                        )
                )
                pairwise = Pairwise(me_, their_, metadata)
                me.getWalleti()?.did?.storeTheirDid(theirEntity.did, theirEntity.verkey)
                me.getPairwiseListi()?.ensureExists(pairwise)
            }
        }
        run {
            var pairwise: Pairwise? = their.getPairwiseListi()?.loadForDid(theirEntity.did)
            val isFilled = pairwise != null && pairwise?.getMetadatai() != null
            if (!isFilled) {
                val me_ = Pairwise.Me(theirEntity.did, theirEntity.verkey)
                val their_ = Pairwise.Their(
                    myEntity.did,
                    myEntity.label,
                    myEndpointAddress,
                    myEntity.verkey,
                    ArrayList<String>()
                )
                val metadata: JSONObject = JSONObject().put(
                    "me",
                    JSONObject().put("did", theirEntity.did).put("verkey", theirEntity.verkey)
                ).put(
                    "their",
                   JSONObject().put("did", myEntity.did).put("verkey", myEntity.verkey)
                        .put("label", myEntity.label).put(
                            "endpoint",
                           JSONObject().put("address", myEndpointAddress)
                                .put("routing_keys", JSONArray())
                        )
                )
                pairwise = Pairwise(me_, their_, metadata)
                their.getWalleti()?.did?.storeTheirDid(myEntity.did, myEntity.verkey)
                pairwise?.let {
                    their.getPairwiseListi()?.ensureExists(it)
                }
            }
        }
        return me.getPairwiseListi()!!.loadForDid(theirEntity.did)!!
    }

    companion object {
        private var instance: ConfTest? = null
        fun newInstance(): ConfTest {
            val confTest = ConfTest()
            confTest.configureTestEnv()
            return confTest
        }

        var proverMasterSecretName = "prover_master_secret_name"
        val singletonInstance: ConfTest
            get() {
                if (instance == null) {
                    instance = newInstance()
                }
                return instance!!
            }

        fun getState(ledger: AbstractMicroledger): JSONObject {
            return JSONObject().put("name", ledger.name()).put("seq_no", ledger.seqNo())
                .put("size", ledger.size()).put("uncommitted_size", ledger.uncommittedSize())
                .put("root_hash", ledger.rootHash()).put("uncommitted_root_hash", ledger.uncommittedRootHash())
        }
    }
}
