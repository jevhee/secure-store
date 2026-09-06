package io.github.jevhee.securestore.sample

import android.content.Context
import io.github.jevhee.securestore.SecureStore
import io.github.jevhee.securestore.SecureStoreClient
import io.github.jevhee.securestore.SecureStoreError
import io.github.jevhee.securestore.SecureStoreResult

internal class ScenarioContext(private val applicationContext: Context) {
    fun newStore(label: String): SecureStoreClient = SecureStore.open(
        context = applicationContext,
        namespace = "sample_$label",
    )

    fun <T> expectSuccess(result: SecureStoreResult<T>): T = when (result) {
        is SecureStoreResult.Success -> result.value
        is SecureStoreResult.Failure -> fail("Expected success but received ${result.error.safeName()}.")
    }

    fun <T> expectValue(result: SecureStoreResult<T>, expected: T) {
        val actual = expectSuccess(result)
        check(actual == expected) { "Decrypted value did not match the expected value." }
    }

    fun expectFailure(result: SecureStoreResult<*>, expected: SecureStoreError) {
        check(result is SecureStoreResult.Failure) { "Expected an explicit failure." }
        check(result.error == expected) {
            "Expected ${expected.safeName()} but received ${result.error.safeName()}."
        }
    }

    fun fail(message: String): Nothing = throw ScenarioAssertionError(message)
}

private fun SecureStoreError.safeName(): String = this::class.java.simpleName
