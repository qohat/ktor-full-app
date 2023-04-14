package com.qohat.entities

import com.qohat.domain.Name
import com.qohat.domain.NewPaginationParams
import com.qohat.domain.Product
import com.qohat.domain.ProductId
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
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object Products: Table("products") {
    val id = long("id").autoIncrement()
    val name = varchar("name", 255)
    val crop_group_id = (integer("crop_group_id") references Lists.id)
    val percentage = decimal("percentage", 5, 4) // Could be one to many
    val maximum_to_subsidize = decimal("maximum_to_subsidize", 7, 0)
    val minimum_to_apply = integer("minimum_to_apply")
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
            percentage,
            maximum_to_subsidize,
            minimum_to_apply,
            active,
            created_at
        )

    fun insertQuery(product: Product): Long =
        insert {
            it[name] = product.name.value
            it[crop_group_id] = product.cropGroup.id
            it[percentage] = product.percentage.value
            it[maximum_to_subsidize] = product.maximumToSubsidize.value
            it[minimum_to_apply] = product.minimumToApply.value
            it[active] = true
            it[created_at] = LocalDateTime.now()
            it[updated_at] = LocalDateTime.now()
        } get id

    fun updateQuery(productId: ProductId, product: Product): Int =
        update ({ id.eq(productId.value) }) {
            it[name] = product.name.value
            it[crop_group_id] = product.cropGroup.id
            it[percentage] = product.percentage.value
            it[maximum_to_subsidize] = product.maximumToSubsidize.value
            it[minimum_to_apply] = product.minimumToApply.value
            it[updated_at] = LocalDateTime.now()
        }

    fun deleteQuery(productId: ProductId): Int =
        update ({ id.eq(productId.value) }) {
            it[active] = false
            it[updated_at] = LocalDateTime.now()
        }

    fun selectBy(productId: ProductId): ResultRow? {
        return query
            .select { id.eq(productId.value) }
            .singleOrNull()
    }

    fun selectAll(): List<ResultRow> =
        query.selectAll()
        .orderBy(id to SortOrder.ASC)
        .map { it }

    fun selectAll(params: NewPaginationParams): List<ResultRow> {
        val select = query.selectAll()
        params.text?.let {
            val text = it.value.lowercase()
            select.andWhere {
                name.lowerCase() like "%$text%"
            }.orWhere {
                Lists.Chain[Lists.name].lowerCase() like "%$text%"
            }.orWhere {
                Lists.CropGroup[Lists.name].lowerCase() like "%$text%"
            }
        }

        return select
            .orderBy(id to SortOrder.ASC)
            .limit(params.pagination.limit.value, params.pagination.offset.value)
            .map { it }
    }

    fun selectAll(name: Name): List<ResultRow> =
        query.select {
            Lists.CropGroup[Lists.name].lowerCase() eq name.value.lowercase()
        }.orderBy(id to SortOrder.ASC).map { it }
}