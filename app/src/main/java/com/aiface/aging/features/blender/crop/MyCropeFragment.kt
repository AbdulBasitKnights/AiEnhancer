package com.aiface.aging.features.blender.crop

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageView
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentMyCropeBinding
import com.aiface.aging.features.blender.editor.BlendActivity
import com.aiface.aging.features.imgpicker.util.Extras
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class MyCropeFragment : Fragment() {

    private var _binding: FragmentMyCropeBinding? = null
    private val binding get() = _binding

    private var imageList: ArrayList<String>? = null
    private var forwardedExtras: Bundle? = null

    private var mActivity: FragmentActivity? = null
    private var cropJob: Job? = null
    private var isCropInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { args ->
            imageList = args.getStringArrayList(Extras.PICKER_IMG_LIST)
            forwardedExtras = Bundle(args).apply {
                remove(Extras.PICKER_IMG_LIST)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyCropeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        imageList?.firstOrNull()?.let { imagePath ->
            val imageUri = resolveImageUri(imagePath)
            if (imageUri != null) {
                binding?.cropImageView?.setImageUriAsync(imageUri)
            } else {
                Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
            }
        }

        binding?.btnStartEditing?.setOnClickListener {
            startCrop()
        }
    }

    /**
     * Crop + compress + file write run off the main thread via CanHub's worker
     * (Default for crop, IO for save). UI only shows progress and navigates.
     */
    private fun startCrop() {
        if (isCropInProgress || cropJob?.isActive == true) return
        val cropView = binding?.cropImageView ?: return
        val cropButton = binding?.btnStartEditing ?: return
        val appContext = requireContext().applicationContext

        isCropInProgress = true
        cropButton.isEnabled = false
        cropButton.text = mActivity?.getString(R.string.plz_wait) ?: getString(R.string.plz_wait)
        cropView.isEnabled = false

        cropView.setOnCropImageCompleteListener { _, result ->
            if (!isAdded) return@setOnCropImageCompleteListener
            cropView.setOnCropImageCompleteListener(null)

            if (result.isSuccessful) {
                val croppedUri = result.uriContent
                if (croppedUri != null) {
                    openBlendActivity(croppedUri)
                    // Leave button disabled; fragment may stay under BlendActivity
                    return@setOnCropImageCompleteListener
                }
                // Library returned bitmap only — save on IO without blocking UI
                val bitmap = result.bitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    cropJob = viewLifecycleOwner.lifecycleScope.launch {
                        val savedUri = withContext(Dispatchers.IO) {
                            saveBitmapAndGetUri(appContext, bitmap)
                        }
                        if (!bitmap.isRecycled) {
                            try {
                                bitmap.recycle()
                            } catch (_: Exception) {
                            }
                        }
                        if (savedUri != null) {
                            openBlendActivity(savedUri)
                        } else {
                            showCropError()
                            resetCropUi()
                        }
                    }
                    return@setOnCropImageCompleteListener
                }
            }

            Log.e(TAG, "Crop failed", result.error)
            showCropError()
            resetCropUi()
        }

        try {
            // Same quality as before (PNG @ 100). Worker: Default crop → IO write → Main callback.
            cropView.croppedImageAsync(
                saveCompressFormat = Bitmap.CompressFormat.PNG,
                saveCompressQuality = 100,
                reqWidth = 0,
                reqHeight = 0,
                options = CropImageView.RequestSizeOptions.RESIZE_INSIDE,
            )
        } catch (e: Exception) {
            Log.e(TAG, "croppedImageAsync failed, falling back", e)
            cropView.setOnCropImageCompleteListener(null)
            startCropFallback(cropView, appContext)
        }
    }

    /**
     * Fallback if async API is unavailable: crop on Default, compress/write on IO.
     * Snapshot crop on a background dispatcher after disabling the view.
     */
    private fun startCropFallback(cropView: CropImageView, appContext: Context) {
        cropJob?.cancel()
        cropJob = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val croppedBitmap = withContext(Dispatchers.Default) {
                    cropView.getCroppedImage()
                }
                ensureActive()
                if (croppedBitmap == null || croppedBitmap.isRecycled) {
                    showCropError()
                    resetCropUi()
                    return@launch
                }

                val imageUri = withContext(Dispatchers.IO) {
                    saveBitmapAndGetUri(appContext, croppedBitmap)
                }
                if (!croppedBitmap.isRecycled) {
                    try {
                        croppedBitmap.recycle()
                    } catch (_: Exception) {
                    }
                }

                if (imageUri != null) {
                    openBlendActivity(imageUri)
                } else {
                    Log.e(TAG, "Failed to save cropped bitmap")
                    showCropError()
                    resetCropUi()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallback crop failed", e)
                showCropError()
                resetCropUi()
            }
        }
    }

    private fun showCropError() {
        if (!isAdded) return
        Toast.makeText(requireContext(), "Unable to crop image", Toast.LENGTH_SHORT).show()
    }

    private fun resetCropUi() {
        isCropInProgress = false
        binding?.cropImageView?.isEnabled = true
        binding?.btnStartEditing?.apply {
            isEnabled = true
            text = mActivity?.getString(R.string.crop) ?: getString(R.string.crop)
        }
    }

    private fun openBlendActivity(croppedImageUri: Uri) {
        if (!isAdded) return
        val intent = Intent(requireContext(), BlendActivity::class.java)
        forwardedExtras?.let { intent.putExtras(it) }
        intent.putStringArrayListExtra(
            Extras.PICKER_IMG_LIST,
            arrayListOf(croppedImageUri.toString())
        )
        startActivity(intent)
        isCropInProgress = false
    }

    private fun resolveImageUri(imagePath: String): Uri? {
        return when {
            imagePath.startsWith("content://") || imagePath.startsWith("file://") -> {
                Uri.parse(imagePath)
            }
            else -> {
                val imageFile = File(imagePath)
                if (imageFile.exists()) Uri.fromFile(imageFile) else null
            }
        }
    }

    private fun saveBitmapAndGetUri(
        context: Context,
        bitmap: Bitmap,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100
    ): Uri? {
        return try {
            val fileName = "temp_image_${System.currentTimeMillis()}.png"
            val file = File(context.cacheDir, fileName)
            BufferedOutputStream(FileOutputStream(file)).use { out ->
                bitmap.compress(format, quality, out)
                out.flush()
            }
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        // Restore crop button after returning from BlendActivity
        if (!isCropInProgress && cropJob?.isActive != true) {
            binding?.btnStartEditing?.apply {
                isEnabled = true
                text = mActivity?.getString(R.string.crop) ?: getString(R.string.crop)
            }
            binding?.cropImageView?.isEnabled = true
        }
    }

    override fun onDestroyView() {
        cropJob?.cancel()
        cropJob = null
        binding?.cropImageView?.setOnCropImageCompleteListener(null)
        isCropInProgress = false
        _binding = null
        super.onDestroyView()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    companion object {
        private const val TAG = "MyCropeFragment"
    }
}
