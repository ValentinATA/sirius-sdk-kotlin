package com.sirius.library.agent.consensus.simple.state_machines

import com.sirius.library.agent.aries_rfc.feature_0015_ack.Ack
import com.sirius.library.agent.aries_rfc.feature_0160_connection_protocol.state_machines.Inviter
import com.sirius.library.agent.consensus.simple.messages.*
import com.sirius.library.agent.microledgers.AbstractMicroledger
import com.sirius.library.agent.microledgers.Transaction
import com.sirius.library.agent.pairwise.Pairwise
import com.sirius.library.base.AbstractStateMachine
import com.sirius.library.errors.StateMachineTerminatedWithError
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidMessage
import com.sirius.library.errors.sirius_exceptions.SiriusInvalidPayloadStructure
import com.sirius.library.errors.sirius_exceptions.SiriusValidationError
import com.sirius.library.hub.Context
import com.sirius.library.hub.coprotocols.CoProtocolThreadedP2P
import com.sirius.library.hub.coprotocols.CoProtocolThreadedTheirs
import com.sirius.library.utils.JSONArray
import com.sirius.library.utils.JSONObject
import com.sirius.library.utils.Logger
import kotlin.coroutines.cancellation.CancellationException

class MicroLedgerSimpleConsensus : AbstractStateMachine {
    var log: Logger = Logger.getLogger(Inviter::class.simpleName)
    var me: Pairwise.Me
    var problemReport: SimpleConsensusProblemReport? = null
    var cachedP2P: MutableMap<String, Pairwise> = HashMap<String, Pairwise>()

    constructor(context: Context<*>, me: Pairwise.Me, timeToLiveSec: Int) : super(context) {
        this.me = me
        this.timeToLiveSec = timeToLiveSec
    }

    constructor(context: Context<*>, me: Pairwise.Me) : super(context){
        this.me = me
        this.timeToLiveSec = 60
    }




    private fun acceptors(
        theirs: List<Pairwise>,
        threadId: String
    ): CoProtocolThreadedTheirs {
        return CoProtocolThreadedTheirs(this.context, threadId, theirs, null, 60)
    }

    private fun leader(their: Pairwise, threadId: String, timeToLiveSec: Int): CoProtocolThreadedP2P {
        return CoProtocolThreadedP2P(context, threadId, their, null, timeToLiveSec)
    }

    /**
     *
     * @param ledgerName name of new microledger
     * @param participants list of DIDs that present pairwise list of the Microledger relationships
     * (Assumed DIDs are public or every participant has relationship with each other via pairwise)
     * @param genesis genesis block of the new microledger if all participants accept transaction
     * @return
     */
    fun initMicroledger(
        ledgerName: String,
        participants: List<String>,
        genesis: List<Transaction>
    ): Pair<Boolean, AbstractMicroledger?> {
      /*  try {
            bootstrap(participants)
            val relationships: List<Pairwise> = ArrayList(cachedP2P.values)
            acceptors(relationships, "simple-consensus-init-" + UUID.randomUUID).also { co ->
                log.info("0% - Create ledger $ledgerName")
                val ledger :  Pair<AbstractMicroledger?, List<Transaction?>?>? = context.getMicrolegders()?.create(ledgerName, genesis)
                log.info("Ledger creation terminated successfully")
                try {
                    initMicroledgerInternal(co, ledger?.first, participants, genesis)
                    log.info("100% - All participants accepted ledger creation")
                    return Pair(true, ledger)
                } catch (ex: StateMachineTerminatedWithError) {
                    log.info(
                        "100% - Terminated with error. Problem code: " + ex.problemCode
                            .toString() + " Explain: " + ex.explain
                    )
                    problemReport = SimpleConsensusProblemReport.builder().setProblemCode(ex.problemCode)
                        .setExplain(ex.explain).build()
                    if (ex.isNotify) {
                        co.send(problemReport!!)
                    }
                    context.getMicrolegders().reset(ledgerName)
                    return Pair(false, null)
                } catch (ex: Exception) {
                    log.info("100% - Terminated with error")
                    ex.printStackTrace()
                    context.getMicrolegders().reset(ledgerName)
                }
            }
        } catch (siriusValidationError: SiriusValidationError) {
            log.info("100% - Terminated with error")
            siriusValidationError.printStackTrace()
        }*/
        return Pair(false, null)
    }

    fun acceptMicroledger(leader: Pairwise, propose: InitRequestLedgerMessage): Pair<Boolean, AbstractMicroledger?> {
     /*   if (!propose.participants
                .contains(me.did)
        ) throw SiriusContextError("Invalid state machine initialization")
        var timeToLive: Int = this.timeToLiveSec
        if (propose.timeoutSec > 0) timeToLive = propose.timeoutSec
        try {
            bootstrap(propose.participants)
        } catch (siriusValidationError: SiriusValidationError) {
            log.info("100% - Terminated with error")
            siriusValidationError.printStackTrace()
            return Pair(false, null)
        }
        leader(leader, propose.getThreadId(), timeToLive).also { co ->
            val ledgerName: String = propose.ledger.optString("name", null)
            try {
                if (ledgerName == null) {
                    throw StateMachineTerminatedWithError(
                        REQUEST_PROCESSING_ERROR,
                        "Ledger name is Empty!"
                    )
                }
                for (theirDid in propose.participants) {
                    if (theirDid != me.did) {
                        if (!cachedP2P.containsKey(theirDid)) {
                            throw StateMachineTerminatedWithError(
                                REQUEST_PROCESSING_ERROR,
                                "Pairwise for DID: $theirDid does not exists!"
                            )
                        }
                    }
                }
                log.info("0% - Start ledger $ledgerName creation process")
                val ledger: AbstractMicroledger? = acceptMicroledgerInternal(co, leader, propose, timeToLive)
                log.info("100% - Ledger creation terminated successfully")
                return Pair(true, ledger)
            } catch (ex: StateMachineTerminatedWithError) {
                problemReport = SimpleConsensusProblemReport.builder().setProblemCode(ex.problemCode)
                    .setExplain(ex.explain).build()
                log.info(
                    "100% - Terminated with error. Problem code: " + ex.problemCode
                        .toString() + " Explain: " + ex.explain
                )
                if (ex.isNotify) {
                    co.send(problemReport)
                    return Pair(false, null)
                }
            }
        }*/
        return Pair(false, null)
    }

    /**
     *
     * @param ledger Microledger instance to operate with
     * @param participants list of DIDs that present pairwise list of the Microledger relationships
     * (Assumed DIDs are public or every participant has relationship with each other via pairwise)
     * @param transactions transactions to commit
     * @return
     */
    fun commit(
        ledger: AbstractMicroledger,
        participants: List<String>,
        transactions: List<Transaction>
    ): Pair<Boolean, List<Transaction>> {
    /*    try {
            bootstrap(participants)
        } catch (siriusValidationError: SiriusValidationError) {
            siriusValidationError.printStackTrace()
            return Pair(false, listOf())
        }
        val relationships: List<Pairwise> = ArrayList(cachedP2P.values)
        acceptors(relationships, "simple-consensus-commit-" + UUID.randomUUID).also { co ->
            try {
                log.info("0% - Start committing " + transactions.size + " transactions")
                val txns: List<Transaction> = commitInternal(co, ledger, transactions, participants)
                log.info("100% - Commit operation was accepted by all participants")
                return Pair(true, txns)
            } catch (ex: StateMachineTerminatedWithError) {
                ledger.resetUncommitted()
                log.info("Reset uncommitted")
                problemReport = SimpleConsensusProblemReport.builder().setProblemCode(ex.problemCode)
                    .setExplain(ex.explain).build()
                log.info(
                    "100% - Terminated with error. Problem code " + ex.problemCode
                        .toString() + " explain " + ex.explain
                )
                if (ex.isNotify) {
                    co.send(problemReport)
                }
            }
        }*/
        return Pair(false, listOf())
    }

    @Throws(StateMachineTerminatedWithError::class)
    private fun commitInternal(
        co: CoProtocolThreadedTheirs, ledger: AbstractMicroledger, transactions: List<Transaction>,
        participants: List<String>
    ): List<Transaction> {
     /*   val (first) = acquire(listOfNotNull(ledger.name()), this.timeToLiveSec)
        if (!first) {
            throw StateMachineTerminatedWithError(
                REQUEST_NOT_ACCEPTED,
                "Preparing: Ledgers are locked by other state-machine",
                false
            )
        }
        return try {
            val df: java.text.DateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            val txnTime: String = df.format(java.android.util.Date(java.lang.System.currentTimeMillis()))
            val (_, _, txns) = ledger.append(transactions, txnTime)
            val propose: ProposeTransactionsMessage =
                ProposeTransactionsMessage.builder().setTransactions(txns).setState(MicroLedgerState.fromLedger(ledger))
                    .setParticipants(participants).setTimeoutSec(this.timeToLiveSec)
                    .build()

            // ==== STAGE-1 Propose transactions to participants ====
            val commit: CommitTransactionsMessage =
                CommitTransactionsMessage.builder().setParticipants(participants).build()
            val selfPreCommit: PreCommitTransactionsMessage =
                PreCommitTransactionsMessage.builder().setState(propose.getState()).build()
            selfPreCommit.signState(context.crypto, me)
            commit.addPreCommit(me.getDid(), selfPreCommit)
            log.info("20% - Send Propose to participants")
            var results: List<CoProtocolThreadedTheirs.SendAndWaitResult> = co.sendAndWait(propose)
            log.info("30% - Received Propose from participants")
            var erroredAcceptorsDid: MutableList<String?> = ArrayList<String>()
            for (r in results) {
                if (!r.success) erroredAcceptorsDid.add(r.pairwise.their.did)
            }
            if (!erroredAcceptorsDid.isEmpty()) {
                throw StateMachineTerminatedWithError(REQUEST_PROCESSING_ERROR, "Stage-1: Participants unreachable")
            }
            log.info("50% - Validate responses")
            for (r in results) {
                if (r.message is PreCommitTransactionsMessage) {
                    try {
                        val preCommit: PreCommitTransactionsMessage = r.message as PreCommitTransactionsMessage
                        preCommit.validate()
                        val (first1) = preCommit.verifyState(context.crypto, r.pairwise.their.getVerkey())
                        if (!first1) {
                            throw SiriusValidationError(
                                "Stage-1: Error verifying signed ledger state for participant }" + r.pairwise.their
                                    .getDid()
                            )
                        }
                        if (!preCommit.hash.equals(propose.state.hash)) {
                            throw SiriusValidationError(
                                "Stage-1: Non-consistent ledger state for participant " + r.pairwise.their.did
                            )
                        }
                        commit.addPreCommit(r.pairwise.their.did, preCommit)
                    } catch (ex: SiriusValidationError) {
                        throw StateMachineTerminatedWithError(
                            RESPONSE_NOT_ACCEPTED,
                            "Stage-1: Error for participant " + r.pairwise.their.did
                                .toString() + ": " + ex.message
                        )
                    }
                } else if (r.message is SimpleConsensusProblemReport) {
                    val problemReport: SimpleConsensusProblemReport = r.message as SimpleConsensusProblemReport
                    val explain = "Stage-1: Problem report from participant " + r.pairwise.their.did
                        .toString() + " " + problemReport.explain
                    throw StateMachineTerminatedWithError(RESPONSE_NOT_ACCEPTED, explain)
                }
            }

            // ===== STAGE-2: Accumulate pre-commits and send commit propose to all participants
            val postCommitAll: PostCommitTransactionsMessage = PostCommitTransactionsMessage.builder().build()
            postCommitAll.addCommitSign(context.crypto, commit, me)
            log.info("60% - Send Commit to participants")
            results = co.sendAndWait(commit)
            log.info("70% - Received Commit response from participants")
            erroredAcceptorsDid = ArrayList<String>()
            for (r in results) {
                if (!r.success) erroredAcceptorsDid.add(r.pairwise.their.did)
            }
            if (!erroredAcceptorsDid.isEmpty()) {
                throw StateMachineTerminatedWithError(REQUEST_PROCESSING_ERROR, "Stage-2: Participants unreachable")
            }
            log.info("80% - Validate responses")
            for (r in results) {
                if (r.message is PostCommitTransactionsMessage) {
                    try {
                        val postCommit: PostCommitTransactionsMessage = r.message as PostCommitTransactionsMessage
                        postCommit.validate()
                        val postCommits: JSONArray = postCommit.commits
                        for (o in postCommits) {
                            postCommitAll.commits.put(o)
                        }
                    } catch (ex: SiriusValidationError) {
                        throw StateMachineTerminatedWithError(
                            RESPONSE_NOT_ACCEPTED,
                            "Stage-2: Error for participant " + r.pairwise.their.did
                                .toString() + ": " + ex.message
                        )
                    }
                } else if (r.message is SimpleConsensusProblemReport) {
                    val problemReport: SimpleConsensusProblemReport = r.message as SimpleConsensusProblemReport
                    val explain = "Stage-2: Problem report from participant " + r.pairwise.their.did
                        .toString() + " " + problemReport.explain
                    throw StateMachineTerminatedWithError(RESPONSE_NOT_ACCEPTED, explain)
                }
            }

            // STAGE-3: Notify all participants with post-commits and finalize process
            log.info("90% - Send Post-Commit")
            co.send(postCommitAll)
            val uncommitted_size: Int = ledger.uncommittedSize() - ledger.size()
            ledger.commit(uncommitted_size)
            txns
        } finally {
            release()
        }*/
        return emptyList()
    }

    fun acceptCommit(leader: Pairwise, propose: ProposeTransactionsMessage): Boolean {
     /*   var timeToLive: Int = this.timeToLiveSec
        if (propose.timeoutSec > 0) timeToLive = propose.timeoutSec
        leader(leader, propose.getThreadId(), timeToLive).also { co ->
            var ledger: AbstractMicroledger? = null
            try {
                log.info("0% - Start acception " + propose.transactions().size.toString() + " transactions")
                ledger = loadLedger(propose)
                acceptCommitInternal(co, ledger, leader, propose)
                log.info("100% - Acception terminated successfully")
                return true
            } catch (ex: StateMachineTerminatedWithError) {
                if (ledger != null) ledger.resetUncommitted()
                problemReport = SimpleConsensusProblemReport.builder().setProblemCode(ex.problemCode)
                    .setExplain(ex.explain).build()
                log.info("100% - Terminated with error " + ex.problemCode.toString() + " " + ex.explain)
                if (ex.isNotify) {
                    co.send(problemReport!!)
                }
            } catch (ex: Exception) {
                if (ledger != null) ledger.resetUncommitted()
                ex.printStackTrace()
            }
        }*/
        return false
    }

    @Throws(SiriusValidationError::class)
    private fun bootstrap(patricipants: List<String>) {
        for (did in patricipants) {
            if (did != me.did) {
                if (!cachedP2P.containsKey(did)) {
                    val p: Pairwise = context.getPairwiseListi().loadForDid(did)
                        ?: throw SiriusValidationError("Unknown pairwise for DID: $did")
                    cachedP2P[did] = p
                }
            }
        }
    }

    @Throws(StateMachineTerminatedWithError::class)
    private fun loadLedger(propose: ProposeTransactionsMessage): AbstractMicroledger {
      /*  try {
            bootstrap(propose.participants)
            propose.validate()
            if (propose.participants.size) < 2) {
                throw SiriusValidationError("Stage-1: participant count less than 2")
            }
            if (!propose.participants.contains(me.did)) {
                throw SiriusValidationError("Stage-1: " + me.did.toString() + " is not participant")
            }
            val isLedgerExists: Boolean = context.getMicrolegders().isExists(propose.state.name)
            if (!isLedgerExists) {
                throw SiriusValidationError(
                    "Stage-1: Ledger with name " + propose.state.name.toString() + " does not exists"
                )
            }
        } catch (ex: SiriusValidationError) {
            throw StateMachineTerminatedWithError(RESPONSE_NOT_ACCEPTED, ex.message ?:"")
        }*/
        return context.getMicrolegdersi().getLedger(propose.state!!.name)!!
    }

    @Throws(StateMachineTerminatedWithError::class, SiriusInvalidMessage::class, SiriusInvalidPayloadStructure::class,
        CancellationException::class
    )
    private suspend fun acceptCommitInternal(
        co: CoProtocolThreadedP2P,
        ledger: AbstractMicroledger?,
        leader: Pairwise,
        propose: ProposeTransactionsMessage
    ) {
        val (first) = acquire(listOfNotNull(ledger?.name()), this.timeToLiveSec)
        if (!first) {
            throw StateMachineTerminatedWithError(
                REQUEST_NOT_ACCEPTED,
                "Preparing: Ledgers are locked by other state-machine"
            )
        }
        try {
            // ===== STAGE-1: Process Propose, apply transactions and response ledger state on self-side
            ledger!!.append(propose.transactions())
            val ledgerState: MicroLedgerState = MicroLedgerState.fromLedger(ledger!!)
            val preCommit: PreCommitTransactionsMessage =
                PreCommitTransactionsMessage.builder().setState(MicroLedgerState.fromLedger(ledger!!)).build()
            preCommit.signState(context.crypto, me)
            log.info("10% - Send Pre-Commit")
            val (first1, second) = co.sendAndWait(preCommit)
            if (first1) {
                log.info("20% - Received Pre-Commit response")
                if (second is CommitTransactionsMessage) {
                    // ===== STAGE-2: Process Commit request, check neighbours signatures
                    try {
                        log.info("30% - Validate Commit")
                        val commit: CommitTransactionsMessage = second as CommitTransactionsMessage
                        if (HashSet<String>(commit.participants) != HashSet<String>(propose.participants)) {
                            throw SiriusValidationError("Non-consistent participants")
                        }
                        commit.validate()
                        commit.verifyPreCommits(context.crypto, ledgerState)

                        // ===== STAGE-3: Process post-commit, verify participants operations
                        val postCommit: PostCommitTransactionsMessage = PostCommitTransactionsMessage.builder().build()
                        postCommit.addCommitSign(context.crypto, commit, me)
                        log.info("50% - Send Post-Commit")
                        val (first2, second1) = co.sendAndWait(postCommit)
                        if (first2) {
                            log.info("60% - Received Post-Commit response")
                            if (second1 is PostCommitTransactionsMessage) {
                                try {
                                    log.info("80% - Validate response")
                                    val postCommitAll: PostCommitTransactionsMessage =
                                        second1 as PostCommitTransactionsMessage
                                    postCommitAll.validate()
                                    val verkeys: MutableList<String> = ArrayList<String>()
                                    for (p in cachedP2P.values) {
                                      //  verkeys.add(p.their.verkey)
                                    }
                                    postCommitAll.verifyCommits(context.crypto, commit, verkeys)
                                    val uncommitted_size: Int = ledgerState.uncommittedSize!!
                                    log.info("90% - Flush transactions to Ledger storage")
                                    ledger.commit(uncommitted_size)
                                } catch (ex: SiriusValidationError) {
                                    throw StateMachineTerminatedWithError(
                                        REQUEST_NOT_ACCEPTED,
                                        "Stage-3: error for leader " + leader.their.did
                                            .toString() + " " + ex.message
                                    )
                                }
                            } else if (second1 is SimpleConsensusProblemReport) {
                                problemReport = second1 as SimpleConsensusProblemReport
                                throw StateMachineTerminatedWithError(
                                    problemReport!!.problemCode!!,
                                    "Stage-3: Problem report from leader" + leader.their.did
                                        .toString() + " " + problemReport!!.explain
                                )
                            }
                        } else {
                            throw StateMachineTerminatedWithError(
                                REQUEST_PROCESSING_ERROR,
                                "Stage-3: Post-Commit awaiting terminated by timeout for leader: " + leader.their
                                    .did
                            )
                        }
                    } catch (ex: SiriusValidationError) {
                        throw StateMachineTerminatedWithError(
                            REQUEST_NOT_ACCEPTED,
                            "Stage-2: error for actor " + leader.their.did.toString() + " " + ex.message
                        )
                    }
                } else if (second is SimpleConsensusProblemReport) {
                    problemReport = second as SimpleConsensusProblemReport
                    val explain = "Stage-1: Problem report from leader" + leader.their.did
                        .toString() + " " + problemReport!!.explain
                    throw StateMachineTerminatedWithError(problemReport!!.problemCode!!, problemReport!!.explain!!)
                } else {
                    throw StateMachineTerminatedWithError(
                        REQUEST_NOT_ACCEPTED,
                        "Unexpected message @type: " + second!!.getType()
                    )
                }
            } else {
                throw StateMachineTerminatedWithError(
                    REQUEST_PROCESSING_ERROR,
                    "Stage-1: Commit awaiting terminated by timeout for leader: " + leader.their.did
                )
            }
        } finally {
            release()
        }
    }

    private fun acquire(names: List<String>, lockTimeoutSec: Int): Pair<Boolean, List<String>> {
        var names = names
        val NAMESPACE = "ledgers"
        names = ArrayList<String>(HashSet<String>(names))
        for (i in names.indices) {
            names[i] = NAMESPACE + "/" + names[i]
        }
        val (first, second) = this.context.acquire(names, lockTimeoutSec, null)
        val lockedLedgers: MutableList<String> = ArrayList<String>()
        for (s in second) {
            lockedLedgers.add(s.split("/").toTypedArray()[1])
        }
        return Pair(first, lockedLedgers)
    }

    private fun release() {
        context.release()
    }

    @Throws(StateMachineTerminatedWithError::class)
    private fun initMicroledgerInternal(
        co: CoProtocolThreadedTheirs,
        ledger: AbstractMicroledger,
        participants: List<String>,
        genesis: List<Transaction>
    ) {
        val (success, busy) = acquire(listOfNotNull(ledger.name()), this.timeToLiveSec)
        if (!success) {
            throw StateMachineTerminatedWithError(
                REQUEST_NOT_ACCEPTED,
                "Preparing: Ledgers are locked by other state-machine",
                false
            )
        }
        try {
            // ============= STAGE 1: PROPOSE =================
            val propose: InitRequestLedgerMessage =
                InitRequestLedgerMessage.builder().setTimeoutSec(this.timeToLiveSec).setLedgerName(ledger.name())
                    .setGenesis(genesis).setRootHash(ledger.rootHash()).setParticipants(participants).build()
            propose.addSignature(context.crypto, me)
            val requestCommit: InitResponseLedgerMessage = InitResponseLedgerMessage.builder().build()
            requestCommit.assignFrom(propose)
            log.info("20% - Send propose")

            // Switch to await transaction acceptors action
            var results: List<CoProtocolThreadedTheirs.SendAndWaitResult> = co.sendAndWait(propose)
            log.info("30% - Received responses from all acceptors")
            var erroredAcceptorsDid: MutableList<String> = ArrayList<String>()
            for (r in results) {
               // if (!r.success) erroredAcceptorsDid.add(r.pairwise.their.did)
            }
            if (!erroredAcceptorsDid.isEmpty()) {
                throw StateMachineTerminatedWithError(REQUEST_PROCESSING_ERROR, "Stage-1: Participants unreachable")
            }
            log.info("40% - Validate responses")
         /*   for (r in results) {
                if (r.message is InitResponseLedgerMessage) {
                    val response: InitResponseLedgerMessage = r.message as InitResponseLedgerMessage
                    response.validate()
                    response.checkSignatures(context.crypto, r.pairwise.their.did))
                    val signature: JSONObject = response.signature(r.pairwise.their.did)
                    requestCommit.signatures().put(signature)
                } else if (r.message is SimpleConsensusProblemReport) {
                    val response: SimpleConsensusProblemReport = r.message as SimpleConsensusProblemReport
                    throw StateMachineTerminatedWithError(response.problemCode, response.explain)
                }
            }*/

            // ============= STAGE 2: COMMIT ============
            log.info("60% - Send commit request")
            results = co.sendAndWait(requestCommit)
            log.info("70% - Received commit responses")
            erroredAcceptorsDid = ArrayList<String>()
            for (r in results) {
               // if (!r.success) erroredAcceptorsDid.add(r.pairwise.their.did)
            }
            if (!erroredAcceptorsDid.isEmpty()) {
                throw StateMachineTerminatedWithError(REQUEST_PROCESSING_ERROR, "Stage-2: Participants unreachable")
            }
            log.info("80% - Validate commit responses from acceptors")
            for (r in results) {
                if (r.message is SimpleConsensusProblemReport) {
                    val response: SimpleConsensusProblemReport = r.message as SimpleConsensusProblemReport
                    throw StateMachineTerminatedWithError(
                        RESPONSE_PROCESSING_ERROR,
                        "Participant DID: " + r.pairwise!!.their!!.did
                            .toString() + " declined operation with error: " + response.explain
                    )
                }
            }

            // ============== STAGE 3: POST-COMMIT ============
            val ack: Ack = Ack.builder().setStatus(Ack.Status.OK).build()
            log.info("90% - All checks OK. Send Ack to acceptors")
            co.send(ack)
        } catch (siriusValidationError: SiriusValidationError) {
            siriusValidationError.printStackTrace()
        } finally {
            release()
        }
    }

    @Throws(StateMachineTerminatedWithError::class, CancellationException::class)
    private suspend fun acceptMicroledgerInternal(
        co: CoProtocolThreadedP2P, leader: Pairwise, propose: InitRequestLedgerMessage,
        timeout: Int
    ): AbstractMicroledger? {
        val (first) = acquire(
            listOfNotNull(propose.ledger.getString("name")),
            this.timeToLiveSec
        )
        if (!first) {
            throw StateMachineTerminatedWithError(
                REQUEST_NOT_ACCEPTED,
                "Preparing: Ledgers are locked by other state-machine"
            )
        }
        try {
            // =============== STAGE 1: PROPOSE ===============
            try {
                propose.validate()
               // propose.checkSignatures(context.crypto, leader.their.did)
                if (propose.participants.size < 2) {
                    throw SiriusValidationError("Stage-1: participants less than 2")
                }
            } catch (ex: SiriusValidationError) {
                throw StateMachineTerminatedWithError(REQUEST_NOT_ACCEPTED, ex.message?:"")
            }
            val genesis: MutableList<Transaction> = ArrayList<Transaction>()
            val genJsonArr: JSONArray = propose!!.ledger!!.getJSONArray("genesis") ?: JSONArray()
            for (o in genJsonArr) {
                genesis.add(Transaction(o as JSONObject))
            }
            log.info("10% - Initialize ledger")
            val (ledger, txns) = context.getMicrolegdersi().create(propose.ledger.getString("name"), genesis)!!
            log.info("20% - Ledger initialized successfully")
            if (!propose.ledger.optString("root_hash").equals(ledger!!.rootHash())) {
                context.getMicrolegdersi().reset(ledger!!.name())
                throw StateMachineTerminatedWithError(REQUEST_PROCESSING_ERROR, "Stage-1: Non-consistent Root Hash")
            }
            val response: InitResponseLedgerMessage = InitResponseLedgerMessage.builder().setTimeoutSec(timeout).build()
            response.assignFrom(propose)
            val commitLedgerHash: JSONObject? = response.ledgerHash()
            response.addSignature(context.crypto, me)

            // =============== STAGE 2: COMMIT ===============
            log.info("30% - Send propose response")
            val (first1, second) = co.sendAndWait(response)
            if (first1) {
                log.info("50% - Validate request commit")
                if (second is InitResponseLedgerMessage) {
                    val requestCommit: InitResponseLedgerMessage = second as InitResponseLedgerMessage
                    try {
                        requestCommit.validate()
                        val hashes: JSONObject = requestCommit.checkSignatures(context.crypto)
                        for (theirDid in hashes.keySet()) {
                            val decoded: JSONObject? = hashes.getJSONObject(theirDid)
                            if (!decoded!!.similar(commitLedgerHash!!)) {
                                throw SiriusValidationError("Stage-2: NonEqual Ledger hash with participant $theirDid")
                            }
                        }
                    } catch (ex: SiriusValidationError) {
                        throw StateMachineTerminatedWithError(REQUEST_NOT_ACCEPTED, ex.message ?:"")
                    }
                    val commitParticipantsSet: Set<String> = HashSet(requestCommit.participants)
                    val proposeParticipantsSet: Set<String> = HashSet(propose.participants)
                    val signersSet: MutableSet<Any> = HashSet<Any>()
                    val signs: JSONArray = requestCommit.signatures()
                    for (sign in signs) {
                        signersSet.add((sign as JSONObject).getString("participant")!!)
                    }
                    var errorExplain: String? = null
                    if (proposeParticipantsSet != signersSet) {
                        errorExplain = "Stage-2: Set of signers differs from proposed participants set"
                    } else if (commitParticipantsSet != signersSet) {
                        errorExplain = "Stage-2: Set of signers differs from commit participants set"
                    }
                    if (errorExplain != null) {
                        throw StateMachineTerminatedWithError(REQUEST_NOT_ACCEPTED, errorExplain)
                    }

                    // Accept commit
                    log.info("70% - Send Ack")
                    val ack: Ack = Ack.builder().setStatus(Ack.Status.OK).build()
                    val (first2, second1) = co.sendAndWait(ack)
                    // =========== STAGE-3: POST-COMMIT ===============
                    if (first2) {
                        log.info("90% - Response to Ack received")
                        if (second1 is Ack) {
                            return ledger
                        } else if (second1 is SimpleConsensusProblemReport) {
                            problemReport = second1 as SimpleConsensusProblemReport
                            log.info(
                                "Code: " + problemReport!!.problemCode
                                    .toString() + "; Explain: " + problemReport!!.explain
                            )
                            throw StateMachineTerminatedWithError(
                                problemReport!!.problemCode ?:"",
                                problemReport!!.explain?:""
                            )
                        }
                    } else {
                        throw StateMachineTerminatedWithError(
                            REQUEST_PROCESSING_ERROR,
                            "Stage-3: Commit accepting was terminated by timeout for actor: " + leader.their.did
                        )
                    }
                } else if (second is SimpleConsensusProblemReport) {
                    problemReport = second as SimpleConsensusProblemReport
                    throw StateMachineTerminatedWithError(problemReport!!.problemCode!!, problemReport!!.explain!!)
                }
            } else {
                throw StateMachineTerminatedWithError(
                    REQUEST_PROCESSING_ERROR,
                    "Stage-2: Commit response awaiting was terminated by timeout for actor: " + leader.their.did
                )
            }
        } catch (siriusInvalidMessage: SiriusInvalidMessage) {
            siriusInvalidMessage.printStackTrace()
        } catch (siriusInvalidPayloadStructure: SiriusInvalidPayloadStructure) {
            siriusInvalidPayloadStructure.printStackTrace()
        } finally {
            release()
        }
        return null
    }

    override fun protocols(): List<String>? {
        return null
    }

    companion object {
        //problem codes
        const val REQUEST_NOT_ACCEPTED = "request_not_accepted"
        const val REQUEST_PROCESSING_ERROR = "request_processing_error"
        const val RESPONSE_NOT_ACCEPTED = "response_not_accepted"
        const val RESPONSE_PROCESSING_ERROR = "response_processing_error"
    }
}
