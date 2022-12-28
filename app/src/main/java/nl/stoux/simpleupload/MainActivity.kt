package nl.stoux.simpleupload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import nl.stoux.simpleupload.ui.theme.SimpleUploadTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.StyledPlayerView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uploadApplication = application as UploadApplication
        val appContainer = uploadApplication.container;
        val pref = appContainer.appPreferences;

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.provideFactory(
                    application = uploadApplication,
                    appContainer = appContainer,
                )
            )

            val uiState by viewModel.uiState.collectAsState()
            val currentView = uiState.currentView

            if (!uiState.hasCheckedIntent) {
                viewModel.setCheckedIntent()
                if (intent?.action == Intent.ACTION_SEND) {
                    (intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
                        Log.d("FILE", "onCreate: With intents")
                        parseUri(it, viewModel)
                    }
                }
            }



            val fileLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    parseUri(uri, viewModel)
                })


            SimpleUploadTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) { // https://9a7b-143-178-38-25.eu.ngrok.io/
                        Row(
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp)
                        ) {

                            Button(
                                onClick = { viewModel.toView(State.OVERVIEW) },
                                enabled = currentView == State.CONFIG
                            ) {
                                Text(text = "Upload")
                            }

                            Button(
                                onClick = { viewModel.toView(State.CONFIG) },
                                enabled = currentView == State.OVERVIEW
                            ) {
                                Text(text = "Settings")
                            }

                        }

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colors.onSurface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (currentView == State.OVERVIEW) {

                            Button(
                                onClick = { fileLauncher.launch("*/*") },
                            ) {
                                Text(text = "Open file")
                            }


                        } else if (currentView == State.CONFIG) {

                            val originalKey = pref.getUploadKey() ?: "";
                            var currentKey = remember { mutableStateOf(originalKey) }

                            Text(text = "API Key")

                            OutlinedTextField(
                                value = currentKey.value,
                                onValueChange = { currentKey.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { pref.setUploadKey(currentKey.value) },
                                enabled = currentKey.value != originalKey,
                            ) {
                                Text(text = "Save")
                            }

                            Divider(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colors.onSurface,
                            )

                            val originalEndpoint = pref.getEndpoint() ?: "";
                            var currentEndpoint = remember { mutableStateOf(originalEndpoint) }

                            Text(text = "API Endpoint without trailing slash!")

                            OutlinedTextField(
                                value = currentEndpoint.value,
                                onValueChange = { currentEndpoint.value = it }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { pref.setEndpoint(currentEndpoint.value) },
                                enabled = currentEndpoint.value != originalEndpoint,
                            ) {
                                Text(text = "Save")
                            }


                        } else if (currentView == State.FILE_SELECTED) {

                            var uploadToDir by remember { mutableStateOf<String?>(null) }
                            var finalFilename by remember { mutableStateOf(uiState.selectedFilename!!) }


                            Text(text = "File selected.")
                            Text(text = uiState.selectedFilename ?: "--ERROR--")

                            if (uiState.isUploading) {
                                Text(text = "Uploading...")
                                CircularProgressIndicator(progress = uiState.uploadProgress.toFloat() / 100)
                            } else if (uiState.isMoving) {
                                Text(text = "Moving file...")
                                CircularProgressIndicator()
                            } else if (uiState.uploadedFile != null) {
                                Spacer(modifier = Modifier.height(8.dp))
//https://5ff2-143-178-38-25.eu.ngrok.io/
                                Text("File uploaded!")
                                Text("Select folder to move to:")

                                var dirsExpanded by remember {
                                    mutableStateOf(false)
                                }

                                Box(
                                    contentAlignment = Alignment.Center
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .padding(4.dp)
                                            .clickable {
                                                dirsExpanded = !dirsExpanded
                                            }
                                            .border(1.dp, Color.Black)
                                    ) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = (uploadToDir ?: "No Folder selected yet"))
                                        Spacer(modifier = Modifier.weight(1f))
                                        IconButton(onClick = {
                                            dirsExpanded = true
                                        }) {
                                            Icon(
                                                imageVector = Icons.Default.MoreVert,
                                                contentDescription = "Open Options"
                                            )
                                        }
                                    }


                                    // drop down menu
                                    DropdownMenu(
                                        expanded = dirsExpanded,
                                        onDismissRequest = {
                                            dirsExpanded = false
                                        }
                                    ) {
                                        // adding items
                                        uiState.savedDirs?.forEachIndexed { itemIndex, itemValue ->
                                            DropdownMenuItem(
                                                onClick = {
                                                    uploadToDir = itemValue
                                                    dirsExpanded = false
                                                },
//                                                enabled = (itemIndex != disabledItem)
                                            ) {
                                                Text(text = itemValue)
                                            }
                                        }
                                    }
                                }

                                if (uploadToDir != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Filename")

                                    OutlinedTextField(
                                        modifier = Modifier.fillMaxWidth(0.9f),
                                        value = finalFilename,
                                        onValueChange = { finalFilename = it },
                                        trailingIcon = {
                                            IconButton(onClick = { finalFilename = "" }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Open Options"
                                                )
                                            }
                                        }
                                    )

                                    var showInvalidDialog by remember{ mutableStateOf(false) }
                                    val invalidFilename = finalFilename.isNotEmpty()
                                            && ( !finalFilename.matches(Regex("^.+\\..+$")) || finalFilename.contains(Regex("\\s") ))

                                    Spacer(modifier = Modifier.height(4.dp))

                                    TextButton(onClick = { finalFilename = viewModel.capitalizeAndClean(finalFilename) }, enabled = invalidFilename) {
                                        Text("Collapse & capitalize")
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        enabled = finalFilename != "",
                                        onClick = {
                                            if (invalidFilename) {
                                                showInvalidDialog = true
                                            } else {
                                                viewModel.move(uploadToDir!!, finalFilename)
                                            }
                                        }
                                    ) {
                                        Text("Move to folder")
                                    }

                                    if (showInvalidDialog) {
                                        AlertDialog(
                                            title = {
                                                Text(
                                                    text = "Are you sure?",
                                                    style = MaterialTheme.typography.h5,
                                                )
                                            },
                                            text = {
                                                Text(text = "The current filename seems to be invalid (spaces or no extension). Are you sure you want to save it like this?")
                                            },
                                            buttons = {
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(
                                                            start = 16.dp,
                                                            end = 16.dp,
                                                            bottom = 16.dp
                                                        ),
                                                ) {
                                                    TextButton(onClick = { showInvalidDialog = false}) {
                                                        Text(text = "Cancel")
                                                    }
                                                    Button(onClick = {
                                                        showInvalidDialog = false
                                                        viewModel.move(uploadToDir!!, finalFilename)
                                                    }) {
                                                        Text(text = "Confirm")
                                                    }
                                                }
                                            },
                                            onDismissRequest = { showInvalidDialog = false }
                                        )
                                    }

                                }

                            } else {
                                Button(onClick = { viewModel.upload() }) {
                                    Text(text = "Upload!")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(onClick = {
                                    viewModel.selectedFile(null, null)
                                }) {
                                    Text(text = "Cancel")
                                }
                            }

                            if (uiState.error != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = uiState.error!!, color = Color.Red)
                            }

                            if (uiState.previewType != null) {
                                Spacer(modifier = Modifier.weight(1f))
                                Row(modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)) {
                                    if (uiState.previewType == PreviewType.IMAGE) {
                                        Image(
                                            painter = rememberAsyncImagePainter(
                                                ImageRequest
                                                    .Builder(applicationContext)
                                                    .data(data = uiState.selectedFile)
                                                    .build()
                                            ),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .fillMaxHeight(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else if (uiState.previewType == PreviewType.VIDEO) {
                                        VideoView(videoUri = uiState.selectedFile!!)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                        }


                    }

                }
            }
        }
    }

    private fun parseUri(uri: Uri, viewModel: MainViewModel) {
        contentResolver.query(uri, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }?.let { filename ->
            viewModel.selectedFile(uri, filename, contentResolver.getType(uri))
        }
    }

    @Composable
    private fun VideoView(videoUri: Uri) {
        val context = LocalContext.current

        val exoPlayer = ExoPlayer.Builder(LocalContext.current)
            .build()
            .also { exoPlayer ->
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
            }

        DisposableEffect(
            AndroidView(factory = {
                StyledPlayerView(context).apply {
                    player = exoPlayer
                }
            })
        ) {
            onDispose { exoPlayer.release() }
        }
    }

}


enum class State {
    OVERVIEW,
    CONFIG,
    FILE_SELECTED,
}
