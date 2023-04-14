package com.qohat.repo

import arrow.core.Either
import com.qohat.entities.Config
import com.qohat.entities.ConfigName
import com.qohat.entities.ConfigType
import com.qohat.entities.ConfigValue
import com.qohat.entities.ConfigValues
import com.qohat.entities.Configs
import com.qohat.error.DomainError
import org.jetbrains.exposed.sql.Database

interface ConfigRepo {
    suspend fun findAll(): Either<DomainError, Map<ConfigName, ConfigValue>>
    suspend fun findBy(name: ConfigName): Either<DomainError, Config?>
    suspend fun findBy(type: ConfigType): Either<DomainError, Map<ConfigName, ConfigValue>>
    suspend fun save(configs: Configs): Either<DomainError, Unit>
}

class DefaultConfigRepo(val db: Database): ConfigRepo {
    override suspend fun findAll(): Either<DomainError, Map<ConfigName, ConfigValue>> =
        transact(db) {
            ConfigValues.selectQuery()
                .associate { Pair(ConfigName(it[ConfigValues.name]), ConfigValue(it[ConfigValues.value])) }
        }

    override suspend fun findBy(name: ConfigName): Either<DomainError, Config?> =
        transact(db) {
            ConfigValues.selectQueryBy(name)?.let { Config(name, ConfigValue(it[ConfigValues.value])) }
        }

    override suspend fun findBy(type: ConfigType): Either<DomainError, Map<ConfigName, ConfigValue>> =
        transact(db) {
            val all = ConfigValues.selectQuery()
            when(type) {
                ConfigType.DeptPercentage -> all.filter { it[ConfigValues.name].startsWith("perc_") }
                ConfigType.FullBudget -> all.filter { it[ConfigValues.name] == "full_budget" }
                ConfigType.PartialBudget -> all.filter { it[ConfigValues.name] == "partial_budget" }
                ConfigType.ChainBudget -> all.filter { it[ConfigValues.name] == "budget_Sector Agrícola" || it[ConfigValues.name] == "budget_Sector Pecuario" }
                ConfigType.SubsidizePecentage -> all.filter { it[ConfigValues.name] == "percentage_to_subsidize" }
            }.associate { Pair(ConfigName(it[ConfigValues.name]), ConfigValue(it[ConfigValues.value])) }
        }

    override suspend fun save(configs: Configs): Either<DomainError, Unit> =
        transact(db) {
            ConfigValues.updateQuery(configs)
        }.void()

}