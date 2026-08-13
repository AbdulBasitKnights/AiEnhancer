package com.aiface.aging.features.cropper

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.databinding.FragmentCropperBinding
import com.aiface.aging.shared.editorui.ModelRatio
import com.aiface.aging.shared.editorui.AdapterRatio
import com.aiface.aging.shared.editorui.RatioListener
import com.aiface.aging.shared.safePopSupportBackStack

import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

interface CropperCallback {
    fun onCropperRatioClick(position: Int, modelRatio: ModelRatio)
    fun onDoneCropping()
    fun onCancelCropping()
}

@AndroidEntryPoint
class FragmentCropper : Fragment(), RatioListener {

    companion object {
        private var cropperCallback: CropperCallback? = null
        fun newInstance(
            cropperCallback: CropperCallback
        ): FragmentCropper {
            val fragment = FragmentCropper()
            Companion.cropperCallback = cropperCallback
            return fragment
        }
    }


    private var mActivity: FragmentActivity? = null
    private var adapterRatio: AdapterRatio? = null
    private val viewModel: ViewModelCropper by viewModels()
    private lateinit var binding: FragmentCropperBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCropperBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            setUpRecyclerView(activity)
            onclickListeners()
            binding.headingText.text = "Custom"
            adapterRatio?.selectBottomItem(0)
        }
    }

    private fun onclickListeners() {
        binding.tick.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                cropperCallback?.onDoneCropping()
                if (!isHostManagedPanel()) {
                    mActivity?.safePopSupportBackStack()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        binding.cross.setOnClickListener {
            if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return@setOnClickListener
            try {
                cropperCallback?.onCancelCropping()
                if (!isHostManagedPanel()) {
                    mActivity?.safePopSupportBackStack()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun isHostManagedPanel(): Boolean {
        val host = mActivity ?: return false
        return host is com.aiface.aging.features.editor.EditorActivity ||
            host is com.aiface.aging.features.bgremover.BGRemoverActivity
    }

    private fun setUpRecyclerView(activity: FragmentActivity) {
        adapterRatio = AdapterRatio(this, activity)
        binding.ratioRecyclerView.adapter = adapterRatio
        adapterRatio?.let { subscribeUI(it) }
    }

    private fun subscribeUI(adapterRatio: AdapterRatio) {
        lifecycleScope.launchWhenStarted {
            viewModel.loadRatioIcons().collectLatest {
                if (it.isNotEmpty()) {
                    adapterRatio.submitList(it)
                }
            }
        }
    }

    override fun onRatioClick(position: Int, modelRatio: ModelRatio) {
        adapterRatio?.selectBottomItem(position)
        binding.headingText.text = modelRatio.imageTitle
        mActivity?.let {
            cropperCallback?.onCropperRatioClick(position, modelRatio)
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
}