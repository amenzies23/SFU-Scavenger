package com.aark.sfuscavenger.ui.social

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SocialViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is Social Screen"
    }
    val text: LiveData<String> = _text
}