package com.tubetone.core.cache

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CachePolicyTest {
    @get:Rule val tmp = TemporaryFolder()

    @Test fun `evicts oldest files when over limit, skipping protected paths`() {
        val dir = tmp.newFolder("originals")
        val fileA = dir.resolve("a.m4a").apply { writeBytes(ByteArray(400)); setLastModified(1_000L) }
        val fileB = dir.resolve("b.m4a").apply { writeBytes(ByteArray(400)); setLastModified(2_000L) }
        val fileC = dir.resolve("c.m4a").apply { writeBytes(ByteArray(400)); setLastModified(3_000L) }

        val result = CachePolicy.enforce(dir, maxBytes = 800, protectedPaths = setOf(fileC.absolutePath))

        assertFalse("oldest should be evicted", fileA.exists())
        assertTrue("middle should survive (under budget)", fileB.exists())
        assertTrue("protected should survive", fileC.exists())
        assertEquals(1, result.evicted.size)
    }

    @Test fun `under limit returns empty evictions`() {
        val dir = tmp.newFolder()
        dir.resolve("x.m4a").writeBytes(ByteArray(100))
        val result = CachePolicy.enforce(dir, maxBytes = 1_000, protectedPaths = emptySet())
        assertTrue(result.evicted.isEmpty())
    }
}
