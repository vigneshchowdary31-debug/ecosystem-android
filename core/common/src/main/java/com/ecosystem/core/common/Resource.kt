package com.ecosystem.core.common

sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val exception: Throwable, val message: String? = exception.message) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}
