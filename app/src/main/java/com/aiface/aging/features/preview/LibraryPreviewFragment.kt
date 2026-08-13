package com.aiface.aging.features.preview

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.databinding.FragmentLibraryPreviewBinding


class LibraryPreviewFragment : Fragment() {

    private lateinit var binding: FragmentLibraryPreviewBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLibraryPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString("imageUri")?.let {
            try {
                val imageUri = it.toUri()
                val originalBitmap =
                    MediaStore.Images.Media.getBitmap(requireContext().contentResolver, imageUri)
                binding.previewIv.setImageBitmap(originalBitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding?.back?.setOnClickListener {
            activity?.finish()
        }

        binding.shareTv.setOnClickListener {
//            toast("Share Image .....  ")
            AppOpenManager.disableAppOpen = true
            shareImage()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AppOpenManager.disableAppOpen = false
    }
    private fun shareImage() {
        val uriString = arguments?.getString("imageUri")
        val imageUri = uriString?.toUri()

        if (imageUri == null) {
            toast("No image to share")
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}