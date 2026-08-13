package com.aiface.aging.features.frames.editor

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.loadEditorAdaptiveBanner
import com.aiface.aging.shared.ads.preloadInterEditSave
import com.aiface.aging.shared.ads.reloadEditorBanner
import com.aiface.aging.shared.ads.showHomeInterstitialThen

import com.aiface.aging.shared.hide
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.hideSystemUI
import com.aiface.aging.shared.show
import com.aiface.aging.shared.showExitEditingDialogue
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.utils.multitouchlistener.MultiTouchListener
import com.aiface.aging.utils.multitouchlistener.OnDoubleTapListener
import com.aiface.aging.utils.multitouchlistener.OnImageViewTouchListener
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.saveBitmapToTempCache

import com.aiface.aging.utils.DailyCounterRemover
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.FrameUtils
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.ToastUtils
import com.aiface.aging.databinding.ActivityAllFramesEditorBinding
import com.aiface.aging.features.editor.model.ModelFrameHeader
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.shared.editorui.AdapterBottomRecycler
import com.aiface.aging.shared.editorui.BottomFeaturesCallback
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.features.frames.FragmentFramesBottom
import com.aiface.aging.features.frames.FrameUpdateListener
import com.aiface.aging.features.text.text.TextFragment
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.frames.maskable.MaskableFrameLayout
import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.StickerView.OnStickerOperationListener
import com.xiaopo.flying.sticker.TextStickerCustom
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class AllFramesEditorActivity : AppCompatActivity(),
    OnDoubleTapListener, BottomFeaturesCallback,
    FrameUpdateListener, OnImageViewTouchListener, BottomActionListener {
    private lateinit var binding: ActivityAllFramesEditorBinding
    private val viewModel: ViewModelAllFramesEditor by viewModels()
    private var currentImageCount = 1
    private var modelFramesPack: ModelFramePack? = null

    private var currentImageIndex = 0
    private var adapterBottomRecycler: AdapterBottomRecycler? = null

    val rewardDialog: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_save_image)
    }

    val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    }
    private var isSaved = false
    private var isSaving = false
    private var isBackPressed = false
    private var originalXUserImage1 = 0f
    private var originalYUserImage1 = 0f
    private var originalXUserImage2 = 0f
    private var originalYUserImage2 = 0f

    private var originalScaleUserImage1 = 1.0f
    private var originalScaleUserImage2 = 1.0f

    private var originalRotationUserImage1 = 0f
    private var originalRotationUserImage2 = 0f

    private var reload = false
    private var bannerAdView: Any? = null


    fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.parentLayout)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {


            binding = ActivityAllFramesEditorBinding.inflate(layoutInflater)
            setContentView(binding.root)
          //  hideSystemUI()
            hideNavigationBar()
            applyWindowInsets()
            FirebaseLogUtils.logEvent("frame_editor_view", "user view editor screen")
            modelFramesPack = intent.extras!!.getParcelable(Extras.MODEL_FRAME_PACK)

            updateFrame(modelFramesPack)

            if (isProVersion.value != true) {
                DailyCounterRemover.incrementCounter(this)
                checkDailyCounterFrame()
            }

            preloadInterEditSave(this)
            loadEditorAdaptiveBanner(
                activity = this,
                bannerContainer = binding.bannerAdView,
                shimmerView = binding.shimmer,
                clAd = binding.clAd,
            )

            setUpToolbar()
            initUserImagePositions()
            addUserImageList()
            setMultiTouchListeners()
            initOnClickListeners()
            setUpBottomRecyclerview()
            stickerOperationListener()
            modelFramesPack?.let {
                showFragment(
                    FragmentFramesBottom.newInstance(
                        getHeader(),
                        this,
                        it,
                        false,
                        null,
                        actionListener = this
                    )
                )
            }

        } catch (e: Exception) {
            finish()
        }
    }

//    private fun loadAds() {
//        AdManager.preloadNativeAd(AdScreen.DISCARD_DIALOG_NATIVE_BOTTOM)
//        AdManager.loadInterstitial(this, AdScreen.TEMPLATE_SCREEN_CLICK_SAVE, {
//            //onAdFailed
//
//        }, {
//            //onAdLoaded
//
//        })
//
//    }

    private fun setUpToolbar() {
        binding.customToolbar.titleActionbar.text = getString(R.string.frames_editor)
        binding.customToolbar.backActionbar.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "frame_editor_btn_back_click",
                "user click button back on frame editor screen"
            )
            onBackPressed()
        }
        binding.customToolbar.doneActionButton.setOnClickListener {
            if (isSaving || isFinishing || isDestroyed) return@setOnClickListener
            reload = true
            FirebaseLogUtils.logEvent(
                "frame_editor_btn_save_click",
                "user click button save on frame editor screen"
            )
            saveImage()
        }
    }

    private fun beginSaveFlow() {
        isSaving = true
        binding.customToolbar.doneActionButton.isEnabled = false
        binding.customToolbar.doneActionButton.isClickable = false
    }

    private fun endSaveFlow() {
        isSaving = false
        binding.customToolbar.doneActionButton.isEnabled = true
        binding.customToolbar.doneActionButton.isClickable = true
    }

    private fun saveImage() {
        if (isSaving || isFinishing || isDestroyed) return
        beginSaveFlow()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    withContext(Dispatchers.Main) {
                        if (!isFinishing && !isDestroyed) {
                            SaveProgressHelper.showProcessing(this@AllFramesEditorActivity)
                        }
                    }

                    val path = withContext(Dispatchers.IO) {
                        ImageUtils.saveBitmapToCache(
                            this@AllFramesEditorActivity,
                            binding.stickerView.createBitmap()
                        )
                    }

                    withContext(Dispatchers.Main) {
                        SaveProgressHelper.hide(this@AllFramesEditorActivity)
                        path?.let {
                            adapterBottomRecycler?.unselectBottomItem()
                            try {
                                FirebaseLogUtils.logEvent(
                                    "frame_editor_save_success",
                                    "user saved image on frame editor screen"
                                )
                                isSaved = true
                                showHomeInterstitialThen {
                                    ResultLauncher.openLocalPreview(
                                        this@AllFramesEditorActivity,
                                        ResultSource.PHOTO_EDITOR,
                                        path = path,
                                    )
                                    endSaveFlow()
                                }
                            } catch (_: Exception) {
                                endSaveFlow()
                            }
                        } ?: run {
                            ToastUtils.showErrorToast(this@AllFramesEditorActivity)
                            endSaveFlow()
                        }
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) {
                        SaveProgressHelper.hide(this@AllFramesEditorActivity)
                        endSaveFlow()
                    }
                }
            }
        } catch (e: Exception) {
            SaveProgressHelper.hide(this)
            ToastUtils.showErrorToast(this@AllFramesEditorActivity)
            endSaveFlow()
        }
    }

    private fun initUserImagePositions() {
        // Store original position
        originalXUserImage1 = binding.userImage1.x
        originalYUserImage1 = binding.userImage1.y
        originalXUserImage2 = binding.userImage2.x
        originalYUserImage2 = binding.userImage2.y

        // Store original zoom scale
        originalScaleUserImage1 = binding.userImage1.scaleX
        originalScaleUserImage2 = binding.userImage2.scaleX

        // Store original rotation angle
        originalRotationUserImage1 = binding.userImage1.rotation
        originalRotationUserImage2 = binding.userImage2.rotation
    }

    private fun updateFrame(modelFramePack: ModelFramePack?) {
        modelFramePack?.let { it ->
            updateMainLayoutDimensionRatio(it.dimensionFrame)
            when (getImageCount(modelFramePack)) {
                1 -> {
                    it.constraintSet1?.let {
                        updateLayoutParameters(binding.layout1, it)
                    }
                    binding.layout2.hide()
                }

                2 -> {
                    binding.layout2.show()
                    it.constraintSet1?.let {
                        updateLayoutParameters(binding.layout1, it)
                    }
                    it.constraintSet2?.let {
                        updateLayoutParameters(binding.layout2, it)
                    }
                }
            }

            showHideBlurImageView()
            removeMask(binding.masklayout1)
            removeMask(binding.masklayout2)
            modelFramePack.mask1?.let { updateMask(binding.masklayout1, it) }
            modelFramePack.mask2?.let { updateMask(binding.masklayout2, it) }

            if (!isFinishing && !isDestroyed) {
                Glide.with(binding.frameImage.context).load(modelFramePack.file).override(800)
                    .into(object : CustomTarget<Drawable>() {
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            binding.frameImage.setImageDrawable(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                    })
            }
        }
    }

    private fun removeMask(maskableFrameLayout: MaskableFrameLayout) {
        if (maskableFrameLayout.drawableMask != null) maskableFrameLayout.removeMask()
    }

    private fun updateMask(maskableFrameLayout: MaskableFrameLayout, maskPath: String) {
        if (!isFinishing && !isDestroyed) {
            Glide.with(maskableFrameLayout.context).load(maskPath).override(800)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        maskableFrameLayout.setMask(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
        }
    }

    private fun getImageCount(modelFramePack: ModelFramePack): Int {
        var count = 0

        if (!modelFramePack.constraintSet1.isNullOrBlank()) {
            count++
        }

        if (!modelFramePack.constraintSet2.isNullOrBlank()) {
            count++
        }

        if (!modelFramePack.constraintSet3.isNullOrBlank()) {
            count++
        }
        currentImageCount = count
        return count
    }

    private fun addUserImageList() {
        var listImages: ArrayList<String>? = intent.getStringArrayListExtra(Extras.PICKER_IMG_LIST)
        listImages?.let { list ->
            for (i in 0 until list.size) {
                val imagePath = list[i]
                loadImage(imagePath, i)
            }
            if (list.isNotEmpty()) {
                applyBlurView(list[0])
            }
        }
    }

    private fun loadImage(imagePath: String, index: Int) {
        val imageView = when (index) {
            0 -> binding.userImage1
            1 -> binding.userImage2
            else -> null
        }
        val plusImageView = when (index) {
            0 -> binding.addUserImage1
            1 -> binding.addUserImage2
            else -> null
        }
        plusImageView?.let { it.hide() }
        imageView?.let {
            it.show()
            resetUserImagePosition(it)
            if (!isFinishing && !isDestroyed) {
                Glide.with(it.context).asBitmap().override(800).load(imagePath)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap,
                            transition: Transition<in Bitmap>?
                        ) {
                            it.setImageBitmap(resource)
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                    })
            }
        }
    }

    private fun applyBlurView(path: String) {
        if (!isFinishing && !isDestroyed) {
            Glide.with(this)
                .asBitmap().load(path)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        binding.blurBgImage.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
        }
    }

    private fun setUpBottomRecyclerview() {
        adapterBottomRecycler = AdapterBottomRecycler(this, this, isEditor = true)
        binding.editorFeaturesRecycler.adapter = adapterBottomRecycler
        adapterBottomRecycler?.let { subscribeUi(it) }
    }

    private fun subscribeUi(adapter: AdapterBottomRecycler) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadBottomIcons(this@AllFramesEditorActivity).collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun updateMainLayoutDimensionRatio(ratioValue: String?) {
        try {
            binding.mainLayout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = 0
                width = 0
                topToBottom = R.id.customToolbar
                bottomToTop = R.id.cl_bottom
                startToStart = R.id.parentLayout
                endToEnd = R.id.parentLayout
                dimensionRatio = ratioValue
            }
        } catch (e: Exception) {
            ToastUtils.showToast(this, "" + e)
        }
    }

    private fun updateLayoutParameters(layout: ConstraintLayout, constraintSets: String?) {
        constraintSets?.let { constraintString ->
            try {
                val parts = constraintString.split(",")
                val constraintSetValues = parts.map { it.toFloat() }
                layout.updateLayoutParams<ConstraintLayout.LayoutParams> {
                    height = 0
                    width = 0
                    topToTop = R.id.childLayout
                    bottomToBottom = R.id.childLayout
                    startToStart = R.id.childLayout
                    endToEnd = R.id.childLayout
                    matchConstraintPercentHeight = constraintSetValues[0]
                    matchConstraintPercentWidth = constraintSetValues[1]
                    verticalBias = constraintSetValues[2]
                    horizontalBias = constraintSetValues[3]
                }
            } catch (e: Exception) {
                ToastUtils.showToast(this, "" + e)
            }
        }
    }

    fun clearAllFragments(activity: AppCompatActivity) {
        val fragmentManager: FragmentManager = activity.supportFragmentManager

        // Start transaction to remove all fragments
        val transaction = fragmentManager.beginTransaction()
        for (fragment in fragmentManager.fragments) {
            transaction.remove(fragment)
        }
        transaction.commit()

        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun showFragment(fragment: Fragment) {
        clearAllFragments(this)
        binding.fragmentContainer.show()
        AppUtils.loadFragment(R.id.fragmentContainer, fragment, this)
        // Keep toolbar visible so back/save always reachable with template sheet open
        binding.customToolbar.toolbar.show()
    }

    private fun hideFragment() {
        try {
            val fragmentManager = supportFragmentManager
            if (!fragmentManager.isDestroyed && !isFinishing) {
                binding.customToolbar.toolbar.show()
                adapterBottomRecycler?.unselectBottomItem()
                fragmentManager.popBackStack()
                binding.fragmentContainer.hide()
            }
        } catch (e: Exception) {

        }
    }

    private fun showHideBlurImageView() {
        binding.blurBgImage.visibility = when (modelFramesPack?.editor) {
            "pip" -> View.VISIBLE
            else -> {
                View.GONE
            }
        }
    }

    private fun initOnClickListeners() {
        binding.addUserImage1.setOnClickListener {
            currentImageIndex = 0
            openImagePicker(currentImageIndex)
        }
        binding.addUserImage2.setOnClickListener {
            currentImageIndex = 1
            openImagePicker(currentImageIndex)
        }
        binding.root.setOnClickListener {
            //   hideFragment()
            //   binding.stickerView.hideBorders()
        }
    }

    private fun openImagePicker(index: Int) {
        TedImagePicker.with(this@AllFramesEditorActivity, "editor").start {
            loadImage(it.toString(), index)
            if (index == 0) applyBlurView(it.toString())
            hideFragment()
        }
    }

    private fun setMultiTouchListeners() {
        binding.userImage1.setOnTouchListener(
            MultiTouchListener(
                applicationContext,
                binding.userImage1,
                false,
                this@AllFramesEditorActivity,
                this
            )
        )

        binding.userImage2.setOnTouchListener(
            MultiTouchListener(
                applicationContext,
                binding.userImage2,
                false,
                this@AllFramesEditorActivity,
                this
            )
        )
    }


    override fun onDoubleTapListner(view: View?) {
        openImagePicker(currentImageIndex)
    }

    override fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        if (isProVersion.value == false) {
            if (AiFaceApp.isBannerEditHf && AiFaceApp.isBannerEdit) {
                reloadEditorBanner(
                    adContainer = binding.bannerAdView,
                    activity = this,
                    shimmerView = binding.shimmer,
                    clAd = binding.clAd,
                    bannerAdView = bannerAdView,
                    normalAdId = BuildConfig.banner_home,
                    highFloorAdId = BuildConfig.banner_home_high,
                )
                binding.clAd.show()
            } else if (AiFaceApp.isBannerEdit) {
                reloadEditorBanner(
                    adContainer = binding.bannerAdView,
                    activity = this,
                    shimmerView = binding.shimmer,
                    clAd = binding.clAd,
                    bannerAdView = bannerAdView,
                    normalAdId = BuildConfig.banner_home,
                    highFloorAdId = BuildConfig.banner_home,
                )
                binding.clAd.show()
            } else {
                binding.clAd.hide()
                binding.shimmer.hide()
            }
        }
        adapterBottomRecycler?.selectBottomItem(position)
        when (modelDrawableAssets.id) {
            1 -> {
                modelFramesPack?.let {
                    showFragment(
                        FragmentFramesBottom.newInstance(
                            getHeader(),
                            this,
                            it,
                            false,
                            null, actionListener = this
                        )
                    )
                }
            }

            5 -> {
                adapterBottomRecycler?.unselectBottomItem()
                binding.fragmentContainer.hide()
                openImagePicker(currentImageIndex)
            }

            4 -> {
                binding.fragmentContainer.hide()
                flipImage(currentImageIndex)
            }

            2 -> {
                // Stickers pack not ported yet — skip
                ToastUtils.showToast(this, "Stickers coming soon")
                adapterBottomRecycler?.unselectBottomItem()
            }

            3 -> {
                showFragment(
                    TextFragment.newInstance(
                        binding.stickerView,
                        true,
                        actionListener = this
                    )
                )
            }
        }
    }

    private fun flipImage(currentImageIndex: Int) {
        val userImageBitmap = when (currentImageIndex) {
            0 -> {
                binding.userImage1.currentBitmap
            }

            1 -> {
                binding.userImage2.currentBitmap
            }

            else -> null
        }
        var matrix = Matrix()
        matrix.preScale(-1f, 1f)
        CoroutineScope(Dispatchers.IO).launch {
            val bitmapUser = withContext(Dispatchers.IO) {
                userImageBitmap?.let {
                    Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, false)
                }
            }
            withContext(Dispatchers.Main) {
                bitmapUser?.let {
                    updateImage(it, currentImageIndex)
                }
            }
        }
    }

    private fun updateImage(bitmap: Bitmap?, currentImageIndex: Int) {
        Glide.with(this).asBitmap().load(bitmap).into(object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                when (currentImageIndex) {
                    0 -> {
                        binding.userImage1.setImageBitmap(resource)
                    }

                    1 -> {
                        binding.userImage2.setImageBitmap(resource)
                    }
                }
                adapterBottomRecycler?.unselectBottomItem()
            }

            override fun onLoadCleared(placeholder: Drawable?) {

            }

        })
    }


    private fun getHeader(): ModelFrameHeader {
        return FrameUtils.getTopFramesHeader()
    }


    private fun stickerOperationListener() {
        binding.stickerView.onStickerOperationListener = object : OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                StickerView.isSelected = sticker is TextStickerCustom
            }

            override fun onStickerClicked(sticker: Sticker) {
                if (sticker is TextStickerCustom) {
                    StickerView.isSelected = true
                    showFragment(
                        TextFragment.newInstance(
                            binding.stickerView,
                            false,
                            actionListener = this@AllFramesEditorActivity
                        )
                    )
                } else {
                    StickerView.isSelected = false
                }
            }

            override fun onStickerDeleted(sticker: Sticker) {
                if (sticker is TextStickerCustom) {
                    binding.customToolbar.toolbar.show()
                    binding.stickerView.hideBorders()
                    StickerView.isSelected = false
                    binding.fragmentContainer.hide()
                    adapterBottomRecycler?.unselectBottomItem()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {}
            override fun onStickerTouchedDown(sticker: Sticker) {}
            override fun onStickerZoomFinished(sticker: Sticker) {}
            override fun onStickerFlipped(sticker: Sticker) {}
            override fun onStickerDoubleTapped(sticker: Sticker) {}
        }
    }


    override fun onFrameUpdate(modelFramePack: ModelFramePack) {
        if (this.modelFramesPack?.id == modelFramePack.id) return
        modelFramesPack = modelFramePack
        updateFrame(modelFramePack)
        showHideBlurImageView()
        resetUserImagePosition(binding.userImage1)
        resetUserImagePosition(binding.userImage2)
    }

    override fun onImageTouchListener(view: View) {
        if (view == binding.userImage1) {
            currentImageIndex = 0
        } else if (view == binding.userImage2) {
            currentImageIndex = 1
        }
        //  hideFragment()
    }


    private fun resetUserImagePosition(imageView: ImageView) {
        // Restore positions to the initial position
        if (imageView == binding.userImage1) {
            binding.userImage1.x = originalXUserImage1
            binding.userImage1.y = originalYUserImage1

            // Restore the zoom to the initial scale
            binding.userImage1.scaleX = originalScaleUserImage1
            binding.userImage1.scaleY = originalScaleUserImage1

            // Restore the rotation to the initial angle (0 degrees)
            binding.userImage1.rotation = originalRotationUserImage1
        } else if (imageView == binding.userImage2) {
            // Restore positions to the initial position
            binding.userImage2.x = originalXUserImage2
            binding.userImage2.y = originalYUserImage2

            // Restore the zoom to the initial scale
            binding.userImage2.scaleX = originalScaleUserImage2
            binding.userImage2.scaleY = originalScaleUserImage2

            // Restore the rotation to the initial angle (0 degrees)
            binding.userImage2.rotation = originalRotationUserImage2
        }
    }

    override fun onBackPressed() {
        isBackPressed = true
        if (binding.fragmentContainer.isVisible) {
            binding.customToolbar.toolbar.show()
            binding.fragmentContainer.hide()
            adapterBottomRecycler?.unselectBottomItem()
            if (supportFragmentManager.backStackEntryCount > 1) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        } else {
            if (isSaved) {
                finish()
            } else {
                showExitEditingDialogue(exitDialogue) {
                    saveImage()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onActionTickClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "frame" -> {
                adapterBottomRecycler?.unselectBottomItem()
                binding.fragmentContainer.hide()
                binding.customToolbar.toolbar.show()
            }
        }
    }

    override fun onActionCancelClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "frame" -> {
                adapterBottomRecycler?.unselectBottomItem()
                binding.fragmentContainer.hide()
                binding.customToolbar.toolbar.show()
            }
        }
    }

    private fun checkDailyCounterFrame() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val lastSavedDate = DailyCounterRemover.getLastSavedDate(this)

        if (currentDate != lastSavedDate) {
            DailyCounterRemover.resetCounter(this)
        }

    }

    override fun onPause() {
        super.onPause()

        if (!isSaved && !isBackPressed) {
            try {
                AiFaceApp.isEditing = true
                AiFaceApp.editorName = "AllFramesEditorActivity"
                saveBitmapToTempCache(this, binding.stickerView.createBitmap())?.let {
                    AiFaceApp.tempImag = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}