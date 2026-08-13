package com.aiface.aging.features.share

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.R
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.di.MainDispatcher
import androidx.appcompat.app.AppCompatActivity
import com.aiface.aging.utils.ToastUtils

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ShareImageViewModel @Inject constructor(
    private val application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : ViewModel() {

    private var pendingDeleteImage: ExtrasShareImageActivity? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender?>()
    val permissionNeededForDelete: LiveData<IntentSender?> = _permissionNeededForDelete

//    fun loadIcons() = flow {
//        emit(
//            listOf(
//                ModelDrawableAssets(1, R.drawable.ic_more_new, "Other"),
//                ModelDrawableAssets(3, R.drawable.ic_whatsapp_new, "Whatsapp"),
//                ModelDrawableAssets(4, R.drawable.ic_instagram_new, "Instagram"),
//                ModelDrawableAssets(2, R.drawable.ic_facebook_new, "Facebook"),
//                ModelDrawableAssets(5, R.drawable.ic_x_new, "X")
//            )
//        )
//    }.flowOn(ioDispatcher)

    //Image Delete functions
    fun deleteImage(image: ExtrasShareImageActivity, activity: AppCompatActivity) {
        viewModelScope.launch {
            performDeleteImage(image, activity)
        }
    }

    fun deletePendingImage(activity: AppCompatActivity) {
        pendingDeleteImage?.let { image ->
            pendingDeleteImage = null
            deleteImage(image, activity)
        }
    }

    private suspend fun performDeleteImage(
        image: ExtrasShareImageActivity,
        activity: AppCompatActivity
    ) {
        withContext(ioDispatcher) {
            try {
                application.applicationContext.contentResolver.delete(
                    image.uri!!,
                    "${MediaStore.Images.Media._ID} = ?",
                    arrayOf(image.id.toString())
                )
                withContext(mainDispatcher) {
                    activity.onBackPressedDispatcher.onBackPressed()
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val recoverableSecurityException =
                        securityException as? RecoverableSecurityException

                    // Signal to the Activity that it needs to request permission and
                    // try the delete again if it succeeds.
                    pendingDeleteImage = image
                    _permissionNeededForDelete.postValue(
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    )
                } else {
                    //throw securityException
                    ToastUtils.showToast(activity, "something went wrong. please try again")
                }
            }
        }
    }
}