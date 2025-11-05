package com.aark.sfuscavenger.ui.lobby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class LobbyViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is the LobbyScreen"
    }
    val text: LiveData<String> = _text
}