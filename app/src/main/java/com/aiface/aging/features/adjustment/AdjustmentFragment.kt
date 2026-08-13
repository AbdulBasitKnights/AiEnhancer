package com.aiface.aging.features.adjustment

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentAdjustmentBinding
import com.aiface.aging.shared.safePopSupportBackStack
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.features.editor.ViewModelEditorActivity

import com.xw.repo.BubbleSeekBar
import com.xw.repo.BubbleSeekBar.OnProgressChangedListener
import dagger.hilt.android.AndroidEntryPoint
import jp.co.cyberagent.android.gpuimage.GPUImage
import jp.co.cyberagent.android.gpuimage.filter.GPUImageBrightnessFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageContrastFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageExposureFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHighlightShadowFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageHueFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSaturationFilter
import jp.co.cyberagent.android.gpuimage.filter.GPUImageSharpenFilter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


interface AdjustmentSeekbarListener {
    fun onAdjustmentChanged(typeFilter: String?, bitmap: Bitmap?)
}

@AndroidEntryPoint
class AdjustmentFragment : Fragment(), SecondaryRecyclerListener {

    companion object {
        private var adjustmentSeekbarListener: AdjustmentSeekbarListener? = null
        private var isBgRemover: Boolean = false
        fun newInstance(
            adjustmentSeekbarListener: AdjustmentSeekbarListener,
            isBgRemover: Boolean
        ): AdjustmentFragment {
            val fragment = AdjustmentFragment()
            Companion.adjustmentSeekbarListener = adjustmentSeekbarListener
            Companion.isBgRemover = isBgRemover
            return fragment
        }
    }


    private lateinit var binding: FragmentAdjustmentBinding
    private var mActivity: FragmentActivity? = null
    private var adapterRecyclerSecondary: AdapterRecyclerSecondary? = null
    private var originalBitmap: Bitmap? = null

    private val viewModelEditor: ViewModelEditorActivity by lazy {
        ViewModelProvider(requireActivity())[ViewModelEditorActivity::class.java]
    }

    private val viewModel: ViewModelAdjustment by viewModels()

    private val gpuImage: GPUImage by lazy {
        GPUImage(requireActivity())
    }
    private var localBitmap: Bitmap? = null

    private var typeFilter: String? = null

    //Filters
    private val highlightShadowFilter: GPUImageHighlightShadowFilter by lazy {
        GPUImageHighlightShadowFilter()
    }
    private val hueFilter: GPUImageHueFilter by lazy {
        GPUImageHueFilter()
    }
    private val contrastFilter: GPUImageContrastFilter by lazy {
        GPUImageContrastFilter()
    }
    private val brightnessFilter: GPUImageBrightnessFilter by lazy {
        GPUImageBrightnessFilter()
    }
    private val saturationFilter: GPUImageSaturationFilter by lazy {
        GPUImageSaturationFilter()
    }
    private val sharpenFilter: GPUImageSharpenFilter by lazy {
        GPUImageSharpenFilter()
    }
    private val exposureFilter: GPUImageExposureFilter by lazy {
        GPUImageExposureFilter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAdjustmentBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            localBitmap = viewModelEditor.userImageBitmap
            originalBitmap = viewModelEditor.userImageBitmap
            initDefaultFilter(isBgRemover)
            setUpRecyclerview(activity, isBgRemover)
            onclickListeners()
        }
    }

    private fun onclickListeners() {
        binding.tick.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                localBitmap?.let { localBitmap ->
                    viewModelEditor.userImageBitmap = localBitmap
                    adjustmentSeekbarListener?.onAdjustmentChanged(typeFilter, localBitmap)
                }
                closePanel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.cross.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                adjustmentSeekbarListener?.onAdjustmentChanged(
                    typeFilter,
                    viewModelEditor.userImageBitmap,
                )
                closePanel()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun closePanel() {
        val host = mActivity
        when (host) {
            is com.aiface.aging.features.editor.EditorActivity ->
                host.dismissBottomPanel()
            else -> host?.safePopSupportBackStack()
        }
    }

    private fun initDefaultFilter(isBgRemover: Boolean) {
       if (isBgRemover){
           typeFilter = requireContext().resources?.getString(R.string.highlight)
           binding.headingText.text = requireContext().resources?.getString(R.string.highlight)
           setProgress(
               viewModel.minHighlightShadow,
               viewModel.maxHighlightShadow,
               viewModel.currentHighlightShadow,
               viewModel.sectionsHighlightShadow
           )
       }else {
           typeFilter = requireContext().resources?.getString(R.string.brightness)
           binding.headingText.text = requireContext().resources?.getString(R.string.brightness)
           setProgress(
               viewModel.minBright,
               viewModel.maxBright,
               viewModel.currentBright,
               viewModel.sectionsBright
           )
       }
    }

    private fun setUpRecyclerview(activity: FragmentActivity, isBgRemover: Boolean) {
        adapterRecyclerSecondary = AdapterRecyclerSecondary(this, activity)
        binding.adjustmentRecyclerView.adapter = adapterRecyclerSecondary
        adapterRecyclerSecondary?.let { subscribeUi(it, isBgRemover) }
    }

    private fun subscribeUi(adapter: AdapterRecyclerSecondary, isBgRemover: Boolean) {
        lifecycleScope.launch {
            viewModel.loadAdjustmentIcons(requireContext().resources?.getString(R.string.highlight),requireContext().resources?.getString(R.string.exposure),requireContext().resources?.getString(R.string.hue),requireContext().resources?.getString(R.string.contrast), requireContext().resources?.getString(R.string.saturation),requireContext().resources?.getString(R.string.sharpen),requireContext().resources?.getString(R.string.brightness)).collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    override fun onSecondaryRecyclerClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        if (adapterRecyclerSecondary?.getSelectedPosition() != position) {
            adapterRecyclerSecondary?.selectBottomItem(position)
            typeFilter = modelDrawableAssets.imageTitle
            binding.headingText.text = modelDrawableAssets.imageTitle
            localBitmap?.let { originalBitmap = it }
            when (modelDrawableAssets.imageTitle) {
                requireContext().resources?.getString(R.string.brightness) -> {
                    setProgress(
                        viewModel.minBright,
                        viewModel.maxBright,
                        viewModel.currentBright,
                        viewModel.sectionsBright
                    )
                }

                requireContext().resources?.getString(R.string.hue) -> {
                    setProgress(
                        viewModel.minHue,
                        viewModel.maxHue,
                        viewModel.currentHue,
                        viewModel.sectionsHue
                    )
                }

                requireContext().resources?.getString(R.string.contrast) -> {
                    setProgress(
                        viewModel.minContrast,
                        viewModel.maxContrast,
                        viewModel.currentContrast,
                        viewModel.sectionsContrast
                    )
                }

                requireContext().resources?.getString(R.string.highlight) -> {
                    setProgress(
                        viewModel.minHighlightShadow,
                        viewModel.maxHighlightShadow,
                        viewModel.currentHighlightShadow,
                        viewModel.sectionsHighlightShadow
                    )
                }

                requireContext().resources?.getString(R.string.saturation) -> {
                    setProgress(
                        viewModel.minSaturation,
                        viewModel.maxSaturation,
                        viewModel.currentSaturation,
                        viewModel.sectionsSaturation
                    )
                }

                requireContext().resources?.getString(R.string.sharpen) -> {
                    setProgress(
                        viewModel.minSharpness,
                        viewModel.maxSharpness,
                        viewModel.currentSharpness,
                        viewModel.sectionsSharpness
                    )
                }

                requireContext().resources?.getString(R.string.exposure) -> {
                    setProgress(
                        viewModel.minExposure,
                        viewModel.maxExposure,
                        viewModel.currentExposure,
                        viewModel.sectionsExposure
                    )
                }
            }
        }
    }

    private fun setProgress(
        progressMin: Float,
        progressMax: Float,
        progressCurrent: Float,
        sectionCount: Int
    ) {
        binding.progressSeekBar.configBuilder
            .min(progressMin)
            .max(progressMax)
            .progress(progressCurrent)
            .sectionCount(sectionCount)
            .build()
        binding.progressSeekBar.onProgressChangedListener = object : OnProgressChangedListener {
            override fun onProgressChanged(
                bubbleSeekBar: BubbleSeekBar?,
                progress: Int,
                progressFloat: Float
            ) {
//                if (fromUser) {
                    when (typeFilter) {
                        requireContext().resources?.getString(R.string.brightness) -> {
                            viewModel.currentBright = progressFloat
                            localBitmap = BrightnessFilter(originalBitmap, progressFloat)
                        }

                        requireContext().resources?.getString(R.string.hue) -> {
                            viewModel.currentHue = progressFloat
                            localBitmap = HueFilter(originalBitmap, progress)
                        }

                        requireContext().resources?.getString(R.string.contrast) -> {
                            viewModel.currentContrast = progressFloat
                            localBitmap =
                                ContrastFilter(originalBitmap, progressFloat)
                        }

                        requireContext().resources?.getString(R.string.highlight) -> {
                            viewModel.currentHighlightShadow = progressFloat
                            localBitmap =
                                HighlightFilter(originalBitmap, progressFloat)
                        }

                        requireContext().resources?.getString(R.string.saturation) -> {
                            viewModel.currentSaturation = progressFloat
                            localBitmap =
                                SaturationFilter(originalBitmap, progressFloat)
                        }

                        requireContext().resources?.getString(R.string.sharpen) -> {
                            viewModel.currentSharpness = progressFloat
                            localBitmap =
                                SharpnessFilter(originalBitmap, progressFloat)
                        }

                        requireContext().resources?.getString(R.string.exposure) -> {
                            viewModel.currentExposure = progressFloat
                            localBitmap =
                                ExposureFilter(originalBitmap, progressFloat)
                        }
                    }
                    adjustmentSeekbarListener?.onAdjustmentChanged(typeFilter, localBitmap)
//                }
            }

            override fun getProgressOnActionUp(
                bubbleSeekBar: BubbleSeekBar,
                progress: Int,
                progressFloat: Float
            ) {
            }

            override fun getProgressOnFinally(
                bubbleSeekBar: BubbleSeekBar?,
                progress: Int,
                progressFloat: Float
            ) {

            }
        }
    }

    // Filters
    private fun BrightnessFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        brightnessFilter.setBrightness(progress * 0.1f)
        gpuImage.setFilter(brightnessFilter) ///for brightness *0.1f
//        bitmap = gpuImage.bitmapWithFilterApplied
//        return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            // Release GPUImage resources to free memory
            gpuImage.deleteImage()
        }
    }

    private fun HueFilter(bitmap: Bitmap?, progress: Int): Bitmap? {
        if (bitmap==null)return null
        var bitmap = bitmap
        try {
            gpuImage.setImage(bitmap)
            hueFilter.setHue(range(progress, 0.0f, 360.0f))
            gpuImage.setFilter(hueFilter)
        //    bitmap = gpuImage.bitmapWithFilterApplied
        } catch (e: Exception) {
        }
       // return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    private fun range(percentage: Int, start: Float, end: Float): Float {
        return (end - start) * percentage / 100.0f + start
    }

    private fun ContrastFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        contrastFilter.setContrast(progress)
        gpuImage.setFilter(contrastFilter)
//        bitmap = gpuImage.bitmapWithFilterApplied
//        return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    private fun HighlightFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        highlightShadowFilter.setHighlights(1.0f)
        highlightShadowFilter.setShadows(progress)
        gpuImage.setFilter(highlightShadowFilter)
       // bitmap = gpuImage.bitmapWithFilterApplied
      //  return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    private fun SaturationFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        saturationFilter.setSaturation(progress)
        gpuImage.setFilter(saturationFilter)
//        bitmap = gpuImage.bitmapWithFilterApplied
//        return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    private fun SharpnessFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        sharpenFilter.setSharpness(progress)
        gpuImage.setFilter(sharpenFilter)
//        bitmap = gpuImage.bitmapWithFilterApplied
//        return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    private fun ExposureFilter(bitmap: Bitmap?, progress: Float): Bitmap? {
        if (bitmap==null) return null
        var bitmap = bitmap
        gpuImage.setImage(bitmap)
        exposureFilter.setExposure(progress)
        gpuImage.setFilter(exposureFilter)
//        bitmap = gpuImage.bitmapWithFilterApplied
//        return bitmap
        return gpuImage.bitmapWithFilterApplied.also {
            gpuImage.deleteImage()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

}