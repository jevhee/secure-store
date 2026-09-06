package io.github.jevhee.securestore.sample

import android.content.Context
import android.os.SystemClock
import io.github.jevhee.securestore.SecureStoreError
import io.github.jevhee.securestore.SecureStoreResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal class ScenarioRunner(private val applicationContext: Context) {
    val scenarios: List<Scenario> = listOf(
        Scenario(
            name = "String round-trip",
            description = "Encrypts, stores, decrypts, and compares a String.",
        ) {
            val store = newStore("string")
            expectSuccess(store.put("access_token", "sample-token"))
            expectValue(store.getString("access_token"), "sample-token")
        },
        Scenario(
            name = "Primitive types",
            description = "Checks Int, Long, Float, Double, and Boolean codecs.",
        ) {
            val store = newStore("primitives")
            expectSuccess(store.put("int", Int.MIN_VALUE))
            expectSuccess(store.put("long", Long.MAX_VALUE))
            expectSuccess(store.put("float", 1.25f))
            expectSuccess(store.put("double", -5.75))
            expectSuccess(store.put("boolean", true))
            expectValue(store.getInt("int"), Int.MIN_VALUE)
            expectValue(store.getLong("long"), Long.MAX_VALUE)
            expectValue(store.getFloat("float"), 1.25f)
            expectValue(store.getDouble("double"), -5.75)
            expectValue(store.getBoolean("boolean"), true)
        },
        Scenario(
            name = "ByteArray round-trip",
            description = "Checks binary data and defensive output ownership.",
        ) {
            val store = newStore("bytes")
            val expected = byteArrayOf(0, 1, 2, 127, -1)
            expectSuccess(store.put("binary", expected))
            val actual = expectSuccess(store.getBytes("binary"))
                ?: fail("Expected binary entry to exist.")
            check(actual.contentEquals(expected)) { "Decrypted bytes did not match." }
            actual.fill(42)
            val reread = expectSuccess(store.getBytes("binary"))
                ?: fail("Expected binary entry to remain readable.")
            check(reread.contentEquals(expected)) { "Returned ByteArray leaked mutable ownership." }
        },
        Scenario(
            name = "Overwrite",
            description = "Writes the same logical key twice and reads the newest value.",
        ) {
            val store = newStore("overwrite")
            expectSuccess(store.put("session", "first"))
            expectSuccess(store.put("session", "second"))
            expectValue(store.getString("session"), "second")
        },
        Scenario(
            name = "Missing entry",
            description = "Confirms a missing key returns Success(null), not an error.",
        ) {
            val store = newStore("missing")
            expectValue<String?>(store.getString("does_not_exist"), null)
            expectValue(store.contains("does_not_exist"), false)
        },
        Scenario(
            name = "Wrong type",
            description = "Confirms a getter with the wrong type fails explicitly.",
        ) {
            val store = newStore("wrong_type")
            expectSuccess(store.put("typed_value", "text"))
            expectFailure(store.getInt("typed_value"), SecureStoreError.TypeMismatch)
        },
        Scenario(
            name = "Remove entry",
            description = "Removes one entry without affecting another entry.",
        ) {
            val store = newStore("remove")
            expectSuccess(store.put("remove_me", "value"))
            expectSuccess(store.put("keep_me", "value"))
            expectSuccess(store.remove("remove_me"))
            expectValue(store.contains("remove_me"), false)
            expectValue(store.contains("keep_me"), true)
        },
        Scenario(
            name = "Clear namespace",
            description = "Clears entries only within the selected namespace.",
        ) {
            val store = newStore("clear")
            expectSuccess(store.put("one", 1))
            expectSuccess(store.put("two", 2))
            expectSuccess(store.clear())
            expectValue(store.contains("one"), false)
            expectValue(store.contains("two"), false)
        },
        Scenario(
            name = "Namespace isolation",
            description = "Stores the same logical key independently in two namespaces.",
        ) {
            val first = newStore("namespace_a")
            val second = newStore("namespace_b")
            expectSuccess(first.put("shared_key", "alpha"))
            expectSuccess(second.put("shared_key", "beta"))
            expectValue(first.getString("shared_key"), "alpha")
            expectValue(second.getString("shared_key"), "beta")
        },
        Scenario(
            name = "Concurrent writes",
            description = "Runs 20 writes concurrently and verifies every entry.",
        ) {
            val store = newStore("concurrent")
            coroutineScope {
                (0 until 20).map { index ->
                    async { expectSuccess(store.put("item_$index", index)) }
                }.awaitAll()
            }
            for (index in 0 until 20) {
                expectValue(store.getInt("item_$index"), index)
            }
        },
        Scenario(
            name = "Key rotation",
            description = "Rotates the AES key and lazily migrates an older entry on read.",
        ) {
            val store = newStore("rotation")
            expectSuccess(store.put("before_rotation", "value"))
            val report = expectSuccess(store.rotateKey())
            check(report.activeVersion == report.previousVersion + 1) {
                "Unexpected key rotation versions."
            }
            expectValue(store.getString("before_rotation"), "value")
            expectSuccess(store.put("after_rotation", "value"))
            expectValue(store.getString("after_rotation"), "value")
        },
        Scenario(
            name = "Invalid input",
            description = "Confirms a blank logical key is rejected without writing data.",
        ) {
            val store = newStore("invalid_input")
            val result = store.put("   ", "value")
            check(result is SecureStoreResult.Failure && result.error is SecureStoreError.InvalidInput) {
                "Expected InvalidInput for a blank key."
            }
        },
    )

    suspend fun run(scenario: Scenario): ScenarioResult {
        val startedAt = SystemClock.elapsedRealtime()
        val context = ScenarioContext(applicationContext)
        return try {
            scenario.execute(context)
            ScenarioResult(
                passed = true,
                durationMillis = SystemClock.elapsedRealtime() - startedAt,
                detail = "All assertions passed.",
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (throwable: Throwable) {
            ScenarioResult(
                passed = false,
                durationMillis = SystemClock.elapsedRealtime() - startedAt,
                detail = throwable.message ?: throwable::class.java.simpleName,
            )
        }
    }
}
