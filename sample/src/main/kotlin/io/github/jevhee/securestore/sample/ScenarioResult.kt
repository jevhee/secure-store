package io.github.jevhee.securestore.sample

internal data class ScenarioResult(
    val passed: Boolean,
    val durationMillis: Long,
    val detail: String,
)
