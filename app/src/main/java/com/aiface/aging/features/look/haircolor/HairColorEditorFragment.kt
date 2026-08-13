package com.aiface.aging.features.look.haircolor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.aiface.aging.databinding.FragmentHairColorEditorBinding
import com.aiface.aging.features.look.adapter.HairColorAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import coil.size.Scale
import androidx.core.graphics.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.R
import com.aiface.aging.features.look.LookFeatureAds
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.shared.safePopBackStack
import com.aiface.aging.utils.FirebaseLogUtils

@AndroidEntryPoint
class HairColorEditorFragment : Fragment() {

    private var _binding : FragmentHairColorEditorBinding? =null
    private val binding get() = _binding

    private val hairEditorViewModel : HairEditorViewModel by activityViewModels()
    private var selectedColor: Int? = Color.BLACK
    private var opacityValue: Float = 1F
    private var imageSegmenter: ImageSegmenter? = null
    private var selectedBitmap: Bitmap? = null

    private var isOpacityChangedByUser = false // Track if user changed opacity

    private var mActivity: FragmentActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity =  requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHairColorEditorBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Firebase event for hair color editor view
//        AppUtils.firebaseUserAction("hair_color_editor_view", "Hair_Color")
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hair_segmenter.tflite")
                .build()
            val options = ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(baseOptions)
                .setRunningMode(RunningMode.IMAGE)
                .setOutputCategoryMask(true)
                .build()
            imageSegmenter = ImageSegmenter.createFromOptions(mActivity, options)
        }catch (e: Exception){
            e.printStackTrace()
        }
        binding?.imageEditorView?.setImageBitmap(hairEditorViewModel.cropedImage)
        hairEditorViewModel.finalBitmap = hairEditorViewModel.cropedImage
        selectedBitmap=hairEditorViewModel.cropedImage
        // Opacity SeekBar
        binding?.opacitySeekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minOpacity = 0.5f
                val maxOpacity = 1f

                // Ensure progress is a float and calculate linearly between min and max
                val clampedOpacity = minOpacity + (progress.toFloat() / seekBar!!.max) * (maxOpacity - minOpacity)

                // Show percentage without rounding errors
                binding?.opacityValue?.text = "${(clampedOpacity * 100).toInt()}"
                hairEditorViewModel.setOpacity(clampedOpacity)
                opacityValue=clampedOpacity

                Log.d("opacity", "progress=$progress -> opacity=$clampedOpacity")
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                runHairSegmentationAndRecolor()
            }
        })
        // Observe opacity
        hairEditorViewModel.opacity.observe(viewLifecycleOwner, Observer { opacity ->
            val minOpacity = 0.5f
            val maxOpacity = 1f

            // Map opacity to progress (0..100)
            val percent = (((opacity - minOpacity) / (maxOpacity - minOpacity)) * binding!!.opacitySeekBar.max).toInt()

            // This will make 0.55f opacity be around the middle if min=0.1 and max=1
            binding?.opacitySeekBar?.progress = percent

            // Show the opacity as percentage
            binding?.opacityValue?.text = ((opacity * 100).toInt()).toString()
            opacityValue = opacity

            Log.d("opacity", "opacityObserve=$opacity, progress=$percent")
        })

        // Color RecyclerView
        val adapter = HairColorAdapter(hairEditorViewModel.colorList) { colorResId ->
            val colorInt = ContextCompat.getColor(requireContext(), colorResId)
            hairEditorViewModel.setSelectedColor(colorInt)
        }
        binding?.colorRecyclerView?.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding?.colorRecyclerView?.adapter = adapter
        // Observe selected color
        hairEditorViewModel.selectedColor.observe(viewLifecycleOwner, Observer { selecte ->
            selecte?.let {
                selectedColor=selecte
                runHairSegmentationAndRecolor()
            }
        })
        // Cancel Button
        binding?.btnCancel?.setOnClickListener {
            handleBackNavigation()
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner
        ) {
            handleBackNavigation()
        }
        // Confirm Button
        binding?.btnConfirm?.setOnClickListener {
            FirebaseLogUtils.logEvent("home_click_hair_color_save", "")
            mActivity?.let { activity ->
                LookFeatureAds.showAdThenNavigate(activity) { navigateToResultScreen() }
            }
        }
        // Done Button
        binding?.btnDone?.setOnClickListener {
            FirebaseLogUtils.logEvent("home_click_hair_color_save", "")
            mActivity?.let { activity ->
                LookFeatureAds.showAdThenNavigate(activity) { navigateToResultScreen() }
            }
        }
    }

//    private fun runHairSegmentationAndRecolor() {
//        // Ensure this is run in a coroutine scope
//        try {
//            CoroutineScope(Dispatchers.IO).launch {
//                val bitmap = selectedBitmap ?: return@launch
//                val segmenter = imageSegmenter ?: return@launch
//                Log.d("HairSegmenter", "Running hair segmentation...")
//                val mpImage = BitmapImageBuilder(bitmap).build()
//                val result = try {
//                    segmenter.segment(mpImage)
//                } catch (e: Exception) {
//                    Log.e("HairColorEditor", "Unexpected error during segmentation", e)
//                    null
//                }
//                val confidenceMaskImage = try {
//                    result?.confidenceMasks()?.orElse(null)?.getOrNull(1)
//                } catch (e: Exception) {
//                    Log.e("HairSegmenter", "Error while extracting confidence mask: ${e.message}", e)
//                    null
//                }
//                if (confidenceMaskImage == null) {
//                    Log.w("HairSegmenter", "Confidence mask is null or not found for class 1.")
//                    return@launch
//                }
//                val width = confidenceMaskImage.width
//                val height = confidenceMaskImage.height
//                Log.d("HairSegmenter", "Confidence mask acquired: width=$width, height=$height")
//                // Use reflection to get internal ByteBuffer and convert to FloatBuffer
//                val buffer: FloatBuffer = try {
//                    val getContainerMethod: Method = confidenceMaskImage.javaClass.getDeclaredMethod("getContainer")
//                    getContainerMethod.isAccessible = true
//                    val container = getContainerMethod.invoke(confidenceMaskImage)
//
//                    val getByteBufferMethod: Method = container.javaClass.getDeclaredMethod("getByteBuffer")
//                    getByteBufferMethod.isAccessible = true
//                    val byteBuffer = getByteBufferMethod.invoke(container) as java.nio.ByteBuffer
//
//                    byteBuffer.order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
//                } catch (e: Exception) {
//                    e.printStackTrace()
//                    Log.e("HairSegmenter", "Failed to access internal buffer via reflection", e)
//                    return@launch
//                }
//                Log.d("HairSegmenter", "Float buffer ready, building mask bitmap...")
//                buffer.rewind()
//                val maskBitmap = createBitmap(width, height)
//                for (y in 0 until height) {
//                    for (x in 0 until width) {
//                        val confidence = buffer.get()
//                        val color = if (confidence > 0.5f) 0xFFFFFFFF.toInt() else 0xFF000000.toInt()
//                        maskBitmap[x, y] = color
//                    }
//                }
//                Log.d("HairSegmenter", "Mask bitmap created, starting recoloring...")
//                // Recolor hair in background thread
//                val opacity = if (isOpacityChangedByUser) (hairEditorViewModel.opacity.value ?: 1f) else 1f
//                val recolored = selectedColor?.let { recolorHair(bitmap, maskBitmap, it, opacity) }
//
//                // Now update UI on the main thread
//                withContext(Dispatchers.Main) {
//                    hairEditorViewModel.finalBitmap=recolored
//                    binding?.imageEditorView?.setImageBitmap(recolored)
//                }
//            }
//        }catch (e: Exception){
//            e.printStackTrace()
//        }
//    }


    private fun runHairSegmentationAndRecolor() {
        try {
            binding?.imageLoadingOverlay?.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = selectedBitmap
                    ?.let { loadCompressedBitmapFromBitmap(it, 1080, 1080) }
                    ?: return@launch
                val segmenter = imageSegmenter ?: return@launch
                Log.d("HairSegmenter", "Running hair segmentation...")

                val mpImage = BitmapImageBuilder(bitmap).build()
                val result = try {
                    segmenter.segment(mpImage)
                } catch (e: Exception) {
                    Log.e("HairColorEditor", "Segmentation failed", e)
                    return@launch
                }

                val confidenceMaskImage = try {
                    result?.confidenceMasks()?.orElse(null)?.getOrNull(1)
                } catch (e: Exception) {
                    Log.e("HairSegmenter", "Failed to extract confidence mask", e)
                    return@launch
                }

                if (confidenceMaskImage == null) {
                    Log.w("HairSegmenter", "Confidence mask is null or missing class 1.")
                    return@launch
                }

                val width = confidenceMaskImage.width
                val height = confidenceMaskImage.height
                Log.d("HairSegmenter", "Mask size: ${width}x$height")

                // Step 1: Get FloatBuffer from internal mask
                val buffer: FloatBuffer = try {
                    val getContainerMethod = confidenceMaskImage.javaClass.getDeclaredMethod("getContainer")
                    getContainerMethod.isAccessible = true
                    val container = getContainerMethod.invoke(confidenceMaskImage)

                    val getByteBufferMethod = container.javaClass.getDeclaredMethod("getByteBuffer")
                    getByteBufferMethod.isAccessible = true
                    val byteBuffer = getByteBufferMethod.invoke(container) as ByteBuffer

                    byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
                } catch (e: Exception) {
                    Log.e("HairSegmenter", "Buffer reflection failed", e)
                    return@launch
                }

                // Step 2: Prepare mask data from FloatBuffer
                buffer.rewind()
                val maskPixels = ByteArray(width * height)
                for (i in maskPixels.indices) {
                    val confidence = buffer.get().coerceIn(0f, 1f)
                    maskPixels[i] = (confidence * 255).toInt().toByte()
                }

                // Step 3: Prepare original pixels
                val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val origPixels = IntArray(bitmap.width * bitmap.height)
                resultBitmap.getPixels(origPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                val scaleX = bitmap.width.toFloat() / width
                val scaleY = bitmap.height.toFloat() / height

                val targetColor = selectedColor ?: return@launch
                Log.d("opacity","finalResult=${opacityValue.toString()}")
                Log.d("opacity","targetColor=${targetColor.toString()}")
                val targetHSV = FloatArray(3)
                Color.colorToHSV(targetColor, targetHSV)

                // Step 4: Apply realistic recoloring using HSV blend
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val maskIndex = y * width + x
                        val alphaByte = maskPixels[maskIndex].toInt() and 0xFF
                        if (alphaByte > 0) {
                            val confidence = alphaByte / 255f
                            val blendFactor = confidence * opacityValue

                            val origX = (x * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                            val origY = (y * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                            val pixelIndex = origY * bitmap.width + origX

                            val origColor = origPixels[pixelIndex]
                            val r = (origColor shr 16) and 0xFF
                            val g = (origColor shr 8) and 0xFF
                            val b = origColor and 0xFF

                            val hsv = FloatArray(3)
                            Color.RGBToHSV(r, g, b, hsv)

                            hsv[0] = targetHSV[0] // Set hue from target
                            hsv[1] = targetHSV[1] // Set saturation from target
                            // Keep value (brightness) as-is for realism

                            val recolored = Color.HSVToColor(hsv)

                            val r2 = (recolored shr 16) and 0xFF
                            val g2 = (recolored shr 8) and 0xFF
                            val b2 = recolored and 0xFF

                            val finalR = (r * (1 - blendFactor) + r2 * blendFactor).toInt()
                            val finalG = (g * (1 - blendFactor) + g2 * blendFactor).toInt()
                            val finalB = (b * (1 - blendFactor) + b2 * blendFactor).toInt()

                            origPixels[pixelIndex] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
                        }
                    }
                }

                // Step 5: Update bitmap
                resultBitmap.setPixels(origPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

                withContext(Dispatchers.Main) {
                    hairEditorViewModel.finalBitmap = resultBitmap
                    binding?.imageEditorView?.setImageBitmap(resultBitmap)
                    binding?.imageLoadingOverlay?.visibility = View.GONE // hide after processing

                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding?.imageLoadingOverlay?.visibility = View.GONE // hide after processing
        }
    }





//    private fun runHairSegmentationAndRecolor() {
//        try {
//            CoroutineScope(Dispatchers.IO).launch {
//                val bitmap = selectedBitmap ?: return@launch
//                val segmenter = imageSegmenter ?: return@launch
//                Log.d("HairSegmenter", "Running hair segmentation...")
//
//                val mpImage = BitmapImageBuilder(bitmap).build()
//                val result = try {
//                    segmenter.segment(mpImage)
//                } catch (e: Exception) {
//                    Log.e("HairColorEditor", "Segmentation failed", e)
//                    return@launch
//                }
//
//                val confidenceMaskImage = try {
//                    result?.confidenceMasks()?.orElse(null)?.getOrNull(1)
//                } catch (e: Exception) {
//                    Log.e("HairSegmenter", "Failed to extract confidence mask", e)
//                    return@launch
//                }
//
//                if (confidenceMaskImage == null) {
//                    Log.w("HairSegmenter", "Confidence mask is null or missing class 1.")
//                    return@launch
//                }
//
//                val width = confidenceMaskImage.width
//                val height = confidenceMaskImage.height
//                Log.d("HairSegmenter", "Mask size: ${width}x$height")
//
//                // Step 1: Get FloatBuffer from internal mask
//                val buffer: FloatBuffer = try {
//                    val getContainerMethod = confidenceMaskImage.javaClass.getDeclaredMethod("getContainer")
//                    getContainerMethod.isAccessible = true
//                    val container = getContainerMethod.invoke(confidenceMaskImage)
//
//                    val getByteBufferMethod = container.javaClass.getDeclaredMethod("getByteBuffer")
//                    getByteBufferMethod.isAccessible = true
//                    val byteBuffer = getByteBufferMethod.invoke(container) as ByteBuffer
//
//                    byteBuffer.order(ByteOrder.nativeOrder()).asFloatBuffer()
//                } catch (e: Exception) {
//                    Log.e("HairSegmenter", "Buffer reflection failed", e)
//                    return@launch
//                }
//
//                // Step 2: Prepare mask data from FloatBuffer
//                buffer.rewind()
//                val maskPixels = ByteArray(width * height)
//                for (i in maskPixels.indices) {
//                    val confidence = buffer.get().coerceIn(0f, 1f)
//                    maskPixels[i] = (confidence * 255).toInt().toByte()
//                }
//
//                // Step 3: Prepare original pixels
//                val resultBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
//                val origPixels = IntArray(bitmap.width * bitmap.height)
//                resultBitmap.getPixels(origPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//
//                val scaleX = bitmap.width.toFloat() / width
//                val scaleY = bitmap.height.toFloat() / height
//
//                val targetColor = selectedColor ?: return@launch
//                val opacity = if (isOpacityChangedByUser) (hairEditorViewModel.opacity.value ?: 1f) else 1f
//
//                val hsvTarget = FloatArray(3)
//                Color.colorToHSV(targetColor, hsvTarget)
//                val targetHue = hsvTarget[0]
//
//                // Step 4: Apply realistic recoloring using HSL blend
//                for (y in 0 until height) {
//                    for (x in 0 until width) {
//                        val maskIndex = y * width + x
//                        val alphaByte = maskPixels[maskIndex].toInt() and 0xFF
//                        if (alphaByte > 0) {
//                            val confidence = alphaByte / 255f
//                            val blendFactor = confidence * opacity
//
//                            val origX = (x * scaleX).toInt().coerceIn(0, bitmap.width - 1)
//                            val origY = (y * scaleY).toInt().coerceIn(0, bitmap.height - 1)
//                            val pixelIndex = origY * bitmap.width + origX
//
//                            val origColor = origPixels[pixelIndex]
//                            val r = (origColor shr 16) and 0xFF
//                            val g = (origColor shr 8) and 0xFF
//                            val b = origColor and 0xFF
//
//                            val hsv = FloatArray(3)
//                            Color.RGBToHSV(r, g, b, hsv)
//
//
//                            hsv[0] = targetHue // Replace hue only
//                            hsv[1] = hsv[1].coerceIn(0.3f, 1f) // keep some saturation
//                            hsv[2] = hsv[2] // preserve brightness
//
//                            val recolor = Color.HSVToColor(hsv)
//
//                            val r2 = (recolor shr 16) and 0xFF
//                            val g2 = (recolor shr 8) and 0xFF
//                            val b2 = recolor and 0xFF
//
//                            val finalR = (r * (1 - blendFactor) + r2 * blendFactor).toInt()
//                            val finalG = (g * (1 - blendFactor) + g2 * blendFactor).toInt()
//                            val finalB = (b * (1 - blendFactor) + b2 * blendFactor).toInt()
//
//                            origPixels[pixelIndex] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
//                        }
//                    }
//                }
//
//                // Step 5: Update bitmap
//                resultBitmap.setPixels(origPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
//
//                withContext(Dispatchers.Main) {
//                    hairEditorViewModel.finalBitmap = resultBitmap
//                    binding?.imageEditorView?.setImageBitmap(resultBitmap)
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }









//    private suspend fun recolorHair(original: Bitmap, mask: Bitmap, color: Int, opacity: Float): Bitmap {
//        return withContext(Dispatchers.Default) { // Offload recoloring to background thread
//            val result = original.copy(Bitmap.Config.ARGB_8888, true)
//            val scaleX = original.width.toFloat() / mask.width
//            val scaleY = original.height.toFloat() / mask.height
//            val paint = Paint().apply {
//                this.color = color
//                this.alpha = (opacity * 255).toInt() // Use opacity from 0.0 to 1.0
//            }
//            // Convert mask to IntArray for faster pixel access
//            val maskPixels = IntArray(mask.width * mask.height)
//            mask.getPixels(maskPixels, 0, mask.width, 0, 0, mask.width, mask.height)
//            val canvas = Canvas(result)
//            for (y in 0 until mask.height) {
//                for (x in 0 until mask.width) {
//                    val pixel = maskPixels[y * mask.width + x]
//                    if (pixel == 0xFFFFFFFF.toInt()) {
//                        val origX = (x * scaleX).toInt()
//                        val origY = (y * scaleY).toInt()
//                        if (origX in 0 until original.width && origY in 0 until original.height) {
//                            canvas.drawPoint(origX.toFloat(), origY.toFloat(), paint)
//                        }
//                    }
//                }
//            }
//            result
//        }
//    }

    private fun loadCompressedBitmapFromBitmap(
        bitmap: Bitmap,
        maxWidth: Int = 1080,
        maxHeight: Int = 1080
    ): Bitmap {
        val ratio = minOf(
            maxWidth.toFloat() / bitmap.width,
            maxHeight.toFloat() / bitmap.height
        )

        // No scaling needed
        if (ratio >= 1f) return bitmap

        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()

        return bitmap.scale(newWidth, newHeight)
    }

    private fun navigateToResultScreen() {
        try {
            safeNavigate(
                HairColorEditorFragmentDirections.actionHairColorEditorFragmentToResultFragment(
                    sourceFeature = "hair_color",
                ),
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleBackNavigation() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            if (!safePopBackStack()) {
                requireActivity().safeFinish()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




    override fun onDestroyView() {
        super.onDestroyView()
        selectedColor=null
        hairEditorViewModel.clearColor()
        _binding = null
    }

}