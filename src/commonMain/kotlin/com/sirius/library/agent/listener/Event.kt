package com.sirius.library.agent.listener

import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageUtil
import com.sirius.library.utils.JSONObject

class Event(pairwise: Pairwise?, message: String) : Message(message) {
    var pairwise: Pairwise?
    fun message(): Message? {
        if (getMessageObjec()?.has("message") ==true) {
            val msgJson: JSONObject? = getMessageObjec()?.getJSONObject("message")
            var restored: Message? = MessageUtil.restoreMessageInstance(msgJson.toString())?.second
            return restored
        }
        return null
    }

    val recipientVerkey: String?
        get() = getMessageObjec()?.optString("recipient_verkey")
    val senderVerkey: String?
        get() = getMessageObjec()?.optString("sender_verkey")

    fun getPairwisei(): Pairwise? {
        return pairwise
    }

    init {
        this.pairwise = pairwise
    }
}