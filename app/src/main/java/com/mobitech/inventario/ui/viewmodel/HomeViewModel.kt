package com.mobitech.inventario.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.mobitech.inventario.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class HomeState(val welcome: String = "Bem-vindo", val placeholder: String = "Funcionalidades em construção")

@HiltViewModel
class HomeViewModel @Inject constructor(userRepository: UserRepository): ViewModel() {
    val state = HomeState()
}

