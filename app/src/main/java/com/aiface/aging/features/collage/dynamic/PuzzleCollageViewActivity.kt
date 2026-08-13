package com.aiface.aging.features.collage.dynamic

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.databinding.ActivityPuzzleCollageViewBinding
import com.aiface.aging.shared.editorui.AdapterBottomRecycler
import com.aiface.aging.features.collage.AdapterCollageBGs
import com.aiface.aging.features.collage.BGsCallback
import com.aiface.aging.shared.editorui.BottomFeaturesCallback
import com.aiface.aging.features.collage.dynamic.puzzle.PuzzleLayout
import com.aiface.aging.utils.DialogueUtils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.aiface.aging.features.collage.dynamic.puzzle.slant.SlantPuzzleLayout
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.features.text.text.TextFragment
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PuzzleCollageViewActivity : AppCompatActivity(),
    BottomFeaturesCallback, BGsCallback, BottomActionListener {
    private lateinit var binding: ActivityPuzzleCollageViewBinding
    private var puzzleLayout: PuzzleLayout? = null
    private var deviceWidth = 0

    private var adapterBottomRecycler: AdapterBottomRecycler? = null
    private var adapterCollageBGs: AdapterCollageBGs? = null

    private var isBackPressed = false

    private var subPuzzleList: List<PuzzleLayout>? = null
    private var adapterBottomPuzzle: PuzzleDynamicBottomRecyclerAdapter? = null
    private var pieceSize = 0

    private var drawables: List<Drawable> = java.util.ArrayList()
    private var isSaved = false

    private val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    }




    private var reload = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPuzzleCollageViewBinding.inflate(layoutInflater)
            .apply { setContentView(root) }
        hideNavigationBar()
        deviceWidth = resources.displayMetrics.widthPixels
        intent.getStringArrayListExtra("imagePath")?.let {
            binding.puzzleView.post {
                loadPhoto(it)
            }
        } ?: loadPhotoFromRes()

        val type = intent.getIntExtra("type", 0)
        pieceSize = intent.getIntExtra("piece_size", 0)
        val themeId = intent.getIntExtra("theme_id", 0)
        setPuzzleLayout(type, pieceSize, themeId)

        setPuzzleViewForeground(intent.getStringExtra("frame_path"))
        setUpBottomRecyclerview()
        setUpBGsRecyclerview()
        setBottomPuzzleRecyclerView()
        setUpSeekbars()
        setClickListeners()

        if (AiFaceApp.isInterCollageSaveHf && AiFaceApp.isInterCollageSave) {
            loadInterCollageHf(this)
        } else if (AiFaceApp.isInterCollageSave) {
            loadInterCollage(this)
        }


        setUpToolbar()

    }


    private fun setPuzzleViewForeground(framePath: String?) {
        framePath ?: return
        if (!isActivityActive()) return

        Glide.with(this).asDrawable().load(framePath).override(800)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    binding.puzzleView.foreground = resource
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun setPuzzleLayout(type: Int, pieceSize: Int, themeId: Int) {
        puzzleLayout = PuzzleUtils.getPuzzleLayout(type, pieceSize, themeId).also {
            binding.puzzleView.apply {
                val color = Color.parseColor("#8850FF")
                puzzleLayout = it
                isTouchEnable = true
                isNeedDrawLine = false
                isNeedDrawOuterLine = false
                lineSize = 4
                lineColor = color
                selectedLineColor = color
                handleBarColor = color
                setAnimateDuration(300)
                piecePadding = 3f
            }
        }
    }

    private fun loadPhoto(imageArrayList: ArrayList<String>) {
        if (!isActivityActive()) return

        val areaCount =
            puzzleLayout?.areaCount ?: return // Return or handle the null case appropriately

        val pieces = mutableListOf<Bitmap>()
        imageArrayList.take(minOf(imageArrayList.size, areaCount))
            .forEach { imagePath ->
                Glide.with(this).asBitmap().load(Uri.parse("file://$imagePath")).override(800)
                    .override(deviceWidth, deviceWidth)
                    .into(createCustomTarget(pieces, imageArrayList.size))
            }
    }


    private fun loadPhotoFromRes() {
        if (!isActivityActive()) return

        puzzleLayout?.let {
            val resIds = intArrayOf(
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt,
                R.drawable.transparentt
            )
            val pieces = mutableListOf<Bitmap>()
            resIds.take(minOf(resIds.size, it.areaCount)).forEach { resId ->
                Glide.with(this).asBitmap().override(800).load(resId)
                    .into(createCustomTarget(pieces, resIds.size))
            }
        }
    }

    private fun createCustomTarget(
        pieces: MutableList<Bitmap>,
        totalSize: Int
    ): CustomTarget<Bitmap> = object : CustomTarget<Bitmap>() {
        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
            puzzleLayout?.let { puzzleLayout ->
                pieces.add(resource)
                if (pieces.size == minOf(totalSize, puzzleLayout.areaCount)) {
                    val puzzlePieces = List(puzzleLayout.areaCount) { pieces[it % pieces.size] }
                    binding.puzzleView.addPieces(puzzlePieces)
                }
            }
        }

        override fun onLoadCleared(placeholder: Drawable?) {}
    }

    private fun setUpBottomRecyclerview() {
        adapterBottomRecycler = AdapterBottomRecycler(this, this, isEditor = true)
        binding.collageFeaturesRecycler.adapter = adapterBottomRecycler
        adapterBottomRecycler?.let { subscribeUi(it) }
    }

    private fun setUpBGsRecyclerview() {
        adapterCollageBGs = AdapterCollageBGs(this, this)
        binding.rvBGs.adapter = adapterCollageBGs
        adapterCollageBGs?.let { subscribeBGsUi(it) }
    }

    private fun subscribeBGsUi(adapter: AdapterCollageBGs) {
        lifecycleScope.launchWhenStarted {
            loadBGsList().collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun subscribeUi(adapter: AdapterBottomRecycler) {
        lifecycleScope.launch {
            loadBottomIcons().collectLatest { icons ->
                if (icons.isNotEmpty()) {
                    adapter.submitList(icons)
                }
            }
        }
    }

    private fun setUpToolbar() {
        binding.customToolbar.titleActionbar.text = "Collage"
        binding.customToolbar.backActionbar.setOnClickListener {

            onBackPressed()
        }
        binding.customToolbar.doneActionButton.setSafeClickListener {
            reload = true
            binding.puzzleView.isNeedDrawLine = false
            binding.puzzleView.isSelected = false
            saveImage()
        }
    }


    fun loadBottomIcons() = flow {
        emit(
            listOf(
                ModelDrawableAssets(
                    1,
                    R.drawable.ic_collage__new,
                    resources.getString(R.string.collage)
                ),
                ModelDrawableAssets(
                    2,
                    R.drawable.ic_backgrounds_new,
                    resources.getString(R.string.background)
                ),
                ModelDrawableAssets(
                    3,
                    R.drawable.ic_border_new,
                    resources.getString(R.string.border)
                ),
//                ModelDrawableAssets(
//                    4,
//                    R.drawable.ic_sticker_new,
//                    resources.getString(R.string.sticker)
//                ),
                ModelDrawableAssets(5, R.drawable.ic_text_new, resources.getString(R.string.text)),
                ModelDrawableAssets(
                    6,
                    R.drawable.ic_rotate_new,
                    resources.getString(R.string.rotate)
                ),
//                ModelDrawableAssets(
//                    7,
//                    R.drawable.ic_replace_photo,
//                    resources.getString(R.string.replace)
//                ),
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterBottomRecycler?.selectBottomItem(position)
        when (position) {
            0 -> {
//                binding.customToolbar.toolbar.hide()
                binding.templateLayout.show()
                binding.borderLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.rotateLayout.adjustLayout.hide()
                binding.bgsLayout.hide()
            }

            1 -> {
//                binding.customToolbar.toolbar.hide()
                binding.templateLayout.hide()
                binding.borderLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.rotateLayout.adjustLayout.hide()
                binding.bgsLayout.show()
            }

            2 -> {
//                binding.customToolbar.toolbar.hide()
                binding.borderLayout.show()
                binding.templateLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.rotateLayout.adjustLayout.hide()
                binding.bgsLayout.hide()
            }

//            3 -> {
//                binding.customToolbar.toolbar.hide()
//                binding.borderLayout.hide()
//                binding.templateLayout.hide()
//                binding.rotateLayout.adjustLayout.hide()
//                binding.bgsLayout.hide()
//                Toast.makeText(this,"puzzlecollageviewactivity line 327", Toast.LENGTH_SHORT).show()
////                showFragment(
////                    FragmentStickers.newInstance(
////                        FrameUtils.getStickersHeader(),
////                        binding.overlayImg,
////                        this
////                    )
////                )
//            }

            3 -> {
//                binding.customToolbar.toolbar.hide()
                binding.borderLayout.hide()
                binding.templateLayout.hide()
                binding.rotateLayout.adjustLayout.hide()
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
//                binding.customToolbar.toolbar.hide()
                binding.rotateLayout.adjustLayout.show()
                binding.borderLayout.hide()
                binding.templateLayout.hide()
                EditorBottomPanelHelper.dismissImmediately(
                    this,
                    binding.fragmentContainer,
                    binding.collageFeaturesRecycler,
                )
                binding.bgsLayout.hide()
                binding.collageFeaturesRecycler.hide()
            }

//            6 -> {
//                adapterBottomRecycler?.unselectBottomItem()
//                binding.borderLayout.hide()
//                binding.templateLayout.hide()
//                EditorBottomPanelHelper.dismissImmediately(
//                    this,
//                    binding.fragmentContainer,
//                    binding.collageFeaturesRecycler,
//                )
//                binding.rotateLayout.adjustLayout.hide()
//                binding.bgsLayout.hide()
//                replaceImage()
//            }
        }
    }

    private fun isActivityActive() = !isFinishing && !isDestroyed

    private fun setBottomPuzzleRecyclerView() {
        try {
            subPuzzleList = PuzzleUtils.getPuzzleLayouts(pieceSize)
            adapterBottomPuzzle = PuzzleDynamicBottomRecyclerAdapter(this@PuzzleCollageViewActivity)
            val themeId = intent.getIntExtra("theme_id", 0)
            val count = intent.getIntExtra("count", 0)
            adapterBottomPuzzle?.selectedPosition = themeId + count
            subPuzzleList?.let {
                binding.templateLayout.show()
                binding.customToolbar.toolbar.hide()
                adapterBottomPuzzle?.addPuzzleData(subPuzzleList!!, null)
                lifecycleScope.launch {
                    val imagesList = AppUtils.fetchImagePathsSorted(
                        this@PuzzleCollageViewActivity,
                        "collage/dynamic/$pieceSize"
                    )
                    adapterBottomPuzzle?.addCustomThumbs(imagesList)
                }
                val layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
                binding.templateView.layoutManager = layoutManager
                binding.templateView.adapter = adapterBottomPuzzle
                binding.templateView.scrollToPosition(adapterBottomPuzzle?.selectedPosition ?: 0)

                adapterBottomPuzzle?.setPuzzleItemClickListener(object : PuzzleItemClickListener {
                    override fun onPuzzleItemClick(layout: PuzzleLayout, themeId: Int) {
                        val type = if (layout is SlantPuzzleLayout) 0 else 1
                        drawables = binding.puzzleView.puzzleDrawableList
                        setPuzzleLayout(type, pieceSize, themeId)
                        binding.puzzleView.post { binding.puzzleView.post { populatePuzzleViewAgain() } }
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun populatePuzzleViewAgain() {
        try {
            for (i in 0 until puzzleLayout!!.areaCount) {
                if (i < drawables.size) binding.puzzleView.addPiece(drawables[i])
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setUpSeekbars() {
        try {
            binding.cornerBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    binding.puzzleView.pieceRadian = progress.toFloat()
                    binding.tvBorderValue.text = progress.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {}
                override fun onStopTrackingTouch(seekBar: SeekBar) {}
            })
        } catch (e: Exception) {
            e.printStackTrace()
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

    private fun hideAllBottomViews() {
        EditorBottomPanelHelper.dismissImmediately(
            this,
            binding.fragmentContainer,
            binding.collageFeaturesRecycler,
        )
        binding.templateLayout.hide()
        binding.bgsLayout.hide()
        binding.borderLayout.hide()
        binding.rotateLayout.adjustLayout.hide()
    }

    private fun setClickListeners() {

        binding.root.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            //  binding.customToolbar.toolbar.show()
            //   hideAllBottomViews()
        }

        binding.rotateLayout.let { layout ->
            binding.puzzleView.let { puzzleView ->
                layout.btnLeft.setOnClickListener {
                    puzzleView.let {
                        if (it.hasPieceSelected()) {
                            it.rotate(-90f)
                        } else ToastUtils.showToast(
                            this,
                            getString(R.string.please_select_an_image_to_rotate)
                        )
                    }
                }
                layout.btnRight.setOnClickListener {
                    puzzleView.let {
                        if (it.hasPieceSelected()) {
                            it.rotate(90f)
                        } else ToastUtils.showToast(
                            this,
                            getString(R.string.please_select_an_image_to_rotate)
                        )
                    }
                }

                layout.btnVer.setOnClickListener {
                    puzzleView.let {
                        if (it.hasPieceSelected()) {
                            it.flipVertically()
                        } else ToastUtils.showToast(
                            this,
                            getString(R.string.please_select_an_image_to_flip)
                        )
                    }
                }
                layout.btnHor.setOnClickListener {
                    puzzleView.let {
                        if (it.hasPieceSelected()) {
                            it.flipHorizontally()
                        } else ToastUtils.showToast(
                            this,
                            getString(R.string.please_select_an_image_to_flip)
                        )
                    }
                }
            }

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
        binding.btnTickSeekbar.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.borderLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.btnCrossBorder.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.borderLayout.hide()
            binding.customToolbar.toolbar.show()
        }
        binding.rotateLayout.btnTick.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.rotateLayout.adjustLayout.hide()
            binding.customToolbar.toolbar.show()
            binding.collageFeaturesRecycler.show()
        }
        binding.rotateLayout.btnCross.setOnClickListener {
            adapterBottomRecycler?.unselectBottomItem()
            binding.rotateLayout.adjustLayout.hide()
            binding.customToolbar.toolbar.show()
            binding.collageFeaturesRecycler.show()
        }
    }

    private fun replaceImage() {
        try {
            if (binding.puzzleView.hasPieceSelected()) {

              //  if (!GlobalValues.isProVersion && MyApplication.isInterGallery){//&& MyApplication.isInterListTemplate
              //      showInterstitialAdGallery(this)
              //  }else{
                    TedImagePicker.with(this@PuzzleCollageViewActivity, "collage").start {
                        if (isActivityActive()) {
                            Glide.with(this)
                                .asBitmap()
                                .load(it).override(800)
                                .into(object : CustomTarget<Bitmap>() {
                                    override fun onResourceReady(
                                        resource: Bitmap,
                                        transition: Transition<in Bitmap>?
                                    ) {
                                        binding.puzzleView.replace(resource, "")
                                    }

                                    override fun onLoadCleared(placeholder: Drawable?) {

                                    }

                                })
                        }
                    }
              //  }



            } else ToastUtils.showToast(this, getString(R.string.please_select_an_image_to_replace))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImage() {
        try {
            FirebaseLogUtils.logEvent("home_click_collage_save", "")
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        SaveProgressHelper.showProcessing(this@PuzzleCollageViewActivity)
                    }
                }
                val path = try {
                    withContext(Dispatchers.IO) {
                        ImageUtils.saveBitmapToCache(
                            this@PuzzleCollageViewActivity, binding.overlayImg.createBitmap()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                withContext(Dispatchers.Main) {
                    SaveProgressHelper.hide(this@PuzzleCollageViewActivity)
                    path?.let {
                        adapterBottomRecycler?.unselectBottomItem()
                        try {
                            showHomeInterstitialThen {
                                openCollageResult(it)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } ?: ToastUtils.showErrorToast(this@PuzzleCollageViewActivity)
                }
            }
        } catch (e: Exception) {
            SaveProgressHelper.hide(this)
            ToastUtils.showErrorToast(this@PuzzleCollageViewActivity)
        }
    }

    fun showInterCollage(
        currentActivity: FragmentActivity, path : String
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value ==false) {

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


    override fun onResume() {
        super.onResume()
        forceImmersiveMode()
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

    private fun loadBGsList() = flow {
        emit(
            listOf(
                ModelDrawableAssets(1, R.drawable.bg_1),
                ModelDrawableAssets(2, R.drawable.bg_2),
                ModelDrawableAssets(3, R.drawable.bg_3),
                ModelDrawableAssets(4, R.drawable.bg_4),
                ModelDrawableAssets(5, R.drawable.bg_5),
                ModelDrawableAssets(6, R.drawable.bg_6),
                ModelDrawableAssets(7, R.drawable.bg_7),
                ModelDrawableAssets(8, R.drawable.bg_8),
                ModelDrawableAssets(9, R.drawable.bg_9),
                ModelDrawableAssets(10, R.drawable.bg_10),
                ModelDrawableAssets(11, R.drawable.bg_11),
                ModelDrawableAssets(12, R.drawable.bg_12),
                ModelDrawableAssets(13, R.drawable.bg_13),
                ModelDrawableAssets(14, R.drawable.bg_14),
                ModelDrawableAssets(15, R.drawable.bg_15),
                ModelDrawableAssets(16, R.drawable.bg_16),
                ModelDrawableAssets(17, R.drawable.bg_17),
                ModelDrawableAssets(18, R.drawable.bg_18),
                ModelDrawableAssets(19, R.drawable.bg_19),
                ModelDrawableAssets(20, R.drawable.bg_20),
            )
        )
    }.flowOn(Dispatchers.IO)

    override fun onBackgroundClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterCollageBGs?.selectBottomItem(position)
        setCollageBG(modelDrawableAssets)
    }

    private fun setCollageBG(modelDrawableAssets: ModelDrawableAssets?) {
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus){
            lifecycleScope.launch {
                delay(3000)
                forceImmersiveMode()
            }
        }
    }


}