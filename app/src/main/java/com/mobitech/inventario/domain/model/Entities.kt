package com.mobitech.inventario.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Usuários
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val role: UserRole,
    val createdAt: Long = System.currentTimeMillis()
)

// Produtos
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val code: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val unit: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Inventário
@Entity(tableName = "inventories")
data class InventoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String? = null,
    val status: InventoryStatus = InventoryStatus.ONGOING,
    val startAt: Long = System.currentTimeMillis(),
    val endAt: Long? = null,
    val createdByUserId: Long,
    val createdAt: Long = System.currentTimeMillis()
)

// Itens contados
@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryId: Long,
    val productCode: String,
    val qtyCounted: Double,
    val countedAt: Long = System.currentTimeMillis(),
    val countedByUserId: Long
)

// Recontagens (conferência)
@Entity(tableName = "conference_items")
data class ConferenceItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryItemId: Long,
    val qty: Double,
    val recountedAt: Long = System.currentTimeMillis(),
    val status: ConferenceStatus = ConferenceStatus.PENDENTE,
    val userId: Long? = null
)

// Pagamentos (placeholder)
@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val inventoryId: Long,
    val amount: Double,
    val method: PaymentMethod,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val createdByUserId: Long? = null
)

// Logs de backup/export
@Entity(tableName = "backup_logs")
data class BackupLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BackupType,
    val path: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val status: String? = null
)

// Configurações (registro único id=1)
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val conferenceMode: ConferenceMode = ConferenceMode.CEGA,
    val allowNegativeStock: Boolean = false
)

// Categorias (habilitáveis)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val originalName: String = name, // Nome original do header do arquivo
    val enabled: Boolean = false,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Atributos dinâmicos de produto
@Entity(tableName = "product_attributes", primaryKeys = ["productCode","key"])
data class ProductAttributeEntity(
    val productCode: String,
    val key: String,
    val value: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

// Configurações de layout da lista de produtos
@Entity(tableName = "product_list_layout")
data class ProductListLayoutEntity(
    @PrimaryKey val id: Int = 1,
    val line1Key: String = "nome",
    val line2Keys: String = "ean,sku", // Separado por vírgula
    val line3Keys: String = "preco,categoria", // Separado por vírgula
    val qtyKey: String = "quantidade",
    val updatedAt: Long = System.currentTimeMillis()
)

// Configuração de Inventário (template reutilizável)
@Entity(tableName = "inventory_configs")
data class InventoryConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Aba A: Mapeamento de Campos
@Entity(tableName = "field_mapping_configs")
data class FieldMappingConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val idKey: String, // OBRIGATÓRIO - campo que identifica unicamente o produto
    val expectedQtyField: String? = null, // campo da base que representa o estoque esperado
    val updateQtyField: String? = null, // campo que será atualizado no fechamento
    val updateMode: UpdateMode = UpdateMode.OVERWRITE, // OVERWRITE ou DELTA
    val readAsString: Boolean = true // sempre ler células como STRING
)

// Aba B: Busca & Leitura
@Entity(tableName = "search_configs")
data class SearchConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val scanKey: String, // campo que o scanner/pesquisa usa
    val filterKeys: String, // JSON array de campos pesquisáveis
    val normalizeSearch: Boolean = true, // case-insensitive, ignorar espaços/hífens
    val incrementOnScan: Boolean = true // somar +1 automático ou abrir numpad
)

// Aba C: Layout do Card
@Entity(tableName = "card_layout_configs")
data class CardLayoutConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val line1Key: String, // campo mostrado na Linha 1 (título, negrito)
    val line2Keys: String, // JSON array ≤2 campos na Linha 2
    val line3Keys: String, // JSON array ≤2 campos na Linha 3
    val qtyKey: String, // campo mostrado/alterado na coluna direita
    val showExpected: Boolean = true, // mostrar quantidade esperada
    val quickSteps: String // JSON array de botões rápidos (ex: [-1, 1, 5, 10])
)

// Aba D: Regras de Criação
@Entity(tableName = "creation_rules_configs")
data class CreationRulesConfigEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val configId: Long,
    val scopeCategory: String? = null, // filtrar por categoria ou usar todos
    val inputMode: InputMode = InputMode.ACCUMULATE // ACCUMULATE ou OVERWRITE
)
