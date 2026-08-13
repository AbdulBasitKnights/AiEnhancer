package com.aiface.aging.features.imgpicker.ui.pager

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.tabs.TabLayoutMediator
import com.aiface.aging.R
import com.aiface.aging.features.imgpicker.ui.FragmentImagePicker
import com.aiface.aging.features.imgpicker.ui.FragmentImagePickerFramesAlbum
import com.aiface.aging.databinding.FragmentImgPickerEditorPagerBinding

class FragmentImagePickerFramesPager : Fragment() {
    private var binding : FragmentImgPickerEditorPagerBinding?=null
    private var mActivity: FragmentActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentImgPickerEditorPagerBinding.inflate(inflater,container,false)
        binding?.lifecycleOwner=this
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity->

            val adapter = ImagePickerFramesPagerAdapter(childFragmentManager, lifecycle)
            adapter.addFragment(FragmentImagePicker())
            adapter.addFragment(FragmentImagePickerFramesAlbum())
            binding?.viewPager?.adapter = adapter
            binding?.viewPager?.isUserInputEnabled = true
            binding?.tabLayout?.selectTab(binding?.tabLayout?.getTabAt(0))
            binding?.tabLayout?.tabRippleColor = null
            binding?.tabLayout?.let {
                binding?.viewPager?.let { it1 ->
                    TabLayoutMediator(it, it1) { tab, position ->
                        when (position) {
                            0 -> tab.text = getString(R.string.all_photos)
                            1 -> tab.text = getString(R.string.albums)
                        }
                    }.attach()
                }
            }

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