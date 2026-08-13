package com.aiface.aging.features.faceswap

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentResultFaceswapBinding
import com.aiface.aging.features.result.ReportBottomsheetFragment
import com.aiface.aging.features.main.MainFragment
import com.aiface.aging.features.share.ExtrasShareImageActivity
import com.aiface.aging.features.share.ShareImageActivity
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.DialogueUtils
import com.aiface.aging.utils.FirebaseLogUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@AndroidEntryPoint
class FaceSwapResultFragment : Fragment() {

    private var _binding: FragmentResultFaceswapBinding? = null
    private val binding get() = _binding!!
    private var mActivity: FragmentActivity? = null
    private var isSaving = false
    private var pendingImageUrlToSave: String? = null
    private var isFinalActionInProgress = false

    private val exitDialogue: Dialog by lazy {
        DialogueUtils.getDialogue(requireActivity(), R.layout.dialog_exit_editing)
    }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingImageUrlToSave?.let { saveResultImageToGallery(it) }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.result_storage_permission_required),
                    Toast.LENGTH_SHORT,
                ).show()
            }
            pendingImageUrlToSave = null
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
        _binding = FragmentResultFaceswapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseLogUtils.logEvent("face_swap_result_scr_view", "user view face swap result")

        mActivity?.let { activity ->
            binding.btnReport.visibility = View.VISIBLE

            applyWindowInsets()
            setupBackNavigation()
            setupShareOptions()
            setupClicks()
            bindResultImage()
            loadAds(activity)
            preloadResultInterstitial(activity)
        }
    }

    override fun onResume() {
        super.onResume()
        mActivity?.let(::preloadResultInterstitial)
    }

    private fun bindResultImage() {
        val imageUrl = arguments?.getString("output_image_url")
        Glide.with(binding.imageResult.context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder_icon)
            .centerCrop()
            .into(binding.imageResult)
    }

    private fun setupBackNavigation() {
        binding.btnBack.setOnClickListener {
            mActivity?.let { showExitEditingDialogue(it) }
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    mActivity?.let { showExitEditingDialogue(it) }
                }
            },
        )
    }

    private fun setupClicks() {
        binding.btnReport.setOnClickListener {
            mActivity?.let {
                if (parentFragmentManager.findFragmentByTag("report") != null) return@let
                val dg = ReportBottomsheetFragment.newInstance(
                    jobId = arguments?.getString("job_id"),
                    imageUrl = arguments?.getString("output_image_url"),
                )
                try {
                    dg.show(parentFragmentManager, "report")
                } catch (_: IllegalStateException) {
                    parentFragmentManager.beginTransaction()
                        .add(dg, "report")
                        .commitAllowingStateLoss()
                }
            }
        }

        binding.btnShare.setOnClickListener {
            FirebaseLogUtils.logEvent(
                "face_swap_result_share_click",
                "user click share on face swap result",
            )
            shareResultImage()
        }
        binding.btnSave.setOnClickListener {
            if (isSaving || isFinalActionInProgress) return@setOnClickListener
            FirebaseLogUtils.logEvent(
                "face_swap_result_save_click",
                "user click save on face swap result",
            )
            mActivity?.let { activity ->
                showInterstitialThen(activity) {
                    saveResultImageToGallery()
                }
            }
        }
    }

    /**
     * ShareAdapterMediaIcons + ic_whatsapp_new / etc. are missing in this project.
     * Hide media row; btnShare still opens system chooser.
     */
    private fun setupShareOptions() {
        _binding?.recyclerviewMedia?.visibility = View.GONE
        _binding?.recyclerviewMedia?.adapter = null
    }

    private fun applyWindowInsets() {
        binding.root.applySystemBarInsets(applyTop = true, applyBottom = true)
    }

    private fun shareResultImage(packageName: String? = null) {
        val imageUrl = arguments?.getString("output_image_url")
        if (imageUrl.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.result_no_image_to_share),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, imageUrl)
            setPackage(packageName)
        }
        if (packageName != null &&
            shareIntent.resolveActivity(requireContext().packageManager) == null
        ) {
            Toast.makeText(
                requireContext(),
                getString(R.string.result_share_app_unavailable),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (packageName == null) {
            startActivity(Intent.createChooser(shareIntent, getString(R.string.result_share_via)))
        } else {
            startActivity(shareIntent)
        }
    }

    private fun saveResultImageToGallery() {
        val imageUrl = arguments?.getString("output_image_url")
        if (imageUrl.isNullOrBlank()) {
            Toast.makeText(
                requireContext(),
                getString(R.string.result_no_image_to_save),
                Toast.LENGTH_SHORT,
            ).show()
            return
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !hasLegacyStoragePermission()) {
            pendingImageUrlToSave = imageUrl
            requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }
        saveResultImageToGallery(imageUrl)
    }

    private fun saveResultImageToGallery(imageUrl: String) {
        if (isSaving) return
        isSaving = true
        binding.btnSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val savedUri = withContext(Dispatchers.IO) { downloadAndStoreImage(imageUrl) }
                if (savedUri != null) {
                    FirebaseLogUtils.logEvent(
                        "face_swap_result_save_success",
                        "face swap result saved to gallery",
                    )
                    mActivity?.let { activity ->
                        Toast.makeText(activity, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                        val filePath = AppUtils.getFilePathFromContentUri(savedUri, activity)
                        val intent = Intent(activity, ShareImageActivity::class.java)
                        val extras = ExtrasShareImageActivity()
                        extras.path = filePath
                        extras.fromMyWork = false
                        intent.putExtra(ShareImageActivity::class.java.simpleName, extras)
                        startActivity(intent)
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT)
                        .show()
                }
            } finally {
                isSaving = false
                _binding?.btnSave?.isEnabled = true
            }
        }
    }

    private fun hasLegacyStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun downloadAndStoreImage(imageUrl: String): Uri? {
        val context = requireContext()
        val resolver = context.contentResolver
        val fileName = "FaceSwap_${System.currentTimeMillis()}.jpg"
        val imageCollection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val contentValues = ContentValues().apply {
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
            val bitmap = Glide.with(context.applicationContext)
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

    /** No-op while FORCE_PRO / Next-Gen migration — ads not loaded here. */
    private fun preloadResultInterstitial(activity: FragmentActivity) = Unit

    /** Continue immediately (no InterstitialAdGate / gms interstitial). */
    private fun showInterstitialThen(
        activity: FragmentActivity,
        action: () -> Unit,
    ) {
        if (isFinalActionInProgress) return
        isFinalActionInProgress = true
        isFinalActionInProgress = false
        action()
    }

    private fun showExitEditingDialogue(activity: FragmentActivity) {
        val cancel = exitDialogue.findViewById<View>(R.id.buttonCancel)
        val discard = exitDialogue.findViewById<View>(R.id.buttonDiscard)
        cancel?.setOnClickListener {
            if (!activity.isFinishing && !activity.isDestroyed) exitDialogue.dismiss()
        }
        discard?.setOnClickListener {
            if (!activity.isFinishing &&
                !activity.isDestroyed &&
                !isFinalActionInProgress
            ) {
                exitDialogue.dismiss()
                showInterstitialThen(activity) {
                    if (isAdded) {
                        MainFragment.selectedItem.value = MainFragment.PAGER_HOME
                        MainFragment.navSelectedTab.value = com.aiface.aging.ui.glassnav.TAB_HOME
                        findNavController().navigate(R.id.mainFragment)
                    }
                }
            }
        }
        if (!activity.isFinishing && !activity.isDestroyed && !exitDialogue.isShowing) {
            exitDialogue.show()
        }
    }

    override fun onDestroyView() {
        isFinalActionInProgress = false
        _binding = null
        super.onDestroyView()
    }

    /** Hide native slot — no gms AdLoader. */
    private fun loadAds(activity: FragmentActivity) {
        _binding?.clAd?.visibility = View.GONE
    }

}
