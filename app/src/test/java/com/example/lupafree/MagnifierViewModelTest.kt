package com.example.lupafree

import com.example.lupafree.ui.MagnifierViewModel
import com.example.lupafree.ui.zoomRatioForLinearZoom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MagnifierViewModelTest {
    @Test
    fun zoomRatioIsClampedToHardwareRange() {
        assertEquals(1f, zoomRatioForLinearZoom(1f, 10f, -1f), 0.001f)
        assertEquals(10f, zoomRatioForLinearZoom(1f, 10f, 2f), 0.001f)
        assertEquals(5.5f, zoomRatioForLinearZoom(1f, 10f, 0.5f), 0.001f)
    }

    @Test
    fun pinchZoomUpdatesLinearStateWithoutHardwareCamera() {
        val viewModel = MagnifierViewModel()

        viewModel.onPinchZoom(2f)

        assertEquals(0.18f, viewModel.state.value.zoom, 0.001f)
        assertTrue(viewModel.state.value.currentZoomRatio >= 1f)
    }

    @Test
    fun permissionStateCanBeUpdated() {
        val viewModel = MagnifierViewModel()

        viewModel.onPermissionResult(true)

        assertTrue(viewModel.state.value.hasCameraPermission)
    }
}
