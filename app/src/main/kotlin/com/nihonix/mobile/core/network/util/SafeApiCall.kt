package com.nihonix.mobile.core.network.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nihonix.mobile.core.network.dto.ApiResponseDto
import com.nihonix.mobile.core.result.Resource
import retrofit2.Response
import java.io.IOException
import kotlinx.coroutines.CancellationException

suspend inline fun <T, R> safeApiCall(
    gson: Gson,
    crossinline apiCall: suspend () -> Response<ApiResponseDto<T>>,
    crossinline transform: (T) -> R
): Resource<R> {
    return try {
        val response = apiCall()
        val statusCode = response.code()

        if (response.isSuccessful) {
            val body = response.body()
            if (body != null && body.success) {
                if (body.data != null) {
                    Resource.Success(transform(body.data), body.message)
                } else {
                    Resource.Empty
                }
            } else {
                val errorMsg = body?.extractErrorMessage() ?: "Request was not successful"
                Resource.Error(
                    message = errorMsg,
                    statusCode = body?.statusCode ?: statusCode,
                    validationErrors = body?.extractValidationErrors()
                )
            }
        } else {
            val errorBodyString = response.errorBody()?.string()
            if (!errorBodyString.isNullOrBlank()) {
                try {
                    val type = object : TypeToken<ApiResponseDto<Any>>() {}.type
                    val errorEnvelope: ApiResponseDto<Any> = gson.fromJson(errorBodyString, type)
                    Resource.Error(
                        message = errorEnvelope.extractErrorMessage(),
                        statusCode = errorEnvelope.statusCode ?: statusCode,
                        validationErrors = errorEnvelope.extractValidationErrors()
                    )
                } catch (parseEx: Exception) {
                    Resource.Error(
                        message = "HTTP $statusCode: ${response.message().ifBlank { "Error occurred" }}",
                        statusCode = statusCode
                    )
                }
            } else {
                Resource.Error(
                    message = "HTTP $statusCode: ${response.message().ifBlank { "Error occurred" }}",
                    statusCode = statusCode
                )
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        Resource.Error(
            message = "Unable to connect to server. Please check your network connection.",
            statusCode = null
        )
    } catch (e: Exception) {
        Resource.Error(
            message = e.localizedMessage ?: "An unexpected error occurred",
            statusCode = null
        )
    }
}
