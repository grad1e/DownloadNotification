package dev.daryl.downloadnotification.data.repo

import dev.daryl.downloadnotification.data.remote.apis.AppApis
import dev.daryl.downloadnotification.utils.network.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Singleton

@Singleton
class AppRepository(private val appApis: AppApis) : ApiResponse(){

    suspend fun getVideoSuspended(url: String?) =
        flow {
            emit(
                safeApiCall {
                    appApis.getVideoSuspended(url)
                }
            )
        }.flowOn(Dispatchers.IO)
}