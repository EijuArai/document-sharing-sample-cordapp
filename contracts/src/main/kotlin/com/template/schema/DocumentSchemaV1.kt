package com.template.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.annotation.Nullable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object DocumentSchema
object DocumentSchemaV1: MappedSchema(
    schemaFamily = DocumentSchema.javaClass,
    version = 1,
    mappedTypes = listOf(PersistentDocument::class.java)) {

    override val migrationResource: String
        get() = "document-state-v1.changelog-master";

    @Entity
    @Table(name = "document_states")
    class PersistentDocument(
        @Column(name = "proposer")
        val proposer: String,

        @Nullable
        @Column(name = "consenter")
        val consenter: String,

        @Column(name = "document_title")
        val documentTitle: String,

        @Column(name = "document_type")
        val documentType: String,

        @Column(name = "comments")
        val comments: String?,

        @Column(name = "version_no")
        val versionNo: Int,

        @Column(name = "update_time")
        val updatedTime: Date,

        @Column(name = "linear_id")
        val linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", "", "", null, 0, Date(), UUID.randomUUID())
    }

}