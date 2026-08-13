package com.aiface.aging.features.imgpicker.ui.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.aiface.aging.features.imgpicker.ui.FragmentImagePicker
import com.aiface.aging.features.imgpicker.ui.FragmentImagePickerFramesAlbum

class ImagePickerFramesPagerAdapter(
    fragmentManager: FragmentManager, lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val fragmentList = arrayListOf<Fragment>()

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> FragmentImagePicker()
            1 -> FragmentImagePickerFramesAlbum()

            else -> FragmentImagePicker()
        }
    }
    fun addFragment(fragment: Fragment) {
        fragmentList.add(fragment)
    }

}