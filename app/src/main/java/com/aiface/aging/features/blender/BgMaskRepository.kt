package com.aiface.aging.features.blender

import android.graphics.Bitmap
import com.aiface.aging.features.bgremover.SubjectSegmentationHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On-device subject segmentation for Photo Blender character masks.
 * Reuses the same ML Kit Subject Segmentation path as BG Remover.
 */
@Singleton
class BgMaskRepository @Inject constructor() {

    /**
     * @return Cropped foreground bitmap with transparent background, or throws on failure.
     */
    suspend fun createCharacterMask(source: Bitmap): Bitmap =
        SubjectSegmentationHelper.segmentForegroundCropped(source)
}
