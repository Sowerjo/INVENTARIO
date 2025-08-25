@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.mobitech.inventario.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mobitech.inventario.ui.theme.InventarioTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLayoutSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            ListItem(
                headlineContent = { Text("Layout da Lista") },
                supportingContent = { Text("Configurar campos exibidos na lista de produtos") },
                leadingContent = { Icon(Icons.Outlined.ViewList, contentDescription = null) },
                modifier = Modifier.clickable { onOpenLayoutSettings() }
            )
            HorizontalDivider()

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Text("Versão 1.0.0", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Configurações")
@Composable
private fun SettingsScreenPreview() {
    InventarioTheme {
        SettingsScreen(onBack = {}, onOpenLayoutSettings = {})
    }
}
