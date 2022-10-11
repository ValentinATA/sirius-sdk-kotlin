package com.sirius.library.hub.coprotocols

import com.sirius.library.agent.coprotocols.AbstractCoProtocolTransport
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.sirius_exceptions.SiriusPendingOperation
import com.sirius.library.hub.Context
import com.sirius.library.messaging.Message
import kotlin.coroutines.cancellation.CancellationException

class CoProtocolThreadedP2P : AbstractP2PCoProtocol {
    var thid: String
    var to: Pairwise
    var pthid: String? = null

    constructor(context: Context<*>, thid: String, to: Pairwise, pthid: String?, timeToLiveSec: Int) : super(context) {
        this.thid = thid
        this.to = to
        this.pthid = pthid
        this.timeToLiveSec = timeToLiveSec
    }

    constructor(context: Context<*>, thid: String, to: Pairwise) : super(context) {
        this.thid = thid
        this.to = to
    }

    constructor(context: Context<*>, thid: String, to: Pairwise, timeToLiveSec: Int) : super(context) {
        this.thid = thid
        this.to = to
        this.timeToLiveSec = timeToLiveSec
    }

    @Throws(SiriusPendingOperation::class, CancellationException::class)
    override suspend fun send(message: Message): Boolean {
        return transportLazy?.send(message) ?: false
    }


    override suspend fun sendAndWait(message: Message): Pair<Boolean, Message?> {
        return transportLazy?.sendAndWait(message) ?: Pair<Boolean, Message?>(false, null)
    }

    private val transportLazy: AbstractCoProtocolTransport?
        private get() {
            if (transport == null) {
                transport = if (pthid == null) {
                    context.currentHub?.agentConnectionLazy?.spawn(thid, to)
                } else {
                    context.currentHub?.agentConnectionLazy?.spawn(thid, to, pthid!!)
                }
                transport?.timeToLiveSec = timeToLiveSec
                transport?.start()
                started = true
            }
            return transport
        }
}
