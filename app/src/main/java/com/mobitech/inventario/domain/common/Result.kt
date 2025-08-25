package com.mobitech.inventario.domain.common

sealed class Result<out T> {
    data class Success<T>(val data: T): Result<T>()
    data class Error(val message: String, val cause: Throwable? = null): Result<Nothing>()
}

inline suspend fun <T> resultOf(block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: Throwable) { Result.Error(e.message ?: "erro", e) }
