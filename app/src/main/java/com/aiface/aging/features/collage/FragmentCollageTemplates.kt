package com.aiface.aging.features.collage

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.shared.CollageType
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.databinding.FragmentCollageHomeBinding
import com.aiface.aging.features.collage.dynamic.PuzzleUtils
import com.aiface.aging.features.collage.dynamic.puzzle.PuzzleLayout
import com.aiface.aging.features.collage.dynamic.puzzle.slant.SlantPuzzleLayout
import com.aiface.aging.features.collage.model.TemplateItem
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.TemplateImageUtils
import com.aiface.aging.utils.frame.FrameImageUtils
import kotlinx.coroutines.launch
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FragmentCollageTemplates : Fragment() {

    private lateinit var binding: FragmentCollageHomeBinding
    private var mActivity: FragmentActivity? = null
    private val args: FragmentCollageTemplatesArgs by navArgs()

    private var mAdapter: TemplateAdapter? = null

    //Template views
    private val mTemplateItemList = ArrayList<TemplateItem>()
    private val mAllTemplateItemList = ArrayList<TemplateItem>()
    private var mFrameImages = false
    private var mImageInTemplateCount = 0

    private var collageType = CollageType.Dynamic

    private var nativeCollage: NativeAd? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCollageHomeBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->


            setCollagesTypeAdapter(activity)

            mFrameImages = args.EXTRAISFRAMEIMAGE
            if (mFrameImages) {
                loadFrameImages(false, activity)
            } else {
                loadFrameImages(true, activity)
            }


            val gridLayoutManager = GridLayoutManager(requireContext(), 3)

            gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (mAdapter?.isNativeAdItem(position) == true) {
                        gridLayoutManager.spanCount
                    } else {
                        1
                    }
                }
            }

            mAdapter = TemplateAdapter(activity)
            loadCategory(selectedCategoryPosition, activity)
            binding.rvTemplates.layoutManager = gridLayoutManager
            binding.rvTemplates.adapter = mAdapter
            setClickListeners()

            loadCollageNativeAd(activity)

        }
    }


    private fun loadCollageNativeAd(activity: FragmentActivity) {
        if (!AdsHelper.shouldShowAds()) {
            mAdapter?.clearNativeAd()
            return
        }
        if (AiFaceApp.isNativeCollageHf && AiFaceApp.isNativeCollage) {
            startNativeCollage(tryHigh = true)
        } else if (AiFaceApp.isNativeCollage) {
            startNativeCollage(tryHigh = false)
        }
    }

    private fun startNativeCollage(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                mAdapter?.clearNativeAd()
                return
            }
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_collage_hf,
                normalUnitId = BuildConfig.native_collage,
                onLoaded = { ad, _ ->
                    try {
                        if (!isAdded || view == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeCollage?.destroy()
                        nativeCollage = ad
                        mAdapter?.setNativeAd(ad)
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        mAdapter?.clearNativeAd()
                    }
                },
                onFailed = {
                    if (!isAdded) return@loadWithFallback
                    mAdapter?.clearNativeAd()
                }
            )
        } catch (t: Throwable) {
            mAdapter?.clearNativeAd()
        }
    }

    private fun loadFrameImages(template: Boolean, activity: FragmentActivity) {
        mAllTemplateItemList.clear()
        if (template) {
            mAllTemplateItemList.addAll(TemplateImageUtils.loadTemplates())
        } else {
            mAllTemplateItemList.addAll(FrameImageUtils.loadFrameImages(activity))
        }
        mTemplateItemList.clear()
        if (mImageInTemplateCount > 0) {
            for (item in mAllTemplateItemList) if (item.photoItemList.size == mImageInTemplateCount) {
                mTemplateItemList.add(item)
            }
        } else {
            mTemplateItemList.addAll(mAllTemplateItemList)
        }
    }

    companion object {
        const val EXTRA_IMAGE_PATHS = "imagePaths"
        const val EXTRA_IMAGE_IN_TEMPLATE_COUNT = "imageInTemplateCount"
        const val EXTRA_SELECTED_TEMPLATE_INDEX = "selectedTemplateIndex"
        const val EXTRA_IS_FRAME_IMAGE = "frameImage"
        var selectedCategoryPosition: Int = 0 // Default to the first category
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    private fun setCollagesTypeAdapter(context: Context) {
        val collageHeaders = arrayListOf<String>()
        mActivity?.resources?.getString(R.string.dynamic)?.let { collageHeaders.add(it) }
        mActivity?.resources?.getString(R.string.shapes)?.let { collageHeaders.add(it) }
        mActivity?.resources?.getString(R.string.classic)?.let { collageHeaders.add(it) }
        val adapter = AdapterCollageHeader(context, collageHeaders)
        binding.rvheader.adapter = adapter
        // Set the previously selected position from the static variable
        adapter.setSelectedPosition(selectedCategoryPosition)
        adapter.setCtgListener(object : CollageTypeClickListener {
            override fun onCollageTypeClick(position: Int, item: String) {


                selectedCategoryPosition = position // Update the static variable
                mActivity?.let {
                    loadCategory(selectedCategoryPosition, it)
                    loadCollageNativeAd(it)
                }
            }
        })
    }

    private fun loadCategory(selectedCategoryPosition: Int, activity: FragmentActivity) {
        when (selectedCategoryPosition) {
            0 -> {
                collageType = CollageType.Dynamic
                mAdapter?.isDynamicView = true
                mAdapter?.addPuzzleData(PuzzleUtils.getAllPuzzleLayouts(), null)
                lifecycleScope.launch {
                    val imagesList = AppUtils.fetchImagePathsSorted(
                        activity,
                        "collage/dynamic/all"
                    )
                    mAdapter?.addCustomThumbs(imagesList)
                }
            }

            1 -> {
                collageType = CollageType.Shapes
                mAdapter?.isDynamicView = false
                mAdapter?.addStaticData(mTemplateItemList)
                loadStaticData()
            }

            2 -> {
                collageType = CollageType.Classic
                mAdapter?.isDynamicView = false
                mAdapter?.addStaticData(mTemplateItemList)
                loadStaticData()
            }

            else -> {}
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStaticData() {
        FrameImageUtils.FRAME_FOLDER = when (collageType) {
            CollageType.Dynamic -> ""
            CollageType.Classic -> "collage/classic"
            CollageType.Shapes -> "collage/shape"
        }

        mActivity?.let { activity ->
            if (mFrameImages) {
                loadFrameImages(false, activity)
            } else {
                loadFrameImages(true, activity)
            }
            mAdapter?.notifyDataSetChanged()
        }
    }

    private fun setClickListeners() {
        mAdapter?.setTemplateClickListener { item ->
            val imageLimit = item?.photoItemList?.size ?: 0
            val bundle = Bundle()
            bundle.apply {
                putParcelableArrayList("mTemplateItemList", mTemplateItemList)
                putBoolean("mFrameImages", mFrameImages)
                putInt("mImageInTemplateCount", mImageInTemplateCount)
                putInt("mSelectedTemplateIndex", mTemplateItemList.indexOf(item))
                putBoolean("isDynamic", false)
            }
            mActivity?.let {
                TedImagePicker.with(it, "collage")
                    .max(imageLimit, "cannot select more than $imageLimit images")
                    .min(imageLimit, "select at least $imageLimit images for current template")
                    .bundleExtras(bundle)
                    .albumType(AlbumType.DROP_DOWN).startMultiImageFragment()
            }
        }

        mAdapter?.setPuzzleItemClickListener(object : PuzzleItemClickListener {
            override fun onPuzzleItemClick(layout: PuzzleLayout, themeId: Int) {

                val type = if (layout is SlantPuzzleLayout) 0 else 1
                val maxCount = layout.areaCount
                //below count is added in PuzzleDynamicBottomRecyclerAdapter's selectedPosition from PuzzleCollageViewActivity
                var count = 0
                if (type == 1) {
                    count = if (maxCount == 2) 2 else if (maxCount == 3) 6 else 0
                }


                mActivity?.let {
                    val bundle = Bundle()
                    bundle.apply {
                        putBoolean("isDynamic", true)
                        putInt("type", type)
                        putInt("theme_id", themeId)
                        putInt("piece_size", maxCount)
                        putInt("count", count)
                    }
                    TedImagePicker.with(it, "collage")
                        .max(maxCount, "cannot select more than $maxCount images")
                        .min(maxCount, "select at least $maxCount images for current template")
                        .bundleExtras(bundle)
                        .albumType(AlbumType.DROP_DOWN).startMultiImageFragment()
                }
            }
        })

        binding?.back?.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        nativeCollage?.destroy()
        nativeCollage = null
        super.onDestroyView()
    }
}