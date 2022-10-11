package com.sirius.library.agent

import Indy.*
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
import com.sirius.library.agent.wallet.results.ErrorHandler
import com.sirius.library.base.WebSocketConnector
import com.sirius.library.errors.IndyException
import com.sirius.library.messaging.Message
import com.sirius.library.utils.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

actual class MobileAgent actual constructor(
    actual var walletConfig: JSONObject?,
    actual var walletCredentials: JSONObject?
) :
    AbstractAgent() {


    actual var timeoutSec: Int = 60
    actual var mediatorAddress: String? = null
    actual var sender: BaseSender? = null
    actual var indyWallet: LocalWallet? = null
    actual var webSockets: MutableMap<String, WebSocketConnector> = HashMap()

    actual inner class MobileAgentEvents : AgentEvents {
        actual var future: CompletableFutureKotlin<Message?>? = null

        actual override fun pull(): CompletableFutureKotlin<Message?>? {
            future = CompletableFutureKotlin<Message?>()
            return future
        }
    }

    actual var events: MutableList<Pair<MobileAgentEvents, Listener>> = mutableListOf()

    actual fun create() {
        try {
           val future = CompletableFutureKotlin<Boolean>()
           IndyWallet().createWalletWithConfig(walletConfig.toString(), walletCredentials.toString()){
                future.complete(true)
               ErrorHandler(it).handleError()
           }
            future.get()
        }catch (e : Exception){
            e.printStackTrace()
        }

    }

    actual override fun open() {
        try {
            val future = CompletableFutureKotlin<IndyHandle?>()
            IndyWallet().openWalletWithConfig(walletConfig.toString(), walletCredentials.toString()){
                    error: NSError?, searchHandle : IndyHandle? ->
                   ErrorHandler(error).handleError()
                   future.complete(searchHandle)
            }
            val handle = future.get()
            handle?.let {
                indyWallet = LocalWallet(handle)
            }
        } catch (e: Exception) {
           e.printStackTrace()
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

                val str: String = "[]"
              //  IndyPool().
                 //   Pool.listPools().get(timeoutSec.toLong(), java.util.concurrent.TimeUnit.SECONDS)
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    actual override val isOpen: Boolean
        get() =  indyWallet != null
    actual override val name: String
        get() = "MobileAgent IOS"

    actual override suspend fun sendMessage(
        message: Message?,
        their_vk: List<String?>?,
        endpoint: String,
        my_vk: String?,
        routing_keys: List<String?>?
    ) {
        if (routing_keys?.isEmpty() == false) throw RuntimeException("Not yet supported!")
        println("sendMessage their_vk=" + their_vk)
        println("sendMessage my_vk=" + my_vk)
        val cryptoMsg = packMessage(message ?: Message(), my_vk, their_vk.orEmpty())
        if (sender != null) {
            val isSend = sender!!.sendTo(endpoint, cryptoMsg)
            //return new Pair<>(isSend, null);
        }
    }

    actual suspend fun sendMessage(
        message: Message?,
        endpoint: String?
    ) : Boolean{

        if (sender != null) {
            val isSend = sender!!.sendTo(endpoint,  StringUtils.stringToBytes(message?.getMessageObjec().toString(), StringUtils.CODEC.UTF_8))
            return isSend
        }
        return false
    }

    actual fun connect(endpoint: String?) {
        sender?.open(endpoint)
    }

    actual fun packMessage(
        msg: Message,
        myVk: String?,
        theirVk: List<String?>
    ): ByteArray? {
        val receivers: JSONArray = JSONArray(theirVk)
        try {
            println(" receivers.toString()=" + receivers.toString())
            println(" myVk=" + myVk)
            println("  StringUtils.stringToBytes(msg.messageObj.toString(), UTF_8)=" + msg.messageObj.toString())
            val future = CompletableFutureKotlin<ByteArray?>()
            //TODO StrinCodec
            val messBytes =   StringUtils.stringToBytes(msg.messageObj.toString(), StringUtils.CODEC.UTF_8)
            val messData = memScoped{
                NSData.create(bytes = allocArrayOf(messBytes), length = messBytes.size.toULong())
            }
            IndyCrypto.packMessage(messData,receivers.toString(),myVk, indyWallet!!.walletHandle ){
                    error: NSError?, data : NSData? ->
                ErrorHandler(error).handleError()
                val d = memScoped{data}
                var  byteArray = ByteArray(0)
                d?.let {
                    byteArray =   ByteArray(d.length.toInt()).apply {
                        usePinned {
                            memcpy(it.addressOf(0), d.bytes,d.length)
                        }
                    }
                }
                future.complete(byteArray)
            }
            return future.get()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    actual fun receiveMsg(bytes: ByteArray) {
        try {
            var unpackedMessageBytes: ByteArray
            val eventMessage: JSONObject
            if (JSONObject(StringUtils.bytesToString(bytes, StringUtils.CODEC.UTF_8)).has("protected")) {
                val future = CompletableFutureKotlin<ByteArray?>()
                val data = memScoped{
                    NSData.create(bytes = allocArrayOf(bytes), length = bytes.size.toULong())
                }
                IndyCrypto.unpackMessage(data ,indyWallet!!.walletHandle){
                        error: NSError?, data : NSData? ->
                    ErrorHandler(error).handleError()
                    val d = memScoped{data}
                    var  byteArray = ByteArray(0)
                    d?.let {
                        byteArray =   ByteArray(d.length.toInt()).apply {
                            usePinned {
                                memcpy(it.addressOf(0), d.bytes,d.length)
                            }
                        }
                    }
                    future.complete(byteArray)
                }
                unpackedMessageBytes = future.get() ?: ByteArray(0)

                println("Chipre String(unpackedMessageBytes)="+StringUtils.bytesToString(unpackedMessageBytes, StringUtils.CODEC.UTF_8))
                val unpackedMessage: JSONObject = JSONObject(StringUtils.bytesToString(unpackedMessageBytes, StringUtils.CODEC.UTF_8))
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
                val unpackedMessage: JSONObject = JSONObject(StringUtils.bytesToString(unpackedMessageBytes, StringUtils.CODEC.UTF_8))
                println("String(unpackedMessageBytes)="+StringUtils.bytesToString(unpackedMessageBytes, StringUtils.CODEC.UTF_8))
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
        }  catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual override fun close() {
        sender?.close()
        try {
            indyWallet!!.wallet.closeWalletWithHandle(indyWallet!!.walletHandle){
                ErrorHandler(it).handleError()
            }
        } catch (e: Exception) {
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

    actual override fun release() {
    }

    actual override fun spawn(
        my_verkey: String,
        endpoint: TheirEndpoint
    ): AbstractCoProtocolTransport? {
        return TheirEndpointMobileCoProtocolTransport(this, my_verkey, endpoint)
    }

    actual override fun spawn(pairwise: Pairwise): AbstractCoProtocolTransport? {
        return PairwiseMobileCoProtocolTransport(this, pairwise)
    }

    actual override fun spawn(
        thid: String,
        pairwise: Pairwise
    ): AbstractCoProtocolTransport? {
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

    actual override fun spawn(
        thid: String,
        pthid: String
    ): AbstractCoProtocolTransport? {
        return null
    }




}