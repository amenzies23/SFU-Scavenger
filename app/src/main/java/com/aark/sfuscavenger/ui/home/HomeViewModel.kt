package com.aark.sfuscavenger.ui.home

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

class HomeViewModel : ViewModel() {
    // Will be populated from backend
    val publicGames: SnapshotStateList<String> = mutableStateListOf()
}