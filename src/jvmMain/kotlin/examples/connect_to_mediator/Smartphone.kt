
package examples.connect_to_mediator

import com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.messages.OfferCredentialMessage
import com.sirius.library.agent.aries_rfc.feature_0036_issue_credential.state_machines.Holder
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.messages.RequestPresentationMessage
import com.sirius.library.agent.aries_rfc.feature_0037_present_proof.state_machines.Prover
import com.sirius.library.agent.aries_rfc.feature_0095_basic_message.Message
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.messages.Invitation
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Invitee
import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.listener.Event
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.errors.indy_exceptions.DuplicateMasterSecretNameException
import com.sirius.library.hub.MobileContext
import com.sirius.library.hub.MobileHub

import com.sirius.library.utils.Logger

class Smartphone {
    var config: MobileHub.Config
    var context: MobileContext? = null
    var me: Pairwise.Me? = null
    var loop = false
    var networkName: String? = null
    var masterSecret = "masterSecret"

    constructor(config: MobileHub.Config, networkName: String?, genesisPath: String?) {
        this.config = config
        this.networkName = networkName
        MobileContext.addPool(networkName, genesisPath)
    }

    constructor(config: MobileHub.Config) {
        this.config = config
    }

    suspend fun start() {
        if (context == null) {
            context = MobileContext(config)
            context!!.connectToMediator("Edge Test agent")
            val (first, second) = context!!.did.createAndStoreMyDid()
            me = Pairwise.Me(first, second)
            //context.addMediatorKey(me.getVerkey());
            loop = true
            routine()
        }
    }

    fun stop() {
        if (context != null) {
            context!!.close()
        }
    }

    suspend fun acceptInvitation(invitation: Invitation?) {
        val invitee = Invitee(context!!, me!!, context!!.endpointWithEmptyRoutingKeys?: Endpoint(""))
        val pw: Pairwise? = invitee.createConnection(invitation!!, "Edge agent")
        if (pw != null) {
            context?.pairwiseList?.ensureExists(pw)
        }
    }

    protected suspend fun routine() {
        try {
            context?.anonCreds?.proverCreateMasterSecret(masterSecret)
        } catch (e: DuplicateMasterSecretNameException) {
            e.printStackTrace()
        }
        val listener: Listener? = context?.subscribe()
        try {
            while (loop) {
                val event: Event? = listener?.one?.get(60)
                if (event?.message() is OfferCredentialMessage && event?.pairwise != null) {
                    val offer: OfferCredentialMessage = event.message() as OfferCredentialMessage
                    val holder = Holder(context!!, event.pairwise!!, masterSecret)
                    val (first, second) = holder.accept(offer)
                } else if (event?.message() is RequestPresentationMessage && event?.pairwise != null) {
                    val request: RequestPresentationMessage = event.message() as RequestPresentationMessage
                    var prover: Prover? = null
                    if (networkName == null) {
                        prover = Prover(context!!, event.pairwise!!, masterSecret)
                    } else {
                        prover = Prover(context!!, event.pairwise!!, masterSecret, networkName)
                    }
                    prover.prove(request)
                } else if (event?.message() is Message && event.pairwise != null) {
                    val message: Message = event.message() as Message
                    Logger.getLogger("Examples").info("Received new message: " + message.content)

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

