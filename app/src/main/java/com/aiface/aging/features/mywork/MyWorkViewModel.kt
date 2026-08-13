package com.aiface.aging.features.mywork

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MyWorkViewModel @Inject constructor(
    private val application: Application,
) : ViewModel() {

    private val _myWorkState = MutableLiveData<List<MediaStoreImage>>(emptyList())
    val myWorkState: LiveData<List<MediaStoreImage>> = _myWorkState

    private val _selectedTab = MutableLiveData(LibraryTab.AI_PHOTOS)
    val selectedTab: LiveData<LibraryTab> = _selectedTab

    private var _selectedAvatar = MutableLiveData<String>()
    var selectedAvatar: LiveData<String> = _selectedAvatar

    private var _userName = MutableLiveData<String>()
    var userName: LiveData<String> = _userName

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun loadGalleryImages(folderName: String, myWorkImageSource: MyWorkImageSource) {
        viewModelScope.launch {
            getGalleryImages(folderName, myWorkImageSource)
                .catch { e ->
                    e.printStackTrace()
                    _myWorkState.postValue(emptyList())
                }
                .collect { list ->
                    _myWorkState.postValue(list)
                }
        }
    }

    fun getGalleryImages(
        folderName: String,
        myWorkImageSource: MyWorkImageSource,
    ): Flow<List<MediaStoreImage>> = kotlinx.coroutines.flow.flow {
        try {
            val data = myWorkImageSource
                .getGalleryImages(folderName)
                .map { it.toMediaStoreImage() }
            emit(data)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    suspend fun sortList(sortType: Int, list: List<MediaStoreImage>): List<MediaStoreImage> =
        withContext(Dispatchers.IO) {
            try {
                when (sortType) {
                    0 -> list.sortedBy { it.displayName }
                    1 -> list.sortedByDescending { it.dateAdded }
                    else -> list
                }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
}
