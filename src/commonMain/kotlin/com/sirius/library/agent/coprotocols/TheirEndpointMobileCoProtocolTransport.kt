package com.sirius.library.agent.coprotocols


import com.sirius.library.agent.MobileAgent
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.TheirEndpoint
import com.sirius.library.messaging.Message


class TheirEndpointMobileCoProtocolTransport(var agent: MobileAgent, var myVerkey: String, var endpoint: TheirEndpoint) :
    AbstractCoProtocolTransport() {
    var listener: Listener = agent.subscribe()

    override fun start() {}
    override fun stop() {
        listener.unsubscribe()
    }

    override suspend fun sendAndWait(message: Message): Pair<Boolean, Message?> {
        println("sendAndWait resTheirEndpointMobileCoProtocolTransport =")
        send(message)
        val r: GetOneResult? = one
        return if (r != null) {
            //if (r.senderVerkey.equals(endpoint.getVerkey())) {
            Pair(true, r.message)
            //}
        } else Pair(false, null)
    }



    override val one: GetOneResult?
        get() {
            try {
                println("sendAndWait one: GetOneResult? listener ")
                val one = listener.one
                println("sendAndWait one: listener ="+listener)
                println("sendAndWait one: one ="+one)
                val event: Event? = one?.get(timeToLiveSec.toLong())
                println("sendAndWait event:  ="+event)
                if(event !=null){
                    if(event.message()!=null && event.senderVerkey!=null &&event.recipientVerkey!=null){
                        return GetOneResult(event.message()!!, event.senderVerkey!!, event.recipientVerkey!!)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    override suspend fun send(message: Message): Boolean {
        println("send endpoint.verkey="+endpoint.verkey)
        println("send endpoint.endpointAddress="+endpoint.endpointAddress)
        println("send myVerkey="+myVerkey)
       return  agent.sendMessage(
            message,
            listOf(endpoint.verkey),
            endpoint.endpointAddress?:"",
            myVerkey,
            listOf()
        )
    }

    override fun sendMany(message: Message, to: List<Pairwise>): List<Pair<Boolean, String?>> {
        return listOf()
    }


}