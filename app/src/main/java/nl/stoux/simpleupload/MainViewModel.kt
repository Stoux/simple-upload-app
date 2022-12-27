package nl.stoux.simpleupload

import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.stoux.simpleupload.data.ApiResult
import nl.stoux.simpleupload.data.AppContainer

data class ViewModelState(
    val hasCheckedIntent: Boolean = false,
    val currentView: State = State.OVERVIEW,
    val selectedFile: Uri? = null,
    val selectedFilename: String? = null,
    val isUploading: Boolean = false,
    val isMoving: Boolean = false,
    val uploadProgress: Int = 0,
    val uploadedFile: String? = null,
    val savedDirs: List<String>? = null,

    val error: String? = null,
)


class MainViewModel(
    private val application: UploadApplication,
    private val appContainer: AppContainer,
) : AndroidViewModel(application) {

    private val viewModelState = MutableStateFlow(ViewModelState())

    val uiState = viewModelState.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        viewModelState.value
    )

    fun selectedFile(uri: Uri?, name: String?) {
        viewModelState.update { it.copy(
            selectedFile = uri,
            selectedFilename = name,
            currentView = if ( uri != null && name != null ) {
                State.FILE_SELECTED
            } else {
                State.OVERVIEW
            }
        ) }
    }

    fun upload() {
        val selectedFile = viewModelState.value.selectedFile ?: return;
        val selectedFilename = viewModelState.value.selectedFilename ?: return;

        viewModelState.update { it.copy(isUploading = true, uploadProgress = 0) }
        viewModelScope.launch {

            val inputStream = application.contentResolver.openInputStream(selectedFile)
            if (inputStream == null) {
                viewModelState.update {
                    it.copy(
                        isUploading = false,
                        error = "Failed to open file?"
                    )
                }
                return@launch
            }

            try {
                when (val uploadResult = appContainer.api.upload(
                    filename = selectedFilename,
                    byteArray = inputStream.readBytes(),
                    progress = { sent, total ->
                        val percentage = ((sent.toDouble() / total) * 100).toInt()
                        if (viewModelState.value.uploadProgress != percentage) {
                            viewModelState.update { it.copy(uploadProgress = percentage) }
                        }
                    })) {
                    is ApiResult.Success -> {
                        viewModelState.update {
                            it.copy(
                                isUploading = false,
                                uploadProgress = 100,
                                uploadedFile = uploadResult.data.file
                            )
                        }

                        if (viewModelState.value.savedDirs == null) {
                            when (val dirs = appContainer.api.listSavedDirs()) {
                                is ApiResult.Error -> viewModelState.update { it.copy(error = "Failed to get upload dirs") }
                                is ApiResult.Success -> viewModelState.update { it.copy(savedDirs = dirs.data) }
                            }
                        }

                    }
                    is ApiResult.Error -> viewModelState.update {
                        it.copy(
                            isUploading = false,
                            error = "Error: " + uploadResult.exception.message
                        )
                    }
                }
            } finally {
                withContext(Dispatchers.IO) {
                    inputStream.close()
                }
            }
        }
    }

    fun move(uploadToDir: String, newFileName: String) {
        val uploadedFile = viewModelState.value.uploadedFile ?: return;

        viewModelState.update { it.copy(isMoving = true) }
        viewModelScope.launch {
            when (val result =
                appContainer.api.moveFile(uploadedFile, "$uploadToDir/$newFileName")) {
                is ApiResult.Success -> {
                    viewModelState.update {
                        it.copy(
                            currentView = State.OVERVIEW,
                            isMoving = false,
                            selectedFilename = null,
                            selectedFile = null,
                            uploadProgress = 0,
                            uploadedFile = null,
                            error = null,
                        )
                    }
                    Toast.makeText(application, "File uploaded & moved: " + result.data.moved, Toast.LENGTH_LONG).show()
                }
                is ApiResult.Error -> viewModelState.update {
                    it.copy(
                        isMoving = false,
                        error = "Failed to move: " + result.exception.message
                    )
                }
            }
        }
    }

    fun toView(view: State) {
        viewModelState.update { it.copy(currentView = view) }
    }

    fun setCheckedIntent() {
        viewModelState.update { it.copy( hasCheckedIntent = true ) }
    }


    companion object {
        fun provideFactory(
            application: UploadApplication,
            appContainer: AppContainer
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(
                    application,
                    appContainer,
                ) as T
            }
        }
    }


}