package com.sirius.library.agent.aries_rfc

import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageUtil
import com.sirius.library.messaging.Type
import com.sirius.library.utils.JSONObject
import kotlin.reflect.KClass

abstract class AriesProtocolMessage : Message {


    val THREAD_DECORATOR = "~thread"

    constructor() : super("{}")

    constructor(message: String) : super(message)



    @Throws(SiriusValidationError::class)
    open  fun validate() {
    }

    open fun getAckMessageId(): String? {
        val pleaseAck: JSONObject? = getJSONOBJECTFromJSON("~please_ack", "{}")
        return if (pleaseAck?.has("message_id")==true) {
            pleaseAck.optString("message_id")
        } else this.getId()
    }

    open fun hasPleaseAck(): Boolean {
        return getMessageObjec()?.has("~please_ack") ?: false
    }

    open fun setPleaseAck(flag: Boolean) {
        if (flag) {
            val pleaseAck = JSONObject()
            pleaseAck.put("message_id", this.getId())
            getMessageObjec()?.put("~please_ack", pleaseAck)
        } else {
            getMessageObjec()?.remove("~please_ack")
        }
    }

    open fun getThreadId(): String? {
        return if (getMessageObjec()?.has(THREAD_DECORATOR) == true && getMessageObjec()?.optJSONObject(THREAD_DECORATOR)?.has("thid") ==true
        ) {
            getMessageObjec()?.optJSONObject(THREAD_DECORATOR)?.optString("thid")
        } else null
    }

    open fun setThreadId(thid: String?) {
        val thread: JSONObject?
        if (getMessageObjec()?.has(THREAD_DECORATOR) == true) {
            thread = getMessageObjec()?.optJSONObject(THREAD_DECORATOR)
        } else {
            thread = JSONObject()
        }
        thread?.put("thid", thid)
        getMessageObjec()?.put(THREAD_DECORATOR, thread)
    }

    abstract class Builder<B : Builder<B>> protected constructor() {
        var version = DEF_VERSION
        var docUri = ARIES_DOC_URI
        var id: String? = null
        abstract fun getClass() : KClass<out Message>
        fun setVersion(version: String): B {
            this.version = version
            return self()
        }

        fun setDocUri(docUri: String): B {
            this.docUri = docUri
            return self()
        }

        fun setId(id: String?): B {
            this.id = id
            return self()
        }


        protected abstract fun self(): B
        open fun generateJSON(): JSONObject {
            val jsonObject = JSONObject()
            val (first, second) = MessageUtil.getProtocolAndName(getClass())
            println("generateJSON()="+"docUri="+docUri+" first="+first)
            jsonObject.put("@type", Type(docUri, first?:"", version, second?:"").typeString)
            jsonObject.put("@id", if (id == null) MessageUtil.generateId() else id)
            return jsonObject
        }
    }

    companion object {
        const val DEF_VERSION = "1.0"
        const val ARIES_DOC_URI = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/"
    }
}

