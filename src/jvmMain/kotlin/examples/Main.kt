package examples

import com.ionspin.kotlin.crypto.LibsodiumInitializer
import com.ionspin.kotlin.crypto.util.LibsodiumUtil
import com.sirius.library.agent.aries_rfc.feature_0095_basic_message.Message
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.ConnRequest
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.wallet.abstract_wallet.model.RetrieveRecordOptions
import com.sirius.library.encryption.P2PConnection
import com.sirius.library.hub.CloudContext
import com.sirius.library.hub.CloudHub
import com.sirius.library.hub.Context
import com.sirius.library.messaging.MessageFabric
import com.sirius.library.utils.Date
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.jvm.JvmStatic

object Main {
    lateinit var context: Context<CloudHub>
    fun qrCode(): Pair<String, String> {
        val namespace = "samples"
        val storeId = "qr"
        val opts = RetrieveRecordOptions()
        // Сохраняем инфо... о QR в самом Wallet чтобы не генерировать ключ при каждом запуске samples
        var retStr: String? = null
        try {
            retStr = context?.nonSecrets?.getWalletRecord(namespace, storeId, opts)
        } catch (ignored: Exception) {
        }
        return if (retStr != null) {
            val ret: JSONObject = JSONObject(retStr)
            val vals: JSONArray = JSONArray(ret.optString("value"))
            val connectionKey: String? = vals.getString(0)
            val qrContent: String? = vals.getString(1)
            val qrUrl: String? = vals.getString(2)
            Pair(connectionKey ?: "", qrUrl ?: "")
        } else { // WalletItemNotFound
            // Ключ установки соединения. Аналог Bob Pre-key
            //см. [2.4. Keys] https://signal.org/docs/specifications/x3dh/
            val connectionKey: String? = context?.crypto?.createKey()
            // Теперь сформируем приглашение для других через 0160
            // шаг 1 - определимся какой endpoint мы возьмем, для простоты возьмем endpoint без доп шифрования
            val endpoints: List<Endpoint> = context?.endpoints ?: listOf()
            var myEndpoint: Endpoint? = null
            for (e in endpoints) {
                if (e.routingKeys.isEmpty()) {
                    myEndpoint = e
                    break
                }
            }
            if (myEndpoint == null) return Pair("", "")
            // шаг 2 - создаем приглашение
            val invitation: Invitation =
                Invitation.builder().setLabel("0160 Sample J")
                    .setRecipientKeys(listOfNotNull(connectionKey))
                    .setEndpoint(myEndpoint.address).build()

            // шаг 3 - согласно Aries-0160 генерируем URL
            val qrContent: String? = invitation.invitationUrl()

            // шаг 4 - создаем QR
            val qrUrl: String? = context?.generateQrCode(qrContent)
            // Кладем в Wallet для повторного использования
            val dump: JSONArray = JSONArray()
            dump.put(connectionKey ?: "").put(qrContent ?: "").put(qrUrl ?: "")
            context?.nonSecrets?.addWalletRecord(namespace, storeId, dump.toString())
            Pair(connectionKey ?: "", qrUrl ?: "")
        }
    }


    @JvmStatic
    fun main(args: Array<String>) {
        MessageFabric.registerAllMessagesClass()
        LibsodiumInitializer.initializeWithCallback {


            val config: CloudHub.Config = CloudHub.Config()
            config.serverUri = "https://demo.socialsirius.com"
            config.credentials =
                "ez8ucxfrTiV1hPX99MHt/JZL1h63sUO9saQCgn2BsaC2EndwDSYpOo6eFpn8xP8ZDoj5B5KN4aaLiyzTqkrbDxrbAe/+2uObPTl6xZdXMBs=".encodeToByteArray()
            config.p2p = P2PConnection(
                "B1n1Hwj1USs7z6FAttHCJcqhg7ARe7xtcyfHJCdXoMnC",
                "y7fwmKxfatm6SLN6sqy6LFFjKufgzSsmqA2D4WZz55Y8W7JFeA3LvmicC36E8rdHoAiFhZgSf4fuKmimk9QyBec",
                "5NUzoX1YNm5VXsgzudvVikN7VQpRf5rhaTnPxyu12eZC"
            )
            context = CloudContext(config)
            val qrCodeRes = qrCode()
            val connectionKey = qrCodeRes!!.first
            val qrUrl = qrCodeRes.second
            println("Открой QR код и просканируй в Sirius App: $qrUrl")
            // Формируем DID - свой идентификатор в контексте relationship и VERKEY - открытый ключ
            val (myDid, myVerkey) = context.did.createAndStoreMyDid(
                null,
                "000000000000000000000000000MISHA"
            )
            println("DID: $myDid")
            println("Verkey: $myVerkey")
            // определимся какой endpoint мы возьмем, для простоты возьмем endpoint без доп шифрования
            val endpoints: List<Endpoint> = context.endpoints ?: listOf()
            var myEndpoint: Endpoint? = null
            for (e in endpoints) {
                if (e.routingKeys.isEmpty()) {
                    myEndpoint = e
                    break
                }
            }
            if (myEndpoint != null) {


                GlobalScope.launch {


                    // Слушаем запросы
                    println("Слушаем запросы")
                    val listener: Listener? = context.subscribe()
                    val event: Event? = listener?.one?.get(60)
                    println("Получено событие")
                    // В рамках Samples интересны только запросы 0160 на установку соединения для connection_key нашего QR
                    if (event?.recipientVerkey.equals(connectionKey) && event?.message() is ConnRequest) {
                        val request: ConnRequest = event!!.message() as ConnRequest
                        // Establish connection with Sirius Communicator via standard Aries protocol
                        // https://github.com/hyperledger/aries-rfcs/blob/master/features/0160-connection-protocol/README.md#states
                        val sm = Inviter(
                            context,
                            Pairwise.Me(myDid, myVerkey),
                            connectionKey,
                            myEndpoint
                        )
                        val p2p: Pairwise? = sm.createConnection(request)
                        if (p2p != null) {
                            // Ensure pairwise is stored
                            context.pairwiseList.ensureExists(p2p)
                            val hello: Message =
                                Message.builder()
                                    .setContent("Привет в новый МИР!!!" + Date().toString())
                                    .setLocale("ru")
                                    .build()
                            println("Sending hello")
                            context.sendTo(hello, p2p)
                            println("sended")
                        }
                    }
                }
            }
        }
    }

}
