package com.aiface.aging.features.imgpicker.builder

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.AnimRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity

import com.aiface.aging.features.imgpicker.builder.listener.ImageSelectCancelListener
import com.aiface.aging.features.imgpicker.builder.listener.OnErrorListener
import com.aiface.aging.features.imgpicker.builder.listener.OnMultiSelectedListener
import com.aiface.aging.features.imgpicker.builder.listener.OnSelectedListener
import com.aiface.aging.features.imgpicker.builder.type.AlbumType
import com.aiface.aging.features.imgpicker.builder.type.ButtonGravity
import com.aiface.aging.features.imgpicker.builder.type.MediaType
import com.aiface.aging.features.imgpicker.builder.type.SelectType
import com.aiface.aging.features.imgpicker.extenstion.lookNextNavigateWithId
import com.aiface.aging.features.imgpicker.extenstion.nextNavigateTo
import com.aiface.aging.features.imgpicker.extenstion.nextNavigateWithId
import com.aiface.aging.features.look.LookFeatureActivity
import com.aiface.aging.features.imgpicker.util.ToastUtil
import com.tedpark.tedonactivityresult.rx2.TedRxOnActivityResult
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize
import com.aiface.aging.R
import com.aiface.aging.shared.editorName
import com.aiface.aging.features.collage.FragmentCollageTemplatesDirections
import com.aiface.aging.features.home.preview.PreviewFragmentDirections
import com.aiface.aging.features.imgpicker.ui.FragmentImagePicker
import com.aiface.aging.features.imgpicker.ui.FragmentImagePickerFramesAlbum
import com.aiface.aging.features.main.MainFragmentDirections

@Suppress("UNCHECKED_CAST")
@Parcelize
open class TedImagePickerBaseBuilder<B : TedImagePickerBaseBuilder<B>>(
    internal var selectType: SelectType = SelectType.SINGLE,
    internal var mediaType: MediaType = MediaType.IMAGE,
    @ColorRes
    internal var cameraTileBackgroundResId: Int = R.color.ted_image_picker_camera_background,
    @DrawableRes
    internal var cameraTileImageResId: Int = R.drawable.ic_camera_pic,
    internal var showCameraTile: Boolean = true,
    internal var scrollIndicatorDateFormat: String = "yyyy.MM",
    internal var showTitle: Boolean = true,
    internal var title: String? = null,
    internal var savedDirectoryName: String? = null,
    @StringRes
    internal var titleResId: Int = R.string.ted_image_picker_title,
    internal var buttonGravity: ButtonGravity = ButtonGravity.TOP,
    internal var buttonText: String? = null,
    @DrawableRes
    internal var buttonBackgroundResId: Int = R.drawable.btn_done_button,
    @ColorRes
    internal var buttonTextColorResId: Int = R.color.white,
    internal var buttonDrawableOnly: Boolean = false,
    @StringRes
    internal var buttonTextResId: Int = R.string.ted_image_picker_upload,
    internal var selectedUriList: List<Uri>? = null,
    @DrawableRes
    internal var backButtonResId: Int = R.drawable.ic_back_arrow,
    internal var maxCount: Int = Int.MAX_VALUE,
    internal var maxCountMessage: String? = null,
    @StringRes
    internal var maxCountMessageResId: Int = R.string.ted_image_picker_max_count,
    internal var minCount: Int = Int.MIN_VALUE,
    internal var minCountMessage: String? = null,
    @StringRes
    internal var minCountMessageResId: Int = R.string.ted_image_picker_min_count,
    internal var showZoomIndicator: Boolean = false,
    internal var albumType: AlbumType = AlbumType.DROP_DOWN,
    internal var imageCountFormat: String = "%s",
    @AnimRes
    internal var startEnterAnim: Int? = null,
    @AnimRes
    internal var startExitAnim: Int? = null,
    @AnimRes
    internal var finishEnterAnim: Int? = null,
    @AnimRes
    internal var finishExitAnim: Int? = null,
    internal var screenOrientation: Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
    internal var showVideoDuration: Boolean = true,

    internal var destinationIntent: Intent? = null,
    internal var bundleExtras: Bundle? = null
) : Parcelable {


    @IgnoredOnParcel
    protected var onSelectedListener: OnSelectedListener? = null

    @IgnoredOnParcel
    protected var onMultiSelectedListener: OnMultiSelectedListener? = null

    @IgnoredOnParcel
    protected var onErrorListener: OnErrorListener? = null

    @IgnoredOnParcel
    protected var imageSelectCancelListener: ImageSelectCancelListener? = null

    @SuppressLint("CheckResult")
    protected fun startInternal(
        builder: TedImagePickerBaseBuilder<B>,
        context: Context,
        isActivity: Boolean,
        editorName: String
    ) {
        if (isActivity) {
          //  startActivity(context)
        } else {
            if (editorName == "collage"){
                navigateToCollageImagePicker(context, builder)
            }else if (editorName == "bodyMaker"){
                navigateToBodyMakerImagePicker(context, builder)
            }
            else if (editorName == "ai"){
                navigateToAiEdit(context, builder)
            }
            else if (editorName == "bgRemover"){
                navigateToBgRemoverImagePicker(context, builder)
            }
            else if (editorName == "magicEraser"){
                navigateToMagicEraserImagePicker(context, builder)
            }
            else if (editorName == "hair"){
                navigateToHair(context, builder)
            }
            else if (editorName == "makeup"){
                navigateToMakeup(context, builder)
            }
            else if (editorName == "blender"){
                navigateToBlenderImagePicker(context, builder)
            }
            else if (editorName == "frames"){
                navigateToFramesImagePicker(context, builder)
            }
            else if (editorName == "faceswap"){
                navigateToFaceSwapImagePicker(context, builder)
            }
            else{
                navigateToPhotoEditImagePicker(context, builder)
            }

        }
    }

    private fun navigateToBlenderImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "blender"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateWithId(R.id.actionBlenderToImagPicker)
    }

    private fun navigateToFramesImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "frames"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateWithId(R.id.actionFramesToImagPicker)
    }

    private fun navigateToFaceSwapImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "faceswap"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateWithId(R.id.actionFaceSwapToImagPicker)
    }

    private fun navigateToBodyMakerImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "bodyMaker"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(MainFragmentDirections.actionHomeToImagPicker(0,true))
    }
    private fun navigateToHair(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        navigateToLookGallery(context, builder, "hair")
    }
    private fun navigateToMakeup(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        navigateToLookGallery(context, builder, "makeup")
    }

    private fun navigateToLookGallery(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>,
        editor: String,
    ) {
        editorName = editor
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        if (activity is LookFeatureActivity) {
            activity.lookNextNavigateWithId(
                R.id.action_instructionFragment_to_lookImagePicker,
                bundleOf(
                    "selectedPosition" to 0,
                    "showToolbar" to true,
                    "albumName" to "",
                ),
            )
        } else {
            activity.nextNavigateTo(MainFragmentDirections.actionHomeToImagPicker(0, true))
        }
    }

    private fun navigateToPhotoEditImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "photoEdit"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(MainFragmentDirections.actionHomeToImagPicker(0,true))
    }

    private fun navigateToBgRemoverImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "bgRemover"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(MainFragmentDirections.actionHomeToImagPicker(0,true))
    }

    private fun navigateToMagicEraserImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>,
    ) {
        editorName = "magicEraser"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(MainFragmentDirections.actionHomeToImagPicker(0, true))
    }

    private fun navigateToCollageImagePicker(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "collage"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(FragmentCollageTemplatesDirections.actionCollageToImagPicker(0,true))
    }

    private fun navigateToAiEdit(
        context: Context,
        builder: TedImagePickerBaseBuilder<B>
    ) {
        editorName = "ai"
        val activity = context as FragmentActivity
        FragmentImagePicker.setBuilder(builder)
        FragmentImagePickerFramesAlbum.setBuilder(builder)
        activity.nextNavigateTo(PreviewFragmentDirections.actionAiPreviewToImagPicker(0,true))
    }


    fun mediaType(mediaType: MediaType): B {
        this.mediaType = mediaType
        return this as B
    }

    fun destinationIntent(destinationIntent: Intent): B {
        this.destinationIntent = destinationIntent
        return this as B
    }

    fun bundleExtras(bundleExtras: Bundle): B {
        this.bundleExtras = bundleExtras
        return this as B
    }

    fun image(): B = mediaType(MediaType.IMAGE)

    fun video(): B = mediaType(MediaType.VIDEO)

    fun imageAndVideo(): B = mediaType(MediaType.IMAGE_AND_VIDEO)

    fun cameraTileBackground(@ColorRes cameraTileBackgroundResId: Int): B {
        this.cameraTileBackgroundResId = cameraTileBackgroundResId
        return this as B
    }

    fun cameraTileImage(@DrawableRes cameraTileImage: Int): B {
        this.cameraTileImageResId = cameraTileImage
        return this as B
    }

    fun showCameraTile(show: Boolean): B {
        this.showCameraTile = show
        return this as B
    }

    fun scrollIndicatorDateFormat(formatString: String): B {
        this.scrollIndicatorDateFormat = formatString
        return this as B
    }

    fun showTitle(show: Boolean): B {
        this.showTitle = show
        return this as B
    }

    fun title(text: String): B {
        this.title = text
        return this as B
    }

    fun title(@StringRes textResId: Int): B {
        this.titleResId = textResId
        return this as B
    }

    fun savedDirectoryName(savedDirectoryName: String): B {
        this.savedDirectoryName = savedDirectoryName
        return this as B
    }

    fun buttonGravity(buttonGravity: ButtonGravity): B {
        this.buttonGravity = buttonGravity
        return this as B
    }

    fun buttonText(text: String): B {
        this.buttonText = text
        return this as B
    }

    fun buttonText(@StringRes textResId: Int): B {
        this.buttonTextResId = textResId
        return this as B
    }

    fun buttonBackground(@DrawableRes buttonBackgroundResId: Int): B {
        this.buttonBackgroundResId = buttonBackgroundResId
        return this as B
    }

    fun buttonTextColor(@ColorRes buttonTextColorResId: Int): B {
        this.buttonTextColorResId = buttonTextColorResId
        return this as B
    }

    fun buttonDrawableOnly() = buttonDrawableOnly(true)

    fun buttonDrawableOnly(value: Boolean): B {
        buttonDrawableOnly = value
        return this as B
    }

    fun selectedUri(uriList: List<Uri>?): B {
        this.selectedUriList = uriList
        return this as B
    }

    fun backButton(@DrawableRes backButtonResId: Int): B {
        this.backButtonResId = backButtonResId
        return this as B
    }

    fun max(maxCount: Int, maxCountMessage: String): B {
        this.maxCount = maxCount
        this.maxCountMessage = maxCountMessage
        return this as B
    }

    fun max(maxCount: Int, @StringRes maxCountMessageResId: Int): B {
        this.maxCount = maxCount
        this.maxCountMessageResId = maxCountMessageResId
        return this as B
    }

    fun min(minCount: Int, minCountMessage: String): B {
        this.minCount = minCount
        this.minCountMessage = minCountMessage
        return this as B
    }

    fun destinationActivity(minCount: Int, minCountMessage: String): B {
        this.minCount = minCount
        this.minCountMessage = minCountMessage
        return this as B
    }

    fun min(minCount: Int, @StringRes minCountMessageResId: Int): B {
        this.minCount = minCount
        this.minCountMessageResId = minCountMessageResId
        return this as B
    }

    fun zoomIndicator(show: Boolean): B {
        this.showZoomIndicator = show
        return this as B
    }

    fun albumType(albumType: AlbumType): B {
        this.albumType = albumType
        if (albumType == AlbumType.DROP_DOWN) {
            showTitle(false)
        }
        return this as B
    }

    fun drawerAlbum(): B {
        return albumType(AlbumType.DRAWER)
    }

    fun dropDownAlbum(): B {
        return albumType(AlbumType.DROP_DOWN)
    }

    fun imageCountTextFormat(formatText: String): B {
        this.imageCountFormat = formatText
        return this as B
    }

    fun startAnimation(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int): B {
        this.startEnterAnim = enterAnim
        this.startExitAnim = exitAnim
        return this as B
    }

    fun finishAnimation(@AnimRes enterAnim: Int, @AnimRes exitAnim: Int): B {
        this.finishEnterAnim = enterAnim
        this.finishExitAnim = exitAnim
        return this as B
    }

    fun toast(toastAction: ((String) -> Unit)): B {
        ToastUtil.toastAction = toastAction
        return this as B
    }

    fun screenOrientation(orientation: Int) {
        this.screenOrientation = orientation
    }

    fun showVideoDuration(show: Boolean): B {
        this.showVideoDuration = show
        return this as B
    }

}
