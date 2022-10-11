package com.sirius.library.agent

import com.sirius.library.agent.connections.Endpoint
import com.sirius.library.agent.ledger.Ledger
import com.sirius.library.agent.listener.Listener
import com.sirius.library.agent.microledgers.AbstractMicroledgerList
import com.sirius.library.agent.microledgers.MicroledgerList
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.agent.pairwise.WalletPairwiseList
import com.sirius.library.agent.wallet.AbstractWallet
import com.sirius.library.messaging.Message
import com.sirius.library.storage.abstract_storage.AbstractImmutableCollection

abstract class AbstractAgent : TransportLayer() {
    var endpoints: MutableList<Endpoint> = ArrayList<Endpoint>()


    var ledgers: MutableMap<String, Ledger> = HashMap<String, Ledger>()
    var pairwiseList: WalletPairwiseList? = null
    var wallet: AbstractWallet? = null
    var microledgers: MicroledgerList? = null
    var storage: AbstractImmutableCollection? = null
    abstract fun open()
    abstract val isOpen: Boolean
    abstract val name: String?

    /**
     * Implementation of basicmessage feature
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/features/0095-basic-message
     *
     * @param message      Message
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/concepts/0020-message-types
     * @param their_vk     Verkey of recipient
     * @param endpoint     Endpoint address of recipient
     * @param my_vk        VerKey of Sender (AuthCrypt mode)
     * See details:
     * - https://github.com/hyperledger/aries-rfcs/tree/master/features/0019-encryption-envelope#authcrypt-mode-vs-anoncrypt-mode
     * @param routing_keys Routing key of recipient
     * @return
     */
    abstract suspend fun sendMessage(
        message: Message?, their_vk: List<String?>?,
        endpoint: String, my_vk: String?, routing_keys: List<String?>?
    ) : Boolean

    suspend fun sendTo(message: Message?, to: Pairwise) : Boolean {
        return sendMessage(
            message,
            listOf(to.their.verkey),
            to.their.endpointAddress ?:"",
            to.me.verkey,
            to.their.routingKeys
        )
    }

    abstract fun close()
    abstract fun checkIsOpen(): Boolean
    abstract fun subscribe(): Listener?
    abstract fun unsubscribe(listener: Listener?)
    abstract fun generateQrCode(value: String?): String?
    fun getWalleti(): AbstractWallet? {
        checkIsOpen()
        return wallet
    }

    fun getEndpointsi(): MutableList<Endpoint> {
        checkIsOpen()
        return endpoints
    }

    fun getLedgersi(): Map<String, Ledger> {
        checkIsOpen()
        return ledgers
    }

    fun getMicroledgersi(): AbstractMicroledgerList? {
        checkIsOpen()
        return microledgers
    }

    fun getPairwiseListi(): WalletPairwiseList? {
        checkIsOpen()
        return pairwiseList
    }

    /**
     * Acquire N resources given by names
     * @param resources names of resources that you are going to lock
     * @param lockTimeoutSec max timeout resources will be locked. Resources will be automatically unlocked on expire
     * @param enterTimeoutSec timeout to wait resources are released
     * @return
     */
    abstract fun acquire(
        resources: List<String?>?,
        lockTimeoutSec: Int?,
        enterTimeoutSec: Int?
    ): Pair<Boolean, List<String>>

    fun acquire(resources: List<String?>?, lockTimeoutSec: Int): Pair<Boolean, List<String>> {
        return acquire(resources, lockTimeoutSec, 3)
    }

    abstract fun release()
}
