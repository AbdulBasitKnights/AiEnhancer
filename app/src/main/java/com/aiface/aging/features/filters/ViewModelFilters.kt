package com.aiface.aging.features.filters

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.features.filters.model.toModelFiltersPack
import com.aiface.aging.data.LocalFiltersDataSource
import com.aiface.aging.features.filters.model.ModelFilterPack
import com.aiface.aging.features.filters.model.ModelFilters
import com.aiface.aging.features.filters.model.toModelFilters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewModelFilters @Inject constructor(

) : ViewModel() {

    // LiveData to expose the filter headers data
    private val _filterHeaders = MutableLiveData<List<ModelFilters>>()
    val filterHeaders: LiveData<List<ModelFilters>> = _filterHeaders

    private val _filterPacks = MutableLiveData<List<ModelFilterPack>>()
    val filterPacks: LiveData<List<ModelFilterPack>> = _filterPacks

    fun getFilterHeaders(option: String, filterDataSource: LocalFiltersDataSource) {
        try {

           viewModelScope.launch {
               try {
                   val data = filterDataSource.getLocalHeadersDataNew(option).map {
                       it.toModelFilters()
                   }
                   _filterHeaders.postValue(data)
               }catch (e : Exception){
                   e.printStackTrace()
               }
           }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFilterPacks(catId: Int,  filterDataSource: LocalFiltersDataSource) {
        try {

            viewModelScope.launch {
                try {
                    val data = filterDataSource.getLocalPacksDataNew(catId).map {
                        it.toModelFiltersPack()
                    }
                    _filterPacks.postValue(data)
                }catch (e : Exception){
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}