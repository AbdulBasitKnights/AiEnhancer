package com.aiface.aging.features.imgpicker.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.shared.nextNavigateTo
import com.aiface.aging.databinding.FragmentImagePickerEditorAlbumBinding
import com.aiface.aging.features.imgpicker.adapter.AlbumAdapterNew

import com.aiface.aging.features.imgpicker.base.BaseFragment
import com.aiface.aging.features.imgpicker.base.BaseRecyclerViewAdapter
import com.aiface.aging.features.imgpicker.builder.TedImagePicker
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.model.Album
import com.aiface.aging.features.imgpicker.util.GalleryUtil
import com.aiface.aging.features.imgpicker.util.ToastUtil
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import com.aiface.aging.shared.ads.AppOpenManager
import kotlin.getValue


internal class FragmentImagePickerFramesAlbum() : BaseFragment() {

    private var mActivity: FragmentActivity? = null
    private lateinit var binding: FragmentImagePickerEditorAlbumBinding
    private val albumAdapter by lazy { AlbumAdapterNew(builder) }
    private  var disposable: Disposable ? = null

    private var selectedPosition = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initBuilder(requireActivity())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagePickerEditorAlbumBinding.inflate(inflater, container, false)
        binding.imageCountFormat = "%s"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let { activity ->
            getPermission()
        }
    }


    override fun onPermissionsGranted() {
        showImageData()
    }

    private fun showImageData() {
        mActivity?.let { activity ->
            binding.dataView.visibility = View.VISIBLE
            binding.permission.visibility = View.GONE
            binding.imageCountFormat = "%s"
            setupRecyclerView(activity)
            setupButton(activity)
            loadMedia(activity)
            handleBackPress(activity)
        //    loadAds(activity)
        }
    }

//    private fun loadAds(activity: FragmentActivity) {
//        AdManager.loadInterstitial(activity, AdScreen.SELECT_PHOTO_SCREEN_CLICK_SAVE, {
//            //onAdFailed
//
//        }, {
//            //onAdLoaded
//
//        })
//
//    }

    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        binding.permission.visibility = View.VISIBLE
        binding.allow.setOnClickListener { getPermission() }
        lifecycleScope.launch {
            if (EasyPermissions.somePermissionPermanentlyDenied(
                    this@FragmentImagePickerFramesAlbum,
                    deniedPermissions
                )
            ) {
                AppOpenManager.suppressForSettings()
                AppSettingsDialog.Builder(this@FragmentImagePickerFramesAlbum).build().show()
            } else {
                if (EasyPermissions.hasPermissions(requireContext(), *permissions)) {
                    //perform action
                    showImageData()
                } else {
                    EasyPermissions.requestPermissions(
                        this@FragmentImagePickerFramesAlbum,
                        resources.getString(R.string.str_request_permissions),
                        0,
                        *permissions
                    )
                }
            }
        }
    }


    private fun initBuilder(context: FragmentActivity) {
        TedImagePicker.with(context, "editor").initBuilder()
    }

    private fun setupButton(activity: FragmentActivity) {
        with(binding) {
            buttonGravity = builder?.buttonGravity
            buttonText = builder?.buttonText ?: builder?.buttonTextResId?.let { getString(it) }
            buttonTextColor =
                builder?.buttonTextColorResId?.let { ContextCompat.getColor(activity, it) }
            buttonBackground = builder?.buttonBackgroundResId
            buttonDrawableOnly = builder?.buttonDrawableOnly == true
        }
    }


    private fun loadMedia(activity: FragmentActivity, isRefresh: Boolean = false) {
        disposable = builder?.mediaType?.let {
            GalleryUtil.getMedia(activity, it)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { albumList: List<Album> ->
                    albumAdapter.replaceAll(albumList)
                    setSelectedAlbum(selectedPosition)
                    if (!isRefresh) {
                        setSelectedUriList(builder?.selectedUriList)
                    }
                }
        }
    }

    private fun setSelectedUriList(uriList: List<Uri>?) =
        uriList?.forEach { uri: Uri -> //onMultiMediaClick(uri)
        }


    private fun setupRecyclerView(activity: FragmentActivity) {
        setupAlbumRecyclerView()
    }


    private fun setupAlbumRecyclerView() {
        val albumAdapter = albumAdapter.apply {
            onItemClickListener = object : BaseRecyclerViewAdapter.OnItemClickListener<Album> {
                override fun onItemClick(data: Album, itemPosition: Int, layoutPosition: Int) {
                    this@FragmentImagePickerFramesAlbum.setSelectedAlbum(itemPosition)
                    mActivity?.let {
                        if(albumAdapter.getItem(0)?.mediaCount == 0 ) return
                        ToastUtil.showToast(requireContext(), "come to framesalbum line 180")
                       // it.nextNavigateTo(FragmentImagePickerFramesPagerDirections.actionFragmentImagePickerFramesToFragmentImagePickerFrameSingle(selectedPosition, true))
                    }
                }
            }
        }
        binding.rvAlbumDropDown.run {
            adapter = albumAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                }
            })
        }

        binding.rvAlbumDropDown.adapter = albumAdapter

    }




    private fun setSelectedAlbum(selectedPosition: Int) {
        val album = albumAdapter.getItem(selectedPosition)
        if (this.selectedPosition == selectedPosition && binding.selectedAlbum == album) {
            return
        }

        binding.selectedAlbum = album
        this.selectedPosition = selectedPosition
        if (album != null) {
            albumAdapter.setSelectedAlbum(album)
        }
    }



    private fun handleBackPress(activity: FragmentActivity) {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().popBackStack()
            }
        }
        activity.onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onDestroy() {
        try {
            if (disposable?.isDisposed == false) {
                disposable?.dispose()
            }
        } catch (e: Exception) {

        }
        super.onDestroy()
    }


    companion object {

        private  var builder: TedImagePickerBaseBuilder<*>? = null
        internal fun setBuilder(builder1: TedImagePickerBaseBuilder<*>?) {
            builder = builder1 ?: TedImagePickerBaseBuilder()
        }
    }
}

