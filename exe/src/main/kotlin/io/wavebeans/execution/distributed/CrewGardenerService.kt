package io.wavebeans.execution.distributed

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.JobKey
import io.wavebeans.execution.TopologySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.MediaType
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*

fun <T> Response<T>.throwIfError(): Response<T> {
    if (!this.isSuccessful) throw IllegalStateException("Request is on 200: $this")
    return this
}

interface CrewGardenerService {

    companion object {
        fun create(endpoint: String): CrewGardenerService {
            val json = Json(JsonConfiguration.Stable, TopologySerializer.paramsModule)
            val retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
                    .build()
            return retrofit.create(CrewGardenerService::class.java)
        }
    }

    @GET("/bush/call")
    fun call(
            @Query("bushKey") bushKey: BushKey,
            @Query("podId") podId: Int,
            @Query("podPartition") podPartition: Int,
            @Query("request") request: String
    ): Call<ResponseBody>

    @GET("/terminate")
    fun terminate(): Call<ResponseBody>

    @DELETE("/job")
    fun stopJob(
            @Query("jobKey") jobKey: JobKey
    ): Call<ResponseBody>

    @PUT("/job")
    fun startJob(
            @Query("jobKey") jobKey: JobKey
    ): Call<ResponseBody>

    @GET("/job/status")
    fun jobStatus(
            @Query("jobKey") jobKey: JobKey
    ): Call<List<JobStatus>>

    @POST("/bush")
    fun plantBush(
            @Body request: PlantBushRequest
    ): Call<ResponseBody>

    @POST("/bush/endpoints")
    fun registerBushEndpoints(
            @Body request: RegisterBushEndpointsRequest
    ): Call<ResponseBody>
}