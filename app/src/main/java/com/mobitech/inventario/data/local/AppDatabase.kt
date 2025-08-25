package com.mobitech.inventario.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mobitech.inventario.domain.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt

@Database(
    entities = [
        UserEntity::class,
        ProductEntity::class,
        InventoryEntity::class,
        InventoryItemEntity::class,
        ConferenceItemEntity::class,
        PaymentEntity::class,
        BackupLogEntity::class,
        SettingsEntity::class,
        CategoryEntity::class,
        ProductAttributeEntity::class,
        ProductListLayoutEntity::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun inventoryItemDao(): InventoryItemDao
    abstract fun conferenceItemDao(): ConferenceItemDao
    abstract fun paymentDao(): PaymentDao
    abstract fun backupLogDao(): BackupLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productAttributeDao(): ProductAttributeDao
    abstract fun productListLayoutDao(): ProductListLayoutDao

    companion object {
        fun build(context: Context, scope: CoroutineScope): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "inventario.db")
                .fallbackToDestructiveMigration()
                .addCallback(object: Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                    }
                })
                .build()

        fun seed(scope: CoroutineScope, db: AppDatabase) {
            scope.launch {
                if (db.userDao().count() == 0L) {
                    val hash = BCrypt.withDefaults().hashToString(12, "admin123".toCharArray())
                    db.userDao().insert(UserEntity(username = "admin", passwordHash = hash, role = UserRole.SUPERVISOR))
                }
                if (db.settingsDao().get() == null) {
                    db.settingsDao().save(SettingsEntity())
                }
                if (db.productDao().count() == 0L) {
                    db.productDao().upsert(ProductEntity(code="P001", name="Produto A", description="Demo", category="CAT", unit="UN"))
                    db.productDao().upsert(ProductEntity(code="P002", name="Produto B", description="Demo", category="CAT", unit="UN"))
                }
            }
        }
    }
}
