package com.sirius.library.agent

import com.sirius.library.agent.connections.AgentRPC
import com.sirius.library.agent.connections.BaseAgentConnection
import com.sirius.library.agent.connections.CloudAgentEvents
import com.sirius.library.agent.connections.RemoteCallWrapper
import com.sirius.library.agent.coprotocols.AbstractCoProtocolTransport
import com.sirius.library.agent.coprotocols.PairwiseCoProtocolTransport
import com.sirius.library.agent.coprotocols.TheirEndpointCoProtocolTransport
import com.sirius.library.agent.coprotocols.ThreadBasedCoProtocolTransport
import com.sirius.library.agent.ledger.Ledger
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.microledgers.MicroledgerList
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.agent.pairwise.WalletPairwiseList
import com.sirius.library.agent.storages.InWalletImmutableCollection
import com.sirius.library.agent.wallet.CloudWallet
import com.sirius.library.encryption.P2PConnection
import com.sirius.library.errors.sirius_exceptions.SiriusFieldValueError
import com.sirius.library.messaging.Message
import com.sirius.library.storage.abstract_storage.AbstractImmutableCollection
import com.sirius.library.utils.JSONObject

/**
 * Agent connection in the self-sovereign identity ecosystem.
 *
 *
 * Managing an identity is complex. It is implementation of tools to help you to develop SSI Smart-Contracts logic.
 * See details:
 * - https://github.com/hyperledger/aries-rfcs/tree/master/concepts/0004-agents
 */
class CloudAgent : AbstractAgent {
    var serverAddress: String
    var credentials: ByteArray?
    var p2p: P2PConnection?
    var timeout: Int = BaseAgentConnection.IO_TIMEOUT
    override var name: String?
    var rpc: AgentRPC? = null
    var events: CloudAgentEvents? = null

    /**
     * @param serverAddress example https://my-cloud-provider.com
     * @param credentials   credentials that point websocket connection to your agent and server-side services like
     * routing keys maintenance ant etc.
     * @param p2p           encrypted connection to establish tunnel to Agent that is running on server-side
     * @param timeout
     * @param storage
     * @param name
     */
    constructor(
        serverAddress: String,
        credentials: ByteArray,
        p2p: P2PConnection,
        timeout: Int,
        storage: AbstractImmutableCollection?,
        name: String?
    ) {
        this.serverAddress = serverAddress
        this.credentials = credentials
        this.p2p = p2p
        this.timeout = timeout
        this.name = name
        this.storage = storage
    }

    /**
     * Overload constructor [.CloudAgent]
     */
    constructor(
        serverAddress: String,
        credentials: ByteArray?,
        p2p: P2PConnection?,
        timeout: Int,
        storage: AbstractImmutableCollection?
    ) {
        this.serverAddress = serverAddress
        this.credentials = credentials
        this.p2p = p2p
        this.timeout = timeout
        name = null
        this.storage = storage
    }

    /**
     * Overload constructor [.CloudAgent]
     */
    constructor(serverAddress: String, credentials: ByteArray, p2p: P2PConnection, timeout: Int) {
        this.serverAddress = serverAddress
        this.credentials = credentials
        this.p2p = p2p
        this.timeout = timeout
        name = null
        storage = null
    }

    override fun open() {
        try {
            rpc = AgentRPC(serverAddress, credentials, p2p, timeout)
            rpc!!.create()
            endpoints = rpc!!.endpoints
            wallet = CloudWallet(rpc!!)
            if (storage == null) {
                storage = InWalletImmutableCollection(wallet!!.nonSecrets)
            }
            for (network in rpc!!.networks) {
                ledgers.put(
                    network,
                    Ledger(network, wallet!!.ledger, wallet!!.anoncreds, wallet!!.cache, storage!!)
                )
            }
            pairwiseList = WalletPairwiseList(wallet!!.pairwise, wallet!!.did)
            microledgers = MicroledgerList(rpc!!)
        } catch (siriusFieldValueError: SiriusFieldValueError) {
            siriusFieldValueError.printStackTrace()
        }
    }

    override val isOpen: Boolean
        get() = rpc != null && rpc?.isOpen ?:false

    fun ping(): Boolean {
        try {
            val response: Any ?= rpc?.remoteCall("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/sirius_rpc/1.0/ping_agent", null)
            return if (response is Boolean) {
                response
            } else false
        } catch (siriusConnectionClosed: Exception) {
            siriusConnectionClosed.printStackTrace()
        }
        return false
    }

    /**
     * Implementation of basicmessage feature
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/features/0095-basic-message
     *
     * @param message      Message
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/concepts/0020-message-types
     * @param their_vk     Verkey of recipient
     * @param endpoint     Endpoint address of recipient
     * @param my_vk        VerKey of Sender (AuthCrypt mode)
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/features/0019-encryption-envelope#authcrypt-mode-vs-anoncrypt-mode
     * @param routing_keys Routing key of recipient
     * @return
     */
    override suspend fun sendMessage(
        message: Message?, their_vk: List<String?>?,
        endpoint: String, my_vk: String?, routing_keys: List<String?>?
    ): Boolean {
        checkIsOpen()
        try {
           val message =  rpc?.sendMessage(message, their_vk, endpoint, my_vk, routing_keys, false)
            return message != null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    override fun close() {
        if (rpc != null) {
            rpc?.close()
        }
        if (events != null) {
            events?.close()
        }
        wallet = null
    }

    override fun checkIsOpen(): Boolean {
        if (rpc != null) {
            return true
        }
        throw RuntimeException("Open Agent at first!")
    }

    override fun subscribe(): Listener {
        checkIsOpen()
        events = CloudAgentEvents(serverAddress, credentials, p2p, timeout)
        try {
            events!!.create()
        } catch (siriusFieldValueError: SiriusFieldValueError) {
            siriusFieldValueError.printStackTrace()
        }
        return Listener(events!!, this)
    }

    override fun unsubscribe(listener: Listener?) {}
    override fun generateQrCode(value: String?): String? {
        checkIsOpen()
        val params = RemoteParams.RemoteParamsBuilder.create()
            .add("value", value!!)
            .build()
        try {
            val response: Any? = rpc?.remoteCall("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/admin/1.0/generate_qr", params)
            if (response is JSONObject) {
                val responseObject: JSONObject = response as JSONObject
                return responseObject.getString("url")
            }
        } catch (siriusConnectionClosed: Exception) {
            siriusConnectionClosed.printStackTrace()
        }
        return null
    }



    override fun spawn(myVerkey: String, endpoint: TheirEndpoint): AbstractCoProtocolTransport? {
        val new_rpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            new_rpc.create()
            return TheirEndpointCoProtocolTransport(myVerkey, endpoint, new_rpc)
        } catch (siriusFieldValueError: SiriusFieldValueError) {
            siriusFieldValueError.printStackTrace()
        }
        return null
    }

    override fun spawn(pairwise: Pairwise): PairwiseCoProtocolTransport? {
        val newRpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            newRpc.create()
            return PairwiseCoProtocolTransport(pairwise, newRpc)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun spawn(thid: String, pairwise: Pairwise): ThreadBasedCoProtocolTransport? {
        val newRpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            newRpc.create()
            return ThreadBasedCoProtocolTransport(thid, pairwise, newRpc, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun spawn(thid: String): ThreadBasedCoProtocolTransport? {
        val newRpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            newRpc.create()
            return ThreadBasedCoProtocolTransport(thid, null, newRpc, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun spawn(thid: String, pairwise: Pairwise, pthid: String): ThreadBasedCoProtocolTransport? {
        val newRpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            newRpc.create()
            return ThreadBasedCoProtocolTransport(thid, pairwise, newRpc, pthid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun spawn(thid: String, pthid: String): ThreadBasedCoProtocolTransport? {
        val newRpc = AgentRPC(serverAddress, credentials, p2p, timeout)
        try {
            newRpc.create()
            return ThreadBasedCoProtocolTransport(thid, null, newRpc, pthid)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Acquire N resources given by names
     * @param resources names of resources that you are going to lock
     * @param lockTimeoutSec max timeout resources will be locked. Resources will be automatically unlocked on expire
     * @param enterTimeoutSec timeout to wait resources are released
     * @return
     */
    override fun acquire(
        resources: List<String?>?,
        lockTimeoutSec: Int?,
        enterTimeoutSec: Int?
    ): Pair<Boolean, List<String>> {
        checkIsOpen()
        return object : RemoteCallWrapper<Pair<Boolean, List<String>>?>(rpc!!) {}.remoteCall(
            "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/admin/1.0/acquire",
            RemoteParams.RemoteParamsBuilder.create()
                .add("names", resources!!)
                .add("enter_timeout", enterTimeoutSec!!)
                .add("lock_timeout", lockTimeoutSec!!)
        ) ?: Pair<Boolean, List<String>>(false, listOf())
    }

    override fun release() {
        object :
            RemoteCallWrapper<Unit>(rpc!!) {}.remoteCall("did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/admin/1.0/release")
    }


}


