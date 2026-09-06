package io.github.jevhee.securestore

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureStoreInstrumentedTest {
    @Test fun valuesRoundTripAndWrongTypeFails() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = SecureStore.open(context, "test_${UUID.randomUUID()}")

        assertEquals(SecureStoreResult.Success(Unit), store.put("name", "Ada"))
        assertEquals(SecureStoreResult.Success("Ada"), store.getString("name"))
        assertEquals(SecureStoreResult.Failure(SecureStoreError.TypeMismatch), store.getInt("name"))

        val bytes = byteArrayOf(1, 2, 3)
        assertEquals(SecureStoreResult.Success(Unit), store.put("bytes", bytes))
        val storedBytes = (store.getBytes("bytes") as SecureStoreResult.Success).value
        assertArrayEquals(bytes, storedBytes)
        assertTrue(store.contains("bytes") == SecureStoreResult.Success(true))
    }

    @Test fun rotationLazilyMigratesReadableData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val store = SecureStore.open(context, "rotation_${UUID.randomUUID()}")

        store.put("secret", "before")
        val report = store.rotateKey()

        assertEquals(SecureStoreResult.Success(KeyRotationReport(1, 2)), report)
        assertEquals(SecureStoreResult.Success("before"), store.getString("secret"))
    }
}
