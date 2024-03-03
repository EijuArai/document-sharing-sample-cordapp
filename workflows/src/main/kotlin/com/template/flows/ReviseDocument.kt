package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.constants.DocumentTypes
import com.template.contracts.DocumentContract
import com.template.flows.utils.FlowUtils
import com.template.states.DocumentState
import com.template.states.DocumentStatus
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.DEFAULT_PAGE_SIZE
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.util.*

@InitiatingFlow
@StartableByRPC
class ReviseDocument(
    private val linearId: UniqueIdentifier,
    private val documentTitle: String,
    private val documentType: DocumentTypes,
    private val documentJson: String,
    private val comments: String?
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Get the DocumentState to be agreed
        val criteria = QueryCriteria.LinearStateQueryCriteria(null, listOf(linearId))
        val pageSpec = PageSpecification(pageNumber = DEFAULT_PAGE_NUM, pageSize = DEFAULT_PAGE_SIZE)
        val queryResults = serviceHub.vaultService.queryBy<DocumentState>(criteria, pageSpec)
        val inputStateAndRef = queryResults.states.single()
        val inputState = inputStateAndRef.state.data

        if (ourIdentity != inputState.proposer) {
            throw FlowException("Initiator node should be proposer.")
        }

        if (inputState.status != DocumentStatus.PROPOSED && inputState.status != DocumentStatus.REJECTED) {
            throw FlowException("Only PROPOSED or REJECTED document can be revised.")
        }

        val participants = inputState.participants

        // Step 1. Get a reference to the notary service on our network and our key pair.
        val notary = FlowUtils.getNotary(serviceHub)

        // Step 2. Compose the State
        val outputState = DocumentState(
            inputState.proposer,
            inputState.consenter,
            documentTitle,
            documentType,
            documentJson,
            comments,
            inputState.versionNo + 1,
            DocumentStatus.PROPOSED,
            Date(),
            inputState.linearId,
            participants
        )

        //Step 3. Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary)
            .addInputState(inputStateAndRef)
            .addCommand(DocumentContract.Commands.Revise(), participants.map { it.owningKey })
            .addOutputState(outputState)

        // Step 4. Verify and sign it with our KeyPair.
        builder.verify(serviceHub)

        // Step 5. Sign and finalize the TX.
        val ptx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        val counterParty = participants.single { it != ourIdentity } as Party
        val session = listOf(initiateFlow(counterParty))
        val stx = subFlow(CollectSignaturesFlow(ptx, session))
        return subFlow(FinalityFlow(stx, session))
    }
}

@InitiatedBy(ReviseDocument::class)
class ReviseDocumentResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks
            }
        }
        val txId = subFlow(signTransactionFlow).id
        return subFlow(
            ReceiveFinalityFlow(
                counterpartySession, expectedTxId = txId,
                statesToRecord = StatesToRecord.ALL_VISIBLE
            )
        )
    }
}