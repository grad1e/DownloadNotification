package dev.daryl.downloadnotification.utils.network

import okhttp3.ResponseBody

sealed class Resource<T> {
    class Loading<T> : Resource<T>()

    data class Success<T>(val data: T?) : Resource<T>()

    data class Error<T>(val errorMessage: String?, val data: ResponseBody? = null) : Resource<T>()
}
