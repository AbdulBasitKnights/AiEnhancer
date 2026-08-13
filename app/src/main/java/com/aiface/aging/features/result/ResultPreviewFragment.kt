package com.aiface.aging.features.result

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.AiFaceApp
import com.aiface.aging.AiFaceApp.Companion.nativeResult
import com.aiface.aging.BuildConfig
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.databinding.FragmentResultPreviewBinding
import com.aiface.aging.features.look.haircolor.HairEditorViewModel
import com.aiface.aging.shared.BackPressGuard
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.showHomeInterstitialThen
import com.aiface.aging.shared.goHomeFresh
import com.aiface.aging.shared.showResultDiscardDialog
import com.aiface.aging.utils.SaveProgressHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Preview before share: show result image + Save to Gallery.
 * Back → discard dialog. Save → gallery then share screen with saved badge.
 */
@AndroidEntryPoint
class ResultPreviewFragment : Fragment() {

    private var _binding: FragmentResultPreviewBinding? = null
    private val binding get() = _binding!!

    private var mActivity: FragmentActivity? = null
    private var resultSource: ResultSource = ResultSource.AI
    private var localPreviewUri: Uri? = null
    private var alreadySavedOnDisk: Boolean = false
    private var nativeAd: NativeAd? = null
    private var isSaving: Boolean = false

    private val hairEditorViewModel: HairEditorViewModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentResultPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isViewActive(): Boolean = isAdded && _binding != null && view != null

    private fun safeToast(message: CharSequence) {
        if (!isAdded) return
        runCatching {
            val ctx = context ?: return
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            applyWindowInsets()
            readArgs()
            bindResultImage()
            binding.btnBack.setOnClickListener {
                runCatching { handleBack() }
            }
            binding.btnSaveGallery.setOnClickListener {
                runCatching { onSaveGalleryClicked() }
            }
            activity?.onBackPressedDispatcher?.addCallback(
                viewLifecycleOwner,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        runCatching { handleBack() }
                    }
                },
            )
            mActivity?.let { loadAds(it) }
        }.onFailure { it.printStackTrace() }
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.resultPreviewRoot) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.clToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top
            }
            binding.clAd.setPadding(
                binding.clAd.paddingLeft,
                binding.clAd.paddingTop,
                binding.clAd.paddingRight,
                bars.bottom,
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.resultPreviewRoot)
    }

    /** Prefer Activity intent extras — Nav start args strip undeclared keys / fill empty defaults. */
    private fun resolveArgBundle(): Bundle {
        val intentExtras = activity?.intent?.extras
        if (intentExtras != null && !intentExtras.isEmpty) {
            return Bundle(intentExtras)
        }
        return arguments ?: Bundle()
    }

    private fun readArgs() {
        val bundle = resolveArgBundle()
        arguments = bundle
        resultSource = ResultArgs.readSource(bundle)
        if (bundle.getString(ResultArgs.SOURCE).isNullOrBlank()) {
            when (bundle.getString("sourceFeature") ?: bundle.getString(ResultArgs.SOURCE_FEATURE)) {
                "face_makeup" -> resultSource = ResultSource.FACE_MAKEUP
                "hair_color" -> resultSource = ResultSource.HAIR_COLOR
            }
        }
        val localPath = bundle.getString(ResultArgs.LOCAL_IMAGE_PATH)?.takeIf { it.isNotBlank() }
        if (localPath != null && resultSource == ResultSource.AI &&
            bundle.getString(ResultArgs.OUTPUT_IMAGE_URL).isNullOrBlank()
        ) {
            resultSource = ResultSource.PHOTO_EDITOR
        }
        alreadySavedOnDisk = bundle.getBoolean(ResultArgs.ALREADY_SAVED, false)
        localPreviewUri = resolveLocalUri(
            bundle.getString(ResultArgs.LOCAL_IMAGE_URI)?.takeIf { !it.isNullOrBlank() },
            localPath,
        )
    }

    private fun bindResultImage() {
        binding.tvTitle.setText(R.string.preview)
        binding.tvTitle.visibility = View.VISIBLE

        when (resultSource) {
            ResultSource.HAIR_COLOR, ResultSource.FACE_MAKEUP -> {
                val bmp = hairEditorViewModel.finalBitmap
                if (bmp != null && !bmp.isRecycled) {
                    binding.imageResult.setImageBitmap(bmp)
                } else {
                    loadLocalOrRemoteImage()
                }
            }

            ResultSource.AI -> {
                val localPath = resolveArgBundle().getString(ResultArgs.LOCAL_IMAGE_PATH)
                if (!localPath.isNullOrBlank() && File(localPath).exists()) {
                    loadLocalOrRemoteImage()
                } else {
                    Glide.with(binding.imageResult)
                        .load(getRemoteImageUrl())
                        .placeholder(R.drawable.placeholder_icon)
                        .error(R.drawable.placeholder_icon)
                        .into(binding.imageResult)
                }
            }

            else -> loadLocalOrRemoteImage()
        }
    }

    private fun loadLocalOrRemoteImage() {
        val bundle = resolveArgBundle()
        val path = bundle.getString(ResultArgs.LOCAL_IMAGE_PATH)?.takeIf { it.isNotBlank() }
        val file = path?.let { File(it) }?.takeIf { it.exists() && it.isFile }
        val uriString = bundle.getString(ResultArgs.LOCAL_IMAGE_URI)?.takeIf { it.isNotBlank() }
        val loadTarget: Any? = when {
            file != null -> file
            localPreviewUri != null -> localPreviewUri
            !uriString.isNullOrBlank() -> Uri.parse(uriString)
            else -> getRemoteImageUrl()
        }
        if (loadTarget != null) {
            Glide.with(binding.imageResult)
                .load(loadTarget)
                .placeholder(R.drawable.placeholder_icon)
                .error(R.drawable.placeholder_icon)
                .into(binding.imageResult)
        } else {
            binding.imageResult.setImageResource(R.drawable.placeholder_icon)
        }
    }

    private fun handleBack() {
        if (!BackPressGuard.tryHandle()) return
        runCatching {
            if (!isAdded) return@runCatching
            showResultDiscardDialog { goHomeFresh() }
        }.onFailure { goHomeFresh() }
    }

    private fun onSaveGalleryClicked() {
        if (isSaving || !isViewActive()) return
        val activity = mActivity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        runCatching {
            activity.showHomeInterstitialThen(forFragment = true) {
                if (!isViewActive()) return@showHomeInterstitialThen
                saveAndGoToShare()
            }
        }.onFailure {
            if (isViewActive()) saveAndGoToShare()
        }
    }

    private fun loadAds(activity: FragmentActivity) {
        if (!isViewActive()) return
        runCatching {
            if (!nativeResult) {
                binding.clAd.visibility = View.GONE
                return
            }
            if (nativeResult) {
                startNative(activity, tryHigh = true)
            }  else {
                binding.clAd.visibility = View.GONE
            }
        }.onFailure {
            _binding?.clAd?.visibility = View.GONE
        }
    }

    private fun startNative(activity: FragmentActivity, tryHigh: Boolean) {
        try {
            if (!isViewActive()) return
            if (!AdsHelper.shouldShowAds()) {
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding.clAd,
                    shimmerWrapper = binding.shimmer,
                    nativeContainer = binding.nativeAdView,
                )
                return
            }
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding.clAd,
                shimmerWrapper = binding.shimmer,
                nativeContainer = binding.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_home_hf,
                normalUnitId = BuildConfig.native_home,
                onLoaded = { ad, _ ->
                    try {
                        if (!isViewActive()) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeAd?.destroy()
                        nativeAd = ad
                        AdsHelper.bindNativeAdToContainerSmall(
                            nativeAd,
                            binding.nativeAdView,
                            binding.shimmerContainerNative.shimmerContainerNative,
                            activity,
                            binding.shimmer,
                        )
                        binding.nativeAdView.visibility = View.VISIBLE
                        binding.shimmer.visibility = View.GONE
                        binding.shimmerContainerNative.shimmerContainerNative.visibility = View.GONE
                        binding.clAd.visibility = View.VISIBLE
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = _binding?.clAd,
                            shimmerWrapper = _binding?.shimmer,
                            nativeContainer = _binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = _binding?.clAd,
                        shimmerWrapper = _binding?.shimmer,
                        nativeContainer = _binding?.nativeAdView,
                    )
                },
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = _binding?.clAd,
                shimmerWrapper = _binding?.shimmer,
                nativeContainer = _binding?.nativeAdView,
            )
        }
    }

    private fun saveAndGoToShare() {
        if (isSaving) return
        isSaving = true
        when {
            alreadySavedOnDisk && localPreviewUri != null -> {
                openShareScreen(localPreviewUri)
                isSaving = false
            }

            resultSource == ResultSource.HAIR_COLOR || resultSource == ResultSource.FACE_MAKEUP -> {
                saveLookBitmap()
            }

            resultSource == ResultSource.AI -> {
                val imageUrl = getRemoteImageUrl()
                if (imageUrl.isNullOrBlank()) {
                    isSaving = false
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.result_no_image_to_save),
                        Toast.LENGTH_SHORT,
                    ).show()
                    return
                }
                saveRemoteImage(imageUrl)
            }

            else -> {
                savePendingLocalToGallery()
            }
        }
    }

    private fun savePendingLocalToGallery() {
        val bundle = resolveArgBundle()
        val path = bundle.getString(ResultArgs.LOCAL_IMAGE_PATH)?.takeIf { it.isNotBlank() }
        val uriString = bundle.getString(ResultArgs.LOCAL_IMAGE_URI)?.takeIf { it.isNotBlank() }
        val loadTarget: Any? = when {
            !path.isNullOrBlank() && File(path).exists() -> File(path)
            localPreviewUri != null -> localPreviewUri
            !uriString.isNullOrBlank() -> Uri.parse(uriString)
            !path.isNullOrBlank() && path.startsWith("content://") -> Uri.parse(path)
            !path.isNullOrBlank() -> File(path)
            else -> null
        }
        if (loadTarget == null) {
            isSaving = false
            Toast.makeText(
                requireContext(),
                getString(R.string.result_no_image_to_save),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        binding.btnSaveGallery.isEnabled = false
        SaveProgressHelper.show(binding.saveProgressOverlay.root, requireContext())
        lifecycleScope.launch {
            val savedUri =
                try {
                    withContext(Dispatchers.IO) {
                        val bitmap =
                            Glide.with(requireContext().applicationContext)
                                .asBitmap()
                                .load(loadTarget)
                                .submit()
                                .get()
                        storeBitmapToGallery(bitmap)
                    }
                } finally {
                    if (isAdded) {
                        SaveProgressHelper.hide(binding.saveProgressOverlay.root)
                        binding.btnSaveGallery.isEnabled = true
                    }
                    isSaving = false
                }
            if (!isAdded) return@launch
            if (savedUri == null) {
                Toast.makeText(requireContext(), getString(R.string.fail_photo_save), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            openShareScreen(savedUri)
        }
    }

    private fun storeBitmapToGallery(bitmap: android.graphics.Bitmap): Uri? {
        val context = requireContext()
        val resolver = context.contentResolver
        val fileName = "Image_${System.currentTimeMillis()}.jpg"
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    val picturesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.DATA, File(picturesDir, fileName).absolutePath)
                }
            }
        return try {
            val uri = resolver.insert(imageCollection, contentValues) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
                } ?: throw IOException("Unable to open output stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveLookBitmap() {
        val bitmap = hairEditorViewModel.finalBitmap
        if (bitmap == null || bitmap.isRecycled) {
            Toast.makeText(
                requireContext(),
                getString(R.string.result_no_image_to_save),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val activity = mActivity ?: return
        binding.btnSaveGallery.isEnabled = false
        SaveProgressHelper.show(binding.saveProgressOverlay.root, requireContext())
        lifecycleScope.launch {
            val uri =
                try {
                    withContext(Dispatchers.IO) {
                        hairEditorViewModel.saveBitmapToGallery(activity.applicationContext, bitmap)
                    }
                } finally {
                    if (isAdded) {
                        SaveProgressHelper.hide(binding.saveProgressOverlay.root)
                        binding.btnSaveGallery.isEnabled = true
                    }
                    isSaving = false
                }
            if (!isAdded) return@launch
            if (uri == null) {
                Toast.makeText(requireContext(), getString(R.string.fail_photo_save), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            openShareScreen(uri)
        }
    }

    private fun saveRemoteImage(imageUrl: String) {
        binding.btnSaveGallery.isEnabled = false
        SaveProgressHelper.show(binding.saveProgressOverlay.root, requireContext())
        lifecycleScope.launch {
            val savedUri =
                try {
                    withContext(Dispatchers.IO) { downloadAndStoreImage(imageUrl) }
                } finally {
                    if (isAdded) {
                        SaveProgressHelper.hide(binding.saveProgressOverlay.root)
                        binding.btnSaveGallery.isEnabled = true
                    }
                    isSaving = false
                }
            if (!isAdded) return@launch
            if (savedUri == null) {
                Toast.makeText(requireContext(), getString(R.string.fail_photo_save), Toast.LENGTH_SHORT)
                    .show()
                return@launch
            }
            openShareScreen(savedUri)
        }
    }

    private fun openShareScreen(savedUri: Uri?) {
        if (!isViewActive()) return
        runCatching {
            val shareArgs = Bundle(resolveArgBundle()).apply {
                putBoolean(ResultArgs.ALREADY_SAVED, true)
                if (savedUri != null) {
                    putString(ResultArgs.LOCAL_IMAGE_URI, savedUri.toString())
                }
                if (!containsKey(ResultArgs.SOURCE) || getString(ResultArgs.SOURCE).isNullOrBlank()) {
                    putString(ResultArgs.SOURCE, resultSource.name)
                }
            }
            val nav = runCatching { findNavController() }.getOrNull() ?: return@runCatching
            val action = nav.currentDestination?.getAction(R.id.action_resultPreviewFragment_to_resultFragment)
            if (action == null) {
                android.util.Log.w("ResultPreview", "navigate skipped — action missing")
                return@runCatching
            }
            nav.navigate(R.id.action_resultPreviewFragment_to_resultFragment, shareArgs)
        }.onFailure {
            it.printStackTrace()
            safeToast(getString(R.string.fail_photo_save))
        }
    }

    private fun getRemoteImageUrl(): String? {
        return getGenerateResponse()?.data?.outputUrl
            ?: arguments?.getString(ResultArgs.OUTPUT_IMAGE_URL)
    }

    private fun getGenerateResponse(): NewGenerateResponse? {
        val bundle = resolveArgBundle()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(ResultArgs.NEW_GENERATE_RESPONSE, NewGenerateResponse::class.java)
                ?: bundle.getParcelable("generate_response", NewGenerateResponse::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(ResultArgs.NEW_GENERATE_RESPONSE)
                ?: bundle.getParcelable("generate_response")
        }
    }

    private fun resolveLocalUri(uriString: String?, path: String?): Uri? {
        if (!uriString.isNullOrBlank()) return Uri.parse(uriString)
        val pathValue = path.orEmpty()
        if (pathValue.startsWith("content://")) return Uri.parse(pathValue)
        if (pathValue.startsWith("file://")) return Uri.parse(pathValue)
        if (pathValue.isBlank()) return null
        val file = File(pathValue)
        if (!file.exists()) return null
        // Prefer FileProvider; fall back to file:// for same-app Glide (cache previews).
        return try {
            FileProvider.getUriForFile(
                requireContext(),
                "${BuildConfig.APPLICATION_ID}.myfileprovider",
                file,
            )
        } catch (_: IllegalArgumentException) {
            try {
                FileProvider.getUriForFile(
                    requireContext(),
                    "${BuildConfig.APPLICATION_ID}.provider",
                    file,
                )
            } catch (_: Exception) {
                Uri.fromFile(file)
            }
        }
    }

    private fun downloadAndStoreImage(imageUrl: String): Uri? {
        val context = requireContext()
        val resolver = context.contentResolver
        val fileName = "Image_${System.currentTimeMillis()}.jpg"
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    val picturesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    put(MediaStore.Images.Media.DATA, File(picturesDir, fileName).absolutePath)
                }
            }
        return try {
            val bitmap =
                Glide.with(context.applicationContext)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
            val uri = resolver.insert(imageCollection, contentValues) ?: return null
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, outputStream)
                } ?: throw IOException("Unable to open output stream")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(
                        uri,
                        ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                        null,
                        null,
                    )
                }
                uri
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override fun onDestroyView() {
        nativeAd?.destroy()
        nativeAd = null
        _binding = null
        super.onDestroyView()
    }
}
