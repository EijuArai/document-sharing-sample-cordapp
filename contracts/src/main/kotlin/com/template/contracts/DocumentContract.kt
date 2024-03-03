package com.template.contracts

import com.template.states.DocumentState
import com.template.states.DocumentStatus
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.contracts.requireThat
import net.corda.core.identity.AbstractParty

// ************
// * Contract *
// ************

class DocumentContract : Contract {
    companion object {
        // Used to identify our contract when building a transaction.
        const val ID = "com.template.contracts.DocumentContract"
    }

    override fun verify(tx: LedgerTransaction) {
        // Verification logic goes here.
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Propose -> requireThat {
                "There should be no DocumentState in the input".using(tx.inRefsOfType<DocumentState>().isEmpty())
                "There should be exactly one DocumentState in the output".using(tx.outputsOfType<DocumentState>().size == 1)

                val output = tx.outputsOfType<DocumentState>().single()

                "Proposer and Consenter should be different parties".using(output.proposer != output.consenter)
                "Participants should be Proposer and Consenter".using(output.participants == listOf(output.proposer, output.consenter))
                "The initial status of the output should be PROPOSED".using(output.status == DocumentStatus.PROPOSED)
                "The versionNo should be 0".using(output.versionNo == 0)

                val signers = tx.commands.single().signers.distinct()
                val distinctParticipants = tx.outputStates.single().participants.map(AbstractParty::owningKey).distinct()

                "Only required signers may sign the transaction".using(signers == distinctParticipants)
            }

            is Commands.Reject -> requireThat {
                "There should be exactly one DocumentState in the input".using(tx.inRefsOfType<DocumentState>().size == 1)
                "There should be exactly one DocumentState in the output".using(tx.outputsOfType<DocumentState>().size == 1)

                val input = tx.inputsOfType<DocumentState>().single()
                val output = tx.outputsOfType<DocumentState>().single()

                "Proposer and Consenter should be different parties".using(output.proposer != output.consenter)
                "Participants should be Proposer and Consenter".using(output.participants == listOf(output.proposer, output.consenter))
                "The status of the input should be PROPOSED".using(input.status == DocumentStatus.PROPOSED)
                "The status of the output should be REJECTED".using(output.status == DocumentStatus.REJECTED)
                "The comments should not be null or empty".using(!output.comments.isNullOrEmpty())
                "The versionNo should be greater than 0".using(output.versionNo > 0)
                "The versionNo should be incremented".using(output.versionNo == input.versionNo + 1)
                "Properties other than versionNo, status, comments and updateTime should not have been changed".using(
                    output.proposer == input.proposer &&
                            output.consenter == input.consenter &&
                            output.documentTitle == input.documentTitle &&
                            output.documentType == input.documentType &&
                            output.documentJson == input.documentJson &&
                            output.linearId == input.linearId &&
                            output.participants == input.participants
                )
                val signers = tx.commands.single().signers.distinct()
                val distinctParticipants = (input.participants + output.participants).map(AbstractParty::owningKey).distinct()

                "Only required signers may sign the transaction".using(signers == distinctParticipants)
            }

            is Commands.Revise -> requireThat {
                "There should be exactly one DocumentState in the input".using(tx.inRefsOfType<DocumentState>().size == 1)
                "There should be exactly one DocumentState in the output".using(tx.outputsOfType<DocumentState>().size == 1)

                val input = tx.inputsOfType<DocumentState>().single()
                val output = tx.outputsOfType<DocumentState>().single()

                "Proposer and Consenter should be different parties".using(output.proposer != output.consenter)
                "Participants should be Proposer and Consenter".using(output.participants == listOf(output.proposer, output.consenter))
                "The status of the input should be PROPOSED or REJECTED".using(input.status == DocumentStatus.PROPOSED || input.status == DocumentStatus.REJECTED)
                "The status of the output should be PROPOSED".using(output.status == DocumentStatus.PROPOSED)
                "The versionNo should be greater than 0".using(output.versionNo > 0)
                "The versionNo should be incremented".using(output.versionNo == input.versionNo + 1)
                "Properties other than status, documentTitle, documentType, documentJson, comments, versionNo and updateTime should not have been changed".using(
                    output.proposer == input.proposer &&
                            output.consenter == input.consenter &&
                            output.linearId == input.linearId &&
                            output.participants == input.participants
                )
                val signers = tx.commands.single().signers.distinct()
                val distinctParticipants = (input.participants + output.participants).map(AbstractParty::owningKey).distinct()

                "Only required signers may sign the transaction".using(signers == distinctParticipants)
            }

            is Commands.Approve -> requireThat {
                "There should be exactly one DocumentState in the input".using(tx.inRefsOfType<DocumentState>().size == 1)
                "There should be exactly one DocumentState in the output".using(tx.outputsOfType<DocumentState>().size == 1)

                val input = tx.inputsOfType<DocumentState>().single()
                val output = tx.outputsOfType<DocumentState>().single()

                "Proposer and Consenter must be different parties".using(output.proposer != output.consenter)
                "Participants should be Proposer and Consenter".using(output.participants == listOf(output.proposer, output.consenter))
                "The status of the input should be PROPOSED".using(input.status == DocumentStatus.PROPOSED)
                "The status of the output should be APPROVED".using(output.status == DocumentStatus.APPROVED)
                "The versionNo should be greater than 0".using(output.versionNo > 0)
                "The versionNo should be incremented".using(output.versionNo == input.versionNo + 1)
                "Properties other than status, versionNo, comments and updateTime should not have been changed".using(
                    output.proposer == input.proposer &&
                            output.consenter == input.consenter &&
                            output.documentTitle == input.documentTitle &&
                            output.documentType == input.documentType &&
                            output.documentJson == input.documentJson &&
                            output.linearId == input.linearId &&
                            output.participants == input.participants
                )
                val signers = tx.commands.single().signers.distinct()
                val distinctParticipants = (input.participants + output.participants).map(AbstractParty::owningKey).distinct()

                "Only required signers may sign the transaction".using(signers == distinctParticipants)
            }
        }
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {
        class Propose : Commands
        class Approve : Commands
        class Revise : Commands
        class Reject : Commands
    }
}