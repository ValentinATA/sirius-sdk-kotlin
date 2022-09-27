package com.sirius.library.agent.pairwise

import com.sirius.library.utils.JSONObject

class Pairwise {
    var me: Me
    var their: Their
    var metadata: JSONObject?
    fun getMetadatai(): JSONObject? {
        return metadata
    }

    fun toJSONObject(): JSONObject {
        val jsonObject = JSONObject()
        val meObject = JSONObject()
        meObject.put("did", me.did)
        meObject.put("verkey", me.verkey)
        meObject.put("did_doc", me.didDoc)

        val theirObject = JSONObject()
        theirObject.put("did", their.did)
        theirObject.put("label", their.label)
        theirObject.put("did_doc", their.didDoc)
        theirObject.put("verkey", their.verkey)
        val endpointObject = JSONObject()
        endpointObject.put("address",their.endpointAddress)
        endpointObject.put("routing_keys",their.routingKeys)

        theirObject.put("endpoint", endpointObject)


        jsonObject.put("me", meObject)
        jsonObject.put("their", theirObject)
        jsonObject.put("metadata",metadata)
        return jsonObject
    }

    constructor(me: Me, their: Their, metadata: JSONObject?) {
        this.me = me
        this.their = their
        this.metadata = metadata
    }

    constructor(me: Me, their: Their) {
        this.me = me
        this.their = their
        metadata = null
    }

    class Their : TheirEndpoint {
        var did: String?
        var didDoc: JSONObject? = null
        var label: String?

        constructor(
            did: String?,
            label: String?,
            endpoint: String?,
            verkey: String?,
            routingKeys: List<String>?
        ) : super(endpoint, verkey, routingKeys) {
            this.did = did
            this.label = label
        }

        constructor(did: String, label: String, endpoint: String, verkey: String) : super(
            endpoint,
            verkey
        ) {
            this.did = did
            this.label = label
        }


        fun setDidDoci(didDoc: JSONObject?) {
            this.didDoc = didDoc
        }
    }

    class Me {
        var did: String?
        var verkey: String?
        var didDoc: JSONObject? = null


        fun setDidDoci(didDoc: JSONObject?) {
            this.didDoc = didDoc
        }

        constructor(did: String?, verkey: String?) {
            this.did = did
            this.verkey = verkey
        }

        constructor(did: String?, verkey: String?, didDoc: JSONObject?) {
            this.did = did
            this.verkey = verkey
            this.didDoc = didDoc
        }

        override fun equals(obj: Any?): Boolean {
            if (obj is Me) {
                val o = obj
                return did == o.did && verkey == o.verkey &&
                        (didDoc == null && o.didDoc == null || didDoc != null && o.didDoc != null && didDoc!!.equals(
                            o.didDoc
                        ))
            }
            return false
        }
    }
}
