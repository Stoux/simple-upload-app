package nl.stoux.simpleupload.data

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.Exception

class Api(
    private val appPreferences: AppPreferences,
    private val client: HttpClient
) {

    suspend fun upload(
        filename: String,
        byteArray: ByteArray,
        progress: ((sent: Long, total: Long) -> Unit)?
    ): ApiResult<UploadResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response: UploadResult = client.submitFormWithBinaryData(
                    url = appPreferences.getEndpoint() + "/api/uploads",
                    formData = formData {
                        //the line below is just an example of how to send other parameters in the same request
                        append("file", byteArray, Headers.build {
                            append(HttpHeaders.ContentDisposition, "filename=" + filename)
                        })
                    }
                ) {
                    addUploadKeyHeader()

                    if (progress != null) {
                        onUpload(progress)
                    }
                }.body()
                ApiResult.Success(response)
            } catch (exception: Exception) {
                ApiResult.Error(exception)
            }
        }
    }

    suspend fun listUploads(): ApiResult<List<String>> {
        return listFiles("uploads")
    }

    suspend fun listSavedFiles(): ApiResult<List<String>> {
        return listFiles("saved")
    }

    suspend fun listSavedDirs(): ApiResult<List<String>> {
        return listFiles("saved?dirs=1")
    }

    suspend fun moveFile(upload: String, saved: String): ApiResult<MovedResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response: MovedResult = client.submitForm(
                    appPreferences.getEndpoint() + "/api/saved",
                    formParameters = Parameters.build {
                        append("upload", upload);
                        append("saved", saved)
                    }) {
                    addUploadKeyHeader()
                }.body()
                ApiResult.Success(response)
            } catch (exception: Exception) {
                ApiResult.Error(exception)
            }
        }
    }

    private suspend fun listFiles(endpoint: String): ApiResult<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val response: List<String> =
                    client.get(appPreferences.getEndpoint() + "/api/$endpoint") {
                        addUploadKeyHeader()
                    }.body()
                ApiResult.Success(response)
            } catch (exception: Exception) {
                ApiResult.Error(exception)
            }
        }
    }

    private fun HttpRequestBuilder.addUploadKeyHeader() {
        appPreferences.getUploadKey()?.let { header("X-Upload-Key", it) }
    }

}

data class UploadResult(
    val file: String
)

data class MovedResult(
    val moved: String
)

sealed class ApiResult<out R> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Exception) : ApiResult<Nothing>()
}