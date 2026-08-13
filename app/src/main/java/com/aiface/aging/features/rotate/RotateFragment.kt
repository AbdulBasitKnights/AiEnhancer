package com.aiface.aging.features.rotate

import android.content.Context
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aiface.aging.databinding.FragmentRotateBinding
import com.aiface.aging.features.adjustment.AdapterRecyclerSecondary
import com.aiface.aging.features.adjustment.SecondaryRecyclerListener
import com.aiface.aging.shared.editorui.BottomActionListener
import com.aiface.aging.shared.editorui.ModelDrawableAssets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


interface RotateListener {
    fun onRotateImage(matrix: Matrix, filter: Boolean)
}

@AndroidEntryPoint
class RotateFragment : Fragment(), SecondaryRecyclerListener {

    companion object {
        private var rotateListener: RotateListener? = null
        private var actionListener: BottomActionListener? = null
        fun newInstance(
            rotateListener: RotateListener,actionListener : BottomActionListener?=null
        ): RotateFragment {
            val fragment = RotateFragment()
            Companion.rotateListener = rotateListener
            Companion.actionListener = actionListener
            return fragment
        }
    }

    private lateinit var binding: FragmentRotateBinding
    private var mActivity: FragmentActivity? = null
    private var adapterRecyclerSecondary: AdapterRecyclerSecondary? = null
    private val viewModel: ViewModelRotate by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRotateBinding.inflate(layoutInflater)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.tick.setOnClickListener {
            actionListener?.onActionTickClick("rotate", null)
        }
        binding.cross.setOnClickListener {
            actionListener?.onActionCancelClick("rotate", null)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            setUpRecyclerview(activity)
        }
    }

    private fun setUpRecyclerview(activity: FragmentActivity) {
        adapterRecyclerSecondary = AdapterRecyclerSecondary(this, activity)
        binding.rotateRecyclerView.adapter = adapterRecyclerSecondary
        adapterRecyclerSecondary?.let { subscribeUi(it) }
        adapterRecyclerSecondary?.selectBottomItem(-1)
    }

    private fun subscribeUi(adapter: AdapterRecyclerSecondary) {
        lifecycleScope.launch {
            mActivity?.let {
                viewModel.loadRotateIcons(it).collectLatest { icons ->
                    if (icons.isNotEmpty()) {
                        adapter.submitList(icons)
                    }
                }
            }
        }
    }

    override fun onSecondaryRecyclerClick(position: Int, modelDrawableAssets: ModelDrawableAssets) {
        adapterRecyclerSecondary?.selectBottomItem(position)
        when (position) {
            0 -> {
                rotateBitmap(-90.0f)
            }

            1 -> {
                rotateBitmap(90.0f)
            }

            2 -> {
                flipHorizontalBitmap()
            }
            3 -> {
                flipVerticalBitmap()
            }
        }
    }


    private fun rotateBitmap(angle: Float) {
        val matrix = Matrix()
        matrix.postRotate(angle)
        rotateListener?.onRotateImage(matrix, true)
    }

    private fun flipVerticalBitmap() {
        val matrix = Matrix()
        matrix.preScale(1f, -1f)
        rotateListener?.onRotateImage(matrix, false)
    }

    private fun flipHorizontalBitmap() {
        val matrix = Matrix()
        matrix.preScale(-1f, 1f)
        rotateListener?.onRotateImage(matrix, false)
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