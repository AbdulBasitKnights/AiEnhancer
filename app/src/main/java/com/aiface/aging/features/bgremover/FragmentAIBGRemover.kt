package com.aiface.aging.features.bgremover

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallClient
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.aiface.aging.R
import com.aiface.aging.shared.hide
import com.aiface.aging.shared.show
import com.aiface.aging.databinding.FragmentAiBgRemoverBinding
import com.aiface.aging.features.eraser.MagicEraserActivity
import com.aiface.aging.features.editor.model.ModelFramePack
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.LogUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface ModuleProgressListener {
    fun onModuleProgressUpdate(@ModuleInstallStatusUpdate.InstallState state: Int)
}

@AndroidEntryPoint
class FragmentAIBGRemover(private val newUri : String = "") : Fragment(), ModuleProgressListener {
    private var binding: FragmentAiBgRemoverBinding? = null
    private var mActivity: FragmentActivity? = null
    private val parentJob = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + parentJob)
    private var isDestroyed: Boolean = false
    private var userImageBitmap: Bitmap? = null
    private lateinit var userImagePath: String


    private var goBack = false


    private var currentCredit = 5


    interface ImageRemoveBGListener {
        fun onBGRemoverd(img : String)
    }


    companion object {
        var userImgPath = ""
        var openMagicEraser = false
        lateinit var moduleInstallClient: ModuleInstallClient
        private var listener : ImageRemoveBGListener ?=null
        fun setRemovedListener(listener: ImageRemoveBGListener){
            this.listener = listener
        }
        fun newInstance(bundle: Bundle): FragmentAIBGRemover {
            val fragment = FragmentAIBGRemover()
            fragment.arguments = bundle
            return fragment
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAiBgRemoverBinding.inflate(inflater, container, false)
        binding?.lifecycleOwner = viewLifecycleOwner
        isDestroyed = false

        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { fragmentActivity ->
            FirebaseLogUtils.logEvent("bg_remover_scr_view", "User view loading screen")
            val list= ArrayList<String>()
            list.add(newUri)
            moduleInstallClient = ModuleInstall.getClient(fragmentActivity)
            handleBackPress(fragmentActivity)
            binding?.btnCancelErasing?.setOnClickListener {
                if (parentJob.isActive) parentJob.cancel()
               if (isAdded){
                   findNavController().popBackStack()
               }
            }

                    val imagePath = userImgPath
                    userImagePath = imagePath
                    binding?.animationProcessing?.show()
                    binding?.eraseUserImageView?.let {
                        Glide.with(fragmentActivity).load(imagePath).into(
                            it
                        )
                    }


                updateImage(fragmentActivity, userImagePath)


            binding?.menuIcon?.setOnClickListener {
              findNavController().popBackStack()

            }

        }

    }


    private fun updateImage(
        mActivity: FragmentActivity?,
        imagePath: String
    ) {
        mActivity?.let { activity ->
            Glide.with(activity).asBitmap().override(800).load(imagePath)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap, transition: Transition<in Bitmap>?
                    ) {

                        binding?.eraseUserImageView?.setImageBitmap(resource)
                        userImageBitmap = resource
                        cropNow(activity, resource)
                        binding?.animationProcessing?.show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Handle clearing of the image if needed
                    }
                })

        }

    }


    private fun cropNow(activity: FragmentActivity, bitmap: Bitmap) {
        binding?.animationProcessing?.show()
        Glide.with(activity).asBitmap().load(bitmap).override(800)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap>?
                ) {
                    coroutineScope.launch {
                        try {
                            val bitmap = withContext(Dispatchers.IO) {
                                getResult(
                                    activity, resource, this@FragmentAIBGRemover
                                )
                            }
                            delay(2000)
                            withContext(Dispatchers.Main) {
                                binding?.eraseUserImageView?.setImageBitmap(bitmap)
                                binding?.animationProcessing?.hide()
                                binding?.animationProcessed?.show()
                            }
                            val erasedBitmapPath = withContext(Dispatchers.IO) {
                                AppUtils.convertBitmapToImagePath(activity, bitmap)
                            }
                            delay(600)
                            withContext(Dispatchers.Main) {
                                erasedBitmapPath?.let { path ->
                                    if (!activity.isFinishing && !activity.isDestroyed) {
                                        val targetActivity = if (openMagicEraser) {
                                            MagicEraserActivity::class.java
                                        } else {
                                            BGRemoverActivity::class.java
                                        }
                                        openMagicEraser = false
                                        val intent = Intent(activity, targetActivity)
                                        intent.putExtra(Extras.ERASED_BITMAP_PATH, path)
                                        intent.putExtra(Extras.USER_IMAGE_PATH, userImagePath)
                                        binding?.animationProcessed?.hide()
                                        goBack = true
                                        startActivity(intent)
                                    }
                                }
                            }
                        } catch (t: Throwable) {
                            Log.e("FragmentAIBGRemover", "BG segmentation failed", t)
                            withContext(Dispatchers.Main) {
                                binding?.animationProcessing?.hide()
                                binding?.animationProcessed?.hide()
                                if (SubjectSegmentationHelper.isNativeModuleError(t)) {
                                    handleSegmentationUnavailable(activity)
                                } else {
                                    ToastUtils.showErrorToast(activity)
                                    if (isAdded) {
                                        findNavController().popBackStack()
                                    }
                                }
                            }
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    // Handle clearing of the image if needed
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if (goBack) {
            if (isAdded){
                findNavController().popBackStack()
            }
        }
    }
    override fun onModuleProgressUpdate(state: Int) {
        try {
            when (state) {
                ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED -> {
                    mActivity?.let { activity ->
                        userImageBitmap?.let { bitmap -> cropNow(activity, bitmap) }
                    }
                }

                ModuleInstallStatusUpdate.InstallState.STATE_CANCELED -> {
                    mActivity?.let { activity ->
                        ToastUtils.showErrorToast(activity)
                        if (isAdded){
                            findNavController().popBackStack()
                        }

                    }
                }

                ModuleInstallStatusUpdate.InstallState.STATE_FAILED -> {
                    mActivity?.let { activity ->
                        ToastUtils.showInternetWarningToast(activity)
                       if (isAdded){
                           findNavController().popBackStack()
                       }
                    }
                }
            }
        } catch (e: Exception) {
           e.printStackTrace()
        }
    }

    /**
     * Segments foreground subject; triggers module download on recoverable failures.
     */
    suspend fun getResult(
        context: FragmentActivity,
        image: Bitmap,
        moduleProgressListener: ModuleProgressListener,
    ): Bitmap {
        return try {
            SubjectSegmentationHelper.segmentForegroundCropped(image)
        } catch (t: Throwable) {
            if (SubjectSegmentationHelper.isNativeModuleError(t)) {
                throw t
            }
            withContext(Dispatchers.Main) {
                requestModule(context, moduleProgressListener)
            }
            throw t
        }
    }

    private fun handleSegmentationUnavailable(activity: FragmentActivity) {
        ToastUtils.showToast(
            activity,
            getString(R.string.bg_remover_ai_unavailable),
        )
        if (isAdded) {
            findNavController().popBackStack()
        }
    }

    private fun requestModule(
        context: Context,
        moduleProgressListener: ModuleProgressListener,
    ) {
        val segmenter: SubjectSegmenter = try {
            SubjectSegmentationHelper.createSegmenter()
        } catch (linkError: LinkageError) {
            Log.e("FragmentAIBGRemover", "ML Kit native module unavailable", linkError)
            if (context is FragmentActivity) {
                handleSegmentationUnavailable(context)
            }
            return
        }

        moduleInstallClient.deferredInstall(segmenter)
        val moduleInstallRequest = ModuleInstallRequest.newBuilder().addApi(segmenter)
            .setListener(ModuleInstallProgressListener(context, moduleProgressListener)).build()
        moduleInstallClient.installModules(moduleInstallRequest).addOnSuccessListener { module ->
            if (module.areModulesAlreadyInstalled()) {
                // Modules are already installed when the request is sent.
            }
        }.addOnFailureListener { error ->
            Log.e("FragmentAIBGRemover", "Module install failed", error)
            if (SubjectSegmentationHelper.isNativeModuleError(error) && context is FragmentActivity) {
                handleSegmentationUnavailable(context)
                return@addOnFailureListener
            }
            Toast.makeText(
                context,
                getString(R.string.failed_to_prepare_ai_please_check_your_network_connection),
                Toast.LENGTH_SHORT,
            ).show()
            coroutineScope.launch {
                delay(3000)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        findNavController().popBackStack()
                    }
                }
            }
        }
    }

    class ModuleInstallProgressListener(
        val context: Context, private val moduleProgressListener: ModuleProgressListener
    ) : InstallStatusListener {
        override fun onInstallStatusUpdated(update: ModuleInstallStatusUpdate) {
            // Progress info is only set when modules are in the progress of downloading.
            update.progressInfo?.let {
                val progress = (it.bytesDownloaded * 100 / it.totalBytesToDownload).toInt()
                if (progress == 0) ToastUtils.showToast(
                    context,
                    context.getString(R.string.ai_in_progress_it_may_take_a_little_longer_for_the_first_time)
                )
                if (progress == 100) ToastUtils.showToast(context,
                    context.getString(R.string.congrats_ready_to_process))
            }
            if (isTerminateState(update.installState)) {
                moduleInstallClient.unregisterListener(this)
                if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED) {
                    // Installation completed, you can hide the progress bar or update UI accordingly

                } else if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED) {
                    // Installation canceled, handle as needed
                } else if (update.installState == ModuleInstallStatusUpdate.InstallState.STATE_FAILED) {
                    // Installation failed, handle as needed
                }
            }
            moduleProgressListener.onModuleProgressUpdate(update.installState)
        }

        private fun isTerminateState(@ModuleInstallStatusUpdate.InstallState state: Int): Boolean {
            return state == ModuleInstallStatusUpdate.InstallState.STATE_CANCELED || state == ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED || state == ModuleInstallStatusUpdate.InstallState.STATE_FAILED
        }
    }

    private fun handleBackPress(activity: FragmentActivity) {
        try {
            val onBackPressedCallback = object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle the back button event
                    if (parentJob.isActive) parentJob.cancel()
                    if (isAdded){

                        findNavController().popBackStack()
                    }

                }
            }
            activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
        } catch (e: Exception) {

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isDestroyed = true
        if (parentJob.isActive) parentJob.cancel()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()

    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }
}