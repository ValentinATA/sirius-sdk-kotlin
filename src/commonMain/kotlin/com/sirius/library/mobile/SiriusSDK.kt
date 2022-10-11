package com.sirius.library.mobile


import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.sirius.library.agent.BaseSender
import com.sirius.library.agent.MobileContextConnection
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.hub.MobileContext
import com.sirius.library.messaging.MessageFabric
import com.sirius.library.mobile.helpers.*
import com.sirius.library.utils.JSONObject


object SiriusSDK {


    fun cleanInstance(){
        context?.currentHub?.close()
        context = null
        PairwiseHelper.cleanInstance()
        ScenarioHelper.cleanInstance()
        WalletHelper.cleanInstance()
        ChanelHelper.cleanInstance()
        InvitationHelper.cleanInstance()
    }





    val walletHelper = WalletHelper;
    //FixMe: Remove label form here
    var label: String? = null
    var context: MobileContext? = null


    private fun createContext(
        indyEndpoint: String,
        config: String,
        credential: String,
        baseSender: BaseSender
    ) {

        context = MobileContext.builder().setIndyEndpoint(indyEndpoint)
            .setWalletConfig(JSONObject(config)).setWalletCredentials(JSONObject(credential))
            .setMediatorInvitation(Invitation.builder().setLabel(label).build())
            .setSender(baseSender)
            .build()
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
        alias: String,
        pass: String,
        mainDirPath: String,
        label: String,
        baseSender: BaseSender
    ) {
        this.label = label
        initAllMessages()
        val config = WalletHelper.createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.createWalletCredential(pass)
        createContext(indyEndpoint, config, credential,baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }


    fun initialize(
        alias: String,
        pass: String,
        mainDirPath: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
        label: String,baseSender: BaseSender
    ) {

        this.label = label
        initAllMessages()
        //   LibIndy.setRuntimeConfig("{\"collect_backtrace\": true }")
        var config = WalletHelper.createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.createWalletCredential(pass)
        //  Os.setenv("TMPDIR",mainDirPath,true)
//        PoolUtils.createPoolLedgerConfig(networkName, genesisPath)
        //   MobileContext.addPool(networkName, genesisPath)
        createContextWitMediator(config, credential, mediatorAddress, recipientKeys, baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }





    suspend fun initializeCorouitine(
        alias: String,
        pass: String,
        mainDirPath: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
        label: String, poolName : String?,baseSender: BaseSender
    ) {

        if(!LibsodiumInitializer.isInitialized()){
            LibsodiumInitializer.initialize()
        }
        this.label = label
        initAllMessages()
        //   LibIndy.setRuntimeConfig("{\"collect_backtrace\": true }")
        var config = WalletHelper.createWalletConfig(alias, mainDirPath)
        val credential = WalletHelper.createWalletCredential(pass)
        //  Os.setenv("TMPDIR",mainDirPath,true)
//        PoolUtils.createPoolLedgerConfig(networkName, genesisPath)
        MobileContext.addPool(poolName, mainDirPath + "/"  +"pool_config.txn" )
        createContextWitMediator(config, credential, mediatorAddress, recipientKeys, baseSender)
        walletHelper.context = context
        walletHelper.setDirsPath(mainDirPath)
    }


    private fun createContextWitMediator(
        config: String,
        credential: String,
        mediatorAddress: String,
        recipientKeys: List<String>,
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
            .setSender(baseSender)
            .build()

    }

    suspend fun connectToMediator(firebaseId: String? = null) {
        if(firebaseId.isNullOrEmpty()){
            context?.connectToMediator(this.label)
        }else{
            val fcmConnection = MobileContextConnection("FCMService", 1, listOf(), firebaseId)
            context?.connectToMediator(this.label, listOf(fcmConnection))
        }
    }

}