package com.aiface.aging.features.faceswap

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.databinding.FragmentPreviewBinding
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.FirebaseLogUtils

class FaceSwapPreviewFragment : Fragment() {

    private var binding: FragmentPreviewBinding? = null
    private var mActivity: FragmentActivity? = null
    private var template: FaceSwapTemplateDto? = null

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
    ): View? {
        binding = FragmentPreviewBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FirebaseLogUtils.logEvent("face_swap_preview_scr_view", "user view face swap preview")

        mActivity?.let { activity ->
            AppUtils.getMain(activity)?.hideHomeBannerAd()
            loadAds(activity)

            binding?.legacyPreviewContainer?.visibility = View.VISIBLE
            binding?.vpTemplates?.visibility = View.GONE
            binding?.btnBack?.imageTintList =
                android.content.res.ColorStateList.valueOf(
                    requireContext().getColor(R.color.icon_primary),
                )
            binding?.btnBack?.setBackgroundResource(0)

            template = readTemplate()
            val previewUrl = template?.displayPreviewUrl()
                ?: arguments?.getString("preview_url")
                ?: arguments?.getString("url")
            val title = template?.name ?: arguments?.getString("title").orEmpty()

            binding?.tvHeaderTemplateCategory?.text = title
            binding?.tvPrompt?.visibility = View.GONE
            binding?.tvTemplateName?.visibility = View.GONE

            binding?.imagePreview?.let { imageView ->
                Glide.with(activity)
                    .load(previewUrl)
                    .placeholder(R.drawable.placeholder_icon)
                    .into(imageView)
            }

            binding?.btnBack?.setOnClickListener { findNavController().navigateUp() }
            binding?.btnTryTemplate?.setOnClickListener {
                FirebaseLogUtils.logEvent(
                    "face_swap_try_template_click",
                    "user click try template on face swap preview",
                )
                goToImagePicker(activity)
            }
        }
    }

    /** Flags default false / pro path — hide native slot (no gms AdLoader). */
    private fun loadAds(activity: FragmentActivity) {
        binding?.clAd?.visibility = View.GONE
    }

    private fun goToImagePicker(activity: FragmentActivity) {
        val current = template ?: readTemplate() ?: return
        val imgCount = current.resolveRequiredImageCount()
        val bundle = bundleOf(
            FaceSwapFragment.ARG_TEMPLATE to current,
            "item_id" to current.id.orEmpty(),
            "title" to current.name.orEmpty(),
            "url" to current.displayPreviewUrl().orEmpty(),
            "imgCount" to imgCount,
            "category_id" to current.categoryId.orEmpty(),
            "media_type" to current.mediaType.orEmpty(),
            "thumbnail_url" to current.thumbnailUrl.orEmpty(),
            "preview_url" to current.previewUrl.orEmpty(),
            "image_url" to current.imageUrl.orEmpty(),
        )

        TedImagePicker.with(activity, "faceswap")
            .image()
            .max(imgCount, "cannot select more than $imgCount image")
            .min(imgCount, "select at least $imgCount image")
            .bundleExtras(bundle)
            .albumType(AlbumType.DROP_DOWN)
            .startMultiImageFragment()
    }

    private fun readTemplate(): FaceSwapTemplateDto? {
        val bundle = arguments ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(FaceSwapFragment.ARG_TEMPLATE, FaceSwapTemplateDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(FaceSwapFragment.ARG_TEMPLATE)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
