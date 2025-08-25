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
