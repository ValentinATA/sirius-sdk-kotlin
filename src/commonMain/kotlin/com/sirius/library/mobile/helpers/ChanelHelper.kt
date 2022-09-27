package com.sirius.library.mobile.helpers



import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener

import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.utils.StringUtils
import kotlinx.coroutines.*


/**
 * This is the helper class to show how the SDK workflow is done. Parse message from different channels (Websocket, FCM etc..)
 * and loop through scenario
 */
object ChanelHelper {


    fun cleanInstance(){
      //  chanelHelper = null
    }



    fun initListener() {
        if (listener == null) {
            CoroutineScope(Dispatchers.Default).launch  {
                listener = SiriusSDK.context?.subscribe()
            }

        }
    }

    var listener: Listener? = null


    fun parseMessage(message: String) {
        initListener()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                println("mylog200 listener=" + listener)
                val cf  = listener!!.one
                SiriusSDK.context?.currentHub?.getAgenti()?.
                receiveMsg(StringUtils.stringToBytes(message, StringUtils.CODEC.UTF_8))
                val event = cf?.get(60L)
              //  val message = event?.message()
                //val type = message.type
                parseMessageByScenario(event)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

    private fun parseMessageByScenario(event: Event?) {
        ScenarioHelper.scenarioMap.forEach { scenario->
            event?.let {
                scenario.value.startScenario(event)
            }
        }
        println("mylog2090 event.message type" + event?.message())
    }

}