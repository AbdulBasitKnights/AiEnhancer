package com.aiface.aging.features.result

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.databinding.DialogResultImagePreviewBinding
import com.aiface.aging.features.look.haircolor.HairEditorViewModel

class ResultImagePreviewDialog : DialogFragment() {

    private var _binding: DialogResultImagePreviewBinding? = null
    private val binding get() = _binding!!

    private val hairEditorViewModel: HairEditorViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = DialogResultImagePreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnPreviewBack.setOnClickListener { dismiss() }
        bindImage()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            setBackgroundDrawable(ColorDrawable(Color.BLACK))
        }
    }

    private fun bindImage() {
        val args = requireArguments()
        val source = runCatching {
            ResultSource.valueOf(args.getString(ARG_SOURCE).orEmpty())
        }.getOrDefault(ResultSource.AI)

        when (source) {
            ResultSource.HAIR_COLOR, ResultSource.FACE_MAKEUP -> {
                hairEditorViewModel.finalBitmap?.let { binding.imagePreview.setImageBitmap(it) }
            }

            ResultSource.AI -> {
                val imageUrl = args.getString(ARG_IMAGE_URL)
                Glide.with(binding.imagePreview)
                    .load(imageUrl)
                    .fitCenter()
                    .into(binding.imagePreview)
            }

            else -> {
                val localUri = args.getString(ARG_LOCAL_URI)
                val imageUrl = args.getString(ARG_IMAGE_URL)
                when {
                    !localUri.isNullOrBlank() -> {
                        Glide.with(binding.imagePreview)
                            .load(localUri)
                            .fitCenter()
                            .into(binding.imagePreview)
                    }

                    !imageUrl.isNullOrBlank() -> {
                        Glide.with(binding.imagePreview)
                            .load(imageUrl)
                            .fitCenter()
                            .into(binding.imagePreview)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SOURCE = "preview_source"
        private const val ARG_IMAGE_URL = "preview_image_url"
        private const val ARG_LOCAL_URI = "preview_local_uri"

        fun newInstance(
            source: ResultSource,
            imageUrl: String? = null,
            localUri: String? = null,
        ): ResultImagePreviewDialog =
            ResultImagePreviewDialog().apply {
                arguments =
                    bundleOf(
                        ARG_SOURCE to source.name,
                        ARG_IMAGE_URL to imageUrl,
                        ARG_LOCAL_URI to localUri,
                    )
            }
    }
}
