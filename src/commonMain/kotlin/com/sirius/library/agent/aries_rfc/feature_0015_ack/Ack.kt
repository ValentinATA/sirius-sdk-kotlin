package com.sirius.library.agent.aries_rfc.feature_0015_ack

import com.sirius.library.agent.aries_rfc.AriesProtocolMessage
import com.sirius.library.messaging.Message
import com.sirius.library.utils.JSONObject
import kotlin.reflect.KClass

open class Ack(message: String) : AriesProtocolMessage(message) {
    companion object {
        const val PROTOCOL = "notification"
        fun builder(): Builder<*> {
            return AckBuilder()
        }

    }

    fun getStatus(): Status? {
        try {
            val statusName = messageObj.getString("status") ?: ""
            return Status.valueOf(statusName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    enum class Status {
        OK, PENDING, FAIL
    }

    abstract class Builder<B : Builder<B>> : AriesProtocolMessage.Builder<B>() {
        var status: Status? = null
        fun setStatus(status: Status?): B {
            this.status = status
            return self()
        }

        override fun generateJSON(): JSONObject {
            val jsonObject: JSONObject = super.generateJSON()
            val id: String? = jsonObject.optString("id")
            if (status != null) {
                jsonObject.put("status", status!!.name)
            }
            return jsonObject
        }

        open fun build(): Ack {
            return Ack(generateJSON().toString())
        }
    }

    private class AckBuilder : Builder<AckBuilder>() {
        protected override fun self(): AckBuilder {
            return this
        }

        override fun getClass(): KClass<out Message> {
            return Ack::class
        }
    }
}
