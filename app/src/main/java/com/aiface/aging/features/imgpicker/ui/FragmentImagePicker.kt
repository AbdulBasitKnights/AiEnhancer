package com.aiface.aging.features.imgpicker.ui

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.gun0912.tedonactivityresult.model.ActivityResult
import com.tedpark.tedonactivityresult.rx2.TedRxOnActivityResult
import com.aiface.aging.BuildConfig
import com.aiface.aging.ads_nextgen.AdShimmerHelper
import com.aiface.aging.ads_nextgen.NativeAdDisplayHelper
import com.aiface.aging.ads_nextgen.NextGenNativeLoader
import com.aiface.aging.AiFaceApp
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdError
import com.aiface.aging.shared.ads.interstitialTrackedUnitId
import com.aiface.aging.shared.ads.AdsHelper
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.ads.FullScreenContentCallback
import com.aiface.aging.shared.ads.adjustRevenueMMP
import com.aiface.aging.shared.ads.canPresentHomeInterstitial
import com.aiface.aging.shared.ads.interstitialHome
import com.aiface.aging.shared.ads.isShowingAd
import com.aiface.aging.shared.ads.showFullscreenAd
import com.aiface.aging.shared.ads.loadInterHome
import com.aiface.aging.shared.ads.loadInterHomeHigh
import com.aiface.aging.shared.applySystemBarInsets
import com.aiface.aging.shared.editorName
import com.aiface.aging.shared.safeFinish
import com.aiface.aging.shared.safePopBackStack
import com.aiface.aging.features.home.TemplateImageRequirements
import com.aiface.aging.features.result.ResultFeatureNavigator
import com.aiface.aging.databinding.FragmentImagePickerBgremoverBinding
import com.aiface.aging.features.bgremover.FragmentAIBGRemover
import com.aiface.aging.features.body.activities.ImageEditingER
import com.aiface.aging.features.collage.FragmentCollageTemplates
import com.aiface.aging.features.collage.dynamic.PuzzleCollageViewActivity
import com.aiface.aging.features.collage.model.TemplateItem
import com.aiface.aging.features.collage.static_.GridPhotoActivity
import com.aiface.aging.features.editor.EditorActivity
import com.aiface.aging.features.home.HomeFragment.Companion.requestPermission
import com.aiface.aging.features.look.LookConstants
import com.aiface.aging.features.look.LookFeatureActivity
import com.aiface.aging.features.imgpicker.adapter.AlbumAdapter
import com.aiface.aging.features.imgpicker.adapter.GridSpacingItemDecoration
import com.aiface.aging.features.imgpicker.adapter.MediaAdapterNew
import com.aiface.aging.features.imgpicker.adapter.MediaAdapterNoAd
import com.aiface.aging.features.imgpicker.adapter.SelectedMediaAdapter
import com.aiface.aging.features.imgpicker.base.BaseFragment
import com.aiface.aging.features.imgpicker.base.BaseRecyclerViewAdapter
import com.aiface.aging.features.imgpicker.builder.TedImagePickerBaseBuilder
import com.aiface.aging.features.imgpicker.builder.type.CameraMedia
import com.aiface.aging.features.imgpicker.builder.type.MediaType
import com.aiface.aging.features.imgpicker.builder.type.SelectType
import com.aiface.aging.features.imgpicker.extenstion.setLock
import com.aiface.aging.features.imgpicker.model.Album
import com.aiface.aging.features.imgpicker.model.Media
import com.aiface.aging.features.imgpicker.util.Extras
import com.aiface.aging.features.imgpicker.util.GalleryUtil
import com.aiface.aging.features.imgpicker.util.MediaUtil
import com.aiface.aging.features.imgpicker.util.ToastUtil
import com.aiface.aging.utils.AppUtils
import com.aiface.aging.utils.GlobalLoader
import com.aiface.aging.utils.LogUtils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import java.util.*
import kotlin.getValue

class FragmentImagePicker : BaseFragment() {

    private var mActivity: FragmentActivity? = null
    private lateinit var binding: FragmentImagePickerBgremoverBinding
    private val albumAdapter by lazy { AlbumAdapter(builder) }
    private lateinit var mediaAdapter: MediaAdapterNoAd
    private lateinit var selectedMediaAdapter: SelectedMediaAdapter
    private lateinit var disposable: Disposable

    private var selectedPosition = 0

    private var isNativeReloaded = false

    private var nativeHome: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments.let {
            val args = it?.let { it1 -> FragmentImagePickerArgs.fromBundle(it1) }
            selectedPosition = args?.selectedPosition ?: 0
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity?.let {

          //  AppUtils.getMain(it).reloadBanner()
        }
        mActivity = null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentImagePickerBgremoverBinding.inflate(inflater, container, false)
        binding.imageCountFormat = "%s"
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applySystemBarInsets(applyTop = true, applyBottom = true)
        mActivity?.let { activity ->
            AppUtils.hideHomeBannerAd(activity)

            AppOpenManager.disableAppOpen = true
            getPermission()

            loadAds(activity)
            loadInterAds(activity)

            binding.layoutSelectedAlbumDropDown.back.setOnClickListener {
              //  parentFragmentManager.popBackStack()
                findNavController().popBackStack()
            }





            binding.camera.setOnClickListener {
                onCameraTileClick(activity)
            }
        }

    }

    override fun onPermissionsGranted() {
        showImageData()
//        dataStoreManager.readDataStoreValue(Constants.SET_DEFAULT_DIALOG_SHOWN, false){
//            if (!this){
//                if (GlobalValues.isFromShortcut){
//                    GlobalValues.isFromShortcut = false
//                }else{
//                    if (!showDefaultDialog){
//                        showDefaultDialog = true
//                        launchDefaultIntentPrompt()
//                    }
//
//                }
//
//            }
//        }
    }
    private fun showImageData() {
        mActivity?.let { activity ->
            try {
                binding.dataView.visibility = View.VISIBLE
                binding.permission.visibility = View.GONE
                binding.imageCountFormat = "%s"
                setupRecyclerView(activity)
                setupListener(activity)
                setupSelectedMediaView()
                setupButton(activity)
                loadMedia(activity)
                handleBackPress(activity)
              //  loadAds(activity)
            } catch (e: Exception) {

            }
        }
    }
//


    override fun onPermissionsDenied(deniedPermissions: List<String>) {
        binding.permission.visibility = View.VISIBLE
        binding.allow.setOnClickListener { getPermission() }
        lifecycleScope.launch {
            if (EasyPermissions.somePermissionPermanentlyDenied(
                    this@FragmentImagePicker,
                    deniedPermissions
                )
            ) {
                AppOpenManager.suppressForSettings()
                AppSettingsDialog.Builder(this@FragmentImagePicker).build().show()
            } else {
                if (EasyPermissions.hasPermissions(requireContext(), *permissions)) {
                    //perform action
                    showImageData()
                } else {
                    EasyPermissions.requestPermissions(
                        this@FragmentImagePicker,
                        resources.getString(R.string.str_request_permissions),
                        0,
                        *permissions
                    )
                }
            }
        }
    }

    private fun setupButton(activity: FragmentActivity) {
        with(binding) {
            buttonGravity = builder.buttonGravity
            buttonText = builder.buttonText ?: getString(builder.buttonTextResId)
            buttonTextColor =
                ContextCompat.getColor(activity, builder.buttonTextColorResId)
            buttonBackground = builder.buttonBackgroundResId
            buttonDrawableOnly = builder.buttonDrawableOnly
        }

        setupButtonVisibility()
    }

    private fun setupButtonVisibility() {
        binding.showButton = when {
            builder.selectType == SelectType.SINGLE -> false
            else -> mediaAdapter.selectedUriList.isNotEmpty()
        }
    }


    private fun loadMedia(activity: FragmentActivity, isRefresh: Boolean = false) {
        disposable = GalleryUtil.getMedia(activity, builder.mediaType)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { albumList: List<Album> ->
                albumAdapter.replaceAll(albumList)
                setSelectedAlbum(selectedPosition)
                if (!isRefresh) {
                    setSelectedUriList(builder.selectedUriList)
                }
                binding.layoutContent.rvMedia.visibility = View.VISIBLE
            }
    }

    private fun setSelectedUriList(uriList: List<Uri>?) =
        uriList?.forEach { uri: Uri -> onMultiMediaClick(uri) }


    private fun setupRecyclerView(activity: FragmentActivity) {
        setupAlbumRecyclerView()
        setupMediaRecyclerView(activity)
        setupSelectedMediaRecyclerView(activity)
    }


    private fun setupAlbumRecyclerView() {
        val albumAdapter = albumAdapter.apply {
            onItemClickListener = object : BaseRecyclerViewAdapter.OnItemClickListener<Album> {
                override fun onItemClick(data: Album, itemPosition: Int, layoutPosition: Int) {
                    this@FragmentImagePicker.setSelectedAlbum(itemPosition)
                    binding.drawerLayout.close()
                    binding.isAlbumOpened = false
                }
            }
        }
        binding.rvAlbum.run {
            adapter = albumAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    binding.drawerLayout.setLock(newState == RecyclerView.SCROLL_STATE_DRAGGING)
                }
            })
        }

        binding.rvAlbumDropDown.adapter = albumAdapter

    }

    private fun setupMediaRecyclerView(activity: FragmentActivity) {
        mediaAdapter = MediaAdapterNoAd(activity, builder).apply {
            onItemClickListener = object : MediaAdapterNoAd.OnItemClickListener<Media> {
                override fun onItemClick(data: Media, itemPosition: Int, layoutPosition: Int) {
                    this@FragmentImagePicker.onMediaClick(data.uri)
                    if (!isNativeReloaded){
                        isNativeReloaded = true

                    }
                }

                override fun onHeaderClick() {
                    onCameraTileClick(activity)
                }
            }

            onMediaAddListener = {
                binding.layoutContent.rvSelectedMedia.smoothScrollToPosition(selectedMediaAdapter.itemCount)
            }

        }
        // Set up GridLayoutManager with custom span size for ad items
        val gridLayoutManager = GridLayoutManager(activity, IMAGE_SPAN_COUNT)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                // For ad items, span the full width (IMAGE_SPAN_COUNT), for media items, span count is 1
                return if (mediaAdapter.getItemViewType(position) == MediaAdapterNew.ViewType.AD.ordinal) {
                    IMAGE_SPAN_COUNT
                } else {
                    1
                }
            }
        }
        binding.layoutContent.rvMedia.run {
            layoutManager = gridLayoutManager
            addItemDecoration(GridSpacingItemDecoration(IMAGE_SPAN_COUNT, 8))
            itemAnimator = null
            adapter = mediaAdapter
        }

//        binding.layoutContent.fastScroller.recyclerView = binding.layoutContent.rvMedia
        binding.layoutContent.rvMedia.setHasFixedSize(true)

    }

    private fun setupSelectedMediaRecyclerView(activity: FragmentActivity) {
        binding.layoutContent.selectType = builder.selectType

        selectedMediaAdapter = SelectedMediaAdapter().apply {
            onClearClickListener = { uri ->
                onMultiMediaClick(uri)
            }
        }
        binding.layoutContent.rvSelectedMedia.run {
            layoutManager = LinearLayoutManager(
                activity,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = selectedMediaAdapter

        }

    }

    @SuppressLint("CheckResult")
    private fun onCameraTileClick(activity: FragmentActivity) {
        val cameraMedia = when (builder.mediaType) {
            MediaType.IMAGE -> CameraMedia.IMAGE
            MediaType.VIDEO -> CameraMedia.VIDEO
            MediaType.IMAGE_AND_VIDEO -> CameraMedia.IMAGE
        }
        val (cameraIntent, uri) = MediaUtil.getMediaIntentUri(
            activity,
            cameraMedia,
            builder.savedDirectoryName
        )
        TedRxOnActivityResult.with(activity)
            .startActivityForResult(cameraIntent)
            .subscribe { activityResult: ActivityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    MediaUtil.scanMedia(activity, uri)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            loadMedia(activity)
                            onMediaClick(uri)
                        }
                }
            }
    }


    private fun onMediaClick(uri: Uri) {
        when (builder.selectType) {
            SelectType.SINGLE -> onSingleMediaClick(uri)
            SelectType.MULTI -> onMultiMediaClick(uri)
        }
    }

    private fun onMultiMediaClick(uri: Uri) {
        mediaAdapter.toggleMediaSelect(uri)
        binding.layoutContent.items = mediaAdapter.selectedUriList
        updateSelectedMediaView()
        setupButtonVisibility()
    }

    private fun setupSelectedMediaView() {
        binding.layoutContent.viewSelectedMedia.run {
            if (mediaAdapter.selectedUriList.size > 0) {
                layoutParams.height =
                    resources.getDimensionPixelSize(R.dimen.ted_image_picker_selected_view_height)
            } else {
                layoutParams.height = 0
            }
            requestLayout()
        }
    }

    private fun updateSelectedMediaView() {
        binding.layoutContent.viewSelectedMedia.post {
            binding.layoutContent.viewSelectedMedia.run {
                if (mediaAdapter.selectedUriList.size > 0) {
                    slideView(
                        this,
                        layoutParams.height,
                        resources.getDimensionPixelSize(R.dimen.ted_image_picker_selected_view_height)
                    )
                } else if (mediaAdapter.selectedUriList.size == 0) {
                    slideView(this, layoutParams.height, 0)
                }
            }
        }
    }

    private fun slideView(view: View, currentHeight: Int, newHeight: Int) {
        val valueAnimator = ValueAnimator.ofInt(currentHeight, newHeight).apply {
            addUpdateListener {
                view.layoutParams.height = it.animatedValue as Int
                view.requestLayout()
            }
        }

        AnimatorSet().apply {
            interpolator = AccelerateDecelerateInterpolator()
            play(valueAnimator)
        }.start()
    }

    private fun onSingleMediaClick(uri: Uri) {
        //navigate to next

    }

    private fun onMultiMediaDone(activity: FragmentActivity) {
        val selectedUriList = mediaAdapter.selectedUriList
        if (selectedUriList.size < builder.minCount) {
            val message = builder.minCountMessage ?: getString(builder.minCountMessageResId)
            ToastUtil.showToast(activity, message)
        } else {
            when(editorName){

                "ai"->{
                    val required = TemplateImageRequirements.requiredCount(
                        builder.bundleExtras?.getInt(TemplateImageRequirements.ARG_IMAGE_COUNT, 1),
                    )
                    if (selectedUriList.size < required) {
                        ToastUtil.showToast(
                            activity,
                            "select at least $required image${if (required == 1) "" else "s"}",
                        )
                        return
                    }
                    val bundle = builder.bundleExtras
                    bundle?.putString(
                        TemplateImageRequirements.ARG_IMAGE_URI,
                        selectedUriList[0].toString(),
                    )
                    if (required >= 2 && selectedUriList.size >= 2) {
                        bundle?.putString(
                            TemplateImageRequirements.ARG_IMAGE_URI_TWO,
                            selectedUriList[1].toString(),
                        )
                    } else {
                        bundle?.remove(TemplateImageRequirements.ARG_IMAGE_URI_TWO)
                    }
                    bundle?.putInt(TemplateImageRequirements.ARG_IMAGE_COUNT, required)
                    showInterAd(activity)

                }
                "hair"->{
                    val url = selectedUriList[0].toString()
                    val featureType = builder.bundleExtras?.getString(LookConstants.EXTRA_FEATURE_TYPE)
                        ?: LookConstants.SCREEN_HAIR_COLOR
                    navigateToLookCrop(activity, url, featureType)
                }
                "makeup"->{
                    val url = selectedUriList[0].toString()
                    val featureType = builder.bundleExtras?.getString(LookConstants.EXTRA_FEATURE_TYPE)
                        ?: LookConstants.SCREEN_MAKEUP
                    navigateToLookCrop(activity, url, featureType)
                }

                "photoEdit"->{

                    val imagePaths = ArrayList<String>()
                    for (item in selectedUriList) {
                        AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
                    }
                    val intent = Intent(activity, EditorActivity::class.java)
                    intent.putExtra(Extras.PICKER_IMG_LIST, selectedUriList[0].toString())
                    if (ResultFeatureNavigator.isLaunchedFromResult(activity.intent)) {
                        intent.putExtra(
                            ResultFeatureNavigator.EXTRA_LAUNCHED_FROM_RESULT,
                            true,
                        )
                    }
                    startActivity(intent)

                }
                "bgRemover"->{
                    val imagePaths = ArrayList<String>()
                    for (item in selectedUriList) {
                        AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
                    }

                    FragmentAIBGRemover.userImgPath = selectedUriList[0].toString()
                    FragmentAIBGRemover.openMagicEraser = false
                   findNavController().navigate(R.id.action_to_bgRemover)
                }
                "magicEraser" -> {
                    FragmentAIBGRemover.userImgPath = selectedUriList[0].toString()
                    FragmentAIBGRemover.openMagicEraser = true
                    findNavController().navigate(R.id.action_to_bgRemover)
                }
                "blender" -> {
                    builder?.let { pickerBuilder ->
                        val imagePaths = ArrayList<String>()
                        for (item in selectedUriList) {
                            AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
                                ?: imagePaths.add(item.toString())
                        }
                        if (imagePaths.isNotEmpty()) {
                            val bundle = Bundle().apply {
                                pickerBuilder.bundleExtras?.let { putAll(it) }
                                putStringArrayList(Extras.PICKER_IMG_LIST, imagePaths)
                            }
                            findNavController().navigate(
                                R.id.action_global_myCropFragment,
                                bundle,
                            )
                        } else {
                            ToastUtil.showToast(activity, "Unable to pick image")
                        }
                    }
                }
                "frames" -> {
                    builder?.let { pickerBuilder ->
                        val imagePaths = ArrayList<String>()
                        for (item in selectedUriList) {
                            AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
                                ?: imagePaths.add(item.toString())
                        }
                        if (imagePaths.isNotEmpty()) {
                            val intent = pickerBuilder.destinationIntent
                                ?: Intent(activity, com.aiface.aging.features.frames.editor.AllFramesEditorActivity::class.java)
                            pickerBuilder.bundleExtras?.let { intent.putExtras(it) }
                            intent.putStringArrayListExtra(Extras.PICKER_IMG_LIST, imagePaths)
                            startActivity(intent)
                        } else {
                            ToastUtil.showToast(activity, "Unable to pick image")
                        }
                    }
                }
                "faceswap" -> {
                    builder?.let { pickerBuilder ->
                        val imagePaths = ArrayList<String>()
                        for (item in selectedUriList) {
                            imagePaths.add(item.toString())
                        }
                        if (imagePaths.isNotEmpty()) {
                            val bundle = Bundle().apply {
                                pickerBuilder.bundleExtras?.let { putAll(it) }
                                putStringArrayList("imageUris", imagePaths)
                                putString("imageUri", imagePaths.first())
                            }
                            findNavController().navigate(
                                R.id.action_to_faceSwapGenerate,
                                bundle,
                            )
                        } else {
                            ToastUtil.showToast(activity, "Unable to pick image")
                        }
                    }
                }
                "bodyMaker"->{
                   // ToastUtil.showToast(activity,"ip line 442")

                    val imagePaths = ArrayList<String>()
                    for (item in selectedUriList) {
                        imagePaths.add(item.toString())
                        // AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
                    }
                    if (!imagePaths.isNullOrEmpty() && imagePaths.size != 0){
                        val imgPath = imagePaths[0]
                        val inte = Intent(activity, ImageEditingER::class.java)
                        inte.putExtra("selected_path", imgPath)
                        inte.putExtra("camera", false)
                        startActivity(inte)
                    }


                }
                "collage"->{
                    builder?.let { builder ->
                        val selectedUriList = mediaAdapter.selectedUriList
                        if (selectedUriList.size < builder.minCount) {
                            val message = builder.minCountMessage ?: getString(builder.minCountMessageResId)
                            ToastUtil.showToast(activity, message)
                        } else {
                            val isDynamic = builder.bundleExtras?.getBoolean("isDynamic", false) ?: true
                            // saveUserLastAction(activity, "collage")
                            if (isDynamic) {
                                //navigate to next
                                val type = builder.bundleExtras?.getInt("type")
                                val themeId = builder.bundleExtras?.getInt("theme_id")
                                val pieceSize = builder.bundleExtras?.getInt("piece_size")
                                val count=builder.bundleExtras?.getInt("count")

                                val imagePaths = ArrayList<String>()
                                for (item in selectedUriList) {
                                    AppUtils.getFilePathFromContentUri(item, activity)
                                        ?.let { imagePaths.add(it) }
                                }
                                if (!imagePaths.isNullOrEmpty() && imagePaths.size != 0) {
                                    prepareDynamicTemplate(imagePaths, type, pieceSize, themeId,count)
                                } else {
                                    ToastUtil.showToast(activity, "Unable to pick image")
                                }

                            } else {
                                //navigate to next
                                val mImageInTemplateCount =
                                    builder.bundleExtras?.getInt("mImageInTemplateCount", 0)
                                val mIsFrameImage =
                                    builder.bundleExtras?.getBoolean("mFrameImages", true)
                                val selectedItemIndex =
                                    builder.bundleExtras?.getInt("mSelectedTemplateIndex", 0)
                                val mTemplateItemList =
                                    builder.bundleExtras?.getParcelableArrayList<TemplateItem>("mTemplateItemList")

                                val imagePaths = ArrayList<String>()
                                for (item in selectedUriList) {
                                    AppUtils.getFilePathFromContentUri(item, activity)
                                        ?.let { imagePaths.add(it) }
                                }
                                if (!imagePaths.isNullOrEmpty() && imagePaths.size != 0) {

                                    prepareStaticTemplate(
                                        imagePaths,
                                        mTemplateItemList!!,
                                        selectedItemIndex!!,
                                        mIsFrameImage!!,
                                        mImageInTemplateCount!!,
                                        activity
                                    )
                                } else {
                                    ToastUtil.showToast(activity, "Unable to pick image")
                                }
                            }
                        }
                    }
                }
            }


/*            val imagePaths = ArrayList<String>()
            for (item in selectedUriList) {
               // AppUtils.getFilePathFromContentUri(item, activity)?.let { imagePaths.add(it) }
            }
            if (!imagePaths.isNullOrEmpty() && imagePaths.size != 0) {
//                val intent = Intent(activity, AllFramesEditorActivity::class.java)
//                intent.putExtra(Extras.PICKER_IMG_LIST, imagePaths)
//                intent.putExtra(Extras.MODEL_FRAME_PACK, modelFramePack)
//                if (!GlobalValues.isProVersion && MyApplication.isInterGallery){
//                    showInterstitialAd(activity, intent)
//                }else{
//                    startActivity(intent)
//                }


            } else {
                ToastUtil.showToast(activity, "Unable to pick image")
            }*/
        }
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
            mediaAdapter.replaceAll(album.mediaUris)
        }
        if (mediaAdapter.itemCount == 0){
           // GlobalValues.reloadBanner = false
        //    AppUtils.getMain(activity).hideBanner()
            binding.camera.visibility = View.VISIBLE
        }
        else{
            binding.camera.visibility = View.GONE
        }
        binding.layoutContent.rvMedia.layoutManager?.scrollToPosition(0)
    }

    private fun setupListener(activity: FragmentActivity) {
        binding.viewDoneTop.root.setOnClickListener {
            onMultiMediaDone(activity)
        }

        binding.viewSelectedAlbumDropDown.setOnClickListener {
            binding.isAlbumOpened = !binding.isAlbumOpened
        }

    }


    private fun handleBackPress(activity: FragmentActivity) {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (!com.aiface.aging.shared.BackPressGuard.tryHandle()) return
                try {
                    val popped = safePopBackStack()
                    if (!popped &&
                        ResultFeatureNavigator.shouldFinishMainToRevealShareHost(activity)
                    ) {
                        activity.safeFinish()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        activity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)
    }


    override fun onDestroy() {
        nativeHome?.destroy()
        nativeHome = null
        try {
            if (!disposable.isDisposed) {
                disposable.dispose()
            }
        } catch (e: Exception) {

        }
        super.onDestroy()
    }


    private fun prepareDynamicTemplate(
        imageList: ArrayList<String>,
        type: Int?,
        pieceSize: Int?,
        themeId: Int?,count : Int?
    ) {
        val intent = Intent(requireContext(), PuzzleCollageViewActivity::class.java)
        intent.putExtra("type", type)
        intent.putExtra("piece_size", pieceSize)
        intent.putExtra("theme_id", themeId)
        intent.putExtra("count", count)
        intent.putStringArrayListExtra("imagePath", imageList)
        mActivity?.let {
            startActivity(intent)
        }
    }



    private fun prepareStaticTemplate(
        imageList: ArrayList<String>,
        mTemplateItemList: ArrayList<TemplateItem>,
        mSelectedTemplateIndex: Int,
        mFrameImages: Boolean,
        mImageInTemplateCount: Int,
        activity: FragmentActivity
    ) {
        try {
            val selectedTemplateItem = mTemplateItemList[mSelectedTemplateIndex]
            val itemSize = selectedTemplateItem.photoItemList.size
            val size = Math.min(itemSize, imageList.size)
            for (idx in 0 until size) {
                selectedTemplateItem.photoItemList[idx].imagePath = imageList[idx]
            }
            val intent = Intent(activity, GridPhotoActivity::class.java)
            intent.putExtra(
                FragmentCollageTemplates.EXTRA_IMAGE_IN_TEMPLATE_COUNT,
                selectedTemplateItem.photoItemList.size
            )
            intent.putExtra(FragmentCollageTemplates.EXTRA_IS_FRAME_IMAGE, mFrameImages)
            if (mImageInTemplateCount == 0) {
                val tmp = ArrayList<TemplateItem>()
                for (item in mTemplateItemList) if (item.photoItemList.size == selectedTemplateItem.photoItemList.size) {
                    tmp.add(item)
                }
                intent.putExtra(
                    FragmentCollageTemplates.EXTRA_SELECTED_TEMPLATE_INDEX,
                    tmp.indexOf(selectedTemplateItem)
                )
            } else {
                intent.putExtra(
                    FragmentCollageTemplates.EXTRA_SELECTED_TEMPLATE_INDEX,
                    mSelectedTemplateIndex
                )
            }
            val imagePaths = ArrayList<String>()
            for (item in selectedTemplateItem.photoItemList) {
                if (item.imagePath == null) item.imagePath = ""
                imagePaths.add(item.imagePath)
            }
            intent.putExtra(FragmentCollageTemplates.EXTRA_IMAGE_PATHS, imagePaths)
            mActivity?.let {
                startActivity(intent)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        private const val IMAGE_SPAN_COUNT = 3
        private lateinit var builder: TedImagePickerBaseBuilder<*>
        internal fun setBuilder(builder1: TedImagePickerBaseBuilder<*>?) {
            builder = builder1 ?: TedImagePickerBaseBuilder()
        }
    }


    private fun loadInterAds(activity: FragmentActivity){
        if (AiFaceApp.isInterPickerHf && AiFaceApp.isInterPicker) {
            loadInterHomeHigh(activity) { onLoaded ->
                if (onLoaded) {

                } else {
                    loadInterHome(activity) { onLoaded ->
                        if (onLoaded) {

                        } else {

                        }
                    }
                }
            }

        } else if (AiFaceApp.isInterPicker) {
            loadInterHome(activity) { onLoaded ->
                if (onLoaded) {

                } else {

                }
            }
        }
    }

    private fun loadAds(activity: FragmentActivity) {
        if (AiFaceApp.isNativeImgPickerHf && AiFaceApp.isNativeImgPicker) {
            startNative(tryHigh = true)
        } else if (AiFaceApp.isNativeImgPicker) {
            startNative(tryHigh = false)
        } else {
            binding?.clAd?.visibility = View.GONE
        }
    }

    private fun startNative(tryHigh: Boolean) {
        try {
            if (!AdsHelper.shouldShowAds()) {
                binding?.clAd?.visibility = View.GONE
                AdShimmerHelper.hideNativeAdSlot(
                    adSlot = binding?.clAd,
                    shimmerWrapper = binding?.shimmer,
                    nativeContainer = binding?.nativeAdView,
                )
                return
            }
            AdShimmerHelper.showLayoutNativePlaceholder(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
            NextGenNativeLoader.loadWithFallback(
                tryHigh = tryHigh,
                highUnitId = BuildConfig.native_share_hf,
                normalUnitId = BuildConfig.native_share,
                onLoaded = { ad, unitId ->
                    try {
                        if (!isAdded || view == null || binding == null) {
                            ad.destroy()
                            return@loadWithFallback
                        }
                        nativeHome?.destroy()
                        nativeHome = ad
                        val container = binding?.nativeAdView
                        if (container == null) {
                            ad.destroy()
                            AdShimmerHelper.hideNativeAdSlot(
                                adSlot = binding?.clAd,
                                shimmerWrapper = binding?.shimmer,
                            )
                            return@loadWithFallback
                        }
                        binding?.clAd?.visibility = View.VISIBLE
                        NativeAdDisplayHelper.display(
                            container = container,
                            inflater = layoutInflater,
                            nativeAd = ad,
                            onDestroyPrevious = {},
                            adUnitId = unitId,
                            layoutResId = R.layout.layout_native_banner_ads,
                            shimmer = binding?.shimmer
                        )
                        binding?.shimmerContainerNative?.root?.visibility = View.GONE
                    } catch (t: Throwable) {
                        try {
                            ad.destroy()
                        } catch (_: Throwable) {
                        }
                        AdShimmerHelper.hideNativeAdSlot(
                            adSlot = binding?.clAd,
                            shimmerWrapper = binding?.shimmer,
                            nativeContainer = binding?.nativeAdView,
                        )
                    }
                },
                onFailed = {
                    AdShimmerHelper.hideNativeAdSlot(
                        adSlot = binding?.clAd,
                        shimmerWrapper = binding?.shimmer,
                        nativeContainer = binding?.nativeAdView,
                    )
                }
            )
        } catch (t: Throwable) {
            AdShimmerHelper.hideNativeAdSlot(
                adSlot = binding?.clAd,
                shimmerWrapper = binding?.shimmer,
                nativeContainer = binding?.nativeAdView,
            )
        }
    }

    fun showInterAd(
        currentActivity: FragmentActivity
    ) {
        currentActivity.lifecycleScope.launch {
            try {
                if (isProVersion.value == false && canPresentHomeInterstitial()) {
                    GlobalLoader.show(currentActivity)
                    delay(1000)
                    if (canPresentHomeInterstitial()) {
                        interstitialHome?.showFullscreenAd(
                            activity = currentActivity,
                            contentCallback = object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    requestPermission.value = false
                                    currentActivity.lifecycleScope.launch {
                                        delay(1500)
                                        GlobalLoader.hide(currentActivity)
                                        LogUtils.printLog(
                                            "inter_home shown",
                                            interstitialTrackedUnitId(interstitialHome)
                                        )
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    GlobalLoader.hide(currentActivity)
                                    interstitialHome = null
                                    LogUtils.printLog(
                                        "inter_home failed to shown",
                                        interstitialTrackedUnitId(interstitialHome)
                                    )
                                    com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                        requestPermission.value = true
                                        navigateNext(currentActivity)
                                    }
                                }

                                override fun onAdImpression() {
                                    super.onAdImpression()
                                    interstitialHome = null
                                }
                            },
                            forFragment = true,
                            onContinue = {
                                GlobalLoader.hide(currentActivity)
                                interstitialHome = null
                                com.aiface.aging.shared.ads.FullscreenAdGate.runWhenAdsClear {
                                    requestPermission.value = true
                                    navigateNext(currentActivity)
                                }
                            },
                        )
                    } else {
                        GlobalLoader.hide(currentActivity)
                        requestPermission.value = true
                        navigateNext(currentActivity)
                    }
                } else {
                    requestPermission.value = true
                    navigateNext(currentActivity)
                }
            } catch (e: Exception) {
                requestPermission.value = true
                e.printStackTrace()
                navigateNext(currentActivity)
            }
        }
    }

    private fun navigateNext(activity: FragmentActivity){
        findNavController().navigate(R.id.action_to_editFragment, builder.bundleExtras)
    }

    private fun navigateToLookCrop(
        activity: FragmentActivity,
        imageUri: String,
        featureType: String,
    ) {
        val isHair = featureType == LookConstants.SCREEN_HAIR_COLOR
        val isFace = featureType == LookConstants.SCREEN_MAKEUP
        if (activity is LookFeatureActivity) {
            findNavController().navigate(
                R.id.action_lookImagePicker_to_cropFragment,
                bundleOf(
                    "imagePath" to imageUri,
                    "isHair" to isHair,
                    "isFace" to isFace,
                ),
            )
        }
    }
}
