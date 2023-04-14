package com.qohat.entities

import arrow.core.getOrElse
import at.favre.lib.crypto.bcrypt.BCrypt
import com.qohat.domain.User
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object Users: Table("users") {
    val id = uuid("id")
    val name = varchar("name", 50)
    val last_name = varchar("last_name", 50)
    val email = varchar("email", 50)
    val password = varchar("password", 255)
    val document_type = (integer("document_type") references Lists.id)
    val document = varchar("document", 10)
    val address = varchar("address", 100).nullable()
    val role_id = (integer("role_id") references Roles.id)
    val active = bool("active").default(true)
    val recovering_password = bool("recovering_password").default(false)
    val recover_token = uuid("recover_token").nullable()
    val recover_expiration = datetime("recover_expiration").nullable()
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    val ccf_id = (integer("ccf_id") references Lists.id).nullable()

    override val primaryKey = PrimaryKey(id)

    fun insertBatch(users: List<User>) =
        batchInsert(users, shouldReturnGeneratedValues = false) {
            this[id] = UUID.randomUUID()
            this[name] = it.name
            this[last_name] = it.lastName
            this[email] = it.email
            this[password] = BCrypt.withDefaults().hashToString(12, it.password.toCharArray())
            this[document_type] = it.documentType.id
            this[document] = it.document
            this[address] = it.address.getOrElse { null }
            this[role_id] = it.role.id.value
            this[active] = true
            //TODO Review this
            this[ccf_id] = it.ccf.value
            this[created_at] = LocalDateTime.now()
            this[updated_at] = LocalDateTime.now()
        }
}