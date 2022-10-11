package com.sirius.library.hub.coprotocols

import com.sirius.library.agent.coprotocols.AbstractCloudCoProtocolTransport.Companion.PLEASE_ACK_DECORATOR
import com.sirius.library.agent.coprotocols.AbstractCloudCoProtocolTransport.Companion.THREAD_DECORATOR
import com.sirius.library.agent.coprotocols.AbstractCoProtocolTransport
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidPayloadStructure
import com.sirius.library.errors.sirius_exceptions.SiriusPendingOperation
import com.sirius.library.hub.Context
import com.sirius.library.messaging.Message
import com.sirius.library.utils.JSONObject
import kotlin.coroutines.cancellation.CancellationException

class CoProtocolP2PAnon(
    context: Context<*>,
    var myVerkey: String,
    var endpoint: TheirEndpoint,
    var protocols: List<String>,
    timeToLiveSec: Int
) :
    AbstractP2PCoProtocol(context) {
    var threadId = ""

    @Throws(SiriusPendingOperation::class, CancellationException::class)
    override suspend fun send(message: Message): Boolean {
        setup(message, false)
        return transportLazy?.send(message) ?: false
    }

    @Throws(SiriusInvalidPayloadStructure::class, SiriusInvalidMessage::class, SiriusPendingOperation::class,CancellationException::class)
    override suspend fun sendAndWait(message: Message): Pair<Boolean, Message?> {
        setup(message)
        val res: Pair<Boolean, Message?> = transportLazy?.sendAndWait(message) ?: Pair<Boolean, Message?>(false, null)
        println("sendAndWait res="+res)
        val response: Message? = res.second
        if (res.first) {
            if (response?.messageObjectHasKey(PLEASE_ACK_DECORATOR) == true) {
                threadId = response.getMessageObjec()?.getJSONObject(PLEASE_ACK_DECORATOR)?.optString("message_id") ?:""
                if (threadId.isEmpty()) threadId = message.getId() ?:""
            } else {
                threadId = ""
            }
        }
        return res
    }

    private fun setup(message: Message, pleaseAck: Boolean) {
        if (pleaseAck) {
            if (!message.messageObjectHasKey(PLEASE_ACK_DECORATOR)) {
                message.getMessageObjec()
                    ?.put(PLEASE_ACK_DECORATOR, JSONObject().put("message_id", message.getId()))
            }
        }
        if (!threadId.isEmpty()) {
            var thread: JSONObject? = message.getMessageObjec()?.optJSONObject(THREAD_DECORATOR)
            thread = if (thread != null) thread else JSONObject()
            if (!thread.has("thid")) {
                thread.put("thid", threadId)
                message.getMessageObjec()?.put(THREAD_DECORATOR, thread)
            }
        }
    }

    private fun setup(message: Message) {
        setup(message, true)
    }

    private val transportLazy: AbstractCoProtocolTransport?
        private get() {
            if (transport == null) {
                println("transportLazy endpoint="+endpoint.endpointAddress)
                println("transportLazy verke="+endpoint.verkey)
                println("transportLazy myVerkey="+myVerkey)
                transport = context.currentHub?.agentConnectionLazy?.spawn(myVerkey, endpoint)
                transport?.protocols = protocols
                transport?.timeToLiveSec = timeToLiveSec
                transport?.start()
                started = true
            }
            return transport
        }

    init {
        this.timeToLiveSec = timeToLiveSec
    }
}
