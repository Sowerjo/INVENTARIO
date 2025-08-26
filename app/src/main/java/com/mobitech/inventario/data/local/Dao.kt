package com.mobitech.inventario.data.local

import androidx.room.*
import com.mobitech.inventario.domain.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Query("SELECT COUNT(*) FROM users")
    suspend fun count(): Long
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name")
    fun observeAll(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products WHERE code = :code LIMIT 1")
    suspend fun findByCode(code: String): ProductEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ProductEntity): Long
    @Delete
    suspend fun delete(product: ProductEntity)
    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Long
    @Query("DELETE FROM products")
    suspend fun clearAll()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventories ORDER BY startAt DESC")
    fun observeAll(): Flow<List<InventoryEntity>>
    @Insert
    suspend fun insert(inv: InventoryEntity): Long
    @Query("UPDATE inventories SET status = :status, endAt = :endAt WHERE id = :id")
    suspend fun updateStatus(id: Long, status: InventoryStatus, endAt: Long?)
    @Query("SELECT * FROM inventories WHERE id = :id")
    suspend fun getById(id: Long): InventoryEntity?
}

@Dao
interface InventoryItemDao {
    @Query("SELECT * FROM inventory_items WHERE inventoryId = :inventoryId")
    fun observeItems(inventoryId: Long): Flow<List<InventoryItemEntity>>
    @Insert
    suspend fun insert(item: InventoryItemEntity): Long
}

@Dao
interface ConferenceItemDao {
    @Query("SELECT * FROM conference_items WHERE inventoryItemId IN (:inventoryItemIds)")
    suspend fun getByInventoryItemIds(inventoryItemIds: List<Long>): List<ConferenceItemEntity>
    @Insert
    suspend fun insert(entity: ConferenceItemEntity): Long
}

@Dao
interface PaymentDao {
    @Insert
    suspend fun insert(payment: PaymentEntity): Long
    @Query("SELECT * FROM payments WHERE inventoryId = :inventoryId")
    fun observeByInventory(inventoryId: Long): Flow<List<PaymentEntity>>
}

@Dao
interface BackupLogDao {
    @Insert
    suspend fun insert(log: BackupLogEntity): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun get(): SettingsEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: SettingsEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex, name")
    fun observeAll(): Flow<List<CategoryEntity>>
    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: CategoryEntity): Long
    @Query("UPDATE categories SET enabled = :enabled, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    @Query("UPDATE categories SET enabled = :enabled, updatedAt = :updatedAt WHERE name IN (:names)")
    suspend fun bulkSetEnabled(names: List<String>, enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    @Query("UPDATE categories SET enabled = :enabled, updatedAt = :updatedAt")
    suspend fun setAllEnabled(enabled: Boolean, updatedAt: Long = System.currentTimeMillis())
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}

@Dao
interface ProductAttributeDao {
    @Query("SELECT * FROM product_attributes WHERE productCode = :code")
    suspend fun findByProduct(code: String): List<ProductAttributeEntity>
    @Query("SELECT * FROM product_attributes WHERE productCode IN (:codes) AND `key` IN (:keys)")
    suspend fun findByProductsAndKeys(codes: List<String>, keys: List<String>): List<ProductAttributeEntity>
    @Query("SELECT * FROM product_attributes WHERE `key` IN (:keys)")
    suspend fun findByKeys(keys: List<String>): List<ProductAttributeEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attr: ProductAttributeEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<ProductAttributeEntity>)
    @Query("DELETE FROM product_attributes WHERE productCode = :code")
    suspend fun deleteByProduct(code: String)
    @Query("DELETE FROM product_attributes")
    suspend fun clearAll()
}

@Dao
interface ProductListLayoutDao {
    @Query("SELECT * FROM product_list_layout WHERE id = 1")
    suspend fun get(): ProductListLayoutEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(layout: ProductListLayoutEntity)

    @Query("UPDATE product_list_layout SET line1Key = :line1Key, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLine1Key(line1Key: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE product_list_layout SET line2Keys = :line2Keys, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLine2Keys(line2Keys: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE product_list_layout SET line3Keys = :line3Keys, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateLine3Keys(line3Keys: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE product_list_layout SET qtyKey = :qtyKey, updatedAt = :updatedAt WHERE id = 1")
    suspend fun updateQtyKey(qtyKey: String, updatedAt: Long = System.currentTimeMillis())
}

@Dao
interface InventoryConfigDao {
    @Query("SELECT * FROM inventory_configs ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<InventoryConfigEntity>>

    @Query("SELECT * FROM inventory_configs WHERE id = :id")
    suspend fun getById(id: Long): InventoryConfigEntity?

    @Query("SELECT * FROM inventory_configs WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): InventoryConfigEntity?

    @Insert
    suspend fun insert(config: InventoryConfigEntity): Long

    @Update
    suspend fun update(config: InventoryConfigEntity)

    @Delete
    suspend fun delete(config: InventoryConfigEntity)

    @Query("UPDATE inventory_configs SET isDefault = 0")
    suspend fun clearAllDefaults()
}

@Dao
interface FieldMappingConfigDao {
    @Query("SELECT * FROM field_mapping_configs WHERE configId = :configId")
    suspend fun getByConfigId(configId: Long): FieldMappingConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: FieldMappingConfigEntity)

    @Delete
    suspend fun delete(config: FieldMappingConfigEntity)
}

@Dao
interface SearchConfigDao {
    @Query("SELECT * FROM search_configs WHERE configId = :configId")
    suspend fun getByConfigId(configId: Long): SearchConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: SearchConfigEntity)

    @Delete
    suspend fun delete(config: SearchConfigEntity)
}

@Dao
interface CardLayoutConfigDao {
    @Query("SELECT * FROM card_layout_configs WHERE configId = :configId")
    suspend fun getByConfigId(configId: Long): CardLayoutConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: CardLayoutConfigEntity)

    @Delete
    suspend fun delete(config: CardLayoutConfigEntity)
}

@Dao
interface CreationRulesConfigDao {
    @Query("SELECT * FROM creation_rules_configs WHERE configId = :configId")
    suspend fun getByConfigId(configId: Long): CreationRulesConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: CreationRulesConfigEntity)

    @Delete
    suspend fun delete(config: CreationRulesConfigEntity)
}

class EnumConverters {
    @TypeConverter fun roleToString(v: UserRole) = v.name
    @TypeConverter fun stringToRole(v: String) = UserRole.valueOf(v)
    @TypeConverter fun invStatusToString(v: InventoryStatus) = v.name
    @TypeConverter fun stringToInvStatus(v: String) = InventoryStatus.valueOf(v)
    @TypeConverter fun confStatusToString(v: ConferenceStatus) = v.name
    @TypeConverter fun stringToConfStatus(v: String) = ConferenceStatus.valueOf(v)
    @TypeConverter fun paymentMethodToString(v: PaymentMethod) = v.name
    @TypeConverter fun stringToPaymentMethod(v: String) = PaymentMethod.valueOf(v)
    @TypeConverter fun backupTypeToString(v: BackupType) = v.name
    @TypeConverter fun stringToBackupType(v: String) = BackupType.valueOf(v)
    @TypeConverter fun conferenceModeToString(v: ConferenceMode) = v.name
    @TypeConverter fun stringToConferenceMode(v: String) = ConferenceMode.valueOf(v)
    @TypeConverter fun updateModeToString(v: UpdateMode) = v.name
    @TypeConverter fun stringToUpdateMode(v: String) = UpdateMode.valueOf(v)
    @TypeConverter fun inputModeToString(v: InputMode) = v.name
    @TypeConverter fun stringToInputMode(v: String) = InputMode.valueOf(v)
}
