package com.sirius.library.hub

import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.ledger.Ledger
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.microledgers.AbstractMicroledger
import com.sirius.library.agent.microledgers.AbstractMicroledgerList
import com.sirius.library.agent.microledgers.Transaction
import com.sirius.library.agent.pairwise.AbstractPairwiseList
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.wallet.abstract_wallet.*
import com.sirius.library.agent.wallet.abstract_wallet.model.AnonCredSchema
import com.sirius.library.agent.wallet.abstract_wallet.model.CacheOptions
import com.sirius.library.agent.wallet.abstract_wallet.model.PurgeOptions
import com.sirius.library.agent.wallet.abstract_wallet.model.RetrieveRecordOptions
import com.sirius.library.errors.indy_exceptions.DuplicateMasterSecretNameException
import com.sirius.library.errors.indy_exceptions.WalletItemNotFoundException
import com.sirius.library.messaging.Message
import com.sirius.library.messaging.MessageFabric
import com.sirius.library.utils.JSONObject

abstract class Context<T: AbstractHub> internal constructor(hub: T) : Closeable {
    companion object {
        // loading all Message classes to force their registration in static block
        init {
            MessageFabric.registerAllMessagesClass()
        }
    }

    open lateinit var currentHub: T
    var nonSecrets: AbstractNonSecrets = object : AbstractNonSecrets() {
        override fun addWalletRecord(type: String?, id: String?, value: String?, tags: String?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.addWalletRecord(type, id, value, tags)
        }

        override fun updateWalletRecordValue(type: String?, id: String?, value: String?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.updateWalletRecordValue(type, id, value)
        }

        override fun updateWalletRecordTags(type: String?, id: String?, tags: String?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.updateWalletRecordTags(type, id, tags)
        }

        override fun addWalletRecordTags(type: String?, id: String?, tags: String?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.addWalletRecordTags(type, id, tags)
        }

        override fun deleteWalletRecord(type: String?, id: String?, tagNames: List<String?>?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.deleteWalletRecord(type, id, tagNames)
        }

        override fun deleteWalletRecord(type: String?, id: String?) {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            service?.deleteWalletRecord(type, id)
        }

        override fun getWalletRecord(type: String?, id: String?, options: RetrieveRecordOptions?): String? {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            return service?.getWalletRecord(type, id, options)
        }

        override fun walletSearch(
            type: String?,
            query: String?,
            options: RetrieveRecordOptions?,
            limit: Int
        ): Pair<List<String>, Int> {
            val service: AbstractNonSecrets? = currentHub.nonSecrets
            return service?.walletSearch(type, query, options, limit) ?: Pair(listOf(),0)
        }
    }
    var crypto: AbstractCrypto = object : AbstractCrypto() {
        override fun createKey(seed: String?, cryptoType: String?): String? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.createKey(seed, cryptoType)
        }

        override fun setKeyMetadata(verkey: String?, metadata: String?) {
            val service: AbstractCrypto? = currentHub.crypto
            service?.setKeyMetadata(verkey, metadata)
        }

        override fun getKeyMetadata(verkey: String?): String? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.getKeyMetadata(verkey)
        }

        override fun cryptoSign(signerVk: String?, msg: ByteArray?): ByteArray? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.cryptoSign(signerVk, msg)
        }

        override fun cryptoVerify(signerVk: String?, msg: ByteArray?, signature: ByteArray?): Boolean {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.cryptoVerify(signerVk, msg, signature) ?: false
        }

        override fun anonCrypt(recipentVk: String?, msg: ByteArray?): ByteArray? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.anonDecrypt(recipentVk, msg)
        }

        override fun anonDecrypt(recipientVk: String?, encryptedMsg: ByteArray?): ByteArray? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.anonDecrypt(recipientVk, encryptedMsg)
        }

        override fun packMessage(message: Any?, recipentVerkeys: List<String>?, senderVerkey: String?): ByteArray? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.packMessage(message, recipentVerkeys, senderVerkey)
        }


        override fun unpackMessage(jwe: ByteArray?): String? {
            val service: AbstractCrypto? = currentHub.crypto
            return service?.unpackMessage(jwe)
        }
    }
    var did: AbstractDID = object : AbstractDID() {
        override fun createAndStoreMyDid(did: String?, seed: String?, cid: Boolean?): Pair<String, String> {
            val service: AbstractDID? = currentHub.did
            return service?.createAndStoreMyDid(did, seed, cid) ?: Pair("","")
        }

        override fun storeTheirDid(did: String?, verkey: String?) {
            val service: AbstractDID? = currentHub.did
            service?.storeTheirDid(did, verkey)
        }

        override fun setDidMetadata(did: String?, metadata: String?) {
            val service: AbstractDID? = currentHub.did
            service?.setDidMetadata(did, metadata)
        }

        override fun listMyDidsWithMeta(): List<Any?> {
            val service: AbstractDID? = currentHub.did
            return service?.listMyDidsWithMeta() ?: listOf<Any?>()
        }

        override fun getDidMetadata(did: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.getDidMetadata(did)
        }

        override fun keyForLocalDid(did: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.keyForLocalDid(did)
        }

        override fun keyForDid(poolName: String?, did: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.keyForDid(poolName, did)
        }

        override fun createKey(seed: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.createKey(seed)
        }

        override  fun replaceKeysStart(did: String?, seed: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.replaceKeysStart(did, seed)
        }

        override fun replaceKeysApply(did: String?) {
            val service: AbstractDID? = currentHub.did
            service?.replaceKeysStart(did)
        }

        override fun setKeyMetadata(verkey: String?, metadata: String?) {
            val service: AbstractDID? = currentHub.did
            service?.setKeyMetadata(verkey, metadata)
        }

        override fun getKeyMetadata(verkey: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.getKeyMetadata(verkey)
        }

        override fun setEndpointForDid(did: String?, address: String?, transportKey: String?) {
            val service: AbstractDID? = currentHub.did
            service?.setEndpointForDid(did, address, transportKey)
        }

        override fun getEndpointForDid(pooName: String?, did: String?): Pair<String?, String?>? {
            val service: AbstractDID? = currentHub.did
            return service?.getEndpointForDid(pooName, did)
        }

        override fun getMyDidMeta(did: String?): Any? {
            val service: AbstractDID? = currentHub.did
            return service?.getMyDidMeta(did)
        }

        override fun abbreviateVerKey(did: String?, fullVerkey: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.abbreviateVerKey(did, fullVerkey)
        }

        override fun qualifyDid(did: String?, method: String?): String? {
            val service: AbstractDID? = currentHub.did
            return service?.qualifyDid(did, method)
        }
    }
    var pairwiseList: AbstractPairwiseList = object : AbstractPairwiseList() {

        override fun create(pairwise: Pairwise) {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            service?.create(pairwise)
        }

        override fun update(pairwise: Pairwise) {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            service?.update(pairwise)
        }

        override fun isExists(theirDid: String): Boolean {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            return service?.isExists(theirDid) ?: false
        }

        override fun ensureExists(pairwise: Pairwise) {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            service?.ensureExists(pairwise)
        }

        override fun loadForDid(theirDid: String): Pairwise? {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            return service?.loadForDid(theirDid)
        }

        override fun loadForVerkey(theirVerkey: String): Pairwise? {
            val service: AbstractPairwiseList? = currentHub.pairwiseList
            return service?.loadForVerkey(theirVerkey)
        }
    }
    var anonCreds: AbstractAnonCreds = object : AbstractAnonCreds() {
        override fun issuerCreateSchema(
            issuerDid: String?,
            name: String?,
            version: String?,
            attrs: List<String?>?
        ): Pair<String?, AnonCredSchema?> {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerCreateSchema(issuerDid, name, version, attrs) ?:  Pair(null, null)
        }

        override fun issuerCreateAndStoreCredentialDef(
            issuerDid: String?,
            schema: Any?,
            tag: String?,
            signatureType: String?,
            config: Any?
        ): Pair<String?, String?> {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerCreateAndStoreCredentialDef(issuerDid, schema, tag, signatureType, config) ?: Pair(null,null)
        }

        override fun issuerRotateCredentialDefStart(credDefId: String?, config: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerRotateCredentialDefStart(credDefId, config)
        }

        override fun issuerRotateCredentialDefApply(credDefId: String?) {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            service?.issuerRotateCredentialDefApply(credDefId)
        }

        override fun issuerCreateAndStoreRevocReg(
            issuerDid: String?,
            revocDefType: String?,
            tag: String?,
            credDefId: String?,
            config: String?,
            tailsWriterHandle: Int
        ): Triple<String?, String?, String?> {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerCreateAndStoreRevocReg(
                issuerDid,
                revocDefType,
                tag,
                credDefId,
                config,
                tailsWriterHandle
            ) ?: Triple(null,null,null)
        }

        override fun issuerCreateCredentialOffer(credDefId: String?): JSONObject? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerCreateCredentialOffer(credDefId)
        }

        override  fun issuerCreateCredential(
            credOffer: JSONObject?,
            credReq: JSONObject?,
            credValues: JSONObject?,
            revRegId: String?,
            blobStorageReaderHandle: Int?
        ): Triple<JSONObject, String?, JSONObject?> {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerCreateCredential(credOffer, credReq, credValues, revRegId, blobStorageReaderHandle) ?: Triple(
                JSONObject(),null,null
            )
        }

        override fun issuerRevokeCredential(blobStorageReaderHandle: Int?, revRegId: String?, credRevocId: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerRevokeCredential(blobStorageReaderHandle, revRegId, credRevocId)
        }

        override fun issuerMergeRevocationRegistryDeltas(revRegDelta: String?, otherRevRegDelta: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.issuerMergeRevocationRegistryDeltas(revRegDelta, otherRevRegDelta)
        }

        @Throws(DuplicateMasterSecretNameException::class)
        override fun proverCreateMasterSecret(masterSecretName: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverCreateMasterSecret(masterSecretName)
        }

        override  fun proverCreateCredentialReq(
            proverDid: String?,
            credOffer: JSONObject?,
            credDef: JSONObject?,
            masterSecretId: String?
        ): Pair<JSONObject?, JSONObject?>? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverCreateCredentialReq(proverDid, credOffer, credDef, masterSecretId)
        }

        override fun proverSetCredentialAttrTagPolicy(credDefId: String?, taAttrs: String?, retroactive: Boolean) {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            service?.proverSetCredentialAttrTagPolicy(credDefId, taAttrs, retroactive)
        }

        override   fun proverGetCredentialAttrTagPolicy(credDefId: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverGetCredentialAttrTagPolicy(credDefId)
        }

        override fun proverStoreCredential(
            credId: String?,
            credReqMetadata: JSONObject?,
            cred: JSONObject?,
            credDef: JSONObject?,
            revReqDef: String?
        ): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverStoreCredential(credId, credReqMetadata, cred, credDef, revReqDef)
        }

        @Throws(WalletItemNotFoundException::class)
        override fun proverGetCredential(credDefId: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverGetCredential(credDefId)
        }

        override  fun proverDeleteCredential(credId: String?) {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            service?.proverDeleteCredential(credId)
        }

        override fun proverGetCredentials(filters: String?): List<String?>? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverGetCredentials(filters)
        }

        override fun proverSearchCredential(query: String?): List<String?>? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverSearchCredential(query)
        }

        override  fun proverGetCredentialsForProofReq(proofRequest: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverGetCredentialsForProofReq(proofRequest)
        }

        override fun proverSearchCredentialsForProofReq(
            proofRequest: JSONObject?,
            extraQuery: String?,
            limitReferents: Int
        ): JSONObject? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverSearchCredentialsForProofReq(proofRequest, extraQuery, limitReferents)
        }

        override fun proverCreateProof(
            proofReq: JSONObject?,
            requestedCredentials: JSONObject?,
            masterSecretName: String?,
            schemas: JSONObject?,
            credentialDefs: JSONObject?,
            revStates: JSONObject?
        ): JSONObject? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.proverCreateProof(
                proofReq,
                requestedCredentials,
                masterSecretName,
                schemas,
                credentialDefs,
                revStates
            )
        }

        override fun verifierVerifyProof(
            proofRequest: JSONObject?,
            proof: JSONObject?,
            schemas: JSONObject?,
            credentialDefs: JSONObject?,
            revRegDefs: JSONObject?,
            revRegs: JSONObject?
        ): Boolean {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.verifierVerifyProof(proofRequest, proof, schemas, credentialDefs, revRegDefs, revRegs) ?: false
        }

        override fun createRevocation(
            blobStorageReaderHandle: Int,
            revRegDef: String?,
            revRegDelta: String?,
            timestamp: Int,
            credRevId: String?
        ): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.createRevocation(blobStorageReaderHandle, revRegDef, revRegDelta, timestamp, credRevId)
        }

        override fun updateRevocationState(
            blobStorageReaderHandle: Int,
            revState: String?,
            revRegDef: String?,
            revRegDelta: String?,
            timestamp: Int,
            credRevId: String?
        ): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.updateRevocationState(
                blobStorageReaderHandle,
                revState,
                revRegDef,
                revRegDelta,
                timestamp,
                credRevId
            )
        }

        override fun generateNonce(): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.generateNonce()
        }

        override fun toUnqualified(entity: String?): String? {
            val service: AbstractAnonCreds? = currentHub.anonCreds
            return service?.toUnqualified(entity)
        }
    }
    var cache: AbstractCache = object : AbstractCache() {
        override fun getSchema(poolName: String?, submitter_did: String?, id: String?, options: CacheOptions?): String? {
            val service: AbstractCache? = currentHub.cache
            return service?.getSchema(poolName, submitter_did, id, options)
        }

        override fun getCredDef(poolName: String?, submitter_did: String?, id: String?, options: CacheOptions?): String? {
            val service: AbstractCache? = currentHub.cache
            return service?.getCredDef(poolName, submitter_did, id, options)
        }

        override fun purgeSchemaCache(options: PurgeOptions?) {
            val service: AbstractCache? = currentHub.cache
            service?.purgeSchemaCache(options)
        }

        override fun purgeCredDefCache(options: PurgeOptions?) {
            val service: AbstractCache? = currentHub.cache
            service?.purgeCredDefCache(options)
        }
    }
    var microlegders: AbstractMicroledgerList = object : AbstractMicroledgerList() {
        override fun create(name: String?, genesis: List<Transaction?>?):  Pair<AbstractMicroledger?, List<Transaction?>?>? {
            val service: AbstractMicroledgerList? = currentHub.microledgers
            return service?.create(name, genesis)
        }

        override fun getLedger(name: String?): AbstractMicroledger? {
            val service: AbstractMicroledgerList? = currentHub.microledgers
            return service?.getLedger(name)
        }

        override fun reset(name: String?) {
            val service: AbstractMicroledgerList? = currentHub.microledgers
            service?.reset(name)
        }

        override fun isExists(name: String?): Boolean {
            val service: AbstractMicroledgerList? = currentHub.microledgers
            return service?.isExists(name) ?: false
        }

        override fun leafHash(txn: Transaction?): ByteArray? {
            val service: AbstractMicroledgerList? = currentHub.microledgers
            return service?.leafHash(txn)
        }

        override val list: List<Any?>?
            get() {
                val service: AbstractMicroledgerList? = currentHub.microledgers
                return service?.list
            }
    }



    val endpoints: MutableList<Endpoint>?
        get() = currentHub?.agentConnectionLazy?.getEndpointsi()



    fun getDidi(): AbstractDID {
        return did
    }

    fun getPairwiseListi(): AbstractPairwiseList {
        return pairwiseList
    }

    fun getAnonCredsi(): AbstractAnonCreds {
        return anonCreds
    }

    fun getCaches(): AbstractCache {
        return cache
    }

    val ledgers: Map<String, Ledger>?
        get() = currentHub.agentConnectionLazy?.getLedgersi()

    fun getMicrolegdersi(): AbstractMicroledgerList {
        return microlegders
    }

    fun generateQrCode(value: String?): String? {
        return currentHub.agentConnectionLazy?.generateQrCode(value)
    }

    //    public boolean ping() {
    //        return getCurrentHub().getAgentConnectionLazy().ping();
    //    }
    val endpointWithEmptyRoutingKeys: Endpoint?
        get() {
            for (e in endpoints.orEmpty()) {
                if (e.routingKeys.size === 0) {
                    return e
                }
            }
            return null
        }

    val endpointAddressWithEmptyRoutingKeys: String
        get() {
            val e: Endpoint? = endpointWithEmptyRoutingKeys
            return if (e != null) e.address else ""
        }

    fun subscribe(): Listener? {
        return currentHub.agentConnectionLazy?.subscribe()
    }

    fun sendTo(message: Message, to: Pairwise) {
        currentHub.agentConnectionLazy?.sendTo(message, to)
    }

    open fun sendMessage(
        message: Message?, theirVk: List<String?>?,
        endpoint: String?, myVk: String?, routingKeys: List<String?>?
    ) {
        endpoint?.let {
            currentHub.agentConnectionLazy?.sendMessage(message, theirVk, endpoint, myVk, routingKeys)
        }
    }

    fun acquire(
        resources: List<String?>?,
        lockTimeoutSec: Int?,
        enterTimeoutSec: Int?
    ): Pair<Boolean, List<String>> {
        return currentHub.agentConnectionLazy?.acquire(resources, lockTimeoutSec, enterTimeoutSec) ?: Pair(false, listOf())
    }

    fun release() {
        currentHub.agentConnectionLazy?.release()
    }

    override fun close() {
        currentHub.close()
    }

    init {
        currentHub = hub
    }
}
