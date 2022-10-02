package dev.daryl.downloadnotification.data.remote.apis

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface AppApis {
    @Streaming
    @GET
    suspend fun getVideoSuspended(@Url url: String?): Response<ResponseBody>
}