package com.ssj.statuswindow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ssj.statuswindow.model.AppNotificationLog
import com.ssj.statuswindow.repo.NotificationLogRepository
import kotlinx.coroutines.flow.StateFlow

class NotificationLogViewModel(
    private val repo: NotificationLogRepository
) : ViewModel() {
    val logs: StateFlow<List<AppNotificationLog>> = repo.logs
}

class NotificationLogViewModelFactory(
    private val repo: NotificationLogRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotificationLogViewModel(repo) as T
}
