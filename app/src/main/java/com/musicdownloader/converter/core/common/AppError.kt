package com.musicdownloader.converter.core.common

sealed class AppError(open val message: String, open val cause: Throwable? = null) {
    data class InvalidUrl(override val message: String) : AppError(message)
    data class BlockedHost(override val message: String) : AppError(message)
    data class NetworkError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class StorageError(override val message: String) : AppError(message)
    data class ConversionError(override val message: String, override val cause: Throwable? = null) : AppError(message, cause)
    data class DrmNotSupported(override val message: String = "Contenido protegido por DRM no soportado.") : AppError(message)
}
