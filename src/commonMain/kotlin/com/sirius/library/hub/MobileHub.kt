
package com.sirius.library.hub

import com.sirius.library.agent.BaseSender
import com.sirius.library.agent.MobileAgent
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.utils.JSONObject

class MobileHub(config: Config) : AbstractHub() {
    class Config : AbstractHub.Config() {
        var walletConfig: JSONObject? = null
        var walletCredentials: JSONObject? = null
        var mediatorInvitation: Invitation? = null
        var indyEndpoint: String? = null
        var sender: BaseSender? = null
    }


    override fun getAgenti(): MobileAgent? {
        return super.agent as MobileAgent?
    }

    override fun createAgentInstance() {
        val mobileConfig = config as Config
        agent = MobileAgent(
            mobileConfig.walletConfig,
            mobileConfig.walletCredentials
        )
        val points: MutableList<Endpoint> = ArrayList<Endpoint>()
        if ((config as Config).indyEndpoint != null && !(config as Config).indyEndpoint!!.isEmpty()) {
            points.add(Endpoint((config as Config).indyEndpoint!!, listOf(), true))
        }
        getAgenti()?.endpoints = points
        getAgenti()?.sender = (config as Config).sender
        getAgenti()?.create()
        getAgenti()?.open()
    }

    init {
        this.config = config
        createAgentInstance()
    }
}

