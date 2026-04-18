package com.tubetone.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(private val repo: RingtoneRepository) : ViewModel() {
    val items = repo.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun delete(id: String) { viewModelScope.launch { repo.delete(id) } }
    fun markApplied(id: String) { viewModelScope.launch { repo.markApplied(id) } }
}
