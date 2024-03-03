package com.template.states

import com.template.constants.DocumentTypes
import com.template.contracts.DocumentContract
import com.template.schema.DocumentSchemaV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.util.*

// *********
// * State *
// *********
@CordaSerializable
enum class DocumentStatus {
    PROPOSED,
    REJECTED,
    APPROVED
}

@BelongsToContract(DocumentContract::class)
data class DocumentState(
    val proposer: Party,
    val consenter: Party,
    val documentTitle: String,
    val documentType: DocumentTypes,
    val documentJson: String,
    val comments: String?,
    val versionNo: Int,
    val status: DocumentStatus,
    val updateTime: Date,
    override val linearId: UniqueIdentifier,
    override val participants: List<AbstractParty> = listOf(proposer, consenter)
) : LinearState, QueryableState {
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is DocumentSchemaV1 -> DocumentSchemaV1.PersistentDocument(
                this.proposer.name.toString(),
                this.consenter.name.toString(),
                this.documentTitle,
                this.documentType.name,
                this.comments,
                this.versionNo,
                this.updateTime,
                this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognized schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(DocumentSchemaV1)
}
