package com.faskn.composeplayground.segmentedwallpaper

import android.content.Context
import com.google.android.gms.common.moduleinstall.ModuleInstall

/**
 * Factory for creating ImageSegmentationDataSource instances
 */
object ImageSegmentationFactory {

    /**
     * Creates an instance of ImageSegmentationDataSource
     */
    fun create(context: Context): ImageSegmentationDataSource {
        val moduleInstallClient = ModuleInstall.getClient(context)
        return ImageSegmentationDataSourceImpl(moduleInstallClient)
    }
}

