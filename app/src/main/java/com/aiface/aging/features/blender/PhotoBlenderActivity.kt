package com.aiface.aging.features.blender

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.aiface.aging.R
import com.aiface.aging.databinding.ActivityPhotoBlenderBinding
import com.aiface.aging.features.result.ResultLauncher
import com.aiface.aging.features.result.ResultSource
import com.aiface.aging.shared.hideNavigationBar
import com.aiface.aging.shared.setSafeClickListener
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.SaveProgressHelper
import com.aiface.aging.utils.multitouchlistener.MultiTouchListener
import com.aiface.aging.utils.multitouchlistener.OnDoubleTapListener
import com.aiface.aging.utils.multitouchlistener.OnImageViewTouchListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Photo Blender — pick base + character, ML Kit subject mask, drag/scale cutout, export.
 *
 * Gallery uses system [PickVisualMedia] (TedImagePicker.start is a no-op for Activities).
 */
@AndroidEntryPoint
class PhotoBlenderActivity : AppCompatActivity(), OnDoubleTapListener, OnImageViewTouchListener {

    private var binding: ActivityPhotoBlenderBinding? = null
    private val viewModel: PhotoBlenderViewModel by viewModels()
    private var canSave = false

    private val pickBaseLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            viewModel.onBaseSelected(uri)
        }

    private val pickCharacterLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri ?: return@registerForActivityResult
            loadBitmapFromUri(uri) { bitmap ->
                viewModel.onCharacterBitmapReady(bitmap)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBlenderBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        hideNavigationBar()
        FirebaseLogUtils.logEvent("photo_blender_view", "")

        setupClicks()
        setupMultiTouch()
        observeState()
        observeEvents()
    }

    private fun setupClicks() {
        val b = binding ?: return
        b.btnBack.setSafeClickListener { finish() }
        b.btnPickBase.setSafeClickListener {
            FirebaseLogUtils.logEvent("photo_blender_pick_base", "")
            launchGallery(pickBaseLauncher)
        }
        b.btnPickCharacter.setSafeClickListener {
            FirebaseLogUtils.logEvent("photo_blender_pick_character", "")
            launchGallery(pickCharacterLauncher)
        }
        b.btnSave.setSafeClickListener {
            if (!canSave) return@setSafeClickListener
            FirebaseLogUtils.logEvent("photo_blender_save", "")
            val canvas = runCatching { b.stickerView.createBitmap() }.getOrNull()
            if (canvas == null) {
                Toast.makeText(this, R.string.blender_export_failed, Toast.LENGTH_SHORT).show()
                return@setSafeClickListener
            }
            viewModel.saveBlend(canvas)
        }
    }

    private fun launchGallery(
        launcher: androidx.activity.result.ActivityResultLauncher<PickVisualMediaRequest>,
    ) {
        launcher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    private fun setupMultiTouch() {
        val userImage = binding?.userImage1 ?: return
        userImage.setOnTouchListener(
            MultiTouchListener(applicationContext, userImage, false, this, this),
        )
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is PhotoBlenderEvent.ShowBase -> showBase(event.uri)
                        is PhotoBlenderEvent.ShowCharacterCutout -> showCharacter(event.bitmap)
                        is PhotoBlenderEvent.OpenResult -> {
                            showHomeInterstitialThen {
                                ResultLauncher.openLocalPreview(
                                    this@PhotoBlenderActivity,
                                    ResultSource.PHOTO_BLENDER,
                                    path = event.path,
                                )
                            }
                        }
                        is PhotoBlenderEvent.Toast -> {
                            Toast.makeText(
                                this@PhotoBlenderActivity,
                                event.message,
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: PhotoBlenderUiState) {
        val b = binding ?: return
        b.progressMask.isVisible = state is PhotoBlenderUiState.Masking
        when (state) {
            is PhotoBlenderUiState.NeedBase -> {
                SaveProgressHelper.hide(this)
                canSave = false
                b.btnSave.isEnabled = false
                b.btnPickCharacter.isEnabled = false
                b.tvHint.setText(R.string.blender_hint_pick_base)
            }
            is PhotoBlenderUiState.NeedCharacter -> {
                SaveProgressHelper.hide(this)
                canSave = false
                b.btnSave.isEnabled = false
                b.btnPickCharacter.isEnabled = true
                b.tvHint.setText(R.string.blender_hint_pick_character)
            }
            is PhotoBlenderUiState.Masking -> {
                SaveProgressHelper.hide(this)
                canSave = false
                b.btnSave.isEnabled = false
                b.btnPickCharacter.isEnabled = false
                b.tvHint.setText(R.string.blender_hint_masking)
            }
            is PhotoBlenderUiState.Ready -> {
                SaveProgressHelper.hide(this)
                canSave = true
                b.btnSave.isEnabled = true
                b.btnPickCharacter.isEnabled = true
                b.tvHint.setText(R.string.blender_hint_ready)
            }
            is PhotoBlenderUiState.Saving -> {
                canSave = false
                b.btnSave.isEnabled = false
                b.tvHint.setText(R.string.processing)
                SaveProgressHelper.showProcessing(this)
            }
            is PhotoBlenderUiState.Saved -> {
                SaveProgressHelper.hide(this)
            }
            is PhotoBlenderUiState.Error -> {
                SaveProgressHelper.hide(this)
                b.btnPickCharacter.isEnabled = b.frameImage.drawable != null
            }
        }
    }

    private fun showBase(uri: Uri) {
        val frame = binding?.frameImage ?: return
        Glide.with(frame).load(uri).into(frame)
    }

    private fun showCharacter(bitmap: Bitmap) {
        val userImage = binding?.userImage1 ?: return
        binding?.addUserImage1?.visibility = View.GONE
        userImage.visibility = View.VISIBLE
        userImage.setImageBitmap(bitmap)
        userImage.scaleX = 1f
        userImage.scaleY = 1f
        userImage.rotation = 0f
        userImage.translationX = 0f
        userImage.translationY = 0f
    }

    private fun loadBitmapFromUri(uri: Uri, onReady: (Bitmap) -> Unit) {
        Glide.with(this)
            .asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    onReady(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) = Unit

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    Toast.makeText(
                        this@PhotoBlenderActivity,
                        R.string.blender_load_failed,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            })
    }

    override fun onDoubleTapListner(view: View?) = Unit

    override fun onImageTouchListener(view: View) = Unit

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }
}
