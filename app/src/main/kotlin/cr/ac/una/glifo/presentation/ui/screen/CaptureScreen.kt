package cr.ac.una.glifo.presentation.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import cr.ac.una.glifo.R
import cr.ac.una.glifo.core.common.UiState
import cr.ac.una.glifo.core.ui.component.GlifoButton
import cr.ac.una.glifo.core.ui.component.GlifoButtonVariant
import cr.ac.una.glifo.core.ui.theme.GlifoTheme
import cr.ac.una.glifo.presentation.viewmodel.CaptureViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CaptureScreen(
    courseId: Long,
    onNavigateBack: () -> Unit,
    onCapturePhoto: (String) -> Unit,
    viewModel: CaptureViewModel = hiltViewModel()
) {
    val colors = GlifoTheme.colors
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.state.collectAsState()
    val course = (uiState as? UiState.Success)?.data

    LaunchedEffect(courseId) { viewModel.load(courseId) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionWasRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionWasRequested = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Camera Viewfinder Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            if (hasCameraPermission) {
                var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val capture = ImageCapture.Builder().build()
                            imageCapture = capture
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    capture
                                )
                            } catch (_: Exception) {
                                // Nothing sensible to do with a bind failure in the dummy phase —
                                // the permission-denied state already covers "no camera available".
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    }
                )

                // Top Camera Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = colors.textPrimary, modifier = Modifier.size(22.dp))
                    }
                    Text(
                        text = stringResource(
                            R.string.capture_page_header,
                            course?.name ?: stringResource(R.string.capture_default_course_name)
                        ),
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.size(22.dp))
                }

                // Framing guide (purely a visual aid — not a real detection result)
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(240.dp, 310.dp)
                        .border(1.dp, colors.accentText.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(colors.surface, RoundedCornerShape(999.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = stringResource(R.string.capture_framing_hint),
                        fontSize = 11.sp,
                        color = colors.accentText
                    )
                }

                // Camera Bottom Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .height(100.dp)
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = stringResource(R.string.capture_gallery_description), tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Shutter Button
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .border(3.dp, colors.textPrimary, CircleShape)
                            .clickable {
                                val capture = imageCapture ?: return@clickable
                                val fileName = "glifo_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(java.util.Date())}.jpg"
                                val outputFile = File(context.cacheDir, fileName)
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
                                capture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            onCapturePhoto(outputFile.absolutePath)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            // Dummy phase: no photo, no navigation — the user can just try again.
                                        }
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(colors.textPrimary, CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.surfaceHigh, RoundedCornerShape(7.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("1/8", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    }
                }
            } else {
                CameraPermissionRationale(
                    permissionWasRequested = permissionWasRequested,
                    onNavigateBack = onNavigateBack,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        val intent = android.content.Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun CameraPermissionRationale(
    permissionWasRequested: Boolean,
    onNavigateBack: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = GlifoTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.Default.Close, contentDescription = stringResource(R.string.common_close), tint = colors.textSecondary, modifier = Modifier.size(22.dp))
            }
        }

        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.capture_permission_title),
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.capture_permission_description),
            fontSize = 13.sp,
            color = colors.textSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (permissionWasRequested) {
            GlifoButton(
                text = stringResource(R.string.capture_open_app_settings),
                onClick = onOpenSettings
            )
        } else {
            GlifoButton(
                text = stringResource(R.string.capture_grant_permission),
                onClick = onRequestPermission
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        GlifoButton(
            text = stringResource(R.string.common_not_now),
            variant = GlifoButtonVariant.Text,
            onClick = onNavigateBack
        )
    }
}
