package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.InventoryEntity
import com.mobitech.inventario.domain.repository.InventoryRepository
import com.mobitech.inventario.domain.usecase.CreateInventoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class InventoryListState(
    val inventories: List<InventoryEntity> = emptyList(),
    val name: String = "",
    val note: String = "",
    val creating: Boolean = false,
    val error: String? = null
)

sealed interface InventoryListEvent {
    data class OnName(val v: String): InventoryListEvent
    data class OnNote(val v: String): InventoryListEvent
    object Create: InventoryListEvent
    object ClearError: InventoryListEvent
}

@HiltViewModel
class InventoryListViewModel @Inject constructor(
    private val repo: InventoryRepository,
    private val createUC: CreateInventoryUseCase
): ViewModel() {
    var state by mutableStateOf(InventoryListState())
        private set

    init { observeInventories() }

    private fun observeInventories() = viewModelScope.launch {
        repo.observeInventories().collectLatest { list ->
            state = state.copy(inventories = list)
        }
    }

    fun onEvent(e: InventoryListEvent) {
        when(e) {
            is InventoryListEvent.OnName -> state = state.copy(name = e.v)
            is InventoryListEvent.OnNote -> state = state.copy(note = e.v)
            InventoryListEvent.Create -> create()
            InventoryListEvent.ClearError -> state = state.copy(error = null)
        }
    }

    private fun create() {
        if (state.name.isBlank()) { state = state.copy(error = "Nome obrigatório"); return }
        viewModelScope.launch {
            state = state.copy(creating = true, error = null)
            when(val res = createUC(state.name.trim(), state.note.trim().ifBlank { null })) {
                is Result.Success -> state = state.copy(creating=false, name="", note="")
                is Result.Error -> state = state.copy(creating=false, error = res.message)
            }
        }
    }
}

