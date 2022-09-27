package com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages

import com.sirius.library.agent.aries_rfc.AriesProtocolMessage
import com.sirius.library.agent.aries_rfc.DidDoc
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import kotlin.jvm.JvmOverloads

abstract class ConnProtocolMessage(msg: String) : AriesProtocolMessage(msg) {
    class ExtractTheirInfoRes {
        var did: String? = null
        var verkey: String? = null
        var endpoint: String? = null
        var routingKeys: List<String>? = null
    }

    @Throws(SiriusInvalidMessage::class)
    fun extractTheirInfo(): ExtractTheirInfoRes {
        if (theirDid().isEmpty()) {
            throw SiriusInvalidMessage("Connection metadata is empty")
        }
        if (didDoc() == null) {
            throw SiriusInvalidMessage("DID Doc is empty")
        }
        val service: JSONObject? = didDoc()!!.extractService()
        val theirEndpoint: String? = service?.optString("serviceEndpoint")
        val publicKeys: JSONArray? = didDoc()!!.getPayloadi().getJSONArray("publicKey")
        val theirVk =
            extractKey(service?.getJSONArray("recipientKeys")?.getString(0) ?: "", publicKeys)
        val routingKeys: MutableList<String> = ArrayList<String>()
        if (service?.has("routingKeys") == true) {
            for (rkObj in service!!.getJSONArray("routingKeys")!!) {
                routingKeys.add(extractKey(rkObj as String, publicKeys))
            }
        }
        val res = ExtractTheirInfoRes()
        res.did = theirDid()
        res.verkey = theirVk
        res.endpoint = theirEndpoint
        res.routingKeys = routingKeys
        return res
    }

    private fun extractKey(name: String, publicKeys: JSONArray?): String {
        return if (name.contains("#")) {
            val splitRes = name.split("#").toTypedArray()
            val controller = splitRes[0]
            val id = splitRes[1]
            publicKeys?.let {
                for (keyObj in publicKeys) {
                    val keyJson: JSONObject = keyObj as JSONObject
                    if (keyJson.optString("controller") == controller && keyJson.optString("id") == id) {
                        return keyJson.optString("publicKeyBase58") ?: ""
                    }
                }
            }
            ""
        } else {
            name
        }
    }

    abstract class Builder<B : Builder<B>> protected constructor() :
        AriesProtocolMessage.Builder<B>() {
        override fun generateJSON(): JSONObject {
            return super.generateJSON()
        }


    }

    fun theirDid(): String {
        val obj: JSONObject = getMessageObjec()
        if (obj.has("connection")) {
            if (obj.getJSONObject("connection")?.has("did") == true) {
                return obj.getJSONObject("connection")!!.getString("did")!!
            }
            if (obj.getJSONObject("connection")?.has("DID") == true) {
                return obj.getJSONObject("connection")!!.getString("DID")!!
            }
        }
        return ""
    }

    fun didDoc(): DidDoc? {
        val obj: JSONObject = getMessageObjec()
        if (obj.has("connection")) {
            if (obj.getJSONObject("connection")?.has("did_doc") == true) {
                return DidDoc(obj.getJSONObject("connection")!!.getJSONObject("did_doc")!!)
            }
            if (obj.getJSONObject("connection")?.has("DIDDoc") == true) {
                return DidDoc(obj.getJSONObject("connection")!!.getJSONObject("DIDDoc")!!)
            }
        }
        return null
    }

    companion object {
        const val PROTOCOL = "connections"

        @JvmOverloads
        fun buildDidDoc(
            did: String,
            verkey: String?,
            endpoint: String?,
            extra: JSONObject? = JSONObject()
        ): JSONObject {
            val keyId = "$did#1"
            val doc: JSONObject = JSONObject()
                .put("@context", "https://w3id.org/did/v1").put("id", did).put(
                    "authentication", JSONArray()
                        .put(
                            JSONObject().put("publicKey", keyId)
                                .put("type", "Ed25519SignatureAuthentication2018")
                        )
                ).put(
                    "publicKey", JSONArray()
                        .put(
                            JSONObject()
                                .put("id", "1").put("type", "Ed25519VerificationKey2018")
                                .put("controller", did)
                                .put("publicKeyBase58", verkey)
                        )
                ).put(
                    "service", JSONArray()
                        .put(
                            JSONObject()
                                .put("id", "did:peer:$did;indy").put("type", "IndyAgent")
                                .put("priority", 0).put(
                                    "recipientKeys", JSONArray()
                                        .put(keyId)
                                ).put("serviceEndpoint", endpoint)
                        )
                )
            extra?.let {
                for (key in extra.keySet()) {
                    doc.put(key, extra.get(key))
                }
            }
            return doc
        }
    }
}

