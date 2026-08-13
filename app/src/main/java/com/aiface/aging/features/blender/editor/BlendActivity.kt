package com.aiface.aging.features.blender.editor

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.SeekBar
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.drawToBitmap
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.ads.loadEditorAdaptiveBanner
import com.aiface.aging.shared.ads.preloadInterEditSave
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.databinding.ActivityBlendBinding
import com.aiface.aging.features.blender.BlendingBottomSheet
import com.aiface.aging.features.blender.BlendingBottomSheetPassData
import com.aiface.aging.features.blender.BlenderEditorViewModel
import com.aiface.aging.features.blender.BlenderState
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.features.blender.adapters.BlendEditorOptionsAdapter
import com.aiface.aging.features.blender.adapters.BlendShapeStyleAdapter
import com.aiface.aging.features.blender.assetsToDrawable
import com.aiface.aging.features.blender.checkBlendStoragePermission
import com.aiface.aging.features.blender.customclasses.BlendMultiTouchListener
import com.aiface.aging.features.blender.customclasses.ZoomListener
import com.aiface.aging.features.blender.dismissBlendLoadingDialog
import com.aiface.aging.features.blender.dismissBlendProgressDialog
import com.aiface.aging.features.blender.getThumbnail
import com.aiface.aging.features.blender.goBackWarningDialog
import com.aiface.aging.features.blender.mirrorImage
import com.aiface.aging.features.blender.showBlendLoadingDialog
import com.aiface.aging.features.blender.showBlendProgressDialog
import com.aiface.aging.features.blender.showDialogForDontAskAgain
import com.aiface.aging.features.blender.sideblur
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.features.share.ExtrasShareImageActivity
import com.aiface.aging.features.share.ShareImageActivity
import com.aiface.aging.utils.ImageUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class BlendActivity : AppCompatActivity() {

    private var binding: ActivityBlendBinding? = null
    private val editorViewModel: BlenderEditorViewModel by viewModels()
    private var path: String = ""
    private lateinit var editorOptionsAdapter: BlendEditorOptionsAdapter
    private lateinit var blendShapeStyleAdapter: BlendShapeStyleAdapter
    private var isSingleClick = false
    private var isImageMirror = false
    private var isLayerIconSelected = false
    private var bgBitmap: Bitmap? = null
    private var lastBlurredBitmap: Bitmap? = null
    private var isSaved = false
    private var isSaving = false
    /** True while navigating to/from the system image picker for Replace. */
    private var suppressAppOpenForReplace = false
    /** True after launching the picker until its result callback runs. */
    private var pendingPickerResult = false

    /** Latest blur SeekBar value; worker always coalesces to this. */
    private var latestBlurProgress: Int = -1
    private var lastAppliedAlphaProgress: Int = -1
    private var blurJob: Job? = null
    private var flipJob: Job? = null
    private var shapeJob: Job? = null
    private var reenableAppOpenJob: Job? = null

    companion object {
        private const val DEFAULT_BLEND_PROGRESS = 50
    }


    fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_blend)

        hideNavigationBar()
        applyWindowInsets()
        FirebaseLogUtils.logEvent("blend_editor_view", "user view blend editor screen")
        initViewModel()
        setupView()
        initRC()
        setupListeners()
        observeBlendEditorOptions()
        observeBlendShapes()
        onBackPressedCall()
        binding?.cardItemOptions?.setBackgroundResource(R.drawable.card_border_top)

        preloadInterEditSave(this)
        binding?.bannerAdView?.let { bannerContainer ->
            binding?.clAd?.let { clAd ->
                loadEditorAdaptiveBanner(
                    activity = this,
                    bannerContainer = bannerContainer,
                    shimmerView = binding?.shimmer,
                    clAd = clAd,
                )
            }
        }
    }

    private fun initViewModel() {
        editorViewModel.getAllBlendEditorOptions()
        editorViewModel.getAllBlendShapeStyles(this)
    }

    private fun setupView() {
        val imagePath = intent.getStringArrayListExtra(Extras.PICKER_IMG_LIST)?.firstOrNull()
            ?: intent.getStringExtra("path")
        path = imagePath.orEmpty()
        val frameType = intent.getStringExtra("frameType")

        if (path.isNotBlank()) {
            loadUserBitmap(path)
        }

        if (!frameType.isNullOrBlank()) {
            showBlendLoadingDialog()
            Glide.with(this).load(frameType)
                .into(object : CustomTarget<Drawable>() {
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        dismissBlendLoadingDialog()
                        ToastUtils.showToast(this@BlendActivity, "Something went wrong")
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}

                    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                        binding?.backgroundImage?.setImageDrawable(resource)
                        dismissBlendLoadingDialog()
                    }
                })
        }
    }

    private fun loadUserBitmap(imagePath: String) {
        Glide.with(this)
            .asBitmap()
            .load(resolveImageLoadModel(imagePath))
            .apply(RequestOptions().disallowHardwareConfig())
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onUserBitmapReady(resource)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    loadUserBitmapFallback(imagePath)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun loadUserBitmapFallback(imagePath: String) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                getThumbnail(resolveImageUri(imagePath))
            }
            if (bitmap != null) {
                onUserBitmapReady(bitmap)
            } else {
                binding?.image?.setImageURI(resolveImageUri(imagePath))
                binding?.image?.alpha = DEFAULT_BLEND_PROGRESS.toFloat() / 100
                binding?.image?.post { applyInitialBlendEffect() }
            }
        }
    }

    private fun resolveImageLoadModel(imagePath: String): Any {
        return when {
            imagePath.startsWith("content://") -> Uri.parse(imagePath)
            imagePath.startsWith("file://") -> Uri.parse(imagePath)
            imagePath.startsWith("/") -> File(imagePath)
            else -> imagePath
        }
    }

    private fun resolveImageUri(imagePath: String): Uri {
        return when {
            imagePath.startsWith("content://") -> Uri.parse(imagePath)
            imagePath.startsWith("file://") -> Uri.parse(imagePath)
            imagePath.startsWith("/") -> Uri.fromFile(File(imagePath))
            else -> Uri.parse(imagePath)
        }
    }

    private fun initRC() {
        editorOptionsAdapter = BlendEditorOptionsAdapter { text -> onOptionsItemClick(text) }
        blendShapeStyleAdapter = BlendShapeStyleAdapter { position, list ->
            onBlendShapeItemClick(position, list)
        }

        binding?.rcEditorOptions?.apply {
            layoutManager = LinearLayoutManager(this@BlendActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = editorOptionsAdapter
        }

        binding?.shapeLayout?.rcDripingStyle?.apply {
            layoutManager = LinearLayoutManager(this@BlendActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = blendShapeStyleAdapter
        }
    }

    private fun observeBlendEditorOptions() {
        editorViewModel.observeBlendEditorOptions().observe(this) { list ->
            if (!list.isNullOrEmpty()) {
                editorOptionsAdapter.updateList(list)
            }
        }
    }

    private fun observeBlendShapes() {
        editorViewModel.observeBlendsShapeStyles().observe(this) { list ->
            if (!list.isNullOrEmpty()) {
                blendShapeStyleAdapter.updateList(list)
            }
        }
    }

    private fun onOptionsItemClick(text: String) {
        when (text) {
            "Replace" -> {
                BlenderState.isBottomSheet = true
                callStoragePermission()
            }

            "Background" -> {
                val sheet = BlendingBottomSheet()
                sheet.show(supportFragmentManager, sheet.tag)
                sheet.setBlendingListener(object : BlendingBottomSheetPassData {
                    override fun onSelectedBlendingItem(
                        position: Int,
                        frameSelectedList: ArrayList<ModelFramePack>
                    ) {
                        val backgroundView = binding?.backgroundImage ?: return
                        val file = frameSelectedList.getOrNull(position)?.file ?: return
                        showBlendLoadingDialog()
                        // Load into the ImageView directly so Glide retains the drawable
                        // (CustomTarget resources can be cleared, leaving a blank editor).
                        Glide.with(this@BlendActivity)
                            .load(file)
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    dismissBlendLoadingDialog()
                                    ToastUtils.showToast(this@BlendActivity, "Something went wrong")
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable,
                                    model: Any,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    dismissBlendLoadingDialog()
                                    return false
                                }
                            })
                            .into(backgroundView)
                    }
                })
            }

            "Flip" -> flipUserImage()

            "Shapes" -> {
                binding?.cardItemOptions?.visibility = View.VISIBLE
                openShapesLayout()
            }

            "Blend" -> {
                binding?.cardItemOptions?.visibility = View.VISIBLE
                openBlurLayout()
            }
        }
    }

    private fun flipUserImage() {
        flipJob?.cancel()
        flipJob = lifecycleScope.launch {
            if (isImageMirror) {
                isImageMirror = false
                val bitmap = withContext(Dispatchers.IO) {
                    getThumbnail(resolveImageUri(path))?.let { ensureSoftwareBitmap(it) }
                }
                if (bitmap != null) {
                    recycleBlurredPreview()
                    bgBitmap = bitmap
                    binding?.image?.setImageBitmap(bitmap)
                } else {
                    binding?.image?.setImageURI(resolveImageUri(path))
                    bgBitmap = null
                }
            } else {
                isImageMirror = true
                val drawn = binding?.image?.drawToBitmap()
                val mirrored = withContext(Dispatchers.Default) {
                    drawn?.let { mirrorImage(ensureSoftwareBitmap(it)) }
                }
                if (mirrored != null) {
                    recycleBlurredPreview()
                    bgBitmap = mirrored
                    binding?.image?.setImageBitmap(mirrored)
                }
            }
            applyInitialBlendEffect()
        }
    }

    private fun onBlendShapeItemClick(position: Int, blendShapeList: ArrayList<String>) {
        shapeJob?.cancel()
        shapeJob = lifecycleScope.launch {
            val drawable = withContext(Dispatchers.IO) {
                assetsToDrawable(blendShapeList[position])
            }
            binding?.maskingLayout?.setMask(drawable)
            binding?.maskingLayout?.setPorterDuffXferMode(PorterDuff.Mode.DST_IN)
        }
    }

    private fun closeAllOptionsLayouts() {
        binding?.shapeLayout?.dripingLayout?.visibility = View.GONE
        binding?.blendLayout?.blendBlurLayout?.visibility = View.GONE
    }

    private fun openShapesLayout() {
        if (binding?.shapeLayout?.dripingLayout?.isVisible == true) {
            binding?.shapeLayout?.dripingLayout?.visibility = View.GONE
            editorOptionsAdapter.updatePosition(-1)
        } else {
            closeAllOptionsLayouts()
            binding?.shapeLayout?.dripingLayout?.visibility = View.VISIBLE
        }
    }

    private fun openBlurLayout() {
        if (binding?.blendLayout?.blendBlurLayout?.isVisible == true) {
            binding?.blendLayout?.blendBlurLayout?.visibility = View.GONE
            editorOptionsAdapter.updatePosition(-1)
        } else {
            closeAllOptionsLayouts()
            binding?.blendLayout?.blendBlurLayout?.visibility = View.VISIBLE
        }
    }

    private fun onUserBitmapReady(bitmap: Bitmap) {
        val softwareBitmap = ensureSoftwareBitmap(bitmap)
        bgBitmap = softwareBitmap
        try {
            recycleBlurredPreview()
            binding?.image?.setImageBitmap(softwareBitmap)
            binding?.image?.alpha = DEFAULT_BLEND_PROGRESS.toFloat() / 100
            binding?.image?.post { applyInitialBlendEffect() }
        } catch (_: OutOfMemoryError) {
            loadUserBitmapFallback(path)
        } catch (_: Exception) {
            loadUserBitmapFallback(path)
        }
    }

    private fun applyInitialBlendEffect() {
        if (binding == null) return
        binding?.blendLayout?.seekbarBlur?.progress = DEFAULT_BLEND_PROGRESS
        binding?.blendLayout?.seekbarAlpha?.progress = DEFAULT_BLEND_PROGRESS
        scheduleBlurApply(DEFAULT_BLEND_PROGRESS)
        applyAlphaProgress(DEFAULT_BLEND_PROGRESS)
        binding?.maskingLayout?.invalidate()
        binding?.image?.invalidate()
    }

    /**
     * Queues blur work off the main thread and coalesces rapid SeekBar updates
     * so only the latest progress is applied (prevents ANR / frame drops while dragging).
     */
    private fun scheduleBlurApply(progress: Int) {
        latestBlurProgress = progress
        if (blurJob?.isActive == true) return

        blurJob = lifecycleScope.launch {
            while (isActive) {
                val target = latestBlurProgress
                try {
                    if (target <= 0 || bgBitmap == null) {
                        restoreUnblurredImage()
                    } else {
                        val source = bgBitmap ?: break
                        val blurred = withContext(Dispatchers.Default) {
                            sideblur(source, target)
                        }
                        if (!isActive) break
                        // Newer SeekBar value arrived while computing — skip stale result
                        if (target != latestBlurProgress) continue
                        if (blurred != null) {
                            setBlurredPreview(blurred)
                        }
                    }
                } catch (_: Exception) {
                }
                if (target == latestBlurProgress) break
            }
        }
    }

    private fun restoreUnblurredImage() {
        if (isImageMirror) {
            binding?.image?.setImageBitmap(bgBitmap)
        } else if (bgBitmap != null) {
            binding?.image?.setImageBitmap(bgBitmap)
        } else {
            binding?.image?.setImageURI(resolveImageUri(path))
        }
        recycleBlurredPreview()
    }

    private fun setBlurredPreview(blurredBitmap: Bitmap) {
        val previous = lastBlurredBitmap
        lastBlurredBitmap = blurredBitmap
        binding?.image?.setImageBitmap(blurredBitmap)
        if (previous != null &&
            previous !== bgBitmap &&
            previous !== blurredBitmap &&
            !previous.isRecycled
        ) {
            try {
                previous.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun recycleBlurredPreview() {
        val previous = lastBlurredBitmap
        lastBlurredBitmap = null
        if (previous != null && previous !== bgBitmap && !previous.isRecycled) {
            try {
                previous.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun ensureSoftwareBitmap(bitmap: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
    }

    private fun applyAlphaProgress(progress: Int) {
        if (progress == lastAppliedAlphaProgress) return
        lastAppliedAlphaProgress = progress
        if (progress > 0) {
            binding?.image?.alpha = progress.toFloat() / 100
        } else {
            binding?.image?.alpha = DEFAULT_BLEND_PROGRESS.toFloat() / 100
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupListeners() {
        binding?.backArrow?.setOnClickListener {
            if (!isSingleClick) {
                isSingleClick = true
                onBackPressedDispatcher.onBackPressed()
            }
            returnToOriginalValue()
        }

        binding?.save?.setOnClickListener {
            if (isSaving || isFinishing || isDestroyed) return@setOnClickListener
            beginSaveFlow()
            BlenderState.isBottomSheet = false
            FirebaseLogUtils.logEvent(
                "blend_editor_btn_next_click",
                "user click Next on blend editor screen"
            )
            // Offline editor: cache → home interstitial (cooldown) → Preview. No rewarded.
            cacheThenInterstitialThenPreview()
        }

        binding?.blendLayout?.seekbarBlur?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scheduleBlurApply(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Ensure final thumb value is applied after coalesced updates
                seekBar?.let { scheduleBlurApply(it.progress) }
            }
        })

        binding?.blendLayout?.seekbarAlpha?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyAlphaProgress(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding?.layersIcon?.setOnClickListener {
            isLayerIconSelected = !isLayerIconSelected
            if (isLayerIconSelected) {
                ToastUtils.showToast(this, "Now you can Move a Image")
                Glide.with(this).load(R.drawable.layer1).into(binding?.layersIcon!!)
            } else {
                ToastUtils.showToast(this, "Now you can Move a Mask")
                Glide.with(this).load(R.drawable.layer2).into(binding?.layersIcon!!)
            }
            applyTouchMode()
        }

        applyTouchMode()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun applyTouchMode() {
        if (isLayerIconSelected) {
            binding?.image?.setOnTouchListener(ZoomListener(binding?.image!!))
            binding?.maskingLayout?.setOnTouchListener(null)
            binding?.maskingLayout?.isEnabled = false
            binding?.image?.isEnabled = true
        } else {
            binding?.maskingLayout?.setOnTouchListener(BlendMultiTouchListener())
            binding?.image?.setOnTouchListener(null)
            binding?.image?.isEnabled = false
            binding?.maskingLayout?.isEnabled = true
        }
    }

    private fun beginSaveFlow() {
        isSaving = true
        binding?.save?.isEnabled = false
        binding?.save?.isClickable = false
    }

    private fun endSaveFlow() {
        isSaving = false
        binding?.save?.isEnabled = true
        binding?.save?.isClickable = true
    }

    /**
     * Offline editor Next: Processing → cache → home interstitial (cooldown) → Preview.
     * Gallery save only on Preview. No rewarded on this path.
     */
    private fun cacheThenInterstitialThenPreview() {
        if (isFinishing || isDestroyed) {
            endSaveFlow()
            return
        }
        lifecycleScope.launch {
            try {
                SaveProgressHelper.showProcessing(this@BlendActivity)
                val captured = try {
                    binding?.mainLayout?.drawToBitmap()
                } catch (_: Exception) {
                    null
                }
                val savedPath = withContext(Dispatchers.IO) {
                    captured?.let { ImageUtils.saveBitmapToCache(this@BlendActivity, it) }
                }
                SaveProgressHelper.hide(this@BlendActivity)
                if (savedPath.isNullOrBlank()) {
                    ToastUtils.showErrorToast(this@BlendActivity)
                    endSaveFlow()
                    return@launch
                }
                isSaved = true
                FirebaseLogUtils.logEvent(
                    "blend_editor_save_success",
                    "user cached blend image for preview"
                )
                showHomeInterstitialThen {
                    if (!isFinishing && !isDestroyed) {
                        ResultLauncher.openLocalPreview(
                            this@BlendActivity,
                            ResultSource.PHOTO_BLENDER,
                            path = savedPath,
                        )
                    }
                    endSaveFlow()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                SaveProgressHelper.hide(this@BlendActivity)
                endSaveFlow()
                ToastUtils.showErrorToast(this@BlendActivity)
            }
        }
    }

    private fun beginReplaceImageFlow() {
        reenableAppOpenJob?.cancel()
        suppressAppOpenForReplace = true
        AppOpenManager.disableAppOpen = true
    }

    private fun endReplaceImageFlow() {
        reenableAppOpenJob?.cancel()
        pendingPickerResult = false
        suppressAppOpenForReplace = false
        AppOpenManager.disableAppOpen = false
    }

    private fun endReplaceImageFlowDelayed() {
        reenableAppOpenJob?.cancel()
        reenableAppOpenJob = lifecycleScope.launch {
            delay(500)
            pendingPickerResult = false
            suppressAppOpenForReplace = false
            AppOpenManager.disableAppOpen = false
        }
    }

    private fun launchImagePicker() {
        beginReplaceImageFlow()
        pendingPickerResult = true
        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun callStoragePermission() {
        // Suppress App Open Ad while navigating to the system image picker / permission UI.
        beginReplaceImageFlow()
        if (checkBlendStoragePermission()) {
            launchImagePicker()
        } else {
            takeStoragePermission()
        }
    }

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            pendingPickerResult = false
            if (uri != null) {
                path = uri.toString()
                lifecycleScope.launch {
                    val decoded = withContext(Dispatchers.IO) {
                        getThumbnail(uri)?.let { ensureSoftwareBitmap(it) }
                    }
                    val bitmap = decoded ?: binding?.image?.drawToBitmap()?.let { ensureSoftwareBitmap(it) }
                    recycleBlurredPreview()
                    bgBitmap = bitmap
                    binding?.image?.setImageBitmap(bitmap)
                    binding?.image?.alpha = DEFAULT_BLEND_PROGRESS.toFloat() / 100
                    binding?.image?.post { applyInitialBlendEffect() }
                }
            }
        }

    private val permissionLauncherForImage =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = if (Build.VERSION.SDK_INT <= 32) {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true &&
                    permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
            } else {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
            }
            if (granted) {
                launchImagePicker()
            } else {
                endReplaceImageFlow()
                showDialogForDontAskAgain()
            }
        }

    override fun onResume() {
        super.onResume()
        // Only re-enable after the picker has returned. Avoid clearing while a picker
        // launch is still pending (e.g. right after the permission dialog).
        if (suppressAppOpenForReplace && !pendingPickerResult) {
            endReplaceImageFlowDelayed()
        }
    }

    private fun takeStoragePermission() {
        if (Build.VERSION.SDK_INT <= 32) {
            permissionLauncherForImage.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        } else {
            permissionLauncherForImage.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        }
    }

    private fun returnToOriginalValue() {
        Handler(Looper.getMainLooper()).postDelayed({ isSingleClick = false }, 1000)
    }

    private fun onBackPressedCall() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!isSaved) goBackWarningDialog() else finish()
            }
        })
    }

    override fun onDestroy() {
        blurJob?.cancel()
        flipJob?.cancel()
        shapeJob?.cancel()
        reenableAppOpenJob?.cancel()
        recycleBlurredPreview()
        BlenderState.isBottomSheet = false
        if (suppressAppOpenForReplace) {
            endReplaceImageFlow()
        }
        binding = null
        super.onDestroy()
    }
}
