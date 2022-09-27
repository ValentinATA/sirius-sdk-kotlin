package com.sirius.library.mobile

import com.sirius.library.agent.wallet.LocalWallet
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq

object SearchHelper {

    fun cleanInstance(){
      //  Companion.searchHelper = null
    }




    fun searchForProofRequest(proofRequest:  String, extraQuery : String? = null, wallet: LocalWallet){
       val credentialsSearchForProofReq =
            CredentialsSearchForProofReq.open(
                wallet,
                proofRequest, extraQuery
            ).get()
    }

}