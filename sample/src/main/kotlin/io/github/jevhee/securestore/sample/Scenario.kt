package io.github.jevhee.securestore.sample

internal data class Scenario(
    val name: String,
    val description: String,
    val execute: suspend ScenarioContext.() -> Unit,
)
