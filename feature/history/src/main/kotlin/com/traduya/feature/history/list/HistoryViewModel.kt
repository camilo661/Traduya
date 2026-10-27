package com.traduya.feature.history.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.model.CallRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Ready(val calls: List<CallRecord>) : HistoryUiState
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = historyRepository.observeCalls()
        .map<List<CallRecord>, HistoryUiState> { HistoryUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    fun clearAll() {
        viewModelScope.launch { historyRepository.clearAll() }
    }
}
