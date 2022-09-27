package com.sirius.library.mobile


import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.wallet.abstract_wallet.AbstractNonSecrets
import com.sirius.library.agent.wallet.abstract_wallet.model.RetrieveRecordOptions
import com.sirius.library.mobile.helpers.PairwiseHelper
import com.sirius.library.utils.Date
import com.sirius.library.utils.DateUtils
import com.sirius.library.utils.JSONObject



class EventWalletStorage(val storage: AbstractNonSecrets) {

    var DEFAULT_FETCH_LIMIT = 1000
    var selectedDb: String = "event"



    companion object {
        private var eventWalletStorage: EventWalletStorage? = null


        fun getInstance(): EventWalletStorage {
            if (eventWalletStorage == null) {
                if (SiriusSDK.context != null) {
                    eventWalletStorage =
                        EventWalletStorage(SiriusSDK.context!!.nonSecrets)
                }
            }
            return eventWalletStorage!!
        }
    }

    fun add(event: Event, key: String, tags: EventTags?) {
        if (event.messageObj.has("message")) {
            val msgJson = event.messageObj.getJSONObject("message")
            if( msgJson?.has("sent_time") == false){
                event.messageObj?.getJSONObject("message")?.put("sent_time",
                    Date().formatTo(DateUtils.PATTERN_ROSTER_STATUS_RESPONSE2))
            }
        }
        val eventObject = serializeEvent(event)
        val eventGet = get(key)
        val tagsString = tags?.serialize()
        if (eventGet == null) {
            storage.addWalletRecord(selectedDb, key, eventObject.toString(), tagsString)
        } else {
            storage.updateWalletRecordValue(selectedDb, key, eventObject.toString())
            storage.updateWalletRecordTags(selectedDb, key, tagsString)
        }
    }


    fun get(key: String?): Event? {
        var event: Event? = null

        val string =
            storage.getWalletRecord(selectedDb, key, RetrieveRecordOptions(false, true, false))
        if (string != null) {
            val jsonObject = JSONObject(string)
            val values = jsonObject.optString("value")
            event = restoreEvent(values)
        }
        return event
    }

    fun delete(key: String?) {
        storage.deleteWalletRecord(selectedDb, key)
    }

    fun serializeEvent(event: Event): JSONObject {
        val messageObject = event.serializeToJSONObject()
        val eventObject = JSONObject()
        val theirVerkey = event.pairwise?.their?.verkey
        val theirDid = event.pairwise?.their?.did
        eventObject.put("message", messageObject)
        eventObject.put("pairwiseVerkey", theirVerkey)
        eventObject.put("pairwiseDid", theirDid)
        return eventObject
    }

    fun restoreEvent(eventString: String?): Event? {
        if (eventString == null) {
            return null
        }
        val eventObject = JSONObject(eventString)
        val message = eventObject.optJSONObject("message")
        val pairwiseVerkey = eventObject.optString("pairwiseVerkey")
        val pairwiseDid = eventObject.optString("pairwiseDid")
        val pairwise = PairwiseHelper.getPairwise(theirDid = pairwiseDid)
        return Event(pairwise, message.toString())
    }


    fun fetch(limit: Int = DEFAULT_FETCH_LIMIT, tags: String? = null): Pair<List<Event>, Int> {
        var searchString = "{}"
        if (!tags.isNullOrEmpty()) {
            searchString = tags
        }
        val result = storage.walletSearch(
            selectedDb, searchString,
            RetrieveRecordOptions(false, true, true), limit
        )
        return if (result.first != null ) {
            val listValue: MutableList<Event> = ArrayList()
            for (i in result.first!!.indices) {
                val `object`: Any = result.first!![i]
                val jsonObject = JSONObject(`object`.toString())
                val values = jsonObject.optString("value")
                val tags = jsonObject.optJSONObject("tags")
                val event = restoreEvent(values)
                event?.messageObj?.put("tags",tags)
                if (event != null) {
                    listValue.add(event)
                }
            }
            Pair(listValue, result.second)
        } else {
            Pair(ArrayList(), result.second)
        }
    }

    /*  override fun selectDb(name: String) {
          selectedDb = name
      }

      override fun add(value: Any?, tags: String?) {

      }

      override fun fetch(tags: String?, limit: Int?): Pair<MutableList<Any>, Int> {

      }*/

}