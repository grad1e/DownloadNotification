package dev.daryl.downloadnotification.utils.network

import retrofit2.Response


abstract class ApiResponse {
    suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): Resource<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                Resource.Success(body)
            } else {
                val errorBody = response.errorBody()
                Resource.Error(response.message(), errorBody)
            }
        } catch (e: Exception) {
            Resource.Error(e.message)
        }
    }
}

