package com.aark.sfuscavenger.ui.create

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the CreateScreen"
    }
    val text: LiveData<String> = _text
}