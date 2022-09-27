package com.sirius.library.mobile.helpers






import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.WalletPairwiseList
import com.sirius.library.mobile.SiriusSDK
import com.sirius.library.mobile.models.CredentialsRecord
import com.sirius.library.utils.JSONObject
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json


object PairwiseHelper {

    fun cleanInstance(){
        //pairwiseHelper = null
    }




    fun getAllPairwise(): List<Pairwise> {
        val list =
            (SiriusSDK.context?.currentHub?.agent?.wallet?.pairwise?.listPairwise() as? List<String>).orEmpty()
        val mutableList: MutableList<Pairwise> = mutableListOf()
        list.forEach {
            val pairwiseObj = JSONObject(it)
            val metadata = pairwiseObj.optString("metadata")
            metadata?.let { metadataObj ->
                val pairwise = WalletPairwiseList.restorePairwise(JSONObject(metadataObj))
                mutableList.add(pairwise)
            }

        }

        return mutableList
    }


    fun getAllCredentials(): List<CredentialsRecord> {
        val list =
            (SiriusSDK.context?.currentHub?.agent?.wallet?.anoncreds?.proverGetCredentials("{}"))
        return list?.mapNotNull {
           return@mapNotNull Json.decodeFromString<CredentialsRecord>(it?:"")

        }.orEmpty()
    }


    fun getPairwise(theirDid: String? = null, theirVerkey: String? = null): Pairwise? {
        if (!theirDid.isNullOrEmpty()) {
            val pairwise =  SiriusSDK.context?.currentHub?.pairwiseList?.loadForDid(theirDid)
           pairwise?.let {
               return it
           }
        }
        if (!theirVerkey.isNullOrEmpty()) {
            return SiriusSDK.context?.currentHub?.pairwiseList?.loadForVerkey(theirVerkey)
        }
        return null;
    }
}