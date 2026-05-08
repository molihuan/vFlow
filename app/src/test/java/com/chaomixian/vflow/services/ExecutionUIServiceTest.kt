package com.chaomixian.vflow.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExecutionUIServiceTest {

    @Test
    fun limitQuickViewContentForIntent_keepsShortContent() {
        val content = "hello"

        assertEquals(
            content,
            ExecutionUIService.limitQuickViewContentForIntent(content) { limit, total ->
                "[truncated $limit/$total]"
            }
        )
    }

    @Test
    fun limitQuickViewContentForIntent_truncatesLongContent() {
        val content = "a".repeat(50_001)
        val limited = ExecutionUIService.limitQuickViewContentForIntent(content) { limit, total ->
            "[truncated $limit/$total]"
        }

        assertTrue(limited.startsWith("a".repeat(50_000)))
        assertTrue(!limited.startsWith("a".repeat(50_001)))
        assertTrue(limited.contains("[truncated 50000/50001]"))
    }
}
