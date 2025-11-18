package com.faskn.composeplayground.segmentedwallpaper

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_CANCELED
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate.InstallState.STATE_FAILED
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Data source for image segmentation operations using ML Kit Subject Segmentation
 */
interface ImageSegmentationDataSource {

    /**
     * Segments the image into foreground and background
     */
    suspend fun segmentImage(bitmap: Bitmap): SegmentationResult
}

/**
 * Result of image segmentation containing both foreground and background bitmaps
 */
data class SegmentationResult(
    val foreground: Bitmap,
    val background: Bitmap?,
    val originalBitmap: Bitmap
)

/**
 * Implementation of ImageSegmentationDataSource using Google ML Kit
 */
class ImageSegmentationDataSourceImpl(
    private val moduleInstallClient: ModuleInstallClient,
) : ImageSegmentationDataSource {

    companion object {
        private const val TAG = "ImageSegmentation"

        val options = SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .enableForegroundConfidenceMask()
            .build()

        val segmenter = SubjectSegmentation.getClient(options)
    }

    /**
     * Check if the Subject Segmentation module is installed on the device
     */
    private suspend fun isSubjectSegmentationModuleInstalled(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Checking if Subject Segmentation module is available...")
            moduleInstallClient.areModulesAvailable(segmenter)
                .addOnSuccessListener { result ->
                    val isAvailable = result.areModulesAvailable()
                    Log.d(TAG, "Module availability check result: $isAvailable")
                    continuation.resume(isAvailable)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to check module availability", exception)
                    continuation.resumeWithException(exception)
                }
        }
    }

    /**
     * Custom listener for module installation progress
     */
    private class ModuleInstallProgressListener(
        val continuation: CancellableContinuation<Boolean>,
    ) : InstallStatusListener {

        override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
            Log.d(TAG, "Module install status: ${update.installState}")

            if (!continuation.isActive) {
                Log.d(TAG, "Continuation is not active, skipping update")
                return
            }

            when (update.installState) {
                ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                    Log.d(TAG, "Module installation completed successfully")
                    continuation.resume(true)
                }

                STATE_FAILED, STATE_CANCELED -> {
                    Log.e(
                        TAG,
                        "Module installation failed or was canceled. State: ${update.installState}"
                    )
                    continuation.resumeWithException(
                        ImageSegmentationException(
                            "Module download failed or was canceled. State: ${update.installState}"
                        )
                    )
                }

                else -> {
                    // Download in progress - could emit progress updates here
                    Log.d(TAG, "Module download in progress...")
                }
            }
        }
    }

    /**
     * Install the Subject Segmentation module if not already installed
     */
    private suspend fun installSubjectSegmentationModule(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Starting module installation...")
            val listener = ModuleInstallProgressListener(continuation)
            val moduleInstallRequest = ModuleInstallRequest.newBuilder()
                .addApi(segmenter)
                .setListener(listener)
                .build()

            moduleInstallClient
                .installModules(moduleInstallRequest)
                .addOnSuccessListener {
                    Log.d(TAG, "Module installation request submitted successfully")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to submit module installation request", exception)
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
        }
    }

    /**
     * Ensures the segmentation module is available, downloading if necessary
     */
    private suspend fun ensureModuleAvailable() {
        try {
            val areModulesAvailable = isSubjectSegmentationModuleInstalled()

            if (!areModulesAvailable) {
                Log.d(TAG, "Module not available, starting download...")
                val installResult = installSubjectSegmentationModule()
                if (!installResult) {
                    throw ImageSegmentationException("Failed to download ML Kit module")
                }
                Log.d(TAG, "Module installed successfully")
            } else {
                Log.d(TAG, "Module already available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring module availability", e)
            throw ImageSegmentationException("Module initialization failed: ${e.message}")
        }
    }

    override suspend fun segmentImage(bitmap: Bitmap): SegmentationResult {
        Log.d(TAG, "Starting image segmentation")
        ensureModuleAvailable()

        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { continuation ->
            Log.d(TAG, "Processing image with ML Kit segmenter...")
            segmenter.process(image)
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Segmentation successful, subjects found: ${result.subjects.size}")
                    if (result.foregroundBitmap != null) {
                        Log.d(TAG, "Foreground bitmap extracted, creating background...")

                        val foreground = result.foregroundBitmap!!

                        // Create background by inverting the foreground mask
                        val background = createBackgroundBitmap(bitmap, foreground)

                        if (background == null) {
                            Log.w(
                                TAG,
                                "Failed to create background bitmap due to memory constraints"
                            )
                        }

                        continuation.resume(
                            SegmentationResult(
                                foreground = foreground,
                                background = background,
                                originalBitmap = bitmap
                            )
                        )
                    } else {
                        Log.w(TAG, "No foreground bitmap found in result")
                        continuation.resumeWithException(
                            ImageSegmentationException("Subject not found in image")
                        )
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Segmentation processing failed", exception)
                    continuation.resumeWithException(exception)
                }

            continuation.invokeOnCancellation {
                Log.d(TAG, "Segmentation cancelled, cleaning up resources")
            }
        }
    }


    /**
     * Creates a background bitmap by removing the foreground from the original image
     * Processes pixels in chunks to avoid OutOfMemoryError with large images
     */
    private fun createBackgroundBitmap(original: Bitmap, foreground: Bitmap): Bitmap? {
        val width = original.width
        val height = original.height

        return try {
            // Create a mutable copy of the original bitmap
            val background = original.copy(Bitmap.Config.ARGB_8888, true)

            // Process pixels row by row to minimize memory usage
            val rowPixels = IntArray(width)
            val foregroundRowPixels = IntArray(width)

            for (y in 0 until height) {
                // Get one row of pixels at a time
                original.getPixels(rowPixels, 0, width, 0, y, width, 1)
                foreground.getPixels(foregroundRowPixels, 0, width, 0, y, width, 1)

                // Process the row
                for (x in 0 until width) {
                    val alpha = (foregroundRowPixels[x] shr 24) and 0xFF
                    if (alpha > 0) {
                        // This pixel is part of the foreground, make it transparent in background
                        rowPixels[x] = 0x00000000
                    }
                }

                // Set the processed row back to the background bitmap
                background.setPixels(rowPixels, 0, width, 0, y, width, 1)
            }

            background
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "OutOfMemoryError while creating background bitmap", e)
            System.gc()
            null
        }
    }
}

/**
 * Exception thrown when image segmentation fails
 */
class ImageSegmentationException(message: String? = null) : Exception(message)

