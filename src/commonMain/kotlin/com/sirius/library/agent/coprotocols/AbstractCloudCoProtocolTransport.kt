package com.sirius.library.agent.coprotocols

import com.sirius.library.agent.connections.AgentRPC
import com.sirius.library.agent.connections.RoutingBatch
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.sirius_exceptions.*
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageUtil
import com.sirius.library.messaging.Type
import com.sirius.library.utils.Date
import com.sirius.library.utils.JSONObject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Abstraction application-level protocols in the context of interactions among agent-like things.
 *
 * Sirius SDK protocol is high-level abstraction over Sirius transport architecture.
 * Approach advantages:
 * - developer build smart-contract logic in block-style that is easy to maintain and control
 * - human-friendly source code of state machines in procedural style
 * - program that is running in separate coroutine: lightweight abstraction to start/kill/state-detection work thread
 * See details:
 * - https://github.com/hyperledger/aries-rfcs/tree/master/concepts/0003-protocols
 */
abstract class AbstractCloudCoProtocolTransport(rpc: AgentRPC) : AbstractCoProtocolTransport() {
    var rpc: AgentRPC
    var theirVK: String? = null
    var myVerkey: String? = null
    var endpoint: String? = null
    var routingKeys: List<String>? = null
    var isSetup = false
    var isStarted = false
    var dieTimestamp: Date? = null
    var pleaseAckIds: MutableList<String> = ArrayList<String>()
    var checkVerkeys = false
    var checkProtocols = true

    /**
     * Should be called in Descendant
     * @param theirVerkey
     * @param endpoint
     * @param myVerkey
     * @param routing_keys
     */
    fun setup(theirVerkey: String?, endpoint: String?, myVerkey: String?, routing_keys: List<String>?) {
        theirVK = theirVerkey
        this.myVerkey = myVerkey
        this.endpoint = endpoint
        routingKeys = routing_keys
        if (routingKeys == null) {
            routingKeys = ArrayList<String>()
        }
        isSetup = true
    }

    override fun start() {
        dieTimestamp = null
        checkProtocols = false
        isStarted = true
    }

    open fun start(protocols: List<String>) {
        dieTimestamp = null
        this.protocols = protocols
        if (protocols.isEmpty()) checkProtocols = false
        isStarted = true
    }

    open fun start(timeToLiveSec: Int) {
        checkProtocols = false
        this.timeToLiveSec = timeToLiveSec
        dieTimestamp = Date(Date().time + timeToLiveSec * 1000L)
        isStarted = true
    }

    open fun start(protocols: List<String>, timeToLiveSec: Int) {
        this.protocols = protocols
        if (protocols.isEmpty()) checkProtocols = false
        this.timeToLiveSec = timeToLiveSec
        dieTimestamp = Date(Date().time + timeToLiveSec * 1000L)
        isStarted = true
    }

    override fun stop() {
        dieTimestamp = null
        isStarted = false
        cleanupContext()
    }

    private fun cleanupContext(message: Message) {
        if (message.messageObjectHasKey(PLEASE_ACK_DECORATOR)) {
            val ackMessageId: String
            ackMessageId =
                if (message?.getJSONOBJECTFromJSON(PLEASE_ACK_DECORATOR)?.has("message_id") == true)
                    message?.getJSONOBJECTFromJSON(
                    PLEASE_ACK_DECORATOR
                )?.getString("message_id") ?:"" else message.getId() ?:""
            rpc.stopProtocolWithThreads(pleaseAckIds, true)
            pleaseAckIds.removeAll {ackId: String ->
                ackId == ackMessageId
            }
        }
    }

    private fun cleanupContext() {
        rpc.stopProtocolWithThreads(pleaseAckIds, true)
        pleaseAckIds.clear()
    }

    private fun setupContext(message: Message) {
        if (message.messageObjectHasKey(PLEASE_ACK_DECORATOR)) {
            val ackMessageId: String
            ackMessageId =
                if (message.getJSONOBJECTFromJSON(PLEASE_ACK_DECORATOR)
                        ?.has("message_id") == true)
                    message?.getJSONOBJECTFromJSON(
                    PLEASE_ACK_DECORATOR
                )?.getString("message_id") ?:"" else message.getId() ?:""
            val ttl: Int = this.timeToLiveSec
            rpc.startProtocolWithThreads(listOf(ackMessageId), ttl)
            pleaseAckIds.add(ackMessageId)
        }
    }

    @Throws(SiriusPendingOperation::class, SiriusInvalidPayloadStructure::class, SiriusInvalidMessage::class,
        CancellationException::class
    )
    override suspend fun sendAndWait(message: Message): Pair<Boolean, Message?> {
        if (!isSetup) {
            throw SiriusPendingOperation("You must Setup protocol instance at first")
        }
        rpc.setTimeouti(timeToLiveSec)
        setupContext(message)
        var event: Message? = null
        try {
            event = rpc.sendMessage(message, listOf(theirVK), endpoint!!, myVerkey, routingKeys, true)
        } catch (siriusConnectionClosed: SiriusConnectionClosed) {
            siriusConnectionClosed.printStackTrace()
        } catch (siriusRPCError: SiriusRPCError) {
            siriusRPCError.printStackTrace()
        } finally {
            cleanupContext(message)
        }
        if (checkVerkeys) {
            val recipientVerkey: String? = event?.getStringFromJSON("recipient_verkey")
            val senderVerkey: String? = event?.getStringFromJSON("sender_verkey")
            if (recipientVerkey != myVerkey) {
                throw SiriusInvalidPayloadStructure("Unexpected recipient_verkey: $recipientVerkey")
            }
            if (senderVerkey != theirVK) {
                throw SiriusInvalidPayloadStructure("Unexpected sender_verkey: $senderVerkey")
            }
        }
        val payload: JSONObject? = event?.getJSONOBJECTFromJSON("message")
        return if (payload != null) {
            var okMsg: Pair<Boolean, Message?> = Pair(false, null)
            try {
                okMsg = MessageUtil.restoreMessageInstance(payload.toString())
            } catch (e:Exception) {
                e.printStackTrace()
            }
            if (!okMsg.first) {
                okMsg = Pair(true, Message(payload.toString()))
            }
            if (checkProtocols) {
                try {
                    if (!protocols.contains(Type.fromStr(message.getType()?:"").protocol)) {
                        throw SiriusInvalidMessage(
                            "@type has unexpected protocol " + Type.fromStr(message.getType()?:"").protocol
                        )
                    }
                } catch (siriusInvalidType: SiriusInvalidType) {
                    siriusInvalidType.printStackTrace()
                }
            }
            okMsg
        } else {
            Pair(false, null)
        }
        return   Pair(false, null)
    }

   // @get:Throws(SiriusInvalidPayloadStructure::class)
    override val one: GetOneResult
        get() {
            val event: Message? = rpc.readProtocolMessage()
            var message: Message? = null
            if (event?.messageObjectHasKey("message")==true) {
                try {
                    val (first, second) = MessageUtil.restoreMessageInstance(
                        event.getMessageObjec().get("message").toString()
                    )
                    if (first) {
                        message = second
                    } else {
                        //message = Message(event?.getMessageObj()?.getJSONObject("message")?:"")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val senderVerkey: String? = event?.getMessageObjec()?.optString("sender_verkey", null)
            val recipientVerkey: String? = event?.getMessageObjec()?.optString("recipient_verkey", null)
            return GetOneResult(message!!, senderVerkey!!, recipientVerkey!!)
        }

    @Throws(SiriusPendingOperation::class,CancellationException::class)
    override suspend fun send(message: Message): Boolean {
        if (!isSetup) {
            throw SiriusPendingOperation("You must Setup protocol instance at first")
        }
        rpc.setTimeouti(timeToLiveSec)
        setupContext(message)
        var messageLast : Message? = null
        try {
            messageLast = rpc.sendMessage(message, listOf(theirVK), endpoint!!, myVerkey, routingKeys, false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cleanupContext(message)
        }
        return messageLast != null
    }


    override fun sendMany(message: Message, to: List<Pairwise>): List<Pair<Boolean, String?>> {
        val batches: MutableList<RoutingBatch> = ArrayList<RoutingBatch>()
        for (p in to) {
            batches.add(
                RoutingBatch(
                    listOfNotNull(p.their.verkey),
                    p.their.endpointAddress,
                    p.me.verkey,
                    p.their.routingKeys
                )
            )
        }
        if (!isSetup) {
            throw SiriusPendingOperation("You must Setup protocol instance at first")
        }
        rpc.setTimeouti(timeToLiveSec)
        setupContext(message)
        try {
            return rpc.sendMessageBatched(message, batches)
        } catch (siriusConnectionClosed: SiriusConnectionClosed) {
            siriusConnectionClosed.printStackTrace()
        }
        return listOf()
    }

    companion object {
        const val THREAD_DECORATOR = "~thread"
        const val PLEASE_ACK_DECORATOR = "~please_ack"
    }

    init {
        this.rpc = rpc
    }
}
