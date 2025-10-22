package com.ssj.statuswindow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ssj.statuswindow.model.CardEvent
import com.ssj.statuswindow.repo.CardEventRepository
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(private val repo: CardEventRepository) : ViewModel() {
    val events: StateFlow<List<CardEvent>> = repo.events
    fun addEvents(list: List<CardEvent>) = repo.addAll(list)
    fun addEvent(e: CardEvent) = repo.add(e)
}

class MainViewModelFactory(
    private val repo: CardEventRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MainViewModel(repo) as T
}
