package com.ssj.statuswindow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ssj.statuswindow.data.model.AppNotificationLog
import com.ssj.statuswindow.repo.NotificationLogRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NotificationLogViewModel(
    private val repo: NotificationLogRepository
) : ViewModel() {
    
    private val viewModelScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    val logs: StateFlow<List<AppNotificationLog>> = repo.logs.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
}

class NotificationLogViewModelFactory(
    private val repo: NotificationLogRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationLogViewModel(repo) as T
}
