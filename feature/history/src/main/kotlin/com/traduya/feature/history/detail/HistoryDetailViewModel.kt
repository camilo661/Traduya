package com.traduya.feature.history.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.traduya.core.domain.repository.HistoryRepository
import com.traduya.core.model.CallRecord
import com.traduya.core.model.TranscriptLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryDetailUiState(
    val isLoading: Boolean = true,
    val call: CallRecord? = null,
    val lines: List<TranscriptLine> = emptyList(),
)

@HiltViewModel
class HistoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val callId: Long = checkNotNull(savedStateHandle[CALL_ID_ARG])

    val uiState: StateFlow<HistoryDetailUiState> = combine(
        historyRepository.observeCall(callId),
        historyRepository.observeTranscript(callId),
    ) { call, lines -> HistoryDetailUiState(isLoading = false, call = call, lines = lines) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryDetailUiState())

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            historyRepository.deleteCall(callId)
            onDeleted()
        }
    }

    companion object {
        const val CALL_ID_ARG = "callId"
    }
}
