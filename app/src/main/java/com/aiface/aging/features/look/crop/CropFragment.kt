package com.aiface.aging.features.look.crop

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aiface.aging.databinding.FragmentCropBinding
import com.aiface.aging.features.look.haircolor.HairEditorViewModel
import com.aiface.aging.shared.safeNavigate
import com.aiface.aging.shared.safePopBackStack
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CropFragment : Fragment() {

    private var _binding : FragmentCropBinding? =null

    private val binding get() = _binding

    private val hairEditorViewModel : HairEditorViewModel by activityViewModels()

    private val args : CropFragmentArgs by navArgs()

    private var mActivity: FragmentActivity? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity =  requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        _binding = FragmentCropBinding.inflate(inflater,container,false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                v.paddingLeft,
                systemBars.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
        binding?.cropImageView?.setImageUriAsync(args.imagePath.toUri())

        binding?.btnConfirm?.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            lifecycleScope.launch {
                try {
                    hairEditorViewModel.cropedImage = binding?.cropImageView?.getCroppedImage()
                    if (args.isHair) {
                        safeNavigate(CropFragmentDirections.actionCropFragmentToHairColorEditorFragment())
                    } else {
                        safeNavigate(CropFragmentDirections.actionCropFragmentToFaceMakeUpFragment())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding?.btnStartEditing?.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            lifecycleScope.launch {
                try {
                    hairEditorViewModel.cropedImage = binding?.cropImageView?.getCroppedImage()
                    if (args.isHair) {
                        safeNavigate(CropFragmentDirections.actionCropFragmentToHairColorEditorFragment())
                    } else {
                        safeNavigate(CropFragmentDirections.actionCropFragmentToFaceMakeUpFragment())
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        binding?.btnCancel?.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            safePopBackStack()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}