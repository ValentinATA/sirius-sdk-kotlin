package com.sirius.library.agent.coprotocols

import com.sirius.library.agent.connections.AgentRPC
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidPayloadStructure
import com.sirius.library.errors.sirius_exceptions.SiriusPendingOperation
import com.sirius.library.messaging.Message
import com.sirius.library.utils.JSONObject

/**
 * CoProtocol based on ~thread decorator
 *
 * See details:
 * - https://github.com/hyperledger/aries-rfcs/tree/master/concepts/0008-message-id-and-threading
 *
 */
class ThreadBasedCoProtocolTransport(var thid: String, pairwise: Pairwise?, rpc: AgentRPC, pthid: String?) :
    AbstractCloudCoProtocolTransport(rpc) {
    var pairwise: Pairwise?
    var pthid: String?
    var receivedOrders: JSONObject?
    var senderOrder = 0
    var their: Pairwise.Their? = null

    @Throws(SiriusPendingOperation::class, SiriusInvalidPayloadStructure::class, SiriusInvalidMessage::class)
    override fun sendAndWait(message: Message): Pair<Boolean, Message?> {
        prepareMessage(message)
        val res: Pair<Boolean, Message?> = super.sendAndWait(message)
        val response: Message? = res.second
        if (res.first) {
            if (response?.messageObjectHasKey(THREAD_DECORATOR) == true) {
                if (response.getMessageObjec().getJSONObject(THREAD_DECORATOR)?.has("sender_order")==true) {
                    val respondSenderOrder: Int? =
                        response.getMessageObjec().getJSONObject(THREAD_DECORATOR)?.getInt("sender_order")
                    if (their != null) {
                        val recipient: String = their!!.did?:""
                        //err = DIDField().validate(recipient)
                        //if err is None:
                        run {
                            val order: Int = receivedOrders?.optInt(recipient, 0) ?:0
                           // receivedOrders?.put(recipient, java.lang.Math.max(order, respondSenderOrder))
                        }
                    }
                }
            }
        }
        return res
    }

    @Throws(SiriusPendingOperation::class)
    override fun send(message: Message) {
        prepareMessage(message)
        super.send(message)
    }


    override fun sendMany(message: Message, to: List<Pairwise>): List<Pair<Boolean, String?>> {
        prepareMessage(message)
        return super.sendMany(message, to)
    }

    private fun prepareMessage(msg: Message) {
        if (!msg.messageObjectHasKey(THREAD_DECORATOR)) {
            val threadDecorator: JSONObject =
                JSONObject().put("thid", thid).put("sender_order", senderOrder)
            if (pthid != null && !pthid!!.isEmpty()) {
                threadDecorator.put("pthid", pthid)
            }
            if (receivedOrders != null) {
                threadDecorator.put("received_orders", receivedOrders)
            }
            senderOrder++
            msg.getMessageObjec().put(THREAD_DECORATOR, threadDecorator)
        }
    }

    override fun start(protocols: List<String>, timeToLiveSec: Int) {
        super.start(protocols, timeToLiveSec)
        rpc.startProtocolWithThreading(thid, timeToLiveSec)
    }

    override fun start(protocols: List<String>) {
        super.start(protocols)
        rpc.startProtocolWithThreading(thid, timeToLiveSec)
    }

    override fun start(timeToLiveSec: Int) {
        super.start(timeToLiveSec)
        rpc.startProtocolWithThreading(thid, timeToLiveSec)
    }

    override fun start() {
        super.start(protocols)
        rpc.startProtocolWithThreading(thid, timeToLiveSec)
    }

    override fun stop() {
        super.stop()
        rpc.stopProtocolWithThreading(thid, true)
    }

    init {
        this.pairwise = pairwise
        this.pthid = pthid
        senderOrder = 0
        receivedOrders = JSONObject()
        if (pairwise != null) {
            their = pairwise.their
            setup(
                pairwise.their.verkey,
                pairwise.their.endpointAddress,
                pairwise.me.verkey,
                pairwise.their.routingKeys
            )
        } else {
            setup(null, null, null, null)
        }
    }
}
