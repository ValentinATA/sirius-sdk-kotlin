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

import org.hyperledger.indy.sdk.LibIndy
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.wallet.Wallet


actual  class MobileAgent actual constructor(walletConfig: JSONObject?, walletCredentials: JSONObject?) :
    AbstractAgent() {
    actual var walletConfig: JSONObject? = null
    actual var walletCredentials: JSONObject? = null
    actual var timeoutSec = 60
    actual var mediatorAddress: String? = null

    actual var sender: BaseSender? = null

    /* var sender: BaseSender? = object : BaseSender() {
         override fun sendTo(endpoint: String?, data: ByteArray?): Boolean {
             if (endpoint.startsWith("http")) {
                 try {
                     val httpClient: HttpClient = HttpClients.createDefault()
                     val httpPost = HttpPost(endpoint)
                     httpPost.setHeader("content-type", "application/ssi-agent-wire")
                     httpPost.setEntity(ByteArrayEntity(cryptoMsg))
                     httpClient.execute(httpPost)
                 } catch (e: java.io.IOException) {
                     e.printStackTrace()
                 }
             } else if (endpoint.startsWith("ws")) {
                 val webSocket: WebSocketConnector? = getWebSocket(endpoint)
                 if (!webSocket.isOpen()) webSocket.open()
                 webSocket.write(cryptoMsg)
             } else {
                 throw java.lang.RuntimeException("Not yet supported!")
             }
             return true
         }

         override fun open(endpoint: String?) {
             getWebSocket(endpoint)
         }

         override fun close() {
             webSockets.forEach {
                 it.value.close()
             }
         }
     }*/
    actual var indyWallet: LocalWallet? = null
    actual var webSockets: MutableMap<String, WebSocketConnector> = HashMap<String, WebSocketConnector>()

    actual inner class MobileAgentEvents : AgentEvents {
        actual var future: CompletableFutureKotlin<Message?>? = null
        actual override fun pull(): CompletableFutureKotlin<Message?>? {
            future = CompletableFutureKotlin<Message?>()
            println("MobileAgentEvents pull="+future)
            return future
        }
    }

    actual var events: MutableList<Pair<MobileAgentEvents, Listener>> =
        ArrayList<Pair<MobileAgentEvents, Listener>>()

    actual fun create() {
        try {
            Wallet.createWallet(walletConfig.toString(), walletCredentials.toString())
                .get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.lang.Exception) {
            if (!e.message!!.contains("WalletExistsException")) e.printStackTrace()
        }
    }

    actual override fun open() {
        try {
            indyWallet = Wallet.openWallet(walletConfig.toString(), walletCredentials.toString())
                .get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.lang.Exception) {
            if (!e.message!!.contains("WalletAlreadyOpenedException")) e.printStackTrace()
        }
        if (indyWallet != null) {
            wallet = MobileWallet(indyWallet!!)
        }
        pairwiseList = WalletPairwiseList(wallet!!.pairwise, wallet!!.did)
        if (storage == null) {
            storage = InWalletImmutableCollection(wallet!!.nonSecrets)
        }
        for (network in networks.orEmpty()) {
            ledgers.put(
                network,
                Ledger(network, wallet!!.ledger, wallet!!.anoncreds, wallet!!.cache, storage!!)
            )
        }
    }

     actual val networks: List<String>?
         get() {
            try {
                val str: String =
                    Pool.listPools().get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                val arr: JSONArray = JSONArray(str)
                val networks: MutableList<String> = ArrayList<String>()
                for (o in arr) {
                    println("o="+o)
                    if (o is JSONObject) {
                        val poolString = (o as JSONObject).optString("pool")
                        poolString?.let {
                            networks.add(poolString)
                        }
                    }
                }
                return networks
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            return null
        }
    actual override val isOpen: Boolean
        get() = indyWallet != null
    actual override val name: String
        get() = "Mobile agent"


    actual override fun sendMessage(
        message: Message?,
        their_vk: List<String?>?,
        endpoint: String,
        my_vk: String?,
        routing_keys: List<String?>?
    ) {
        if (routing_keys?.isEmpty() == false) throw java.lang.RuntimeException("Not yet supported!")
        println("sendMessage their_vk=" + their_vk)
        println("sendMessage my_vk=" + my_vk)
        val cryptoMsg = packMessage(message ?: Message(), my_vk, their_vk.orEmpty())
        if (sender != null) {
            val isSend = sender!!.sendTo(endpoint, cryptoMsg)
            //return new Pair<>(isSend, null);
        }
    }

    actual fun sendMessage(
        message: Message?,
        endpoint: String?
    ) {
        if (sender != null && endpoint!=null) {
            val isSend = sender!!.sendTo(endpoint, StringUtils.stringToBytes(message?.getMessageObjec().toString(), StringUtils.CODEC.UTF_8))
            //return new Pair<>(isSend, null);
        }
    }

    /*  fun getWebSocket(endpoint: String?): WebSocketConnector? {
          return if (webSockets.containsKey(endpoint)) {
              webSockets[endpoint]
          } else {
              val webSocket = WebSocketConnector(endpoint ?:"", "", null)
              val fAgent = this
              webSocket.readCallback = object : java.util.function.Function<ByteArray?, java.lang.Void?>() {

                  override fun apply(bytes: ByteArray?): Void? {
                      fAgent.receiveMsg(bytes)
                      return null
                  }
              }
              webSocket.open()
              webSockets[endpoint?:""] = webSocket
              webSocket
          }
      }*/

    actual fun connect(endpoint: String?) {
        sender!!.open(endpoint)
    }

    actual fun packMessage(msg: Message, myVk: String?, theirVk: List<String?>): ByteArray? {
        val receivers: JSONArray = JSONArray(theirVk)
        try {
            println(" receivers.toString()=" + receivers.toString())
            println(" myVk=" + myVk)
           println("  StringUtils.stringToBytes(msg.messageObj.toString(), UTF_8)=" + msg.messageObj.toString())
            val escapedMessage =  StringCodec().escapeStringLikePython(msg.messageObj.toString())
            println("  escapedMessage, UTF_8)=" + escapedMessage)
            return Crypto.packMessage(
                indyWallet, receivers.toString(),
                myVk, StringUtils.stringToBytes(escapedMessage, StringUtils.CODEC.UTF_8)
            ).get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    actual fun receiveMsg(bytes: ByteArray) {
        try {
            val unpackedMessageBytes: ByteArray
            val eventMessage: JSONObject
            if (JSONObject(String(bytes)).has("protected")) {
                unpackedMessageBytes =
                    Crypto.unpackMessage(indyWallet, bytes)
                        .get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
                println("Chipre String(unpackedMessageBytes)="+String(unpackedMessageBytes))
                val unpackedMessage: JSONObject = JSONObject(String(unpackedMessageBytes))
                eventMessage =
                    JSONObject().put(
                        "@type",
                        "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/sirius_rpc/1.0/event"
                    )
                        .put("content_type", "application/ssi-agent-wire")
                        .put("@id", UUID.randomUUID)
                        .put("message", JSONObject(unpackedMessage.optString("message")))
                        .put("recipient_verkey", unpackedMessage.optString("recipient_verkey"))
                if (unpackedMessage.has("sender_verkey")) {
                    eventMessage.put("sender_verkey", unpackedMessage.optString("sender_verkey"))
                }
            } else {
                unpackedMessageBytes = bytes
                val unpackedMessage: JSONObject = JSONObject(String(unpackedMessageBytes))
                println("String(unpackedMessageBytes)="+String(unpackedMessageBytes))
                eventMessage =
                    JSONObject().put(
                        "@type",
                        "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/sirius_rpc/1.0/event"
                    )
                        .put("content_type", "application/ssi-agent-wire")
                        .put("@id", UUID.randomUUID)
                        .put("message", unpackedMessage)
            }
            for (one: Pair<MobileAgentEvents, Listener> in events) {
                one.first.future?.complete(Message(eventMessage))
            }
        } catch (e: java.lang.InterruptedException) {
            e.printStackTrace()
        } catch (e: java.util.concurrent.ExecutionException) {
            e.printStackTrace()
        } catch (e: java.util.concurrent.TimeoutException) {
            e.printStackTrace()
        } catch (e: IndyException) {
            e.printStackTrace()
        }
    }

    actual override fun close() {
        sender!!.close()
        try {
            indyWallet!!.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    actual override fun checkIsOpen(): Boolean {
        return indyWallet != null
    }

    actual override fun subscribe(): Listener {
        val e: MobileAgentEvents = MobileAgentEvents()
        val listener = Listener(e, this)
        events.add(Pair(e, listener))
        return Listener(e, this)
    }


    actual override fun unsubscribe(listener: Listener?) {
        for (e in events) {
            if (e.second === listener) {
                events.remove(e)
                break
            }
        }
    }

    actual override fun generateQrCode(value: String?): String? {
        return null
    }


    actual override fun acquire(
        resources: List<String?>?,
        lockTimeoutSec: Int?,
        enterTimeoutSec: Int?
    ): Pair<Boolean, List<String>> {
        return Pair(false, listOf())
    }

    actual override fun release() {}


    actual override fun spawn(my_verkey: String, endpoint: TheirEndpoint): AbstractCoProtocolTransport? {
        return TheirEndpointMobileCoProtocolTransport(this, my_verkey, endpoint)
    }

    actual override fun spawn(pairwise: Pairwise): AbstractCoProtocolTransport? {
        return PairwiseMobileCoProtocolTransport(this, pairwise)
    }

    actual override fun spawn(thid: String, pairwise: Pairwise): AbstractCoProtocolTransport? {
        return null
    }

    actual override fun spawn(thid: String): AbstractCoProtocolTransport? {
        return null
    }

    actual override fun spawn(
        thid: String,
        pairwise: Pairwise,
        pthid: String
    ): AbstractCoProtocolTransport? {
        return null
    }


    actual override fun spawn(thid: String, pthid: String): AbstractCoProtocolTransport? {
        return null
    }

    init {
        this.walletConfig = walletConfig
        this.walletCredentials = walletCredentials
        LibIndy.init()
    }


}

