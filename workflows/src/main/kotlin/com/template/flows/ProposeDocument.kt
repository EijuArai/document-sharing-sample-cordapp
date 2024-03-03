package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.constants.DocumentTypes
import net.corda.core.flows.*
import net.corda.core.flows.FinalityFlow
import net.corda.core.identity.Party
import com.template.contracts.DocumentContract
import com.template.flows.utils.FlowUtils
import com.template.states.DocumentState
import com.template.states.DocumentStatus
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import java.util.*


// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class ProposeDocument(
    private val proposer: Party,
    private val consenter: Party,
    private val documentTitle: String,
    private val documentType: String,
    private val documentJson: String,
    private val comments: String?
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        if (ourIdentity != proposer) {
            throw FlowException("Proposer node should be initiator node.")
        }

        // Step 1. Get a reference to the notary service on our network and our key pair.
        val notary = FlowUtils.getNotary(serviceHub)

        // Step 2. Compose the State
        val output = DocumentState(
            proposer,
            consenter,
            documentTitle,
            DocumentTypes.valueOf(documentType),
            documentJson,
            comments,
            0,
            DocumentStatus.PROPOSED,
            Date(),
            UniqueIdentifier(),
            listOf(proposer, consenter)
        )

        //Step 3. Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary)
            .addCommand(DocumentContract.Commands.Propose(), listOf(proposer.owningKey, consenter.owningKey))
            .addOutputState(output)

        // Step 4. Verify and sign it with our KeyPair.
        builder.verify(serviceHub)

        // Step 5. Sign and finalize the TX.
        val ptx = serviceHub.signInitialTransaction(builder, proposer.owningKey)
        val session = listOf(initiateFlow(consenter))
        val stx = subFlow(CollectSignaturesFlow(ptx, session))
        return subFlow(FinalityFlow(stx, session))
    }
}

@InitiatedBy(ProposeDocument::class)
class ProposeDocumentResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                //Addition checks if you need
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