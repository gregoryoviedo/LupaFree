package com.example.lupafree.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.lupafree.R

private val GlassBackground = Color.Black.copy(alpha = 0.40f)
private val GlassBorderColor = Color.White.copy(alpha = 0.20f)
private val GlassBorderWidth = 1.dp
private val GlassBorder = BorderStroke(GlassBorderWidth, GlassBorderColor)
private val SheetContainerColor = Color(0xFF121212)
private val DialogContainerColor = Color(0xFF1A1A1A)

private object AppLinks {
    const val GITHUB = "https://github.com/gregoryoviedo/LupaFree"
}

private const val MIT_LICENSE_YEAR = "2026"
private const val MIT_LICENSE_AUTHOR = "Gregory Oviedo"
private const val BINANCE_PAY_ID = "371811579"

@Composable
fun MagnifierScreen(viewModel: MagnifierViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val localView = LocalView.current

    var hasAsked by remember { mutableStateOf(false) }
    var denialCount by remember { mutableStateOf(0) }
    var showSheet by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showDonationDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        if (!granted) {
            denialCount += 1
        }
    }

    LaunchedEffect(Unit) {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            viewModel.onPermissionResult(true)
        } else if (!hasAsked) {
            hasAsked = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                viewModel.onPermissionResult(granted)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        localView.keepScreenOn = true
        onDispose {
            localView.keepScreenOn = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (state.hasCameraPermission) {
            CameraLayer(
                state = state,
                viewModel = viewModel,
                onPinchZoom = viewModel::onPinchZoom
            )
            FreezeOverlay(image = state.frozenImage, isFrozen = state.isFrozen)
            FocusReticle(
                point = state.focusPoint,
                isFocusing = state.isFocusing,
                onFinished = viewModel::clearFocusIndicator
            )
            ZoomLabelAndSlider(
                zoom = state.zoom,
                ratio = state.currentZoomRatio,
                onZoomChange = viewModel::onZoomChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            )
            state.error?.let { cameraError ->
                CameraErrorBanner(
                    error = cameraError,
                    onRetry = viewModel::retryCamera,
                    onDismiss = viewModel::clearError,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            TopControlBar(
                state = state,
                onToggleTorch = viewModel::toggleTorch,
                onToggleFreeze = viewModel::toggleFreeze,
                onMoreClick = { showSheet = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp, end = 16.dp)
            )
        } else {
            PermissionGate(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onOpenSettings = { openAppSettings(context) },
                showSettingsButton = denialCount > 0
            )
        }
    }

    if (showSheet) {
        MoreOptionsSheet(
            onDismiss = { showSheet = false },
            onLicenseClick = {
                showSheet = false
                showLicenseDialog = true
            },
            onPermissionsClick = {
                showSheet = false
                openAppSettings(context)
            },
            onGitHubClick = {
                showSheet = false
                openUrl(context, AppLinks.GITHUB)
            },
            onDonateClick = {
                showSheet = false
                showDonationDialog = true
            }
        )
    }

    if (showLicenseDialog) {
        LicenseDialog(onDismiss = { showLicenseDialog = false })
    }
    if (showDonationDialog) {
        DonationDialog(
            onDismiss = { showDonationDialog = false },
            context = context
        )
    }
}

@Composable
private fun CameraLayer(
    state: MagnifierUiState,
    viewModel: MagnifierViewModel,
    onPinchZoom: (Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(previewView) {
                detectTapGestures { offset ->
                    viewModel.focusAt(offset.x, offset.y)
                }
            }
            .pointerInput(previewView) {
                detectTransformGestures { _, _, zoom, _ ->
                    onPinchZoom(zoom)
                }
            }
    )

    LaunchedEffect(previewView) {
        viewModel.bindCamera(lifecycleOwner, previewView)
        previewView.post {
            viewModel.focusAt(previewView.width / 2f, previewView.height / 2f)
        }
    }
}

@Composable
private fun FocusReticle(
    point: androidx.compose.ui.geometry.Offset?,
    isFocusing: Boolean,
    onFinished: () -> Unit
) {
    if (point == null) return
    val reticleDescription = stringResource(R.string.reticle_content_description)
    LaunchedEffect(point) {
        kotlinx.coroutines.delay(1200)
        onFinished()
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = reticleDescription }
    ) {
        drawCircle(
            color = if (isFocusing) Color.White else Color(0xFF9CCC65),
            radius = 28.dp.toPx(),
            center = point,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun CameraErrorBanner(
    error: CameraError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val message = when (error) {
        CameraError.INITIALIZATION -> stringResource(R.string.camera_initialization_error)
        CameraError.OPEN -> stringResource(R.string.camera_open_error)
    }
    Surface(
        modifier = modifier.padding(24.dp),
        shape = RoundedCornerShape(18.dp),
        color = DialogContainerColor,
        contentColor = Color.White,
        border = GlassBorder
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close_button)) }
                Button(onClick = onRetry) { Text(stringResource(R.string.retry_camera_button)) }
            }
        }
    }
}

@Composable
private fun FreezeOverlay(
    image: ImageBitmap?,
    isFrozen: Boolean
) {
    if (isFrozen && image != null) {
        Image(
            bitmap = image,
            contentDescription = stringResource(R.string.freeze_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ZoomLabelAndSlider(
    zoom: Float,
    ratio: Float,
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GlassBackground,
            contentColor = Color.White,
            border = GlassBorder
        ) {
            Text(
                text = "%.1fx".format(ratio),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = GlassBackground,
            contentColor = Color.White,
            border = GlassBorder
        ) {
            Slider(
                value = zoom,
                onValueChange = onZoomChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White.copy(alpha = 0.85f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun TopControlBar(
    state: MagnifierUiState,
    onToggleTorch: () -> Unit,
    onToggleFreeze: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.hasFlashUnit) {
            GlassFab(
                onClick = onToggleTorch,
                contentDescription = stringResource(R.string.torch_content_description)
            ) {
                Icon(
                    imageVector = if (state.isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                    contentDescription = null
                )
            }
        }
        GlassFab(
            onClick = onToggleFreeze,
            contentDescription = if (state.isFrozen) {
                stringResource(R.string.resume_content_description)
            } else {
                stringResource(R.string.freeze_content_description)
            }
        ) {
            Icon(
                imageVector = if (state.isFrozen) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = null
            )
        }
        GlassFab(
            onClick = onMoreClick,
            contentDescription = stringResource(R.string.more_options_content_description)
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun GlassFab(
    onClick: () -> Unit,
    contentDescription: String,
    icon: @Composable () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = GlassBackground,
        contentColor = Color.White,
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionsSheet(
    onDismiss: () -> Unit,
    onLicenseClick: () -> Unit,
    onPermissionsClick: () -> Unit,
    onGitHubClick: () -> Unit,
    onDonateClick: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SheetContainerColor,
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            MenuItem(
                icon = Icons.Filled.Info,
                label = stringResource(R.string.menu_license),
                onClick = onLicenseClick
            )
            MenuItem(
                icon = Icons.Filled.Lock,
                label = stringResource(R.string.menu_permissions),
                onClick = onPermissionsClick
            )
            MenuItem(
                icon = Icons.Filled.Code,
                label = stringResource(R.string.menu_github),
                onClick = onGitHubClick
            )
            MenuItem(
                icon = Icons.Filled.Favorite,
                label = stringResource(R.string.menu_donate),
                onClick = onDonateClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun LicenseDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.license_dialog_title),
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(
                        R.string.mit_license_text,
                        MIT_LICENSE_YEAR,
                        MIT_LICENSE_AUTHOR
                    ),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.license_dialog_close),
                    color = Color.White
                )
            }
        },
        containerColor = DialogContainerColor,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun DonationDialog(onDismiss: () -> Unit, context: Context) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.donation_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.donation_dialog_message))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GlassBackground,
                    border = GlassBorder
                ) {
                    Text(
                        text = stringResource(R.string.binance_pay_id),
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                clipboard?.setPrimaryClip(ClipData.newPlainText("Binance Pay ID", BINANCE_PAY_ID))
                Toast.makeText(context, R.string.copied_message, Toast.LENGTH_SHORT).show()
            }) {
                Text(stringResource(R.string.copy_id_button), color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button), color = Color.White)
            }
        },
        containerColor = DialogContainerColor,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
private fun PermissionGate(
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    showSettingsButton: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.permission_required_message),
                color = Color.White,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRequest) {
                Text(text = stringResource(R.string.grant_permission_button))
            }
            if (showSettingsButton) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onOpenSettings) {
                    Text(text = stringResource(R.string.open_settings_button))
                }
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_browser_available, Toast.LENGTH_SHORT).show()
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    )
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
