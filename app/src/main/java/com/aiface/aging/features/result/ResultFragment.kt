package com.aiface.aging.features.result

import android.content.Context
import android.content.ContentValues
import android.content.Intent
import com.aiface.aging.features.editor.EditorActivity
import com.aiface.aging.features.imgpicker.util.Extras
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.aiface.aging.BuildConfig
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.databinding.FragmentResultBinding
import com.aiface.aging.features.look.haircolor.HairEditorViewModel
import com.aiface.aging.features.mywork.FragmentMyWork
import com.aiface.aging.features.share.ExtrasShareImageActivity
import com.aiface.aging.features.share.ShareImageViewModel
import com.aiface.aging.features.rateus.RateUsFragment
import com.aiface.aging.shared.DataStoreManager
import com.aiface.aging.shared.HAS_SHOWN_RATE_US
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.adjustRevenueMMP
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.goHomeFresh
import com.aiface.aging.shared.safePopBackStack
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.SaveProgressHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    private fun isViewActive(): Boolean = isAdded && _binding != null && view != null

    private fun safeToast(message: CharSequence) {
        if (!isAdded) return
        runCatching {
            val ctx = context ?: return
            Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
        }
    }

    private var mActivity: FragmentActivity? = null
    private var nativeAd: NativeAd? = null
    private var resultSource: ResultSource = ResultSource.AI
    private var savedShareUri: Uri? = null
    private var pendingImageUrlToSave: String? = null
    private var myWorkMediaId: Long? = null
    private var myWorkDisplayName: String? = null
    private var fromMyWork: Boolean = false

    private val hairEditorViewModel: HairEditorViewModel by activityViewModels()
    private val shareViewModel: ShareImageViewModel by viewModels()

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    companion object {
        /**
         * Temporary toggle:
         * false = social share row (current)
         * true  = restore Try More Features list
         */
        private const val SHOW_TRY_MORE_FEATURES = true
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!isAdded) {
                pendingImageUrlToSave = null
                return@registerForActivityResult
            }
            if (granted) {
                pendingImageUrlToSave?.let { saveRemoteImage(it) }
            } else {
                safeToast(getString(R.string.result_storage_permission_required))
            }
            pendingImageUrlToSave = null
        }

    private val deletePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val activity = mActivity as? AppCompatActivity ?: return@registerForActivityResult
                shareViewModel.deletePendingImage(activity)
                FragmentMyWork.isDeleted.value = true
                activity.onBackPressedDispatcher.onBackPressed()
            }
        }

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
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        runCatching {
            applyWindowInsets()
            readArgs()
            logAnalytics()
            configureToolbar()
            bindResultImage()
            setupClicks()
            setupBackNavigation()
            setupBottomFeatureOrShareRow()
            observeDeletePermission()
            mActivity?.let { loadAds(it) }
            maybeShowRateUs()
        }.onFailure { it.printStackTrace() }
    }

    /** First share after generate/edit (skip My Work reopen). */
    private fun maybeShowRateUs() {
        if (fromMyWork) return
        if (!AiFaceApp.showRateUsOnShare) return
        dataStoreManager.readDataStoreValue(HAS_SHOWN_RATE_US, false) {
            if (this) return@readDataStoreValue
            dataStoreManager.writeDataStoreValue(HAS_SHOWN_RATE_US, true)
            mActivity?.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread
                view?.post {
                    if (!isAdded || _binding == null) return@post
                    RateUsFragment.show(childFragmentManager)
                }
            }
        }
    }

    private fun setupBottomFeatureOrShareRow() {
        if (SHOW_TRY_MORE_FEATURES) {
            binding.tvTryMoreFeatures.isVisible = true
            binding.rvTryMoreFeatures.isVisible = true
            binding.shareSocialSection.isVisible = true
            ResultScreenHelper.setupTryMoreFeatures(binding.rvTryMoreFeatures, this)
            setupSocialShareRow()
        } else {
            binding.tvTryMoreFeatures.isVisible = false
            binding.rvTryMoreFeatures.isVisible = false
            binding.shareSocialSection.isVisible = true
            setupSocialShareRow()
        }
    }

    private fun setupSocialShareRow() {
        val adapter = ResultSocialShareAdapter { option ->
            shareResult(packageName = option.packageName)
        }
        binding.rvSocialShare.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvSocialShare.adapter = adapter
        adapter.submit(
            listOf(
                ResultSocialShareOption(1, R.drawable.ic_facebook_new, "Facebook", "com.facebook.katana"),
                ResultSocialShareOption(2, R.drawable.ic_whatsapp_new, "Whatsapp", "com.whatsapp"),
                ResultSocialShareOption(3, R.drawable.ic_tiktok_new, "TikTok", "com.zhiliaoapp.musically"),
                ResultSocialShareOption(4, R.drawable.ic_instagram_new, "Instagram", "com.instagram.android"),
            ),
        )
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
        fromMyWork = bundle.getBoolean(ResultArgs.FROM_MY_WORK, false)
        myWorkMediaId = bundle.getLong(ResultArgs.MEDIA_ID, -1L).takeIf { it > 0L }
        myWorkDisplayName = bundle.getString(ResultArgs.DISPLAY_NAME)

        val alreadySaved = bundle.getBoolean(ResultArgs.ALREADY_SAVED, false)
        if (alreadySaved) {
            savedShareUri = resolveLocalUri(
                bundle.getString(ResultArgs.LOCAL_IMAGE_URI)?.takeIf { !it.isNullOrBlank() },
                bundle.getString(ResultArgs.LOCAL_IMAGE_PATH)?.takeIf { !it.isNullOrBlank() },
            )
            ResultScreenHelper.showSavedBadge(binding.savedBadge)
        }
    }

    private fun resolveArgBundle(): Bundle {
        val args = arguments
        if (args != null && args.getBoolean(ResultArgs.ALREADY_SAVED, false)) {
            return args
        }
        val intentExtras = activity?.intent?.extras
        if (intentExtras != null && !intentExtras.isEmpty) {
            if (args == null || args.isEmpty) return Bundle(intentExtras)
            // Intent fills blanks left by Nav default empty strings.
            return Bundle(intentExtras).apply {
                putAll(args)
                if (getString(ResultArgs.LOCAL_IMAGE_PATH).isNullOrBlank()) {
                    intentExtras.getString(ResultArgs.LOCAL_IMAGE_PATH)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { putString(ResultArgs.LOCAL_IMAGE_PATH, it) }
                }
                if (getString(ResultArgs.LOCAL_IMAGE_URI).isNullOrBlank()) {
                    intentExtras.getString(ResultArgs.LOCAL_IMAGE_URI)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { putString(ResultArgs.LOCAL_IMAGE_URI, it) }
                }
                if (getString(ResultArgs.SOURCE).isNullOrBlank()) {
                    intentExtras.getString(ResultArgs.SOURCE)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { putString(ResultArgs.SOURCE, it) }
                }
            }
        }
        return args ?: Bundle()
    }

    private fun logAnalytics() {
        when (resultSource) {
            ResultSource.HAIR_COLOR -> FirebaseLogUtils.logEvent("hair_color_result_view", "")
            ResultSource.FACE_MAKEUP -> FirebaseLogUtils.logEvent("makeup_result_view", "")
            ResultSource.PHOTO_BLENDER -> FirebaseLogUtils.logEvent("photo_blender_result_view", "")
            else -> Unit
        }
    }

    private fun configureToolbar() {
        val isLook = resultSource == ResultSource.HAIR_COLOR || resultSource == ResultSource.FACE_MAKEUP
        val isLocalSaved = arguments?.getBoolean(ResultArgs.ALREADY_SAVED, false) == true
        val isHostActivity = activity is ResultHostActivity

        binding.btnHome.visibility = if (isLook || isHostActivity) View.VISIBLE else View.GONE
        binding.btnEdit.visibility = if (isLook) View.VISIBLE else View.GONE
        binding.btnDelete.visibility = if (fromMyWork) View.VISIBLE else View.GONE
        binding.btnReport.visibility = if (resultSource == ResultSource.AI) View.VISIBLE else View.GONE
        if (binding.btnReport.isVisible) {
            binding.btnReport.bringToFront()
        }
        binding.btnSave.visibility = if (isLocalSaved) View.GONE else View.VISIBLE
    }

    private fun bindResultImage() {
        if (!isViewActive()) return
        runCatching {
            when (resultSource) {
                ResultSource.HAIR_COLOR, ResultSource.FACE_MAKEUP -> {
                    binding.imageResult.setImageBitmap(hairEditorViewModel.finalBitmap)
                }

                ResultSource.AI -> {
                    val imageUrl = getRemoteImageUrl()
                    Glide.with(this@ResultFragment)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_icon)
                        .error(R.drawable.placeholder_icon)
                        .into(binding.imageResult)
                }

                else -> {
                    val bundle = resolveArgBundle()
                    val path = bundle.getString(ResultArgs.LOCAL_IMAGE_PATH)?.takeIf { it.isNotBlank() }
                    val file = path?.let { java.io.File(it) }?.takeIf { it.exists() }
                    val uri = savedShareUri
                    val loadTarget: Any? = when {
                        file != null -> file
                        uri != null -> uri
                        else -> getRemoteImageUrl()
                    }
                    Glide.with(this@ResultFragment)
                        .load(loadTarget)
                        .placeholder(R.drawable.placeholder_icon)
                        .error(R.drawable.placeholder_icon)
                        .into(binding.imageResult)
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    fun openPhotoEditorFromResult() {
        if (!isViewActive()) return
        binding.root.isEnabled = false
        lifecycleScope.launch {
            val imageRef = runCatching { resolveEditableImageReference() }.getOrNull()
            if (isViewActive()) {
                binding.root.isEnabled = true
            }
            if (!isViewActive()) return@launch
            if (imageRef != null) {
                runCatching { startPhotoEditor(imageRef) }
                return@launch
            }
            safeToast(getString(R.string.result_no_image_to_share))
        }
    }

    private suspend fun resolveEditableImageReference(): String? {
        resolveLocalImageReference()?.let { return it }
        if (resultSource == ResultSource.HAIR_COLOR || resultSource == ResultSource.FACE_MAKEUP) {
            val bitmap = hairEditorViewModel.finalBitmap ?: return null
            return writeBitmapToCache(bitmap)
        }
        val remoteUrl = getRemoteImageUrl()?.takeIf { it.isNotBlank() } ?: return null
        return downloadToCache(remoteUrl)
    }

    private fun resolveLocalImageReference(): String? {
        savedShareUri?.toString()?.let { return it }
        arguments?.getString(ResultArgs.LOCAL_IMAGE_PATH)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        arguments?.getString(ResultArgs.LOCAL_IMAGE_URI)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
        return null
    }

    private suspend fun writeBitmapToCache(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val appCtx = context?.applicationContext ?: return@runCatching null
            val file = File(appCtx.cacheDir, "result_edit_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output) }
            file.absolutePath
        }.getOrNull()
    }

    private suspend fun downloadToCache(imageUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val appCtx = context?.applicationContext ?: return@runCatching null
            val bitmap =
                Glide.with(appCtx)
                    .asBitmap()
                    .load(imageUrl)
                    .submit()
                    .get()
            writeBitmapToCache(bitmap)
        }.getOrNull()
    }

    private fun startPhotoEditor(imageRef: String) {
        val ctx = context ?: return
        val host = activity ?: return
        if (host.isFinishing || host.isDestroyed) return
        runCatching {
            startActivity(
                Intent(ctx, EditorActivity::class.java).apply {
                    putExtra(Extras.PICKER_IMG_LIST, imageRef)
                    putExtra(ResultFeatureNavigator.EXTRA_LAUNCHED_FROM_RESULT, true)
                },
            )
        }
    }

    private fun setupClicks() {
        if (!isViewActive()) return
        binding.btnBack.setOnClickListener {
            runCatching { handleBack() }
        }
        binding.btnHome.setOnClickListener {
            runCatching { handleHome() }
        }
        binding.btnEdit.setOnClickListener {
            runCatching {
                if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@runCatching
                if (safePopBackStack()) return@runCatching
                activity?.safeFinish()
            }
        }
        binding.btnShare.setOnClickListener {
            runCatching { shareResult(packageName = null) }
        }
        binding.btnSave.setOnClickListener {
            runCatching { saveResult() }
        }
        binding.btnDelete.setOnClickListener {
            runCatching { confirmDelete() }
        }
        binding.btnReport.setOnClickListener {
            runCatching { openReportBottomSheet() }
                .onFailure { Log.e("ResultFragment", "report click failed", it) }
        }
        binding.imageResult.setOnClickListener {
            runCatching { openImagePreview() }
        }
    }

    private fun openReportBottomSheet() {
        if (!isViewActive()) return
        val tag = "report"
        if (parentFragmentManager.findFragmentByTag(tag) != null) return

        val sheet = ReportBottomsheetFragment.newInstance(
            jobId = getGenerateResponse()?.data?.jobId,
            imageUrl = getRemoteImageUrl(),
        )
        try {
            sheet.show(parentFragmentManager, tag)
        } catch (e: IllegalStateException) {
            // After ads / config change, FM may already have saved state.
            parentFragmentManager.beginTransaction()
                .add(sheet, tag)
                .commitAllowingStateLoss()
        }
    }

    private fun setupBackNavigation() {
        val activity = activity ?: return
        activity.onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    runCatching { handleBack() }
                }
            },
        )
    }

    private fun observeDeletePermission() {
        shareViewModel.permissionNeededForDelete.observe(viewLifecycleOwner) { intentSender ->
            intentSender?.let {
                val request = IntentSenderRequest.Builder(it).build()
                deletePermissionLauncher.launch(request)
            }
        }
    }

    private fun handleBack() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        try {
            if (fromMyWork) {
                // Library preview stack — pop only.
                if (!safePopBackStack()) {
                    activity?.safeFinish()
                }
                return
            }
            // Share screen: image already saved — back goes home (discard lives on preview).
            goHomeFresh()
        } catch (e: Exception) {
            e.printStackTrace()
            goHomeFresh()
        }
    }

    private fun openImagePreview() {
        if (!isViewActive()) return
        runCatching {
            if (parentFragmentManager.isStateSaved) return@runCatching
            val localUri =
                savedShareUri?.toString()
                    ?: arguments?.getString(ResultArgs.LOCAL_IMAGE_URI)
                    ?: arguments?.getString(ResultArgs.LOCAL_IMAGE_PATH)
            ResultImagePreviewDialog
                .newInstance(
                    source = resultSource,
                    imageUrl = getRemoteImageUrl(),
                    localUri = localUri,
                )
                .show(parentFragmentManager, "result_image_preview")
        }
    }

    private fun handleHome() {
        if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
        goHomeFresh()
    }

    private fun shareResult(packageName: String? = null) {
        if (!isViewActive()) return
        runCatching {
            when {
                savedShareUri != null -> shareLocalUri(savedShareUri!!, packageName)
                resultSource == ResultSource.HAIR_COLOR || resultSource == ResultSource.FACE_MAKEUP -> {
                    val uri = savedShareUri
                    if (uri == null) {
                        safeToast(getString(R.string.result_no_image_to_share))
                    } else {
                        shareLocalUri(uri, packageName)
                    }
                }

                resultSource == ResultSource.AI -> shareRemoteUrl(packageName)
                else -> {
                    val uri = savedShareUri
                    if (uri != null) shareLocalUri(uri, packageName)
                    else shareRemoteUrl(packageName)
                }
            }
        }.onFailure { it.printStackTrace() }
    }

    private fun shareRemoteUrl(packageName: String? = null) {
        if (!isViewActive()) return
        val imageUrl = getRemoteImageUrl()
        if (imageUrl.isNullOrBlank()) {
            safeToast(getString(R.string.result_no_image_to_share))
            return
        }
        runCatching {
            val ctx = context ?: return
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, imageUrl)
                    if (!packageName.isNullOrBlank()) setPackage(packageName)
                }
            if (!packageName.isNullOrBlank() &&
                shareIntent.resolveActivity(ctx.packageManager) == null
            ) {
                safeToast(getString(R.string.result_share_app_unavailable))
                return
            }
            if (packageName.isNullOrBlank()) {
                startActivity(Intent.createChooser(shareIntent, getString(R.string.result_share_via)))
            } else {
                startActivity(shareIntent)
            }
        }.onFailure {
            safeToast(getString(R.string.result_no_image_to_share))
        }
    }

    private fun shareLocalUri(uri: Uri, packageName: String? = null) {
        if (!isViewActive()) return
        try {
            val ctx = context ?: return
            val storeUrl =
                "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, storeUrl)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    if (!packageName.isNullOrBlank()) setPackage(packageName)
                }
            fun launchTarget(targetPackage: String): Boolean {
                shareIntent.setPackage(targetPackage)
                if (shareIntent.resolveActivity(ctx.packageManager) == null) {
                    return false
                }
                ctx.grantUriPermission(
                    targetPackage,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                startActivity(shareIntent)
                return true
            }
            if (!packageName.isNullOrBlank()) {
                val launched =
                    launchTarget(packageName) ||
                        (packageName == "com.zhiliaoapp.musically" &&
                            launchTarget("com.ss.android.ugc.trill"))
                if (!launched) {
                    safeToast(getString(R.string.result_share_app_unavailable))
                }
                return
            }
            startActivity(Intent.createChooser(shareIntent, getString(R.string.result_share_via)))
        } catch (e: Exception) {
            e.printStackTrace()
            safeToast(getString(R.string.result_no_image_to_share))
        }
    }

    private fun saveResult() {
        logSaveClickEvent()
        when (resultSource) {
            ResultSource.HAIR_COLOR, ResultSource.FACE_MAKEUP -> saveLookBitmap()
            ResultSource.AI -> saveAiResult()
            else -> Unit
        }
    }

    private fun logSaveClickEvent() {
        if (resultSource != ResultSource.AI) return
        val feature = arguments?.getString(ResultArgs.SOURCE_FEATURE)
            ?: arguments?.getString("url")
        if (feature == "enhancer") {
            FirebaseLogUtils.logEvent("home_click_enhancer_save", "")
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
        binding.btnSave.isEnabled = false
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
                        binding.btnSave.isEnabled = true
                    }
                }
            if (!isAdded) return@launch
            onSaved(uri)
        }
    }

    private fun saveAiResult() {
        val imageUrl = getRemoteImageUrl()
        if (imageUrl.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.result_no_image_to_save),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        saveRemoteImage(imageUrl)
    }

    private fun saveRemoteImage(imageUrl: String) {
        if (!isAdded) return
        binding.btnSave.isEnabled = false
        SaveProgressHelper.show(binding.saveProgressOverlay.root, requireContext())
        lifecycleScope.launch {
            val savedUri =
                try {
                    withContext(Dispatchers.IO) { downloadAndStoreImage(imageUrl) }
                } finally {
                    if (isAdded) {
                        SaveProgressHelper.hide(binding.saveProgressOverlay.root)
                        binding.btnSave.isEnabled = true
                    }
                }
            if (!isAdded) return@launch
            onSaved(savedUri)
        }
    }

    private fun onSaved(uri: Uri?) {
        if (uri == null) {
            Toast.makeText(requireContext(), getString(R.string.fail_photo_save), Toast.LENGTH_SHORT)
                .show()
            return
        }
        savedShareUri = uri
        ResultScreenHelper.showSavedBadge(binding.savedBadge)
        binding.btnSave.visibility = View.GONE
        Toast.makeText(requireContext(), getString(R.string.lb_saved_to_phone), Toast.LENGTH_SHORT)
            .show()
    }

    private fun confirmDelete() {
        val uri = savedShareUri
        if (!fromMyWork || uri == null || myWorkMediaId == null) {
            handleHome()
            return
        }
        val dialog = DialogueUtils.getDialogue(requireContext(), R.layout.dialog_delete_image)
        dialog.findViewById<TextView>(R.id.buttonYes)?.setOnClickListener {
            dialog.dismiss()
            val activity = mActivity as? AppCompatActivity ?: return@setOnClickListener
            shareViewModel.deleteImage(
                ExtrasShareImageActivity(
                    id = myWorkMediaId,
                    uri = uri,
                    displayName = myWorkDisplayName,
                    fromMyWork = true,
                ),
                activity,
            )
        }
        dialog.findViewById<TextView>(R.id.buttonNo)?.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun getRemoteImageUrl(): String? {
        return getGenerateResponse()?.data?.outputUrl
            ?: arguments?.getString(ResultArgs.OUTPUT_IMAGE_URL)
    }

    private fun getGenerateResponse(): NewGenerateResponse? {
        val bundle = arguments ?: return null
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
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
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

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.resultRoot) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.clToolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = statusBars.top
            }
            binding.clAd.setPadding(0, 0, 0, navBars.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.resultRoot)
    }

    private fun loadAds(activity: FragmentActivity) {
        if (!AdsHelper.shouldShowAds() || !AiFaceApp.isNativeShare) {
            binding.clAd.visibility = View.GONE
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding.clAd,
                shimmerWrapper = binding.shimmer,
                nativeContainer = binding.nativeAdView,
            )
            return
        }
        val useShareAds = arguments?.getBoolean(ResultArgs.ALREADY_SAVED, false) == true
        if (useShareAds) {
            if (AiFaceApp.isNativeShare) {
                startNative(
                    tryHigh = true,
                    highUnitId = BuildConfig.native_share_hf,
                    normalUnitId = BuildConfig.native_share,
                )
            } else {
                binding.clAd.visibility = View.GONE
            }
        } else {
            if (AiFaceApp.isNativeShare) {
                startNative(
                    tryHigh = true,
                    highUnitId = BuildConfig.native_home_hf,
                    normalUnitId = BuildConfig.native_home,
                )
            }  else {
                binding.clAd.visibility = View.GONE
            }
        }
    }

    private fun startNative(tryHigh: Boolean, highUnitId: String, normalUnitId: String) {
        try {
            if (!AdsHelper.shouldShowAds() || !AiFaceApp.isNativeShare) {
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
                highUnitId = highUnitId,
                normalUnitId = normalUnitId,
                onLoaded = { ad, unitId ->
                    try {
                        if (!isViewActive()) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeAd?.destroy()
                        nativeAd = ad
                        val container = _binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = _binding?.clAd,
                                shimmerWrapper = _binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        NativeAdDisplayHelper.displayWithoutMedia(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = ad,
                            onDestroyPrevious = {},
                            adUnitId = unitId,
                            layoutResId = R.layout.layout_native_ads_without_mediaview_b,
                            shimmer = _binding?.shimmer
                        )
                        _binding?.clAd?.visibility = View.VISIBLE
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
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = _binding?.clAd,
                shimmerWrapper = _binding?.shimmer,
                nativeContainer = _binding?.nativeAdView,
            )
        }
    }

    override fun onDestroyView() {
        SaveProgressHelper.hide(_binding?.saveProgressOverlay?.root)
        nativeAd?.destroy()
        nativeAd = null
        super.onDestroyView()
        _binding = null
    }
}
