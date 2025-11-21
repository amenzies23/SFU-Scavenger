package com.aark.sfuscavenger.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.aark.sfuscavenger.repositories.GameRepository

class EventsViewModel(
    private val gameRepo: GameRepository = GameRepository()
) : ViewModel() {
}