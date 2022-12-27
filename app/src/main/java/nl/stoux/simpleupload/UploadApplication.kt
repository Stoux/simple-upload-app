package nl.stoux.simpleupload

import android.app.Application
import nl.stoux.simpleupload.data.AppContainer

class UploadApplication: Application() {

    lateinit var container : AppContainer;


    override fun onCreate() {
        super.onCreate()

        container = AppContainer(this)
    }


}