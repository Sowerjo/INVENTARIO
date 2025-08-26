package com.mobitech.inventario.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mobitech.inventario.ui.screens.*
import com.mobitech.inventario.ui.viewmodel.*

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = NavRoutes.LOGIN) {
        composable(NavRoutes.LOGIN) {
            val vm: LoginViewModel = hiltViewModel()
            LoginScreen(state = vm.state, onEvent = { e -> vm.onEvent(e) }, navigateHome = { nav.navigate(NavRoutes.HOME) { popUpTo(NavRoutes.LOGIN) { inclusive = true } } })
        }
        composable(NavRoutes.HOME) {
            val vm: HomeViewModel = hiltViewModel()
            HomeScreen(
                state = vm.state,
                onInventories = { nav.navigate(NavRoutes.INVENTORY_LIST) },
                onProducts = { nav.navigate(NavRoutes.PRODUCTS) },
                onSettings = { nav.navigate(NavRoutes.SETTINGS) }
            )
        }
        composable(NavRoutes.INVENTORY_LIST) {
            val vm: InventoryListViewModel = hiltViewModel()
            InventoryListScreen(
                state = vm.state,
                onEvent = vm::onEvent,
                onOpen = { id -> nav.navigate("inventario/detail/$id") },
                onBack = { nav.popBackStack() },
                onOpenConfig = { nav.navigate(NavRoutes.INVENTORY_CONFIG) }
            )
        }
        composable(
            route = "inventario/detail/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: 0L
            val vm: InventoryDetailViewModel = hiltViewModel()
            vm.load(id)
            InventoryDetailScreen(
                state = vm.state,
                onEvent = vm::onEvent,
                onBack = { nav.popBackStack() }
            )
        }
        composable(NavRoutes.INVENTORY_CONFIG) {
            InventoryConfigScreen(
                onBack = { nav.popBackStack() }
            )
        }
        composable(NavRoutes.PRODUCTS) {
            val vm: ProductListViewModel = hiltViewModel()
            ProductListScreen(
                state = vm.state,
                onEvent = vm::onEvent,
                onBack = { nav.popBackStack() },
                onOpenLayoutSettings = { nav.navigate(NavRoutes.PRODUCT_LIST_LAYOUT_SETTINGS) }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onOpenLayoutSettings = { nav.navigate(NavRoutes.PRODUCT_LIST_LAYOUT_SETTINGS) }
            )
        }
        composable(NavRoutes.PRODUCT_LIST_LAYOUT_SETTINGS) {
            val vm: ProductListLayoutViewModel = hiltViewModel()
            ProductListLayoutScreen(state = vm.state, onEvent = vm::onEvent, onBack = { nav.popBackStack() })
        }
    }
}
