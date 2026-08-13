package com.aiface.aging.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

abstract class BaseViewModel : ViewModel() {
    protected val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    protected val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    protected fun handleError(message: String?) {
        _error.postValue(message ?: "An unknown error occurred")
    }

    protected fun handleLoading(isLoading: Boolean) {
        _loading.postValue(isLoading)
    }
}
