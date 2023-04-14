package com.qohat.entities

import com.qohat.domain.Name
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Supply
import com.qohat.domain.SupplyId
import org.jetbrains.exposed.sql.Concat
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.orWhere
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.stringLiteral
import org.jetbrains.exposed.sql.substring
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object Supplies: Table("supplies") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val price = decimal("price", 10, 2)
    val crop_group_id = (integer("crop_group_id") references Lists.id)
    val active = bool("active").default(true)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    private val query = join(Lists.CropGroup, JoinType.INNER, crop_group_id, Lists.CropGroup[Lists.id])
        .join(Lists.Chain, JoinType.INNER, Lists.CropGroup[Lists.list], Concat("_", stringLiteral("CROP_GROUP"), Lists.Chain[Lists.name]))
        .slice(
            id,
            name,
            Lists.Chain[Lists.name],
            Lists.CropGroup[Lists.name],
            price,
            active,
            created_at
        )

    fun insertQuery(supply: Supply): Long =
        insert {
            it[name] = supply.name.value
            it[crop_group_id] = supply.cropGroup.id
            it[price] = supply.price.value
            it[active] = true
            it[created_at] = LocalDateTime.now()
            it[updated_at] = LocalDateTime.now()
        } get id

    fun updateQuery(supplyId: SupplyId, supply: Supply): Int =
        update ({ id.eq(supplyId.value) }) {
            it[name] = supply.name.value
            it[crop_group_id] = supply.cropGroup.id
            it[price] = supply.price.value
            it[updated_at] = LocalDateTime.now()
        }

    fun deleteQuery(supplyId: SupplyId): Int =
        update ({ id.eq(supplyId.value) }) {
            it[active] = false
            it[updated_at] = LocalDateTime.now()
        }

    fun selectBy(productId: SupplyId): ResultRow? {
        return query
            .select { id.eq(productId.value) }
            .singleOrNull()
    }

    fun selectAll(params: NewPaginationParams): List<ResultRow> {
        val select = query.selectAll()
        params.text?.let {
            val text = it.value.lowercase()
            select.andWhere {
                name.lowerCase() like "%$text%"
            }.orWhere {
                Lists.CropGroup[Lists.name].lowerCase() like "%$text%"
            }.orWhere {
                Lists.Chain[Lists.name].lowerCase() like "%$text%"
            }
        }

        return select
            .orderBy(id to SortOrder.ASC)
            .limit(params.pagination.limit.value, params.pagination.offset.value)
            .map { it }
    }

    fun selectAll(): List<ResultRow> =
        query.selectAll()
        .orderBy(id to SortOrder.ASC)
        .map { it }

    fun selectAll(name: Name): List<ResultRow> =
        query.select {
            Lists.CropGroup[Lists.name].lowerCase() eq name.value.lowercase()
        }.orderBy(id to SortOrder.ASC).map { it }
}