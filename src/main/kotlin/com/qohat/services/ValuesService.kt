package com.qohat.services

object ValuesService {
    val baseSalary = mapOf("2021" to 908526, "2022" to 1000000)
    val arlRisk = mapOf(
        "Riesgo I" to 0.00522,
        "Riesgo II" to 0.01044,
        "Riesgo III" to 0.02435999,
        "Riesgo IV" to 0.0435,
        "Riesgo V" to 0.0696
    )
    val deductiblePercentage = mapOf(
        "health" to 0.085,
        "pension" to 0.12,
        "ccf" to 0.04,
        "transport" to 0.117172,
        "global" to 0.55
    )
}