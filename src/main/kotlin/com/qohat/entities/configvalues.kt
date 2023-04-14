package com.qohat.entities

import arrow.core.Either
import arrow.core.continuations.either
import com.qohat.codec.Codecs
import com.qohat.error.DomainError
import com.qohat.error.PersistConfigError
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@JvmInline
@Serializable
value class ConfigName(val value: String)

@JvmInline
@Serializable
value class ConfigValue(@Serializable(Codecs.BigDecimalSerializer::class)  val value: BigDecimal)

@Serializable
data class Config(val name: ConfigName, val value: ConfigValue)
@Serializable
data class Configs(val values: Map<ConfigName, ConfigValue>)
suspend fun Configs.validate(type: ConfigType): Either<DomainError, Configs> = either {
    val configs = when(type) {
        ConfigType.DeptPercentage -> {
            ensure(values.keys.size == 32) { PersistConfigError("The depts should be 32 and are only ${values.size}, type: $type") }
            ensure(values.keys.all { it.value.startsWith("perc_") }) { PersistConfigError("The dept config should start by perc_ ${values.size}, type: $type") }
            val sum = values.values.toList().map { it.value }.reduce { a, b -> a.add(b) }
            ensure(sum.setScale(6, RoundingMode.HALF_EVEN) == BigDecimal.ONE.setScale(6))
            { PersistConfigError("The sum is not 100%, It is approximately " +
                    "${(sum * BigDecimal(100)).setScale(6, RoundingMode.HALF_EVEN)}%, type: $type") }
            Configs(values)
        }
        ConfigType.FullBudget -> {
            ensure(values.size == 1) { PersistConfigError("FullBudgetConfig map should be one, type: $type") }
            ensure(values.values.first().value > BigDecimal.ZERO) { PersistConfigError("FullBudgetConfig Value should be greather than 0, type: $type") }
            ensure(values.keys.first().value == "full_budget") { PersistConfigError("FullBudgetKeyName is invalid, type: $type") }
            Configs(values)
        }
        ConfigType.PartialBudget -> {
            ensure(values.size == 1) { PersistConfigError("PartialBudgetConfig map should be one, type: $type") }
            ensure(values.values.first().value > BigDecimal.ZERO) { PersistConfigError("PartialBudgetConfig Value should be greather than 0, type: $type") }
            ensure(values.keys.first().value == "partial_budget") { PersistConfigError("PartialBudgetKeyName is invalid, type: $type") }
            Configs(values)
        }
        ConfigType.SubsidizePecentage -> {
            ensure(values.size == 1) { PersistConfigError("SubsidizePecentage map should be one, type: $type") }
            ensure(values.values.first().value > BigDecimal.ZERO) { PersistConfigError("SubsidizePecentage Value should be greather than 0, type: $type") }
            ensure(values.keys.first().value == "percentage_to_subsidize") { PersistConfigError("SubsidizePecentage name is invalid, type: $type") }
            Configs(values)
        }
        ConfigType.ChainBudget -> {
            ensure(values.size == 2) { PersistConfigError("The chains should be 2 and are ${values.size}, type: $type") }
            ensure(values.keys.map { it.value }.containsAll(listOf("budget_Sector Agrícola", "budget_Sector Pecuario"))) { PersistConfigError("The chains config should contains budget_Sector Agrícola and budget_Sector Pecuario, type: $type") }
            val sum = values.values.toList().map { it.value }.reduce { a, b -> a.add(b) }
            ensure(sum.setScale(4, RoundingMode.HALF_EVEN) == BigDecimal.ONE.setScale(4))
            { PersistConfigError("The chains sum is not 100%, It is approximately " +
                    "${(sum * BigDecimal(100)).setScale(4, RoundingMode.HALF_EVEN)}% - type: $type") }
            Configs(values)
        }
    }
    configs
}

enum class ConfigType(val type: String) {
    DeptPercentage("dept-percentage"),
    FullBudget("full-budget"),
    ChainBudget("chain-budget"),
    SubsidizePecentage("percentage-to-subsidize"),
    PartialBudget("partial-budget");

    companion object {
        fun unApply(arg: String): ConfigType? =
            values().find { it.type == arg }
    }
}

object ConfigValues: Table("config_values") {
    val name = varchar("name", 100)
    val value = decimal("value", 7, 6)
    val created_at = datetime("created_at")
    val updated_at = datetime("updated_at")

    override val primaryKey = PrimaryKey(name)

    val query = slice(name, value)

    fun selectQueryBy(configName: ConfigName): ResultRow? =
        query.select { name.eq(configName.value) }.singleOrNull()

    fun selectQuery(): List<ResultRow> =
        query.selectAll().map { it }

    fun updateQuery(config: Config) =
        update({ name.eq(config.name.value) }) {
            it[value] = config.value.value
            it[updated_at] = LocalDateTime.now()
        }

    fun updateQuery(configs: Configs) =
        configs
            .values
            .forEach { (t, u) ->
                update({ name.eq(t.value) }) {
                    it[value] = u.value
                    it[updated_at] = LocalDateTime.now()
                }
            }
}