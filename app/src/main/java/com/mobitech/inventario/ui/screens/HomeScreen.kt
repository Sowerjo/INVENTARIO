package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ListAlt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.ui.theme.InventarioTheme
import com.mobitech.inventario.ui.viewmodel.HomeState

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: HomeState, onInventories: () -> Unit, onProducts: () -> Unit, onSettings: () -> Unit) {
    val modules = listOf(
        HomeModule(
            title = "Inventários",
            desc = "Criar e acompanhar inventários",
            icon = Icons.Outlined.ListAlt,
            enabled = true,
            action = onInventories,
            container = MaterialTheme.colorScheme.primaryContainer
        ),
        HomeModule(
            title = "Produtos",
            desc = "Importar listagem de produtos",
            icon = Icons.Outlined.ShoppingCart,
            enabled = true,
            action = onProducts,
            container = MaterialTheme.colorScheme.secondaryContainer
        ),
        HomeModule(
            title = "Conferência",
            desc = "Recontagem e divergências (em breve)",
            icon = Icons.Outlined.Inventory2,
            enabled = false
        ),
        HomeModule(
            title = "Relatórios",
            desc = "Exportações e análises (em breve)",
            icon = Icons.Outlined.Assessment,
            enabled = false
        ),
        HomeModule(
            title = "Backup",
            desc = "Exportar / restaurar (em breve)",
            icon = Icons.Outlined.Backup,
            enabled = false
        ),
        HomeModule(
            title = "Configurações",
            desc = "Preferências do app (categorias)",
            icon = Icons.Outlined.Settings,
            enabled = true,
            action = onSettings
        )
    )

    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.SemiBold) })
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(state.welcome, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(state.placeholder, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
        }
        // Grid ocupa o restante e é o único scrollável
        LazyVerticalGrid(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            columns = GridCells.Adaptive(minSize = 160.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(modules) { m -> ModuleCard(m) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

private data class HomeModule(
    val title: String,
    val desc: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val enabled: Boolean,
    val action: (() -> Unit)? = null,
    val container: Color? = null
)

@Composable
private fun ModuleCard(m: HomeModule) {
    val shape = MaterialTheme.shapes.medium
    val alphaDisabled = if (m.enabled) 1f else 0.35f
    Surface(
        shape = shape,
        color = m.container ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        tonalElevation = if (m.enabled) 4.dp else 0.dp,
        shadowElevation = if (m.enabled) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.1f)
            .then(if (m.enabled && m.action != null) Modifier.clickable { m.action.invoke() } else Modifier),
    ) {
        Column(Modifier.padding(14.dp).fillMaxSize()) {
            Icon(m.icon, contentDescription = m.title, tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = alphaDisabled))
            Spacer(Modifier.height(8.dp))
            Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = alphaDisabled))
            Spacer(Modifier.height(4.dp))
            Text(m.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaDisabled), maxLines = 3)
            Spacer(Modifier.weight(1f))
            if (m.enabled && m.action != null) {
                AssistChip(onClick = m.action, label = { Text("Abrir") })
            } else {
                AssistChip(onClick = {}, enabled = false, label = { Text("Em breve") })
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Home")
@Composable
private fun HomeScreenPreview() {
    InventarioTheme {
        HomeScreen(state = HomeState(welcome = "Bem-vindo", placeholder = "Demonstração"), onInventories = {}, onProducts = {}, onSettings = {})
    }
}
