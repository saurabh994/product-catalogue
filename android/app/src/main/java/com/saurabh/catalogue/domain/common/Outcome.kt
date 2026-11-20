package com.saurabh.catalogue.domain.common

/** Result of a data operation. Repositories return this instead of throwing. */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError) : Outcome<Nothing>
}

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

/** Every failure the UI needs to tell apart. Mapped from HTTP/network errors in the data layer. */
sealed interface AppError {
    /** No connection, timeout, or the backend isn't running. */
    data object Network : AppError

    data object NotFound : AppError

    data object RateLimited : AppError

    /** Server rejected input; keyed by field name (e.g. "email"). */
    data class Validation(val fieldErrors: Map<String, String>) : AppError

    /** Backend or its upstream (product source, email) failed. */
    data class Server(val message: String?) : AppError

    /** Anything we didn't expect, e.g. a response we couldn't parse. */
    data object Unexpected : AppError
}
