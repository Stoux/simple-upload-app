package nl.stoux.simpleupload.data

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import nl.stoux.simpleupload.BuildConfig

class AppContainer(private val applicationContext: Context) {

    val client = HttpClient(CIO) {
        expectSuccess = true
        install(ContentNegotiation) {
            gson()
        }
    }


    val api: Api by lazy {
        Api(this.appPreferences, this.client);
    }

    val appPreferences: AppPreferences by lazy {
        AppPreferences(
            applicationContext.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
        )
    }

    

}