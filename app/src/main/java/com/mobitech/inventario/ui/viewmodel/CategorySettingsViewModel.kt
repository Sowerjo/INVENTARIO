package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.CategoryEntity
import com.mobitech.inventario.domain.usecase.ObserveCategoriesUseCase
import com.mobitech.inventario.domain.usecase.SetAllCategoriesEnabledUseCase
import com.mobitech.inventario.domain.usecase.SetCategoryEnabledUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class CategorySettingsState(
    val categories: List<CategoryEntity> = emptyList(),
    val filtered: List<CategoryEntity> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

sealed interface CategorySettingsEvent {
    data class Toggle(val id: Long, val enabled: Boolean): CategorySettingsEvent
    object EnableAll: CategorySettingsEvent
    object DisableAll: CategorySettingsEvent
    object ClearMessage: CategorySettingsEvent
    object ClearError: CategorySettingsEvent
    data class UpdateQuery(val q: String): CategorySettingsEvent
}

@HiltViewModel
class CategorySettingsViewModel @Inject constructor(
    observeUC: ObserveCategoriesUseCase,
    private val setEnabledUC: SetCategoryEnabledUseCase,
    private val setAllUC: SetAllCategoriesEnabledUseCase
): ViewModel() {
    var state by mutableStateOf(CategorySettingsState())
        private set

    init {
        viewModelScope.launch {
            observeUC().collectLatest { list ->
                val sorted = list.sortedBy { it.name.lowercase() }
                state = state.copy(categories = sorted)
                applyFilter()
            }
        }
    }

    fun onEvent(e: CategorySettingsEvent) {
        when(e) {
            is CategorySettingsEvent.Toggle -> toggle(e.id, e.enabled)
            CategorySettingsEvent.EnableAll -> bulk(true)
            CategorySettingsEvent.DisableAll -> bulk(false)
            CategorySettingsEvent.ClearMessage -> state = state.copy(message = null)
            CategorySettingsEvent.ClearError -> state = state.copy(error = null)
            is CategorySettingsEvent.UpdateQuery -> { state = state.copy(query = e.q); applyFilter() }
        }
    }

    private fun toggle(id: Long, enabled: Boolean) = viewModelScope.launch {
        when(val res = setEnabledUC(id, enabled)) {
            is Result.Success -> state = state.copy(message = "Atualizado")
            is Result.Error -> state = state.copy(error = res.message)
        }
    }

    private fun bulk(enabled: Boolean) = viewModelScope.launch {
        state = state.copy(loading = true, error = null, message = null)
        when(val res = setAllUC(enabled)) {
            is Result.Success -> state = state.copy(loading=false, message = if (enabled) "Todas habilitadas" else "Todas desabilitadas")
            is Result.Error -> state = state.copy(loading=false, error = res.message)
        }
    }

    private fun applyFilter() {
        val q = state.query.trim().lowercase()
        val filtered = if (q.isEmpty()) state.categories else state.categories.filter { it.name.lowercase().contains(q) }
        state = state.copy(filtered = filtered)
    }
}
