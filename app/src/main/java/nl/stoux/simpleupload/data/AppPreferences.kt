package nl.stoux.simpleupload.data

import android.content.SharedPreferences

private const val ENDPOINT = "api-endpoint"
private const val UPLOAD_KEY = "api-upload-key";

class AppPreferences(private val pref: SharedPreferences) {

    fun getUploadKey(): String? {
        return pref.getString(UPLOAD_KEY, null)
    }

    fun setUploadKey(key: String) {
        with(pref.edit()) {
            putString(UPLOAD_KEY, key)
            apply()
        }
    }

    fun getEndpoint(): String? {
        return pref.getString(ENDPOINT, null)
    }

    fun setEndpoint(key: String) {
        with(pref.edit()) {
            putString(ENDPOINT, key)
            apply()
        }
    }


}