package com.qohat.entities

import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Storage
import com.qohat.domain.StorageId
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object Storages: Table("storages") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val document = varchar("document", 17)
    val address = varchar("address", 255).nullable()
    val register_number = varchar("register_number", 100).nullable()
    val activity_id = (integer("activity_id") references Lists.id).nullable()
    val department = (integer("department_id") references Lists.id)
    val city = (integer("city_id") references Lists.id)
    val phone = varchar("phone", 255).nullable()
    val email = varchar("email", 255).nullable()
    val active = bool("active").default(true)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    val StorageDepartment = Lists.alias("StorageDepartment")
    val StorageCity = Lists.alias("StorageCity")

    private val query =
        join(Lists.Activity, JoinType.INNER, activity_id, Lists.Activity[Lists.id])
        .join(StorageDepartment, JoinType.INNER, department, StorageDepartment[Lists.id])
        .join(StorageCity, JoinType.INNER, city, StorageCity[Lists.id])
        .slice(
            id,
            name,
            document,
            email,
            phone,
            address,
            register_number,
            Lists.Activity[Lists.name],
            StorageCity[Lists.name],
            StorageDepartment[Lists.name],
            active,
            created_at
        )

    fun insertQuery(storage: Storage) =
        insert {
            it[name] = storage.name.value
            it[document] = storage.document.value
            it[address] = storage.address?.value
            it[register_number] = storage.registerNumber.value
            it[activity_id] = storage.activityRegistered.id
            it[department] = storage.department.id
            it[phone] = storage.phone?.value
            it[email] = storage.email?.value
            it[city] = storage.city.id
            it[active] = true
            it[created_at] = LocalDateTime.now()
            it[updated_at] = LocalDateTime.now()
        } get id

    fun updateQuery(storageId: StorageId, storage: Storage): Int =
        update ({ id.eq(storageId.value) }) {
            it[name] = storage.name.value
            it[document] = storage.document.value
            it[address] = storage.address?.value
            it[register_number] = storage.registerNumber.value
            it[activity_id] = storage.activityRegistered.id
            it[department] = storage.department.id
            it[city] = storage.city.id
            it[phone] = storage.phone?.value
            it[email] = storage.email?.value
            it[updated_at] = LocalDateTime.now()
        }

    fun deleteQuery(storageId: StorageId): Int =
        update ({ id.eq(storageId.value) }) {
            it[active] = false
            it[updated_at] = LocalDateTime.now()
        }

    fun selectBy(storageId: StorageId): ResultRow? {
        return query
            .select { id.eq(storageId.value) }
            .singleOrNull()
    }

    fun selectAll(params: NewPaginationParams): List<ResultRow> {
        val select = query.selectAll()

        params.text?.let {
            val text = it.value.lowercase()
            select.andWhere {
                name.lowerCase() like "%$text%"
            }.orWhere {
                document.lowerCase() like "%$text%"
            }.orWhere {
                Lists.Activity[Lists.name].lowerCase() like "%$text%"
            }.orWhere {
                StorageDepartment[Lists.name].lowerCase() like "%$text%"
            }.orWhere {
                StorageCity[Lists.name].lowerCase() like "%$text%"
            }.orWhere {
                email.lowerCase() like "%$text%"
            }
        }

        return select
            .orderBy(id to SortOrder.ASC)
            .limit(params.pagination.limit.value, params.pagination.offset.value)
            .map { it }
    }

}