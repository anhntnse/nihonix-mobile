package com.nihonix.mobile.core.result

/**
 * Generic Resource class for Clean Architecture data flow.
 * Handles Loading, Success, Empty, and Error states across layers.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T, val message: String? = null) : Resource<T>()
    data class Error(
        val message: String,
        val statusCode: Int? = null,
        val validationErrors: List<String>? = null
    ) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    data object Empty : Resource<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading
    val isEmpty: Boolean get() = this is Empty

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
}
