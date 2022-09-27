package com.sirius.library.mobile.helpers


import com.sirius.library.hub.MobileContext
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.scenario.BaseScenario
import com.sirius.library.mobile.scenario.EventAction
import com.sirius.library.mobile.scenario.EventActionAbstract
import com.sirius.library.mobile.scenario.EventActionListener
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object ScenarioHelper {

    fun cleanInstance(){
       // scenarioHelper = null
    }

    val scenarioMap : MutableMap<String, BaseScenario> = HashMap()

    lateinit var context: MobileContext


    fun addScenario(name : String,scenario : BaseScenario){
        scenarioMap[name] = scenario
    }

    fun getScenarioBy(name : String) : BaseScenario?{
        return scenarioMap[name]
    }

    fun removeScenario(name : String){
        scenarioMap.remove(name)
    }

    fun stopScenario(name : String, id : String, cause : String, eventActionListener: EventActionListener? = null){
        val scenario = getScenarioBy(name)
        scenario?.let {
            if(it is EventActionAbstract){
               CoroutineScope(Dispatchers.Default).launch {
                    it.actionStart(EventAction.cancel,id, cause,eventActionListener)
                }
            }
        }
    }

    fun acceptScenario(name: String, id: String, comment: String? = null, eventActionListener: EventActionListener? = null){
        val scenario = getScenarioBy(name)
        scenario?.let {
            if(it is EventActionAbstract){
                CoroutineScope(Dispatchers.Default).launch  {
                    it.actionStart(EventAction.accept,id, comment,eventActionListener)
                }
            }
        }


    }

}