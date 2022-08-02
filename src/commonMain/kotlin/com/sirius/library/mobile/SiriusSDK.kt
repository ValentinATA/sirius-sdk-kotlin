package com.sirius.library.mobile


import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.BaseSender
import com.sirius.library.agent.MobileContextConnection
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.hub.MobileContext
import com.sirius.library.messaging.MessageFabric
import com.sirius.library.mobile.helpers.*
import com.sirius.library.utils.JSONObject



class SiriusSDK {

    companion object {
        private var instanceSDK: SiriusSDK? = null

        fun getInstance(): SiriusSDK {
            if (instanceSDK == null) {
                instanceSDK = SiriusSDK()
            }
            return instanceSDK!!
        }

        fun cleanInstance(){
            PairwiseHelper.cleanInstance()
            ScenarioHelper.cleanInstance()
            WalletHelper.cleanInstance()
            ChanelHelper.cleanInstance()
            InvitationHelper.cleanInstance()
            instanceSDK = null
        }
    }




    val walletHelper = WalletHelper.getInstance();
    var label: String? = null

    lateinit var context: MobileContext


    private fun createContext(
        indyEndpoint: String,
        serverUri: String,
        config: String,
        credential: String,
        baseSender: BaseSender
    ) {

        context = MobileContext.builder().setIndyEndpoint(indyEndpoint).setServerUri(serverUri)
            .setWalletConfig(JSONObject(config)).setWalletCredentials(JSONObject(credential))
            .setMediatorInvitation(Invitation.builder().setLabel(label).build())
            .setSender(baseSender)
            .build() as MobileContext
    }

    private fun initAllMessages() {
        MessageFabric.registerAllMessagesClass()
       /* object : ClassScanner(mycontext) {
            override fun isTargetClassName(className: String): Boolean {
                return (className.startsWith("com.sirius.sdk.") //I want classes under my package
                        && !className.contains("$") //I don't need none-static inner classes
                        )
            }

            override fun isTargetClass(clazz: Class<*>): Boolean {
                return (Message::class.java.isAssignableFrom(clazz) //I want subclasses of AbsFactory
                        && !Modifier.isAbstract(clazz.modifiers) //I don't want abstract classes
                        )
            }

            override fun onScanResult(clazz: Class<*>) {
                try {
                    Class.forName(clazz.name, true, clazz.classLoader)
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
        }.scan()*/
    }

    fun initialize(
        indyEndpoint: String,
        myHost: String,
        alias: String,
        pass: String,
        mainDirPath: String,
        label: String,
        baseSender: BaseSender
    ) {
        this.label = label
        initAllMessages()
        val config = WalletHelper.getInstance().createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.getInstance().createWalletCredential(pass)
        createContext(indyEndpoint, myHost, config, credential,baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }


    fun initialize(
        alias: String,
        pass: String,
        mainDirPath: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
        label: String, serverUri: String,baseSender: BaseSender
    ) {

        this.label = label
        initAllMessages()
        //   LibIndy.setRuntimeConfig("{\"collect_backtrace\": true }")
        var config = WalletHelper.getInstance().createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.getInstance().createWalletCredential(pass)
        //  Os.setenv("TMPDIR",mainDirPath,true)
//        PoolUtils.createPoolLedgerConfig(networkName, genesisPath)
        //   MobileContext.addPool(networkName, genesisPath)
        createContextWitMediator(config, credential, mediatorAddress, recipientKeys,serverUri, baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }

    suspend fun initializeCorouitine(
        alias: String,
        pass: String,
        mainDirPath: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
        label: String, poolName : String?, serverUri: String,baseSender: BaseSender
    ) {
        LibsodiumInitializer.initialize()
        this.label = label
        initAllMessages()
        //   LibIndy.setRuntimeConfig("{\"collect_backtrace\": true }")
        var config = WalletHelper.getInstance().createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.getInstance().createWalletCredential(pass)
        //  Os.setenv("TMPDIR",mainDirPath,true)
//        PoolUtils.createPoolLedgerConfig(networkName, genesisPath)
        MobileContext.addPool(poolName, mainDirPath + "/"  +"pool_config.txn" )
        createContextWitMediator(config, credential, mediatorAddress, recipientKeys, serverUri, baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }


    private fun createContextWitMediator(
        config: String,
        credential: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
        serverUri: String,
        baseSender: BaseSender
    ) {

        val mediatorLabel = "Mediator"
        context = MobileContext.builder()
            .setWalletConfig(JSONObject(config)).setWalletCredentials(JSONObject(credential))
            .setMediatorInvitation(
                Invitation.builder().setLabel(mediatorLabel)
                    .setEndpoint(mediatorAddress)
                    .setRecipientKeys(recipientKeys).build()
            )
            .setServerUri(serverUri)
            .setSender(baseSender)
            .build() as MobileContext

    }

    fun connectToMediator(firebaseId: String? = null) {
        if(firebaseId.isNullOrEmpty()){
            context.connectToMediator(this.label)
        }else{
            val fcmConnection = MobileContextConnection("FCMService", 1, listOf(), firebaseId)
            context.connectToMediator(this.label, listOf(fcmConnection))
        }
    }

}