package com.aiface.aging.features.collage.static_

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.databinding.ActivityCollageEditorBinding
import com.aiface.aging.shared.editorui.AdapterBottomRecycler
import com.aiface.aging.features.collage.AdapterCollageBGs
import com.aiface.aging.features.collage.BGsCallback
import com.aiface.aging.shared.editorui.BottomFeaturesCallback
import com.aiface.aging.features.collage.FragmentCollageTemplates
import com.aiface.aging.features.collage.ImageUtils
import com.aiface.aging.shared.editorui.AdapterRatio
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.shared.editorui.RatioListener
import com.aiface.aging.features.collage.model.TemplateItem
import com.aiface.aging.utils.TemplateImageUtils
import com.aiface.aging.utils.frame.FrameImageUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.getValue

/**
 * Created by admin on 7/4/2016.
 */
@AndroidEntryPoint
abstract class CollageBaseActivity : AppCompatActivity(),
    HorizontalPreviewTemplateAdapter.OnPreviewTemplateClickListener,
    BottomFeaturesCallback, GridPhotoLayout.OnQuickActionClickListener, RatioListener, BGsCallback {

    protected val viewModel: ViewModelCollageEditor by viewModels()

    protected var mOutputScale = 1f

    protected var mSelectedTemplateItem: TemplateItem? = null
    private var mTemplateItemList: ArrayList<TemplateItem?>? = ArrayList()
    private var mImageInTemplateCount = 0

    private var mTemplateAdapter: HorizontalPreviewTemplateAdapter? = null

    protected var mSelectedPhotoPaths = ArrayList<String>() // paths

    protected var mLayoutRatio = RATIO_SQUARE
    private var mIsFrameImage = true

    var adapterBottomRecycler: AdapterBottomRecycler? = null
    var adapterCollageBGs: AdapterCollageBGs? = null
    var adapterRatio: AdapterRatio? = null

    var currentImage: GridPhotoImageView? = null

    protected var lastEditPosition = 0

    protected lateinit var binding: ActivityCollageEditorBinding

    protected abstract fun buildLayout(templateItem: TemplateItem?)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCollageEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mLayoutRatio = RATIO_FIT
        mImageInTemplateCount =
            intent.getIntExtra(FragmentCollageTemplates.EXTRA_IMAGE_IN_TEMPLATE_COUNT, 0)
        mIsFrameImage = intent.getBooleanExtra(FragmentCollageTemplates.EXTRA_IS_FRAME_IMAGE, true)
        val selectedItemIndex =
            intent.getIntExtra(FragmentCollageTemplates.EXTRA_SELECTED_TEMPLATE_INDEX, 0)
        val extraImagePaths = intent
            .getStringArrayListExtra(FragmentCollageTemplates.EXTRA_IMAGE_PATHS)

        setUpBottomRecyclerview()
        setUpRatioRecyclerview()
        setUpBGsRecyclerview()
        //loading data
        if (savedInstanceState != null) {
            val idx = savedInstanceState.getInt("mSelectedTemplateItemIndex", 0)
            mImageInTemplateCount = savedInstanceState.getInt("mImageInTemplateCount", 0)
            mIsFrameImage = savedInstanceState.getBoolean("mIsFrameImage", false)
            loadFrameImages(mIsFrameImage)
            if (idx < mTemplateItemList!!.size && idx >= 0) mSelectedTemplateItem =
                mTemplateItemList!![idx]
            if (mSelectedTemplateItem != null) {
                val imagePaths = savedInstanceState.getStringArrayList("photoItemImagePaths")
                if (imagePaths != null) {
                    val size = Math.min(imagePaths.size, mSelectedTemplateItem!!.photoItemList.size)
                    for (i in 0 until size) mSelectedTemplateItem!!.photoItemList[i].imagePath =
                        imagePaths[i]
                }
            }
            val entities =
                savedInstanceState.getParcelableArrayList<MultiTouchEntity>("mPhotoViewImageEntities")
            if (entities != null) {
            }
        } else {
            try {
                loadFrameImages(mIsFrameImage)
                mSelectedTemplateItem = mTemplateItemList?.get(selectedItemIndex)
                mSelectedTemplateItem?.isSelected = true
                if (extraImagePaths != null) {
                    val size =
                        extraImagePaths.size.coerceAtMost(mSelectedTemplateItem?.photoItemList?.size?:2)
                    for (i in 0 until size) mSelectedTemplateItem?.photoItemList[i]?.imagePath =
                        extraImagePaths[i]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mTemplateItemList?.let {
            mTemplateAdapter = HorizontalPreviewTemplateAdapter(it, this)
            //Show templates
            binding.templateView.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.templateView.adapter = mTemplateAdapter
        }
        //Create after initializing
        binding.containerLayout.viewTreeObserver.addOnGlobalLayoutListener(object :
            OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mOutputScale = ImageUtils.calculateOutputScaleFactor(
                    binding.containerLayout.width, binding.containerLayout!!.height
                )
                buildLayout(mSelectedTemplateItem)
                // remove listener
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    binding.containerLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
                } else {
                    binding.containerLayout.viewTreeObserver.removeGlobalOnLayoutListener(this)
                }
            }
        })
        //Scroll to selected single_item
        if (mTemplateItemList != null && selectedItemIndex >= 0 && selectedItemIndex < mTemplateItemList!!.size) {
            binding.templateView.scrollToPosition(selectedItemIndex)
        }
    }


    private fun setUpBottomRecyclerview() {
        adapterBottomRecycler = AdapterBottomRecycler(this, this, isEditor=true)
        binding.collageFeaturesRecycler.adapter = adapterBottomRecycler
        adapterBottomRecycler?.let { subscribeUi(it) }
    }

    private fun subscribeUi(adapter: AdapterBottomRecycler) {
        lifecycleScope.launch {
            viewModel.loadBottomIcons(this@CollageBaseActivity).collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }


    private fun setUpBGsRecyclerview() {
        adapterCollageBGs = AdapterCollageBGs(this, this)
        binding.rvBGs.adapter = adapterCollageBGs
        adapterCollageBGs?.let { subscribeBGsUi(it) }
    }

    private fun subscribeBGsUi(adapter: AdapterCollageBGs) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadBGsList().collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun setUpRatioRecyclerview() {
        adapterRatio = AdapterRatio(this, this)
        binding.rvRatios.adapter = adapterRatio
        adapterRatio?.let { subscribeRatioUi(it) }
    }

    private fun subscribeRatioUi(adapter: AdapterRatio) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadRatioIcons().collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun loadFrameImages(isFrameImage: Boolean) {
        val mAllTemplateItemList = ArrayList<TemplateItem?>()
        if (!isFrameImage) {
            mAllTemplateItemList.addAll(TemplateImageUtils.loadTemplates())
        } else {
            mAllTemplateItemList.addAll(FrameImageUtils.loadFrameImages(this))
        }
        mTemplateItemList = ArrayList()
        if (mImageInTemplateCount > 0) {
            for (item in mAllTemplateItemList) if (item!!.photoItemList.size == mImageInTemplateCount) {
                mTemplateItemList!!.add(item)
            }
        } else {
            mTemplateItemList!!.addAll(mAllTemplateItemList)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var idx = mTemplateItemList!!.indexOf(mSelectedTemplateItem)
        if (idx < 0) idx = 0
        outState.putInt("mSelectedTemplateItemIndex", idx)
        //saved all image path of template single_item
        val imagePaths = ArrayList<String>()
        for (item in mSelectedTemplateItem!!.photoItemList) {
            if (item.imagePath == null) item.imagePath = ""
            imagePaths.add(item.imagePath)
        }
        outState.putStringArrayList("photoItemImagePaths", imagePaths)
        outState.putInt("mImageInTemplateCount", mImageInTemplateCount)
        outState.putBoolean("mIsFrameImage", mIsFrameImage)
    }

    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
    }

    override fun onPreviewTemplateClick(item: TemplateItem?) {
        item?.let { loadTemplate(it) }
    }

    protected fun loadTemplate(item: TemplateItem) {
        mSelectedTemplateItem!!.isSelected = false
        for (idx in mSelectedTemplateItem!!.photoItemList.indices) {
            val photoItem = mSelectedTemplateItem!!.photoItemList[idx]
            if (photoItem.imagePath != null && photoItem.imagePath.length > 0) {
                if (idx < mSelectedPhotoPaths.size) {
                    mSelectedPhotoPaths.add(idx, photoItem.imagePath)
                } else {
                    mSelectedPhotoPaths.add(photoItem.imagePath)
                }
            }
        }
        val size = Math.min(mSelectedPhotoPaths.size, item.photoItemList.size)
        for (idx in 0 until size) {
            val photoItem = item.photoItemList[idx]
            if (photoItem.imagePath == null || photoItem.imagePath.isEmpty()) {
                photoItem.imagePath = mSelectedPhotoPaths[idx]
            }
        }
        mSelectedTemplateItem = item
        mSelectedTemplateItem!!.isSelected = true
        mTemplateAdapter!!.notifyDataSetChanged()
        buildLayout(item)
    }
    protected fun setCollageBG(modelDrawableAssets: ModelDrawableAssets?) {
        modelDrawableAssets?.let {
            Glide.with(binding.containerLayout.context)
                .load(it.drawable).override(800)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        binding.containerLayout.background = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
        }
    }

    companion object {
        const val RATIO_SQUARE = 0
        const val RATIO_GOLDEN = 2
        const val RATIO_FIT = 1
        var isEditor = false
    }
}