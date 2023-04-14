package com.qohat.entities

import com.qohat.domain.Role
import com.qohat.domain.RoleId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

object Roles: Table("roles") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 50)
    val permissions = text("permissions")
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id, name)

    val selectAll: List<ResultRow> =
        selectAll()
        .map { it }

    fun selectQuery(roleId: RoleId): ResultRow? = select { id.eq(roleId.value) }.singleOrNull()
    fun insertQuery(role: Role): Int = insertWithRawStatement({
        it[name] = role.name.value
        it[permissions] = role.permissions.value
    }) { "ON CONFLICT (name) DO UPDATE SET permissions = '${role.permissions.value}'" } get id
}