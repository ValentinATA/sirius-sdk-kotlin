package com.sirius.library.agent

import com.sirius.library.agent.connections.AgentEvents
import com.sirius.library.agent.coprotocols.AbstractCoProtocolTransport
import com.sirius.library.agent.coprotocols.PairwiseMobileCoProtocolTransport
import com.sirius.library.agent.coprotocols.TheirEndpointMobileCoProtocolTransport
import com.sirius.library.agent.ledger.Ledger
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.agent.pairwise.WalletPairwiseList
import com.sirius.library.agent.storages.InWalletImmutableCollection
import com.sirius.library.agent.wallet.LocalWallet
import com.sirius.library.agent.wallet.MobileWallet
import com.sirius.library.base.WebSocketConnector
import com.sirius.library.errors.IndyException
import com.sirius.library.messaging.Message
import com.sirius.library.utils.*



expect class MobileAgent(walletConfig: JSONObject?, walletCredentials: JSONObject?) :
    AbstractAgent {
    var walletConfig: JSONObject?
    var walletCredentials: JSONObject?
    var timeoutSec : Int
    var mediatorAddress: String?

    var sender: BaseSender?
    var indyWallet: LocalWallet?
    var webSockets: MutableMap<String, WebSocketConnector>

    inner class MobileAgentEvents : AgentEvents {
        var future: CompletableFutureKotlin<Message?>?
        override fun pull(): CompletableFutureKotlin<Message?>?
    }

    var events: MutableList<Pair<MobileAgentEvents, Listener>>

    fun create()

    override fun open()

     val networks: List<String>?
    override val isOpen: Boolean
    override val name: String


    override fun sendMessage(
        message: Message?,
        their_vk: List<String?>?,
        endpoint: String,
        my_vk: String?,
        routing_keys: List<String?>?
    )

     fun sendMessage(
        message: Message?,
        endpoint: String?
    )

    fun connect(endpoint: String?)

    fun packMessage(msg: Message, myVk: String?, theirVk: List<String?>): ByteArray?

    fun receiveMsg(bytes: ByteArray)

    override fun close()

    override fun checkIsOpen(): Boolean

    override fun subscribe(): Listener


    override fun unsubscribe(listener: Listener?)

    override fun generateQrCode(value: String?): String?


    override fun acquire(
        resources: List<String?>?,
        lockTimeoutSec: Int?,
        enterTimeoutSec: Int?
    ): Pair<Boolean, List<String>>
    override fun release()


    override fun spawn(my_verkey: String, endpoint: TheirEndpoint): AbstractCoProtocolTransport?

    override fun spawn(pairwise: Pairwise): AbstractCoProtocolTransport?

    override fun spawn(thid: String, pairwise: Pairwise): AbstractCoProtocolTransport?

    override fun spawn(thid: String): AbstractCoProtocolTransport?

    override fun spawn(
        thid: String,
        pairwise: Pairwise,
        pthid: String
    ): AbstractCoProtocolTransport?


    override fun spawn(thid: String, pthid: String): AbstractCoProtocolTransport?

}

