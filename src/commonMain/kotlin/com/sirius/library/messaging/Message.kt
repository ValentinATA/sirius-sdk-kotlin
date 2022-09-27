package com.sirius.library.messaging

import com.sirius.library.base.JsonSerializable
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidType
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.UUID
import kotlin.native.concurrent.ThreadLocal
import kotlin.reflect.KClass




 object MessageUtil {
    val MSG_REGISTRY: MutableList<Triple<KClass<out Message>, String, String>> =
        ArrayList<Triple<KClass<out Message>, String, String>>()
    val MSG_REGISTRY2: MutableList<Pair<KClass<out Message>, (String) -> Message?>> =
        ArrayList<Pair<KClass<out Message>, (String) -> Message?>>()

    fun generateId(): String {
        return UUID.randomUUID.toString()
    }

    fun registerMessageClass(clas: KClass<out Message>, protocol: String, constructor: (String) -> Message?) {
        registerMessageClass(clas, protocol, "*",constructor)
    }

    fun registerMessageClass(
        clas: KClass<out Message>,
        protocol: String,
        name: String?, constructor: (String) -> Message?
    ) {
        var name = name
        if (name == null) name = "*"

        for (i in MSG_REGISTRY.indices) {
            if (MSG_REGISTRY[i].first.equals(clas)) {
                MSG_REGISTRY.set(i, Triple(clas, protocol, name))
                return
            }
        }
        MSG_REGISTRY.add(Triple(clas, protocol, name))


        registerMessageClass2(clas,constructor)
    }

    fun registerMessageClass2(
        clas: KClass<out Message>,
        constructor: (String) -> Message?
    ) {
        for (i in MSG_REGISTRY2.indices) {
            if (MSG_REGISTRY2[i].first.equals(clas)) {
                MSG_REGISTRY2.set(i, Pair(clas, constructor))
                return
            }
        }
        MSG_REGISTRY2.add(Pair(clas, constructor))

    }

    fun getProtocolAndName(clas: KClass<out Message>): Pair<String?, String?> {
        for (triple in MSG_REGISTRY) {
            if (triple.first == clas) {
                return Pair(triple.second, triple.third)
            }
        }
        return Pair(null, null)
    }

    fun restoreMessageInstance(payload: String?): Pair<Boolean, Message?> {
        if(payload==null){
            return Pair(false, null)
        }
        val message: Message = Message(payload)
        val protocol: String? = message.typeOfType?.protocol
        val name: String? = message.typeOfType?.name
        var clssTo: KClass<out Message>? = null
        for (triple  in MSG_REGISTRY) {
            if (triple.second.equals(protocol) && (triple.third.equals(name) || triple.third.equals("*"))) {
                clssTo = triple.first
            }
        }
        if (clssTo != null) {
            return Pair(true, MessageFabric.restoreMessageInstance(clssTo,payload))
        }
        return Pair(false, null)
    }
}

open class Message : JsonSerializable<Message> {
    val FIELD_TYPE = "@type"
    val FIELD_ID = "@id"

    constructor() {

    }


    constructor(message: String) {
        init(message)
    }

    constructor(message: JSONObject) {
        init(message.toString())
    }


    fun getType(): String? {
        return messageObj.optString(FIELD_TYPE)
    }

    fun getId(): String? {
        return messageObj.optString(FIELD_ID)
    }

    fun setId(id: String?) {
        this.messageObj.put(FIELD_ID, id)
    }

    fun getMessageObjec(): JSONObject {
        return messageObj
    }

    lateinit var messageObj: JSONObject
    var typeOfType: Type? = null

    fun getVersion(): String? {
        return this.typeOfType?.version
    }

    fun getDocUri(): String? {
        return typeOfType!!.docUri
    }

/*
    fun Message(message: String?) {
        init(message)
    }

    fun Message(message: JSONObject) {
        init(message.toString())
    }
*/

    open fun init(message: String) {
        messageObj = JSONObject(message)
        if (!messageObjectHasKey(FIELD_TYPE)) {
            SiriusInvalidMessage("No @type in message").printStackTrace()
        }
        try {
            typeOfType = Type.fromStr(messageObj!!.optString(FIELD_TYPE)?:"")
        } catch (siriusInvalidType: SiriusInvalidType) {
            siriusInvalidType.printStackTrace()
        }
        if (!messageObj!!.has(FIELD_ID)) {
            messageObj!!.put(FIELD_ID, generateId())
        }
    }

    fun getObjectFromJSON(key: String): Any? {
        return if (messageObjectHasKey(key)) {
            if (messageObj!!.isNull(key)) {
                null
            } else messageObj!!.get(key)
        } else null
    }

    fun getStringFromJSON(key: String): String? {
        if (messageObjectHasKey(key)) {
            val value: String? = messageObj?.optString(key)
            return if (value == null || value.isEmpty()) {
                ""
            } else value
        }
        return ""
    }

    fun getBooleanFromJSON(key: String): Boolean? {
        return if (messageObjectHasKey(key)) {
            messageObj.getBoolean(key)
        } else null
    }

    fun messageObjectHasKey(key: String): Boolean {
        return messageObj.has(key) ?: false
    }

    fun getJSONOBJECTFromJSON(key: String): JSONObject? {
        return if (messageObjectHasKey(key)) {
            messageObj.optJSONObject(key)
        } else null
    }

    fun getJSONOBJECTFromJSON(key: String, defaultValue: JSONObject?): JSONObject? {
        return if (messageObjectHasKey(key)) {
            messageObj.optJSONObject(key)
        } else defaultValue
    }

    fun getJSONOBJECTFromJSON(key: String, defaultValue: String?): JSONObject? {
        return if (messageObjectHasKey(key)) {
            messageObj.optJSONObject(key)
        } else JSONObject(defaultValue)
    }

    fun getJSONArrayFromJSON(key: String, defaultValue: JSONArray?): JSONArray? {
        return if (messageObjectHasKey(key)) {
            messageObj.getJSONArray(key) ?: return defaultValue
        } else defaultValue
    }

    fun generateId(): String {
        return UUID.randomUUID.toString()
    }

    override fun serialize(): String {
        return messageObj.toString()
    }

    override fun serializeToJSONObject(): JSONObject {
        return JSONObject(messageObj.toString())
    }

    override fun deserialize(string: String): Message {
        return Message(string ?: "")
    }






}





