package com.aiface.aging.features.editor

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kaopiz.kprogresshud.KProgressHUD
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.features.filters.model.ModelFilterPack

import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.rememberAdUnitId
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.BackPressGuard
import com.aiface.aging.shared.ClickGuard
import com.aiface.aging.shared.goHomeFresh
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.applyLightSystemBars
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.shared.show
import com.aiface.aging.shared.showExitEditingDialogue
import com.aiface.aging.databinding.ActivityEditorBinding
import com.aiface.aging.features.adjustment.AdjustmentFragment
import com.aiface.aging.features.adjustment.AdjustmentSeekbarListener
import com.aiface.aging.shared.editorui.AdapterBottomRecycler
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.shared.editorui.BottomFeaturesCallback
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.shared.editorui.ModelRatio
import com.aiface.aging.features.cropper.CropperCallback
import com.aiface.aging.features.cropper.FragmentCropper
import com.aiface.aging.features.filters.FilterUpdateCallback
import com.aiface.aging.features.filters.FragmentFilters
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.features.rotate.RotateFragment
import com.aiface.aging.features.rotate.RotateListener
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.features.text.text.TextFragment
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.CropImageView
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.FrameUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.ToastUtils


import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.StickerView.OnStickerOperationListener
import com.xiaopo.flying.sticker.TextStickerCustom
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wysaid.common.SharedContext
import org.wysaid.nativePort.CGEImageHandler


@AndroidEntryPoint
class EditorActivity : AppCompatActivity(),
    BottomFeaturesCallback, FilterUpdateCallback, AdjustmentSeekbarListener, CropperCallback,
    RotateListener, BottomActionListener {
    private lateinit var binding: ActivityEditorBinding
    private val viewModel: ViewModelEditorActivity by viewModels()
    private lateinit var filtersLoadingDialogue: KProgressHUD
    private var adapterBottomRecycler: AdapterBottomRecycler? = null
    private var localOriginalBitmap: Bitmap? = null
    private var localFilterBitmap: Bitmap? = null

    private var isBackPressed = false
    private var originalRemovedBackgroundPath = ""


    val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    }
    private var isSaved = false
    /** Blocks multi-tap save → duplicate inter / navigate. */
    private var isSaveInProgress = false
    private var isInterFlowInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        applyLightSystemBars()
        applyEditorWindowInsets()
        hideNavigationBar()
        setUpToolbar()
        initCropper()
        loadImage()

        initOnClickListeners()
        setUpBottomRecyclerview()
        stickerOperationListener()

        filtersLoadingDialogue = DialogueUtils.getWaitDialogue(getString(R.string.applying), this)


        if (AiFaceApp.isInterEditSaveHf && AiFaceApp.isInterEditSave) {
            loadInterCollageHf(this)
        } else if (AiFaceApp.isInterEditSave) {
            loadInterCollage(this)
        }

        loadAdaptiveBannerAd(
            binding?.bannerAdView!!,
            this,
            BuildConfig.banner_home,
            BuildConfig.banner_home_high
        )
    }


    private fun loadAdaptiveBannerAd(
        adContainer: FrameLayout,
        activity: FragmentActivity,
        normalAdId: String,
        highFloorAdId: String
    ) {
        try {
            // Premium ya offline → hide
            if (isProVersion.value == true || !NetworkUtils.isOnline(activity)) {
                adContainer.visibility = View.GONE
                binding?.shimmer?.visibility = View.GONE
                binding?.clAd?.visibility = View.GONE
                return
            }

            if (AiFaceApp.isBannerEditHf && AiFaceApp.isBannerEdit) {
                binding?.clAd?.visibility = View.VISIBLE
                binding?.shimmer?.visibility = View.GONE
                AdsHelper.loadBanner(
                    activity = activity,
                    highFloorAdId = highFloorAdId,
                    normalAdId = normalAdId,
                    showHighFloor = true,
                    showNormalFloor = true,
                    onLoaded = {
                        binding?.clAd?.visibility = View.VISIBLE
                        LogUtils.printLog("home_banner loaded", highFloorAdId)
                    },
                    onAdFailed = {
                        adContainer.visibility = View.GONE
                        binding?.clAd?.visibility = View.GONE
                        LogUtils.printLog("home_banner failed to load", highFloorAdId)
                    },
                    adContainer = adContainer
                )
            } else if (AiFaceApp.isBannerEdit) {
                binding?.clAd?.visibility = View.VISIBLE
                binding?.shimmer?.visibility = View.GONE
                AdsHelper.loadBannerAd(
                    activity = activity,
                    container = adContainer,
                    adId = normalAdId,
                    onLoaded = {
                        binding?.clAd?.visibility = View.VISIBLE
                        LogUtils.printLog("home_banner loaded", normalAdId)
                    },
                    onFailure = {
                        adContainer.visibility = View.GONE
                        binding?.clAd?.visibility = View.GONE
                        LogUtils.printLog("home_banner failed to load", normalAdId)
                    }
                )
            } else {
                binding?.clAd?.visibility = View.GONE
                binding?.shimmer?.visibility = View.GONE
            }
        } catch (e: Exception) {
            e.printStackTrace()
            adContainer.visibility = View.GONE
            binding?.shimmer?.visibility = View.GONE
            binding?.clAd?.visibility = View.GONE
        }
    }

    private fun applyEditorWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.customToolbar.root) { toolbar, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            toolbar.setPadding(0, 0, 0, 0)
            toolbar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                topMargin = statusBars.top
            }
            binding.clAd.setPadding(0, 0, 0, navBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.customToolbar.root)
    }

    private fun openEditor(position: Int) {
        adapterBottomRecycler?.selectBottomItem(position)
        binding.cropImageView.hide()

        when (position) {
            0 -> {
                showFragment(
                    FragmentFilters.newInstance(
                        FrameUtils.getFilterHeader(), this
                    )
                )
            }

            1 -> {
                showFragment(
                    AdjustmentFragment.newInstance(
                        this, false
                    )
                )
            }

            2 -> {
                showFragment(
                    TextFragment.newInstance(
                        binding.stickerView,
                        true,
                        actionListener = this
                    )
                )
            }

            3 -> {
                binding.cropImageView.show()
                showFragment(
                    FragmentCropper.newInstance(
                        this
                    )
                )
            }

            4 -> {
                showFragment(
                    RotateFragment.newInstance(
                        this, actionListener = this
                    )
                )
            }
        }
    }

    private fun initCropper() {
        binding.cropImageView.setHandleColor(
            ContextCompat.getColor(
                this,
                R.color.colorHighlightBlueDark
            )
        )
        binding.cropImageView.setCropMode(CropImageView.CropMode.FREE)
    }

    private fun setUpToolbar() {
        binding.customToolbar.titleActionbar.text = "Editor"
        binding.customToolbar.backActionbar.setOnClickListener {
            handleEditorBack()
        }
        binding.customToolbar.doneActionButton.setSafeClickListener {
            if (isSaveInProgress || isSaved || isInterFlowInProgress || isShowingAd) return@setSafeClickListener
            hideFragment()
            binding.cropImageView.hide()
            binding.userImage.show()
            saveImage()
        }
    }

    private fun setSaveUiLocked(locked: Boolean) {
        isSaveInProgress = locked
        binding.customToolbar.doneActionButton.isEnabled = !locked
        binding.customToolbar.doneActionButton.isClickable = !locked
        // Keep back usable to close panels; only save path blocks exit via handleEditorBack.
    }

    private fun ensureBottomToolsVisible() {
        if (binding.fragmentContainerViewEraser.isVisible) return
        if (binding.fragmentContainer.isVisible) {
            // Panel open — tools stay hidden (under panel).
            binding.editorFeaturesRecycler.visibility = View.GONE
        } else {
            binding.editorFeaturesRecycler.visibility = View.VISIBLE
            binding.clBottom.visibility = View.VISIBLE
        }
    }

    private fun hasEditorPanelOpen(): Boolean {
        return binding.fragmentContainerViewEraser.isVisible || binding.fragmentContainer.isVisible
    }

    private fun handleEditorBack() {
        if (GlobalLoader.isLoaderShowing || isShowingAd) return
        if (!BackPressGuard.tryHandle()) return
        // While saving, allow closing tool panel only — not exit.
        if (isSaveInProgress && !hasEditorPanelOpen()) return

        try {
            if (hasEditorPanelOpen()) {
                navBack()
                return
            }

            // Leave editor → fresh Home (skip fragile nav-graph finish).
            if (isSaved) {
                goHomeFresh()
                return
            }

            showExitEditingDialogue(exitDialogue)
        } catch (e: Exception) {
            e.printStackTrace()
            goHomeFresh()
        }
    }

    private fun saveImage() {
        if (isSaveInProgress || isSaved || isInterFlowInProgress || isShowingAd) return
        FirebaseLogUtils.logEvent("home_click_photo_editor_save", "")
        setSaveUiLocked(true)
        try {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        SaveProgressHelper.showProcessing(this@EditorActivity)
                    }
                }
                val path = try {
                    withContext(Dispatchers.IO) {
                        ImageUtils.saveBitmapToCache(
                            this@EditorActivity, binding.stickerView.createBitmap()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                withContext(Dispatchers.Main) {
                    SaveProgressHelper.hide(this@EditorActivity)
                    path?.let {
                        adapterBottomRecycler?.unselectBottomItem()
                        try {
                            showHomeInterstitialThen {
                                navigateNext(path)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            setSaveUiLocked(false)
                        }
                    } ?: run {
                        ToastUtils.showErrorToast(this@EditorActivity)
                        setSaveUiLocked(false)
                    }
                }
            }
        } catch (e: Exception) {
            SaveProgressHelper.hide(this)
            ToastUtils.showErrorToast(this@EditorActivity)
            setSaveUiLocked(false)
        }
    }


    private fun loadImage() {

        val imagePath = intent.extras?.getString(Extras.PICKER_IMG_LIST)
        Extras.ORIGNAL_IMAGE = imagePath
        Extras.isSingleImage = true
        imagePath?.let {
            Glide.with(binding.userImage.context).asBitmap().override(800).load(it)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap, transition: Transition<in Bitmap>?
                    ) {
                        viewModel.userImageBitmap = resource
                        localOriginalBitmap = resource
                        binding.userImage.setImageBitmap(resource)
                        binding.cropImageView.imageBitmap = resource
                        // openEditor(0)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
            originalRemovedBackgroundPath = imagePath
        }

    }

    private fun setUpBottomRecyclerview() {
        adapterBottomRecycler =
            AdapterBottomRecycler(this, this, isEditor = true, showByDefault = false)
        binding.editorFeaturesRecycler.adapter = adapterBottomRecycler
        binding.editorFeaturesRecycler.itemAnimator = null
        binding.editorFeaturesRecycler.layoutManager?.isItemPrefetchEnabled = false
        adapterBottomRecycler?.let { subscribeUi(it) }
    }

    private fun subscribeUi(adapter: AdapterBottomRecycler) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadBottomIcons(this@EditorActivity).collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        if (supportFragmentManager.isDestroyed || isFinishing) return
        binding.customToolbar.toolbar.hide()
        EditorBottomPanelHelper.show(
            activity = this,
            containerId = R.id.fragmentContainer,
            container = binding.fragmentContainer,
            fragment = fragment,
            toolsView = binding.editorFeaturesRecycler,
        )
        ensureBottomToolsVisible()
    }

    fun dismissBottomPanel(onHidden: (() -> Unit)? = null) {
        hideFragment(onHidden)
    }

    private fun hideFragment(onHidden: (() -> Unit)? = null) {
        try {
            if (supportFragmentManager.isDestroyed || isFinishing) {
                ensureBottomToolsVisible()
                onHidden?.invoke()
                return
            }
            adapterBottomRecycler?.unselectBottomItem()
            EditorBottomPanelHelper.hide(
                activity = this,
                container = binding.fragmentContainer,
                toolsView = binding.editorFeaturesRecycler,
            ) {
                binding.customToolbar.toolbar.show()
                ensureBottomToolsVisible()
                onHidden?.invoke()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            binding.fragmentContainer.visibility = View.GONE
            binding.customToolbar.toolbar.show()
            ensureBottomToolsVisible()
            onHidden?.invoke()
        }
    }

    private fun initOnClickListeners() {
        binding.root.setOnClickListener {
            //  hideFragment()
            viewModel.userImageBitmap?.let { it1 -> updateImage(it1) }
            //   binding.stickerView.hideBorders()
            binding.cropImageView.hide()
            binding.userImage.show()
        }
    }

    override fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        val selectedPosition = adapterBottomRecycler?.getSelectedPosition() ?: -1
        if (selectedPosition == position && binding.fragmentContainer.isVisible) {
            return
        }
        viewModel.userImageBitmap?.let { updateImage(it) }
        openEditor(position)
    }


    override fun onRotateImage(matrix: Matrix, filter: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val bitmapUser = withContext(Dispatchers.IO) {
                viewModel.userImageBitmap?.let {
                    Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, filter)
                }
            }
            val bitmapFilter = withContext(Dispatchers.IO) {
                localOriginalBitmap?.let {
                    Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, filter)
                }
            }
            bitmapFilter?.let {
                localOriginalBitmap = it
            }
            withContext(Dispatchers.Main) {
                bitmapUser?.let {
                    viewModel.userImageBitmap = it
                    updateImage(it)
                }
            }
        }
    }

    override fun onCropperRatioClick(position: Int, modelRatio: ModelRatio) {
        modelRatio.ratio?.let { ratio ->
            val ratio = ratio.split(":")
            binding.cropImageView.setCustomRatio(ratio[0].toInt(), ratio[1].toInt())
        } ?: binding.cropImageView.setCropMode(CropImageView.CropMode.FREE)
    }

    override fun onDoneCropping() {
        binding.customToolbar.toolbar.show()
        val bitmap = binding.cropImageView.croppedBitmap
        bitmap?.let {
            viewModel.userImageBitmap = it
            localOriginalBitmap = it
            updateImage(it)
            binding.cropImageView.hide()
            binding.userImage.show()
        }
        hideFragment { ensureBottomToolsVisible() }
    }

    override fun onCancelCropping() {
        binding.customToolbar.toolbar.show()
        binding.cropImageView.hide()
        binding.userImage.show()
        hideFragment { ensureBottomToolsVisible() }
    }

    override fun onAdjustmentChanged(typeFilter: String?, bitmap: Bitmap?) {
        binding.customToolbar.toolbar.show()
        bitmap?.let { updateImage(it) }
    }

    override fun onFilterClick(position: Int, modelFilterPack: ModelFilterPack) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                filtersLoadingDialogue.show()
            }
            val filteredBitmap = withContext(Dispatchers.IO) {
                localOriginalBitmap?.let { bitmap ->
                    modelFilterPack.intensity?.let {
                        applyCustomFilter(
                            bitmap,
                            modelFilterPack.rule,
                            it.toFloat()
                        )
                    }
                }
            }

            withContext(Dispatchers.Main) {
                filtersLoadingDialogue.dismiss()
                filteredBitmap?.let {
                    localFilterBitmap = it
                    updateImage(it)
                } ?: ToastUtils.showErrorToast(this@EditorActivity)
            }
        }
    }

    override fun onFilterDone() {
        binding.customToolbar.toolbar.show()
        if (localFilterBitmap != null) {
            viewModel.userImageBitmap = localFilterBitmap
        }
        viewModel.userImageBitmap?.let { updateImage(it) }
        hideFragment { ensureBottomToolsVisible() }
    }

    override fun onFilterCancel() {
        binding.customToolbar.toolbar.show()
        viewModel.userImageBitmap?.let { updateImage(it) }
        hideFragment { ensureBottomToolsVisible() }
    }


    private fun applyCustomFilter(
        userImageBitmap: Bitmap,
        ruleString: String?,
        intensity: Float
    ): Bitmap? {
        val glContext = SharedContext.create()
        val handler = CGEImageHandler()

        return try {
            // Make the GL context current before any OpenGL operations.
            glContext.makeCurrent()

            // Initialize the handler with the bitmap and apply the filter.
            handler.initWithBitmap(userImageBitmap)
            handler.setFilterWithConfig(ruleString)
            handler.setFilterIntensity(intensity)
            handler.processFilters()

            // Get the resulting filtered bitmap.
            handler.resultBitmap
        } catch (e: Exception) {
            // Log the exception if needed for debugging purposes.
            e.printStackTrace()
            null
        } finally {
            // Ensure that both the GL context and the handler are released properly.
            try {
                handler.release() // Release handler resources if this method exists.
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                glContext.release() // Always release the GL context to avoid memory leaks.
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateImage(bitmap: Bitmap) {
        if (!isFinishing && !isDestroyed) {
            Glide.with(this)
                .asBitmap()
                .load(bitmap).override(800)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        binding.userImage.setImageBitmap(resource)
                        binding.cropImageView.imageBitmap = resource
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
        }
    }

    private fun stickerOperationListener() {
        binding.stickerView.onStickerOperationListener = object : OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                StickerView.isSelected = sticker is TextStickerCustom
            }

            override fun onStickerClicked(sticker: Sticker) {
                //stickerView.removeAllSticker();
                if (sticker is TextStickerCustom) {
                    StickerView.isSelected = true
                    binding.customToolbar.toolbar.hide()

                    showFragment(
                        TextFragment.newInstance(
                            binding.stickerView,
                            false,
                            actionListener = this@EditorActivity
                        )
                    )
                } else {
                    StickerView.isSelected = false
                    //  hideFragment()
                }
            }

            override fun onStickerDeleted(sticker: Sticker) {
                if (sticker is TextStickerCustom) {
                    binding.stickerView.hideBorders()
                    StickerView.isSelected = false
                    hideFragment()
                }
            }

            override fun onStickerDragFinished(sticker: Sticker) {}
            override fun onStickerTouchedDown(sticker: Sticker) {}
            override fun onStickerZoomFinished(sticker: Sticker) {}
            override fun onStickerFlipped(sticker: Sticker) {}
            override fun onStickerDoubleTapped(sticker: Sticker) {}
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleEditorBack()
    }

    fun navBack() {
        try {
            isBackPressed = true
            if (binding.fragmentContainerViewEraser.visibility == View.VISIBLE) {
                binding.fragmentContainerViewEraser.hide()
                ensureBottomToolsVisible()
            } else if (binding.fragmentContainer.isVisible) {
                hideFragment {
                    binding.cropImageView.hide()
                    binding.userImage.show()
                    viewModel.userImageBitmap?.let { updateImage(it) }
                    ensureBottomToolsVisible()
                }
            } else {
                if (isSaved) {
                    goHomeFresh()
                } else {
                    showExitEditingDialogue(exitDialogue)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ensureBottomToolsVisible()
        }
    }

    override fun onResume() {
        super.onResume()
        hideNavigationBar()
        // Recover stuck ad/click flags that were blocking back / tools restore.
        if (!isShowingAd && !GlobalLoader.isLoaderShowing) {
            isInterFlowInProgress = false
            ClickGuard.unlock()
        }
        ensureBottomToolsVisible()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideNavigationBar()
        }
    }

    override fun onActionTickClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "rotate" -> {
                hideFragment { ensureBottomToolsVisible() }
            }
        }
    }

    override fun onActionCancelClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "rotate" -> {
                hideFragment { ensureBottomToolsVisible() }
            }
        }
    }


    fun loadInterCollageHf(
        context: Context
    ) {
        if (isProVersion.value == true || interstitialHome != null) return
        NextGenInterstitialHelper.load(
            adUnitId = BuildConfig.inter_home_high,
            onLoaded = { ad ->
                interstitialHome = ad.rememberAdUnitId(BuildConfig.inter_home_high)
                LogUtils.printLog("collage_inter hf loaded", BuildConfig.inter_home_high)
            },
            onFailed = {
                interstitialHome = null
                loadInterCollage(context)
                LogUtils.printLog("collage_inter hf failed", BuildConfig.inter_home_high)
            }
        )
    }

    fun loadInterCollage(
        context: Context
    ) {
        if (isProVersion.value == true || interstitialHome != null) return
        NextGenInterstitialHelper.load(
            adUnitId = BuildConfig.inter_home,
            onLoaded = { ad ->
                interstitialHome = ad.rememberAdUnitId(BuildConfig.inter_home)
                LogUtils.printLog("collage_inter  loaded", BuildConfig.inter_home)
            },
            onFailed = {
                interstitialHome = null
                LogUtils.printLog("collage_inter  failed", BuildConfig.inter_home)
            }
        )
    }


    fun showInterCollage(
        currentActivity: FragmentActivity, onClose: () -> Unit,
    ) {
        if (isInterFlowInProgress || isShowingAd) return
        if (!com.aiface.aging.shared.ClickGuard.tryLock()) return
        isInterFlowInProgress = true
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false) {

                    if (canPresentHomeInterstitial()) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        val ad = interstitialHome
                        // Hold one ad instance — avoid null race from multi show.
                        interstitialHome = null
                        onClose.invoke()
                        // Continue/nav done; keep save lock if save path set it.
                        isInterFlowInProgress = false

                        if (ad != null && !isShowingAd) {
                            ad.showFullscreenAd(
                                currentActivity,
                                object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(ad)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(ad)
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                }
                            },
                            )
                        } else {
                            GlobalLoader.hide(currentActivity)
                            com.aiface.aging.shared.ClickGuard.unlock()
                        }
                    } else {
                        onClose.invoke()
                        isInterFlowInProgress = false
                        com.aiface.aging.shared.ClickGuard.unlock()
                    }


                } else {
                    onClose.invoke()
                    isInterFlowInProgress = false
                    com.aiface.aging.shared.ClickGuard.unlock()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                GlobalLoader.hide(currentActivity)
                isInterFlowInProgress = false
                com.aiface.aging.shared.ClickGuard.unlock()
                if (isSaveInProgress && !isSaved) {
                    setSaveUiLocked(false)
                }
            }

        }

    }

    private fun navigateNext(path: String) {
        ResultLauncher.openLocalPreview(this, ResultSource.PHOTO_EDITOR, path = path)
        isSaved = true
    }

}