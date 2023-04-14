package com.qohat.entities

import com.qohat.domain.RequestAssignment
import com.qohat.domain.RoleName
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.batchInsert
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

object Assignments: Table("user_assignments") {
    val user_id = (uuid("user_id") references Users.id)
    val people_request_id = uuid("people_request_id").references(PeopleRequests.id)

    private fun query(role: RoleName) = join(Users, JoinType.INNER, user_id, Users.id)
        .join(Roles, JoinType.INNER, Roles.id, Users.role_id)
        .slice(user_id, people_request_id.count())
        .select { Roles.name.lowerCase().eq(role.value) }
        .groupBy(user_id)
        .orderBy(people_request_id.count() to SortOrder.ASC)

    val selectAll = slice(user_id, people_request_id.count())
        .selectAll()
        .groupBy(user_id)
        .orderBy(people_request_id.count() to SortOrder.ASC)

    fun selectBy(role: RoleName) = query(role).map { it }

    fun insertBatch(assignments: List<RequestAssignment>) =
        batchInsert(assignments, shouldReturnGeneratedValues = false) {
            this[user_id] = it.userId.value
            this[people_request_id] = it.peopleRequestId.value
        }

    fun insertQuery(assignment: RequestAssignment) =
        insert {
            it[user_id] = assignment.userId.value
            it[people_request_id] = assignment.peopleRequestId.value
        }
}