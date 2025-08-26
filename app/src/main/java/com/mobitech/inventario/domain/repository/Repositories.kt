package com.mobitech.inventario.domain.repository

import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun login(username: String, password: String): Result<UserEntity>
    suspend fun getCurrent(): UserEntity?
    suspend fun logout()
}

interface ProductRepository {
    fun observeProducts(): Flow<List<ProductEntity>>
    suspend fun upsert(product: ProductEntity): Result<Long>
    suspend fun findByCode(code: String): ProductEntity?
    suspend fun delete(product: ProductEntity): Result<Unit>
    suspend fun importCsv(content: String, overwrite: Boolean = false): Result<Int>
    suspend fun exportCsv(): Result<String>
    suspend fun exportXlsx(): Result<String>
    suspend fun exportXlsxToUri(uri: android.net.Uri): Result<Unit>
    suspend fun prepareXmlParser(): Result<Unit>
    suspend fun importXlsx(bytes: ByteArray, overwrite: Boolean = false, onProgress: ((Int, Int) -> Unit)? = null): Result<Int>
    suspend fun getAttributesFor(codes: List<String>, keys: List<String>): Result<Map<String, Map<String, String?>>> // corrected
}

interface InventoryRepository {
    fun observeInventories(): Flow<List<InventoryEntity>>
    suspend fun create(name: String, note: String?, userId: Long): Result<Long>
    suspend fun finalize(id: Long, userId: Long): Result<Unit>
    suspend fun delete(id: Long, userId: Long): Result<Unit>
    suspend fun getById(id: Long): InventoryEntity?
}

interface InventoryItemRepository {
    fun observeItems(inventoryId: Long): Flow<List<InventoryItemEntity>>
    suspend fun addItem(inventoryId: Long, productCode: String, qty: Double, userId: Long): Result<Long>
}

interface ConferenceRepository {
    suspend fun recount(inventoryItemId: Long, qty: Double, userId: Long): Result<Long>
    suspend fun markStatus(conferenceItemId: Long, status: ConferenceStatus): Result<Unit>
    suspend fun buildDivergenceReport(inventoryId: Long): Result<String>
}

interface PaymentRepository {
    fun observeByInventory(inventoryId: Long): Flow<List<PaymentEntity>>
    suspend fun register(payment: PaymentEntity): Result<Long>
}

interface BackupRepository {
    suspend fun backupDb(): Result<String>
    suspend fun restoreDb(path: String): Result<Unit>
}

interface SettingsRepository {
    suspend fun get(): SettingsEntity
    suspend fun save(settings: SettingsEntity): Result<Unit>
}

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    suspend fun setEnabled(id: Long, enabled: Boolean): Result<Unit>
    suspend fun ensure(name: String): Result<Long>
    suspend fun setAllEnabled(enabled: Boolean): Result<Unit>
}

interface CsvExportRepository {
    suspend fun exportInventoryItems(inventory: InventoryEntity, items: List<InventoryItemEntity>): Result<String>
}

interface ProductListLayoutRepository {
    suspend fun get(): ProductListLayoutEntity
    suspend fun save(layout: ProductListLayoutEntity): Result<Unit>
    suspend fun updateLine1Key(key: String): Result<Unit>
    suspend fun updateLine2Keys(keys: List<String>): Result<Unit>
    suspend fun updateLine3Keys(keys: List<String>): Result<Unit>
    suspend fun updateQtyKey(key: String): Result<Unit>
    suspend fun getAvailableKeys(): Result<List<String>>
    suspend fun suggestDefaults(availableKeys: List<String>): ProductListLayoutEntity
}

interface InventoryConfigRepository {
    fun observeAll(): Flow<List<InventoryConfigEntity>>
    suspend fun getById(id: Long): InventoryConfigEntity?
    suspend fun getDefault(): InventoryConfigEntity?
    suspend fun save(
        config: InventoryConfigEntity,
        fieldMapping: FieldMappingConfigEntity,
        searchConfig: SearchConfigEntity,
        cardLayout: CardLayoutConfigEntity,
        creationRules: CreationRulesConfigEntity
    ): Result<Long>
    suspend fun load(id: Long): Result<CompleteInventoryConfig>
    suspend fun delete(id: Long): Result<Unit>
}

data class CompleteInventoryConfig(
    val config: InventoryConfigEntity,
    val fieldMapping: FieldMappingConfigEntity?,
    val searchConfig: SearchConfigEntity?,
    val cardLayout: CardLayoutConfigEntity?,
    val creationRules: CreationRulesConfigEntity?
)
