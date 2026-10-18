package com.musicdownloader.converter.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SsrfValidatorTest {

    @Test
    fun testSafeUrl() {
        val safeUrl = "https://example.com/audio.mp3"
        assertTrue(SsrfValidator.isSafeUrl(safeUrl))
    }

    @Test
    fun testBlockedLocalhostUrl() {
        val unsafeUrl = "http://localhost:8080/admin"
        assertFalse(SsrfValidator.isSafeUrl(unsafeUrl))
    }

    @Test
    fun testBlockedIpUrl() {
        val unsafeUrl = "http://127.0.0.1/secret"
        assertFalse(SsrfValidator.isSafeUrl(unsafeUrl))
    }
}
