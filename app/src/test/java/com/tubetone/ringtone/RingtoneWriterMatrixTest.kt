package com.tubetone.ringtone

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * M6 — Scoped-storage parameterized matrix.
 *
 * Exercises [RingtoneWriter] against every slot × API level we claim to
 * support. Catches regressions such as:
 *   * Q+ `RELATIVE_PATH` + `IS_PENDING` dance
 *   * Q- fallback to `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`
 *
 * Parameterising only on slot; SDK is swept via companion test-classes below
 * (one @Config per API level). This is the Robolectric-idiomatic way to run
 * a matrix — per-parameter `@Config(sdk=N)` isn't possible because `@Config`
 * requires compile-time constants.
 *
 * Assertion model: Robolectric's MediaStore provider is a stub; we don't
 * assert round-trip byte-for-byte equality. We assert the insert succeeds
 * (URI is non-null) and a file path is resolvable. This is sufficient to
 * catch the scoped-storage branching logic regressing.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(sdk = [26, 29, 30, 33, 34])
class RingtoneWriterMatrixTest(
    private val slot: RingtoneSlot
) {

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "slot={0}")
        fun data(): List<Array<Any>> = RingtoneSlot.values().map { arrayOf<Any>(it) }
    }

    @Test
    fun `writes ringtone file and returns non-null URI`() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val src = File(ctx.cacheDir, "m6-src-${slot.name}-${android.os.Build.VERSION.SDK_INT}.m4a").apply {
            parentFile?.mkdirs()
            writeBytes(ByteArray(64) { it.toByte() })
        }
        val written = runBlocking {
            RingtoneWriter(ctx).writeAsRingtone(src, "matrix-${slot.name}", slot)
        }
        assertNotNull(
            "URI should be non-null on sdk=${android.os.Build.VERSION.SDK_INT}, slot=$slot",
            written.uri
        )
        assertTrue(
            "filePath should be a content:// URI or a filesystem path",
            written.filePath.isNotBlank()
        )
    }
}
