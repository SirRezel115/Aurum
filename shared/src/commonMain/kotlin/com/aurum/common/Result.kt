package com.aurum.common

/**
 * Standard error-handling wrapper used across the app.
 * Replaces raw try/catch with a composable result type.
 */
sealed interface Result<out T> {

    /** Request is in progress. */
    object Loading : Result<Nothing>

    /** Request succeeded with the given data. */
    data class Success<T>(val data: T) : Result<T>

    /** Request failed with the given error. */
    data class Error(val exception: Throwable) : Result<Nothing>

    // ── Transform helpers ──────────────────────────────────────

    /** Transform the success value; propagates Loading and Error unchanged. */
    fun <R> map(transform: (T) -> R): Result<R> =
        when (this) {
            is Success -> Success(transform(data))
            is Loading -> Loading
            is Error   -> this
        }

    /** Transform with a function that itself returns a Result. */
    fun <R> flatMap(transform: (T) -> Result<R>): Result<R> =
        when (this) {
            is Success -> transform(data)
            is Loading -> Loading
            is Error   -> this
        }

    /** Safely extract the success value, returning null for other states. */
    fun getOrNull(): T? =
        when (this) {
            is Success -> data
            else       -> null
        }

    /** Convenience: true when this is a Success. */
    val isSuccess: Boolean get() = this is Success

    /** Convenience: true when this is an Error. */
    val isError: Boolean get() = this is Error
}

/** Run a side-effect block on success; returns this. */
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

/** Run a side-effect block on error; returns this. */
inline fun <T> Result<T>.onError(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Error) action(exception)
    return this
}

/** Create a Result.Success in a single expression. */
fun <T> resultOf(data: T): Result<T> = Result.Success(data)

/** Run a suspending block and wrap any thrown exception as Result.Error. */
suspend fun <T> safeExec(block: () -> T): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Throwable) {
        Result.Error(e)
    }
}
