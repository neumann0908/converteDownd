package com.musicdownloader.converter.core.common

sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val error: AppError) : Result<Nothing>
    object Loading : Result<Nothing>
}
