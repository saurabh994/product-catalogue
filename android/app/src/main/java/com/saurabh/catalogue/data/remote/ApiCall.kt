package com.saurabh.catalogue.data.remote

import com.saurabh.catalogue.data.remote.dto.ErrorResponseDto
import com.saurabh.catalogue.domain.common.AppError
import com.saurabh.catalogue.domain.common.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Runs an API call and turns every failure into an [AppError]. Cancellation is re-thrown, never swallowed. */
suspend fun <T> apiCall(json: Json, block: suspend () -> T): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        Outcome.Failure(e.toAppError(json))
    } catch (e: IOException) {
        Outcome.Failure(AppError.Network)
    } catch (e: SerializationException) {
        Outcome.Failure(AppError.Unexpected)
    }

internal fun HttpException.toAppError(json: Json): AppError {
    val body = runCatching {
        response()?.errorBody()?.string()?.let { json.decodeFromString(ErrorResponseDto.serializer(), it).error }
    }.getOrNull()

    return when (code()) {
        400 -> AppError.Validation(body?.details.orEmpty().associate { it.field to it.message })
        404 -> AppError.NotFound
        429 -> AppError.RateLimited
        in 500..599 -> AppError.Server(body?.message)
        else -> AppError.Unexpected
    }
}
