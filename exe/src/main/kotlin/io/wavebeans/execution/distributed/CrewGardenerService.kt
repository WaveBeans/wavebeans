package io.wavebeans.execution.distributed

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.wavebeans.execution.BushKey
import io.wavebeans.execution.JobKey
import io.wavebeans.execution.TopologySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okhttp3.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES

fun <T> Response<T>.throwIfError(): Response<T> {
    if (!this.isSuccessful) throw IllegalStateException("Unsuccessful request: $this")
    return this
}

interface CrewGardenerService {

    companion object {

        private val connectionPool = ConnectionPool(10, 30, MINUTES)

        fun create(endpoint: String): CrewGardenerService {
            val json = Json(JsonConfiguration.Stable, TopologySerializer.paramsModule)
            val client = OkHttpClient.Builder()
                    .callTimeout(60000, MILLISECONDS)
                    .connectTimeout(60000, MILLISECONDS)
                    .connectionPool(connectionPool)
                    .build()
            val retrofit = Retrofit.Builder()
                    .baseUrl(endpoint)
                    .addConverterFactory(json.asConverterFactory(MediaType.get("application/json")))
                    .client(client)
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

    @PUT("/job")
    fun startJob(
            @Query("jobKey") jobKey: JobKey
    ): Call<ResponseBody>

    @DELETE("/job")
    fun stopJob(
            @Query("jobKey") jobKey: JobKey
    ): Call<ResponseBody>

    @GET("/job/status")
    fun jobStatus(
            @Query("jobKey") jobKey: JobKey
    ): Call<List<JobStatus>>

    @Multipart
    @POST("/code/upload")
    fun uploadCode(
            @Part("jobKey") jobKey: RequestBody,
            @Part code: MultipartBody.Part
    ): Call<ResponseBody>

    @POST("/bush")
    fun plantBush(
            @Body request: PlantBushRequest
    ): Call<ResponseBody>

    @POST("/bush/endpoints")
    fun registerBushEndpoints(
            @Body request: RegisterBushEndpointsRequest
    ): Call<ResponseBody>
}