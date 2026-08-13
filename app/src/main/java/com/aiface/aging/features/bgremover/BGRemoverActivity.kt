package com.aiface.aging.features.bgremover

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope

import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.kaopiz.kprogresshud.KProgressHUD
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
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
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.show
import com.aiface.aging.shared.showExitEditingDialogue
import com.aiface.aging.databinding.ActivityBgRemoverBinding
import com.aiface.aging.features.adjustment.AdjustmentFragment
import com.aiface.aging.features.editor.EditorBottomPanelHelper
import com.aiface.aging.features.adjustment.AdjustmentSeekbarListener
import com.aiface.aging.shared.editorui.AdapterBottomRecycler
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.shared.editorui.BottomFeaturesCallback
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import com.aiface.aging.features.editor.ViewModelEditorActivity
import com.aiface.aging.features.eraser.EraserFragment
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.filters.FilterUpdateCallback
import com.aiface.aging.features.filters.FragmentFilters
import com.aiface.aging.features.filters.model.ModelFilterPack
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.features.text.text.TextFragment
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.FrameUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.NetworkUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.ToastUtils
import com.aiface.aging.utils.multitouchlistener.MultiTouchListener
import com.aiface.aging.utils.multitouchlistener.OnDoubleTapListener
import com.aiface.aging.utils.multitouchlistener.OnImageViewTouchListener


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
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow


private const val REQUEST_CODE_ERASER = 777

@AndroidEntryPoint
class BGRemoverActivity : AppCompatActivity(),
    OnDoubleTapListener,
    BottomFeaturesCallback, OnImageViewTouchListener, BottomActionListener,
    FilterUpdateCallback, AdjustmentSeekbarListener,
    EraserFragment.OnFragmentInteractionListener {
    private lateinit var binding: ActivityBgRemoverBinding
    private val viewModel: ViewModelBGRemover by viewModels()
    private val viewModelEditor: ViewModelEditorActivity by viewModels()

    private var filtersLoadingDialogue: KProgressHUD? = null
    private var adapterBottomRecycler: AdapterBottomRecycler? = null
    private var localOriginalBitmap: Bitmap? = null
    private var localFilterBitmap: Bitmap? = null
    private var isSaved = false

    val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(this, R.layout.dialog_exit_editing)
    }


    private var originalRemovedBackgroundPath = ""

    private var isRemovingOnline = true
    private var currentCredit = 5


    private var reload = false


    companion object {
        var removeFragment = MutableLiveData<Boolean>(false)
        var isRemoverEntered = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityBgRemoverBinding.inflate(layoutInflater)
            setContentView(binding.root)


            FirebaseLogUtils.logEvent(
                "bgremover_view",
                "user view ai creator editor screen"
            )

            loadAdaptiveBannerAd(
                binding?.bannerAdView!!,
                this,
                BuildConfig.banner_home,
                BuildConfig.banner_home_high
            )
            if (AiFaceApp.isInterBgRemoverHf && AiFaceApp.isInterBgRemover) {
                loadInterCollageHf(this)
            } else if (AiFaceApp.isInterBgRemover) {
                loadInterCollage(this)
            }

            isRemoverEntered = true




            addUserImageList()

            setMultiTouchListeners()
            initOnClickListeners()
            setUpBottomRecyclerview()
            stickerOperationListener()


            setUpToolbar()

            removeFragment.observe(this, Observer {
                if (it) {

                    removeFragment.value = false
                }
            })


        } catch (e: Exception) {
            finish()
        }
    }


    private fun setUpToolbar() {
        binding.customToolbar.titleActionbar.text = "Bg Remover"
        binding.customToolbar.backActionbar.setOnClickListener {

            onBackPressed()
        }
        binding.customToolbar.doneActionButton.setOnClickListener {
            reload = true
            FirebaseLogUtils.logEvent(
                "ai_creator_editor_btn_save_click",
                "user click butotn save on ai creator editor screen"
            )

            saveImage()

        }
    }

    private fun saveImage() {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    if (!isFinishing && !isDestroyed) {
                        binding.frameImage.show()
                        SaveProgressHelper.showProcessing(this@BGRemoverActivity)
                    }
                }

                val path = try {
                    withContext(Dispatchers.IO) {
                        ImageUtils.saveBitmapToCache(
                            this@BGRemoverActivity, binding.stickerView.createBitmap()
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                withContext(Dispatchers.Main) {
                    SaveProgressHelper.hide(this@BGRemoverActivity)
                    path?.let {
                        adapterBottomRecycler?.unselectBottomItem()
                        binding.frameImage.show()
                        try {
                            showHomeInterstitialThen {
                                navigateNext(path)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } ?: ToastUtils.showErrorToast(this@BGRemoverActivity)
                }
            }
        } catch (e: Exception) {
            SaveProgressHelper.hide(this)
            ToastUtils.showErrorToast(this@BGRemoverActivity)
        }
    }


    override fun onFilterClick(position: Int, modelFilterPack: ModelFilterPack) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                filtersLoadingDialogue?.show()
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
                filtersLoadingDialogue?.dismiss()
                filteredBitmap?.let {
                    localFilterBitmap = it
                    updateImage(localFilterBitmap)
                } ?: ToastUtils.showErrorToast(this@BGRemoverActivity)
            }
        }
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


    override fun onFilterDone() {
        binding.customToolbar.toolbar.show()
        if (localFilterBitmap != null) {
            viewModelEditor.userImageBitmap = localFilterBitmap
        }
        viewModelEditor.userImageBitmap?.let { updateImage(it) }
        hideFragment()
    }

    override fun onFilterCancel() {
        binding.customToolbar.toolbar.show()
        viewModelEditor.userImageBitmap?.let { updateImage(it) }
        hideFragment()
    }


    private fun updateMask(maskableFrameLayout: ImageView, maskPath: String) {
        if (!isFinishing && !isDestroyed) {
            Glide.with(maskableFrameLayout.context)
                .load(maskPath)
                .diskCacheStrategy(DiskCacheStrategy.NONE).override(800)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable, transition: Transition<in Drawable>?
                    ) {
                        maskableFrameLayout.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {

                    }

                })
        }
    }

    private fun addUserImageList() {
        val bitmapPath = intent.getStringExtra(Extras.ERASED_BITMAP_PATH)
        isRemovingOnline = intent.getBooleanExtra("isOnline", true)
        currentCredit = intent.getIntExtra("credits", currentCredit)
        //    binding.customToolbar.tvCredits.text = "$currentCredit Credits"
        bitmapPath?.let {
            originalRemovedBackgroundPath = bitmapPath
            loadImage(it)
        }
    }

    private fun loadImage(imagePath: String) {
        binding.addUserImage1.hide()
        binding.userImage1.let {
            it.show()
            resetUserImagePosition(it)
            if (!isFinishing && !isDestroyed) {
                Glide.with(it.context).asBitmap().load(imagePath)
                    .placeholder(R.drawable.ic_background_)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(
                            resource: Bitmap, transition: Transition<in Bitmap>?
                        ) {
                            viewModelEditor.userImageBitmap = resource
                            localOriginalBitmap = resource
                            it.setImageBitmap(resource)
                            binding.loadImgAnim.cancelAnimation()
                            binding.loadImgAnim.hide()
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {

                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            binding.loadImgAnim.cancelAnimation()
                            binding.loadImgAnim.hide()
                            it.setImageResource(R.drawable.ic_background_)
                        }

                    })
            }
        }
    }

    private fun setUpBottomRecyclerview() {
        adapterBottomRecycler =
            AdapterBottomRecycler(this, this, isEditor = true, showByDefault = false)
        binding.editorFeaturesRecycler.adapter = adapterBottomRecycler
        adapterBottomRecycler?.let { subscribeUi(it) }
    }

    private fun subscribeUi(adapter: AdapterBottomRecycler) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadBottomIcons().collectLatest { icons ->
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

    private fun showFragment(fragment: Fragment) {
        binding.customToolbar.toolbar.hide()
        EditorBottomPanelHelper.show(
            this,
            R.id.fragmentContainer,
            binding.fragmentContainer,
            fragment,
            binding.editorFeaturesRecycler,
        )
    }

    private fun hideFragment(onHidden: (() -> Unit)? = null) {
        try {
            if (!supportFragmentManager.isDestroyed && !isFinishing) {
                adapterBottomRecycler?.unselectBottomItem()
                EditorBottomPanelHelper.hide(
                    this,
                    binding.fragmentContainer,
                    binding.editorFeaturesRecycler,
                ) {
                    binding.customToolbar.toolbar.show()
                    onHidden?.invoke()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun initOnClickListeners() {


        binding.addUserImage1.setOnClickListener {
            openImagePicker()
        }

    }

    private fun openImagePicker() {
        TedImagePicker.with(this@BGRemoverActivity, "editor")
            .start {
                loadImage(it.toString())
                hideFragment()
            }
    }

    private fun setMultiTouchListeners() {
        binding.userImage1.setOnTouchListener(
            MultiTouchListener(
                applicationContext, binding.userImage1, false, this@BGRemoverActivity, this
            )
        )
    }


    override fun onDoubleTapListner(view: View?) {
//        openImagePicker()
    }

    private fun setBitmapToEraser() {
        binding.editorFeaturesRecycler.visibility = View.GONE
        binding.fragmentContainerViewEraser.visibility = View.VISIBLE
        binding.fragmentContainer.hide()
    }

    private fun hideEraserOverlay() {
        binding.fragmentContainerViewEraser.visibility = View.GONE
        binding.editorFeaturesRecycler.visibility = View.VISIBLE
    }

    private fun loadEraserFragment() {
        val path = originalRemovedBackgroundPath
        if (path.isBlank() || isFinishing || isDestroyed) return
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_view_eraser, EraserFragment.newInstance(path))
            .commit()
    }

    override fun onErasedImage(path: String?) {
        if (!path.isNullOrBlank()) {
            originalRemovedBackgroundPath = path
            loadImage(path)
        }
        hideEraserOverlay()
        adapterBottomRecycler?.unselectBottomItem()
    }


    override fun onBottomItemClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {

        if (isProVersion.value == false) {
            if (AiFaceApp.isBannerEditHf && AiFaceApp.isBannerEdit) {
                loadAdaptiveBannerAd(
                    binding?.bannerAdView!!,
                    this,
                    BuildConfig.banner_home,
                    BuildConfig.banner_home_high
                )
                binding?.clAd?.visibility = View.VISIBLE
            } else if (AiFaceApp.isBannerEdit) {
                loadAdaptiveBannerAd(
                    binding?.bannerAdView!!,
                    this,
                    BuildConfig.banner_home,
                    BuildConfig.banner_home
                )
                binding?.clAd?.visibility = View.VISIBLE
            } else {
                binding?.clAd?.visibility = View.GONE
                binding?.shimmer?.visibility = View.GONE
            }
        }

        when (modelDrawableAssets.imageTitle) {
            "AI Eraser" -> {
                hideFragment()
                loadEraserFragment()
                setBitmapToEraser()
            }

            "Background" -> {
                // setBitmapToEraser()
            }


            "Replace" -> {
                openImagePicker()
            }

            "Flip" -> {
                hideFragment { flipImage() }
            }

            "Sticker" -> {

            }

            "Filters" -> {
                binding.customToolbar.toolbar.hide()
                showFragment(
                    FragmentFilters.newInstance(
                        FrameUtils.getFilterHeader(), this
                    )
                )
            }

            "Adjust" -> {
                binding.customToolbar.toolbar.hide()
                showFragment(
                    AdjustmentFragment.newInstance(
                        this, true
                    )
                )
            }

            "Write" -> {
                showFragment(
                    TextFragment.newInstance(
                        binding.stickerView,
                        true,
                        actionListener = this
                    )
                )
            }

            "Filters" -> {
                binding.fragmentContainer.hide()
                ToastUtils.showToast(this, "Coming soon")
            }
        }
    }

    override fun onAdjustmentChanged(typeFilter: String?, bitmap: Bitmap?) {
        binding.customToolbar.toolbar.show()
        bitmap?.let { updateImage(it) }
    }

    private fun flipImage() {
        val userImageBitmap = binding.userImage1.currentBitmap
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
                    updateImage(it)
                }
            }
        }
    }

    private fun updateImage(bitmap: Bitmap?) {
        Glide.with(this)
            .asBitmap()
            .load(bitmap)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    binding.userImage1.setImageBitmap(resource)
                    adapterBottomRecycler?.unselectBottomItem()
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }

            })
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
                    showFragment(
                        TextFragment.newInstance(
                            binding.stickerView,
                            false,
                            actionListener = this@BGRemoverActivity
                        )
                    )
                } else {
                    StickerView.isSelected = false
                    // hideFragment()
                }
            }

            override fun onStickerDeleted(sticker: Sticker) {
                if (sticker is TextStickerCustom) {
                    binding.stickerView.hideBorders()
                    StickerView.isSelected = false
                    binding.fragmentContainer.hide()
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


    override fun onImageTouchListener(view: View) {
        //   hideFragment()
    }


    private fun resetUserImagePosition(imageView: ImageView) {
        // Restore positions to the initial position
        if (imageView == binding.userImage1) {
            binding.userImage1.x = binding.layout1.x
            binding.userImage1.y = binding.layout1.y

            // Restore the zoom to the initial scale
            binding.userImage1.scaleX = binding.layout1.scaleX
            binding.userImage1.scaleY = binding.layout1.scaleY

            // Restore the rotation to the initial angle (0 degrees)
            binding.userImage1.rotation = binding.layout1.rotation
        }
    }

    override fun onBackPressed() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            if (binding.fragmentContainer.isVisible) {
                hideFragment()
            } else {
                showExitEditingDialogue(exitDialogue) {
                    saveImage()
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

    override fun onActionTickClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "frame" -> {
                FirebaseLogUtils.logEvent(
                    "review_ai_creator_v_click",
                    "user click v on review ai creator screen"
                )

                hideFragment()
            }
        }
    }

    override fun onActionCancelClick(type: String, action: ((String) -> Unit)?) {
        when (type) {
            "sticker", "text", "frame" -> {
                FirebaseLogUtils.logEvent(
                    "review_ai_creator_x_click",
                    "user click x on review ai creator screen"
                )
                adapterBottomRecycler?.unselectBottomItem()
                hideFragment()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_ERASER && resultCode == RESULT_OK) {
            val updatedString: String = data?.getStringExtra("updatedString") ?: "tobenull"
            originalRemovedBackgroundPath = updatedString
            Log.d("xxxxx", updatedString)
            val bitmap = getBitmap(updatedString)
            loadImage(updatedString)
        }
    }

    private fun getBitmap(path: String): Bitmap? {
        val uri = getImageUri(path)
        var inputStream: InputStream?
        try {
            val IMAGE_MAX_SIZE = 1024
            inputStream = contentResolver.openInputStream(uri)

            // Decode image size
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true

            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream!!.close()

            var scale = 1
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = 2.0.pow(
                    Math.round(
                        ln(
                            IMAGE_MAX_SIZE / o.outHeight.coerceAtLeast(o.outWidth).toDouble()
                        ) / ln(0.5)
                    ).toInt().toDouble()
                ).toInt()
            }

            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = contentResolver.openInputStream(uri)
            var b = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream!!.close()

            b = Bitmap.createBitmap(
                (b)!!, 0, 0, o2.outWidth, o2.outHeight, getOrientationMatrix(path), true
            )

            return b
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getImageUri(path: String): Uri {
        return Uri.fromFile(File(path))
    }

    private fun getOrientationMatrix(path: String): Matrix {
        val matrix = Matrix()
        val exif: ExifInterface
        try {
            exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }


        return matrix
    }


    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
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
        if (hasFocus) {
            lifecycleScope.launch {
                delay(3000)
                forceImmersiveMode()
            }
        }
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
        currentActivity: FragmentActivity, path: String
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false && AiFaceApp.isInterBgRemover) {

                    if (canPresentHomeInterstitial()) {
                        GlobalLoader.show(currentActivity)
                        delay(1000)
                        navigateNext(path)

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
                    } else {
                        navigateNext(path)
                    }


                } else {
                    navigateNext(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun navigateNext(path: String){
        ResultLauncher.openLocalPreview(this, ResultSource.BG_REMOVER, path = path)
        isSaved = true
    }
}