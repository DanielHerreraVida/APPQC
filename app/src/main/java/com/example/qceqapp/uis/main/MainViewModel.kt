package com.example.qceqapp.uis.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {

    private val _currentTitle = MutableLiveData("To Inspect")
    val currentTitle: LiveData<String> get() = _currentTitle

    fun updateTitle(title: String) {
        _currentTitle.value = title
    }
}