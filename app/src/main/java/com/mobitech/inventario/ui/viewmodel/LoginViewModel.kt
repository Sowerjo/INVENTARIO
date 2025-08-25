package com.mobitech.inventario.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.usecase.LoginUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

data class LoginState(
    val username: String = "admin",
    val password: String = "admin123",
    val loading: Boolean = false,
    val error: String? = null,
    val logged: Boolean = false
)

sealed interface LoginEvent {
    data class OnUsername(val v: String): LoginEvent
    data class OnPassword(val v: String): LoginEvent
    object Submit: LoginEvent
}

@HiltViewModel
class LoginViewModel @Inject constructor(private val loginUC: LoginUserUseCase): ViewModel() {
    var state: LoginState by mutableStateOf(LoginState())
        private set

    fun onEvent(e: LoginEvent) {
        when(e) {
            is LoginEvent.OnUsername -> setState { copy(username = e.v) }
            is LoginEvent.OnPassword -> setState { copy(password = e.v) }
            LoginEvent.Submit -> doLogin()
        }
    }

    private fun doLogin() {
        setState { copy(loading = true, error = null) }
        viewModelScope.launch {
            when(val res = loginUC(state.username.trim(), state.password)) {
                is Result.Success -> setState { copy(loading=false, logged = true) }
                is Result.Error -> setState { copy(loading=false, error = res.message) }
            }
        }
    }

    private fun setState(reducer: LoginState.() -> LoginState) { state = state.reducer() }
}
