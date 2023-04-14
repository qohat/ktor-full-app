package com.qohat.entities

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.alias

object Lists: Table("lists") {

    val id = integer("id").autoIncrement()
    val name = varchar("name", 100)
    val list = varchar("list", 50)
    val active = bool("active").default(true)

    override val primaryKey = PrimaryKey(id)

    // Aliases
    val Locality = Lists.alias("Locality")
    val Bank = Lists.alias("Bank")
    val BankBranch = Lists.alias("BankBranch")
    val AccountBankType = Lists.alias("AccountBankType")
    val DocumentType = Lists.alias("DocumentType")
    val Gender = Lists.alias("Gender")
    val PopulationGroup = Lists.alias("PopulationGroup")
    val EthnicGroup = Lists.alias("EthnicGroup")
    val Disability = Lists.alias("Disability")
    val Sex = Lists.alias("Sex")
    val Department = Lists.alias("Department")
    val City = Lists.alias("City")
    val OrganizationType = Lists.alias("OrganizationType")
    val PaymentType = Lists.alias("PaymentType")
    val Reason = Lists.alias("Reason")
    val Activity = Lists.alias("Activity")
    val Chain = Lists.alias("Chain")
    val CropGroup = Lists.alias("CropGroup")
    val Files = Lists.alias("Files")
    val MeasurementUnit = Lists.alias("MeasurementUnit")
    val Presentation = Lists.alias("Presentation")
}