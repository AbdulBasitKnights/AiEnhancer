package com.aiface.aging.features.collage.static_

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.NextGenInterstitialHelper
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.rememberAdUnitId
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.shared.show
import com.aiface.aging.shared.showExitEditingDialogue
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.features.editor.EditorBottomPanelHelper
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.shared.editorui.ModelRatio
import com.aiface.aging.features.collage.model.TemplateItem
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.features.text.text.TextFragment
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.ToastUtils


import com.xiaopo.flying.sticker.Sticker
import com.xiaopo.flying.sticker.StickerView
import com.xiaopo.flying.sticker.TextStickerCustom
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


/**
 * Created by admin on 4/28/2016.
 */
@AndroidEntryPoint
class GridPhotoActivity : CollageBaseActivity(), BottomActionListener {

    private val MAX_SPACE_PROGRESS = 300.0f
    private val MAX_CORNER_PROGRESS = 200.0f

    private var mFramePhotoLayout: GridPhotoLayout? = null
    private var mCorner = 1f

    private var isBackPressed = false



    val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    }
    private var isSaved = false

    private var currentlyHighlighted: GridPhotoImageView? = null

    private var reload = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideNavigationBar()
        setUpSeekbars()
        stickerOperationListener()
        setClickListeners()
        binding.templateLayout.show()
        binding.customToolbar.toolbar.hide()
        setUpToolbar()

        if (AiFaceApp.isInterCollageSaveHf && AiFaceApp.isInterCollageSave) {
            loadInterCollageHf(this)
        } else if (AiFaceApp.isInterCollageSave) {
            loadInterCollage(this)
        }
    }


    private fun setUpToolbar() {
        binding.customToolbar.titleActionbar.text = "Collage"
        binding.customToolbar.backActionbar.setOnClickListener {
            onBackPressed()
        }
        binding.customToolbar.doneActionButton.setSafeClickListener {
            reload = true
            currentlyHighlighted?.unhighlight()
            saveImage()
        }
    }

    private fun receiveImageResult(uri: Uri) {
        lifecycleScope.launch {
            val path =
                async { AppUtils.getFilePathFromContentUri(uri, this@GridPhotoActivity) }.await()
            path?.let {
                if (currentImage != null) {
                    if (lastEditPosition < mSelectedPhotoPaths.size) mSelectedPhotoPaths[lastEditPosition] =
                        it
                    currentImage!!.setImagePath(it)
                }
            }
        }

    }

    private fun replaceImage() {
        try {
            if (currentImage != null) {
            //    if (!GlobalValues.isProVersion && MyApplication.isInterGallery){//&& MyApplication.isInterListTemplate
               //     showInterstitialAdGallery(this)
               // }else{
                    TedImagePicker.with(this@GridPhotoActivity, "collage").start {
                        receiveImageResult(it)
                    }
             //   }


            } else ToastUtils.showToast(this, getString(R.string.please_select_an_image_to_replace))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImage() {
        try {
            com.aiface.aging.utils.FirebaseLogUtils.logEvent(
                "home_click_collage_save",
                "",
            )
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        SaveProgressHelper.showProcessing(this@GridPhotoActivity)
                    }
                }
                val path = try {
                    withContext(Dispatchers.IO) {
                        com.aiface.aging.utils.ImageUtils.saveBitmapToCache(
                            this@GridPhotoActivity, binding.overlayImg.createBitmap()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                withContext(Dispatchers.Main) {
                    SaveProgressHelper.hide(this@GridPhotoActivity)
                    path?.let {
                        adapterBottomRecycler?.unselectBottomItem()
                        try {
                            showHomeInterstitialThen {
                                openCollageResult(it)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } ?: ToastUtils.showErrorToast(this@GridPhotoActivity)
                }
            }
        } catch (e: Exception) {
            SaveProgressHelper.hide(this)
            ToastUtils.showErrorToast(this@GridPhotoActivity)
        }
    }



    fun showInterCollage(
        currentActivity: FragmentActivity, path : String
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false) {

                    if (canPresentHomeInterstitial()) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        openCollageResult(path)
                        if (canPresentHomeInterstitial()) {
                            interstitialHome?.showFullscreenAd(
                                currentActivity,
                                object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(interstitialHome)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome)
                                    )
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialHome = null

                                }
                            },
                            )
                        } else {
                            GlobalLoader.hide(currentActivity)
                        }
                        interstitialHome = null
                    }
                    else{
                        interstitialHome = null
                        openCollageResult(path)
                    }


                } else {
                    openCollageResult(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun openCollageResult(path: String) {
        ResultLauncher.openLocalPreview(this, ResultSource.COLLAGE, path = path)
        isSaved = true
    }

    fun loadInterCollageHf(
        context: Context
    ) {
        if (isProVersion.value == true || interstitialHome!=null) return
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
        if (isProVersion.value == true || interstitialHome!=null) return
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

    private fun setUpSeekbars() {
        binding.borderLayout.spaceBar.setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                viewModel.mSpace = viewModel.MAX_SPACE * seekBar.progress / MAX_SPACE_PROGRESS
                if (mFramePhotoLayout != null) mFramePhotoLayout!!.setSpace(
                    viewModel.mSpace,
                    mCorner
                )
                binding.borderLayout.tvBorderValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        binding.borderLayout.cornerBar.setOnSeekBarChangeListener(object :
            OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                mCorner = viewModel.MAX_CORNER * seekBar.progress / MAX_CORNER_PROGRESS
                if (mFramePhotoLayout != null) mFramePhotoLayout!!.setSpace(
                    viewModel.mSpace,
                    mCorner
                )
                binding.borderLayout.tvRoundValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    override fun buildLayout(item1: TemplateItem?) {
        item1?.let { templateItem ->
            mFramePhotoLayout = GridPhotoLayout(this, templateItem.photoItemList)
            mFramePhotoLayout?.setQuickActionClickListener(this)
            var viewWidth = binding.containerLayout!!.width
            var viewHeight = binding.containerLayout!!.height
            if (mLayoutRatio == RATIO_SQUARE) {
                if (viewWidth > viewHeight) {
                    viewWidth = viewHeight
                } else {
                    viewHeight = viewWidth
                }
            } else if (mLayoutRatio == RATIO_GOLDEN) {
                val goldenRatio = 1.61803398875
                if (viewWidth <= viewHeight) {
                    if (viewWidth * goldenRatio >= viewHeight) {
                        viewWidth = (viewHeight / goldenRatio).toInt()
                    } else {
                        viewHeight = (viewWidth * goldenRatio).toInt()
                    }
                } else if (viewHeight <= viewWidth) {
                    if (viewHeight * goldenRatio >= viewWidth) {
                        viewHeight = (viewWidth / goldenRatio).toInt()
                    } else {
                        viewWidth = (viewHeight * goldenRatio).toInt()
                    }
                }
            }
            mOutputScale = com.aiface.aging.features.collage.ImageUtils.calculateOutputScaleFactor(viewWidth, viewHeight)
            mFramePhotoLayout?.build(
                viewWidth,
                viewHeight,
                mOutputScale,
                viewModel.mSpace,
                mCorner
            )
            val params = RelativeLayout.LayoutParams(viewWidth, viewHeight)
            params.addRule(RelativeLayout.CENTER_IN_PARENT)
            binding.containerLayout!!.removeAllViews()
            binding.containerLayout!!.addView(mFramePhotoLayout, params)
            //reset space and corner seek bars
            binding.borderLayout.spaceBar!!.progress =
                (MAX_SPACE_PROGRESS * viewModel.mSpace / viewModel.MAX_SPACE).toInt()
            binding.borderLayout.cornerBar!!.progress =
                (MAX_CORNER_PROGRESS * mCorner / viewModel.MAX_CORNER).toInt()
        }
    }

    override fun onImageTouchListner(v: GridPhotoImageView) {
        val fragmentManager = supportFragmentManager
        if (!fragmentManager.isDestroyed) {
            //        if (appIconsRecyclerAdapter != null)
            //     binding.customToolbar.toolbar.show()
            //    binding.fragmentContainer.hide()
            //    supportFragmentManager.popBackStack()
            adapterBottomRecycler?.unselectBottomItem()
            //    binding.templateLayout.hide()
            //   binding.ratioLayout.hide()
            //   binding.bgsLayout.hide()
            //   binding.borderLayout.adjustLayout.hide()

            currentlyHighlighted?.unhighlight()
            currentlyHighlighted = v

            currentImage = v
            v.toggleHighlight()
            if (isEditor) {
                isEditor = false
                currentImage = v
                if (v.image != null && v.photoItem.imagePath != null && v.photoItem.imagePath.isNotEmpty()) {
                    val uri = Uri.fromFile(File(v.photoItem.imagePath))
                    lastEditPosition = getUriPosition(uri)
                }
            }
        }
    }

    override fun onDoubleTouchListner(v: GridPhotoImageView) {
        currentImage = v
        //        replaceImageDialogue();
    }

    private fun getUriPosition(uri: Uri): Int {
        for (i in mSelectedPhotoPaths.indices) {
            if (mSelectedPhotoPaths[i] == uri.path) {
                //                showInfoToast(""+i);
                return i
            }
        }
        return 0
    }

    override fun onRatioClick(position: Int, modelRatio: ModelRatio) {
        adapterRatio?.selectBottomItem(position)
        val layoutParams = binding.overlayImg!!.layoutParams as ConstraintLayout.LayoutParams
        layoutParams.dimensionRatio = modelRatio.ratio
        binding.overlayImg!!.layoutParams = layoutParams
        Handler().postDelayed({ loadTemplate(mSelectedTemplateItem!!) }, 0)
    }

    override fun onBackgroundClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterCollageBGs?.selectBottomItem(position)

        val assetsToUse = if (position == 0) {
            ModelDrawableAssets().apply {
                id = 21
                drawable = R.drawable.transparent_icon
            }
        } else {
            modelDrawableAssets
        }
        setCollageBG(assetsToUse)
    }


    override fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterBottomRecycler?.selectBottomItem(position)
        when (position) {
            0 -> {
                binding.templateLayout.show()
                binding.customToolbar.toolbar.hide()
                binding.borderLayout.adjustLayout.hide()
                binding.ratioLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.bgsLayout.hide()
            }

            2 -> {
                binding.customToolbar.toolbar.hide()
                binding.borderLayout.adjustLayout.hide()
                binding.templateLayout.hide()
                binding.ratioLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.bgsLayout.show()
            }

            3 -> {
                binding.customToolbar.toolbar.hide()
                binding.templateLayout.hide()
                binding.ratioLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.bgsLayout.hide()
                binding.borderLayout.adjustLayout.show()
            }

//            1 -> {
//                binding.customToolbar.toolbar.hide()
//                binding.borderLayout.adjustLayout.hide()
//                binding.templateLayout.hide()
//                binding.ratioLayout.hide()
//                binding.bgsLayout.hide()
//                ToastUtils.showToast(this@GridPhotoActivity, "come here gridphotoactivity line 387")
////                showFragment(
////                    FragmentStickers.newInstance(
////                        FrameUtils.getStickersHeader(),
////                        binding.overlayImg, actionListener = this
////                    )
////                )
//            }

            1 -> {
                binding.customToolbar.toolbar.hide()
                binding.borderLayout.adjustLayout.hide()
                binding.templateLayout.hide()
                binding.ratioLayout.hide()
                binding.bgsLayout.hide()
                showFragment(
                    TextFragment.newInstance(
                        binding.overlayImg,
                        true,
                        actionListener = this
                    )
                )
            }

            4 -> {
                binding.customToolbar.toolbar.hide()
                binding.borderLayout.adjustLayout.hide()
                binding.templateLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.bgsLayout.hide()
                binding.ratioLayout.show()
            }

//            6 -> {
//                adapterBottomRecycler?.unselectBottomItem()
//                binding.borderLayout.adjustLayout.hide()
//                binding.templateLayout.hide()
//                EditorBottomPanelHelper.dismissImmediately(
//                    this,
//                    binding.fragmentContainer,
//                    binding.collageFeaturesRecycler,
//                )
//                binding.bgsLayout.hide()
//                binding.ratioLayout.hide()
//                replaceImage()
//            }

        }
    }

    private fun showFragment(fragment: Fragment) {
        EditorBottomPanelHelper.show(
            this,
            R.id.fragmentContainer,
            binding.fragmentContainer,
            fragment,
            binding.collageFeaturesRecycler,
        )
    }

    private fun hideEditorPanel(onHidden: (() -> Unit)? = null) {
        EditorBottomPanelHelper.hide(
            this,
            binding.fragmentContainer,
            binding.collageFeaturesRecycler,
            onHidden,
        )
    }

    private fun stickerOperationListener() {
        binding.overlayImg.onStickerOperationListener = object :
            StickerView.OnStickerOperationListener {
            override fun onStickerAdded(sticker: Sticker) {
                StickerView.isSelected = sticker is TextStickerCustom
            }

            override fun onStickerClicked(sticker: Sticker) {
                //stickerView.removeAllSticker();
                if (sticker is TextStickerCustom) {
                    StickerView.isSelected = true
                    ToastUtils.showToast(this@GridPhotoActivity, "come here gridphotoactivity line 466")
                  //  showFragment(TextFragment.newInstance(binding.overlayImg, false))
                } else {
                    StickerView.isSelected = false
                    //    binding.fragmentContainer.hide()
                    adapterBottomRecycler?.unselectBottomItem()
                }
            }

            override fun onStickerDeleted(sticker: Sticker) {
                if (sticker is TextStickerCustom) {
                    binding.overlayImg.hideBorders()
                    StickerView.isSelected = false
                    EditorBottomPanelHelper.dismissImmediately(
                    this@GridPhotoActivity,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                    binding.customToolbar.toolbar.show()
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

    private fun setClickListeners() {
        binding.root.setOnClickListener {
            currentlyHighlighted?.unhighlight()
            adapterBottomRecycler?.unselectBottomItem()
            //   binding.customToolbar.toolbar.show()
            //   hideAllBottomViews()

        }

        binding.btnCrossTemplate.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.templateLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.btnTickTemplate.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.templateLayout.hide()
            binding.customToolbar.toolbar.show()
        }

        binding.btnTickBg.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.bgsLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.btnCrossBg.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.bgsLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.btnCrossRatio.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.ratioLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.btnTickRatio.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.ratioLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.borderLayout.btnTickBg.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.borderLayout.adjustLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.borderLayout.btnCrossBorder.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.borderLayout.adjustLayout.hide()
            binding.customToolbar.toolbar.show()
        }
    }

    private fun hideAllBottomViews() {
        EditorBottomPanelHelper.dismissImmediately(
            this,
            binding.fragmentContainer,
            binding.collageFeaturesRecycler,
        )
        binding.templateLayout.hide()
        binding.bgsLayout.hide()
        binding.borderLayout.adjustLayout.hide()
        binding.ratioLayout.hide()
    }

    override fun onBackPressed() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            isBackPressed = true
            if (binding.fragmentContainer.isVisible) {
                hideEditorPanel {
                    binding.customToolbar.toolbar.show()
                }
            } else {
                if (isSaved) {
                    safeFinish()
                } else {
                    showExitEditingDialogue(exitDialogue) {
                        saveImage()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        forceImmersiveMode()
    }

    override fun onActionTickClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text" -> {
                hideEditorPanel {
                    binding.customToolbar.toolbar.show()
                }
            }
        }
    }

    override fun onActionCancelClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text" -> {
                hideEditorPanel {
                    binding.customToolbar.toolbar.show()
                }
            }
        }
    }

    private fun forceImmersiveMode() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            // Make the content appear under system bars
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val controller = window.insetsController ?: return
            controller.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

            // Hide both system bars
            controller.hide(WindowInsets.Type.systemBars())

            // Add an additional attempt with delay to handle race conditions
            window.decorView.post {
                controller.hide(WindowInsets.Type.systemBars())
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            lifecycleScope.launch {
                delay(3000)
                forceImmersiveMode()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        if (!isSaved && !isBackPressed) {
            try {

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }
}