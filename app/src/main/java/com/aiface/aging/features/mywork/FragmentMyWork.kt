package com.aiface.aging.features.mywork

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentMyworkLayoutBinding
import com.aiface.aging.features.home.HomeFragment
import com.aiface.aging.features.imgpicker.base.BaseFragment
import com.aiface.aging.features.main.MainFragment
import com.aiface.aging.features.share.ExtrasShareImageActivity
import com.aiface.aging.features.share.ShareImageActivity
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.ads.AppOpenManager.Companion.disableAppOpen
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

@AndroidEntryPoint
class FragmentMyWork : BaseFragment(), MyWorkClickListener {

    private val viewModel: MyWorkViewModel by viewModels()
    private var mActivity: FragmentActivity? = null
    private var binding: FragmentMyworkLayoutBinding? = null
    private var photosAdapter: AdapterMyWork? = null

    companion object {
        var isDeleted = MutableLiveData(false)
        private const val PERMISSION_NEEDED_TAG = "permission_needed"
    }

    private val isEmbeddedInMainTab: Boolean
        get() = parentFragment is MainFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMyworkLayoutBinding.inflate(inflater, container, false)
        binding?.viewModel = viewModel
        binding?.lifecycleOwner = viewLifecycleOwner
        binding?.fragment = this
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = mActivity ?: return

        setupHeader()
        setupTabs()
        setupPhotosRecycler(activity)
        observeViewModel()
        observeLibraryTabRequests()
        applyPendingLibraryTabRequest()
        getPermission()

        HomeFragment.requestPermission.observe(viewLifecycleOwner) { granted ->
            if (!granted || !isViewReady()) return@observe
            disableAppOpen = true
            com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                if (isViewReady()) getPermission()
            }
        }

        isDeleted.observe(viewLifecycleOwner) { deleted ->
            if (!deleted || !isViewReady()) return@observe
            isDeleted.value = false
            reloadPhotos(activity)
        }
    }

    override fun onDestroyView() {
        binding = null
        photosAdapter = null
        super.onDestroyView()
    }

    private fun isViewReady(): Boolean = isAdded && view != null && binding != null

    private fun setupHeader() {
        binding?.back?.apply {
            visibility = if (isEmbeddedInMainTab) View.GONE else View.VISIBLE
            setOnClickListener {
                if (!isEmbeddedInMainTab && isAdded) {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    private fun setupTabs() {
        binding?.btnAiPhotos?.setOnClickListener {
            viewModel.selectTab(LibraryTab.AI_PHOTOS)
        }
        binding?.btnAiVideos?.setOnClickListener {
            viewModel.selectTab(LibraryTab.AI_VIDEOS)
        }
        binding?.btnExplore?.setOnClickListener {
            navigateToHomeTab()
        }
    }

    private fun setupPhotosRecycler(activity: FragmentActivity) {
        photosAdapter = AdapterMyWork(emptyList(), this)
        binding?.myWorkRecycler?.apply {
            layoutManager = GridLayoutManager(activity, 2)
            adapter = photosAdapter
            (itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        }
    }

    private fun observeLibraryTabRequests() {
        MainFragment.requestLibraryPhotosTab.observe(viewLifecycleOwner) { open ->
            if (open != true || !isViewReady()) return@observe
            MainFragment.requestLibraryPhotosTab.value = false
            viewModel.selectTab(LibraryTab.AI_PHOTOS)
        }
    }

    private fun applyPendingLibraryTabRequest() {
        if (MainFragment.requestLibraryPhotosTab.value == true) {
            MainFragment.requestLibraryPhotosTab.value = false
            viewModel.selectTab(LibraryTab.AI_PHOTOS)
        }
    }

    private fun observeViewModel() {
        viewModel.selectedTab.observe(viewLifecycleOwner) { tab ->
            if (!isViewReady()) return@observe
            renderSelectedTab(tab ?: LibraryTab.AI_PHOTOS)
        }

        viewModel.myWorkState.observe(viewLifecycleOwner) { images ->
            if (!isViewReady()) return@observe
            photosAdapter?.updateList(images.orEmpty())
            updatePhotosEmptyState(images.orEmpty())
        }
    }

    private fun renderSelectedTab(tab: LibraryTab) {
        val binding = binding ?: return
        val context = context ?: return
        val photosSelected = tab == LibraryTab.AI_PHOTOS
        val permissionNeeded = binding.permission.tag == PERMISSION_NEEDED_TAG

        binding.photosContent.visibility =
            if (photosSelected && !permissionNeeded) View.VISIBLE else View.GONE
        binding.videosContent.visibility = if (photosSelected) View.GONE else View.VISIBLE
        binding.permission.visibility =
            if (photosSelected && permissionNeeded) View.VISIBLE else View.GONE

        binding.btnAiPhotos.apply {
            setBackgroundResource(
                if (photosSelected) R.drawable.bg_segment_selected else R.drawable.bg_segment_unselected,
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (photosSelected) R.color.white else R.color.text_secondary,
                ),
            )
        }
        binding.btnAiVideos.apply {
            setBackgroundResource(
                if (photosSelected) R.drawable.bg_segment_unselected else R.drawable.bg_segment_selected,
            )
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (photosSelected) R.color.text_secondary else R.color.white,
                ),
            )
        }
    }

    private fun updatePhotosEmptyState(images: List<MediaStoreImage>) {
        val binding = binding ?: return
        if (binding.permission.visibility == View.VISIBLE) return

        val hasData = images.isNotEmpty()
        binding.myWorkRecycler.visibility = if (hasData) View.VISIBLE else View.GONE
        binding.photosEmptyState.visibility = if (hasData) View.GONE else View.VISIBLE
    }

    private fun showPhotosContent() {
        if (!isViewReady()) return
        val activity = mActivity ?: return

        binding?.permission?.tag = null
        binding?.permission?.visibility = View.GONE
        renderSelectedTab(viewModel.selectedTab.value ?: LibraryTab.AI_PHOTOS)
        reloadPhotos(activity)
    }

    private fun reloadPhotos(activity: FragmentActivity) {
        if (!isAdded) return
        val folderName = activity.getString(R.string.app_name)
        val myWorkSource = MyWorkImageSource(activity)
        viewModel.loadGalleryImages(folderName, myWorkSource)
    }

    private fun navigateToHomeTab() {
        (parentFragment as? MainFragment)?.openHomeTab()
    }

    private fun openPhotoPreview(mediaStoreImage: MediaStoreImage) {
        if (!isAdded) return
        try {
            val intent = Intent(requireContext(), ShareImageActivity::class.java)
            val extras = ExtrasShareImageActivity().apply {
                id = mediaStoreImage.id
                uri = mediaStoreImage.contentUri
                path = mediaStoreImage.path
                displayName = mediaStoreImage.displayName
                fromMyWork = true
            }
            intent.putExtra(ShareImageActivity::class.java.simpleName, extras)
            startActivity(intent)
        } catch (e: Exception) {
            ToastUtils.showErrorToast(requireContext())
        }
    }

    override fun onImageClick(image: MediaStoreImage) {
        openPhotoPreview(image)
    }

    override fun onPermissionsGranted() {
        if (!isViewReady()) return
        showPhotosContent()
    }

    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        if (!isViewReady()) return

        binding?.permission?.tag = PERMISSION_NEEDED_TAG
        renderSelectedTab(viewModel.selectedTab.value ?: LibraryTab.AI_PHOTOS)
        binding?.allow?.setOnClickListener {
            if (isViewReady()) getPermission()
        }

        if (EasyPermissions.somePermissionPermanentlyDenied(this, deniedPermissions)) {
            AppOpenManager.suppressForSettings()
            AppSettingsDialog.Builder(this).build().show()
        } else if (
            isAdded &&
            !EasyPermissions.hasPermissions(requireContext(), *permissions)
        ) {
            EasyPermissions.requestPermissions(
                this,
                resources.getString(R.string.str_request_permissions),
                PERMISSION_REQUEST_CODE,
                *permissions,
            )
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
