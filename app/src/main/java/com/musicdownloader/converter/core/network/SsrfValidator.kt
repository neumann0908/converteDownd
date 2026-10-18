package com.musicdownloader.converter.core.network

import java.net.InetAddress
import java.net.URL

object SsrfValidator {
    private val BLOCKED_HOSTS = setOf("localhost", "127.0.0.1", "0.0.0.0", "::1")

    fun isSafeUrl(urlString: String): Boolean {
        return try {
            val url = URL(urlString)
            val host = url.host.lowercase()

            if (host in BLOCKED_HOSTS) return false
            if (url.protocol != "http" && url.protocol != "https") return false

            val address = InetAddress.getByName(host)
            !address.isLoopbackAddress && !address.isSiteLocalAddress && !address.isAnyLocalAddress
        } catch (e: Exception) {
            false
        }
    }
}
