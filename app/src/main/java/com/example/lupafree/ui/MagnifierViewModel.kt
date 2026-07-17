package com.example.lupafree.ui

import android.graphics.Bitmap
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

data class MagnifierUiState(
    val zoom: Float = 0f,
    val isTorchOn: Boolean = false,
    val hasFlashUnit: Boolean = false,
    val isFrozen: Boolean = false,
    val frozenImage: ImageBitmap? = null,
    val hasCameraPermission: Boolean = false,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    val currentZoomRatio: Float = 1f,
    val errorMessage: String? = null
)

class MagnifierViewModel : ViewModel() {

    private val _state = MutableStateFlow(MagnifierUiState())
    val state: StateFlow<MagnifierUiState> = _state.asStateFlow()

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentBitmap: Bitmap? = null
    private var boundLifecycleOwner: LifecycleOwner? = null
    private var boundPreviewView: PreviewView? = null

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val context = previewView.context.applicationContext
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = try {
                providerFuture.get()
            } catch (t: Throwable) {
                _state.update { it.copy(errorMessage = "No se pudo inicializar la cámara") }
                return@addListener
            }
            cameraProvider = provider
            provider.unbindAll()
            camera = null
            val newPreview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cam = try {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    newPreview
                )
            } catch (t: Throwable) {
                _state.update { it.copy(errorMessage = "No se pudo abrir la cámara") }
                return@addListener
            }
            camera = cam
            boundLifecycleOwner = lifecycleOwner
            boundPreviewView = previewView
            _state.update { it.copy(hasFlashUnit = cam.cameraInfo.hasFlashUnit()) }
            cam.cameraInfo.zoomState.observe(lifecycleOwner) { zs ->
                if (zs != null) {
                    val newMin = zs.minZoomRatio
                    val newMax = zs.maxZoomRatio
                    val currentLinear = _state.value.zoom
                    val ratio = newMin + (newMax - newMin) * currentLinear
                    _state.update {
                        it.copy(
                            minZoomRatio = newMin,
                            maxZoomRatio = newMax,
                            currentZoomRatio = ratio
                        )
                    }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun onZoomChange(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        camera?.cameraControl?.setLinearZoom(clamped)
        _state.update {
            val ratio = it.minZoomRatio + (it.maxZoomRatio - it.minZoomRatio) * clamped
            it.copy(zoom = clamped, currentZoomRatio = ratio)
        }
    }

    fun toggleTorch() {
        val cam = camera ?: return
        if (!_state.value.hasFlashUnit) return
        val next = !_state.value.isTorchOn
        cam.cameraControl.enableTorch(next)
        _state.update { it.copy(isTorchOn = next) }
    }

    fun toggleFreeze() {
        val previewView = boundPreviewView ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val current = _state.value
            if (current.isFrozen) {
                withContext(Dispatchers.Main) {
                    currentBitmap?.recycle()
                    currentBitmap = null
                }
                _state.update { it.copy(isFrozen = false, frozenImage = null) }
            } else {
                val bmp = withContext(Dispatchers.Main) { previewView.getBitmap() } ?: return@launch
                currentBitmap = bmp
                val img = bmp.asImageBitmap()
                _state.update { it.copy(isFrozen = true, frozenImage = img) }
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(hasCameraPermission = granted) }
    }

    fun focusAt(x: Float, y: Float) {
        val cam = camera ?: return
        val pv = boundPreviewView ?: return
        if (pv.width <= 0 || pv.height <= 0) return
        val clampedX = x.coerceIn(0f, pv.width.toFloat())
        val clampedY = y.coerceIn(0f, pv.height.toFloat())
        val factory = pv.meteringPointFactory
        val point = factory.createPoint(clampedX, clampedY)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        cam.cameraControl.startFocusAndMetering(action)
    }

    override fun onCleared() {
        super.onCleared()
        cameraProvider?.unbindAll()
        currentBitmap?.recycle()
        currentBitmap = null
        camera = null
        cameraProvider = null
        boundLifecycleOwner = null
        boundPreviewView = null
    }
}
