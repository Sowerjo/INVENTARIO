package com.mobitech.inventario.di

import android.app.Application
import com.mobitech.inventario.data.local.*
import com.mobitech.inventario.data.repository.*
import com.mobitech.inventario.domain.repository.*
import com.mobitech.inventario.domain.usecase.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton fun provideAppScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton fun provideDb(app: Application, scope: CoroutineScope): AppDatabase = AppDatabase.build(app, scope)

    @Provides fun provideUserDao(db: AppDatabase) = db.userDao()
    @Provides fun provideProductDao(db: AppDatabase) = db.productDao()
    @Provides fun provideInventoryDao(db: AppDatabase) = db.inventoryDao()
    @Provides fun provideInventoryItemDao(db: AppDatabase) = db.inventoryItemDao()
    @Provides fun provideSettingsDao(db: AppDatabase) = db.settingsDao()
    @Provides fun provideCategoryDao(db: AppDatabase) = db.categoryDao()
    @Provides fun provideProductAttributeDao(db: AppDatabase) = db.productAttributeDao()
    @Provides fun provideProductListLayoutDao(db: AppDatabase) = db.productListLayoutDao()
    @Provides fun provideInventoryConfigDao(db: AppDatabase) = db.inventoryConfigDao()
    @Provides fun provideFieldMappingConfigDao(db: AppDatabase) = db.fieldMappingConfigDao()
    @Provides fun provideSearchConfigDao(db: AppDatabase) = db.searchConfigDao()
    @Provides fun provideCardLayoutConfigDao(db: AppDatabase) = db.cardLayoutConfigDao()
    @Provides fun provideCreationRulesConfigDao(db: AppDatabase) = db.creationRulesConfigDao()

    @Provides @Singleton fun provideUserRepo(userDao: UserDao): UserRepository = UserRepositoryImpl(userDao)
    @Provides @Singleton fun provideCategoryRepo(categoryDao: CategoryDao): CategoryRepository = CategoryRepositoryImpl(categoryDao)
    @Provides @Singleton fun provideProductRepo(app: Application, productDao: ProductDao, categoryDao: CategoryDao, productAttributeDao: ProductAttributeDao): ProductRepository = ProductRepositoryImpl(productDao, app, categoryDao, productAttributeDao)
    @Provides @Singleton fun provideInventoryRepo(inventoryDao: InventoryDao): InventoryRepository = InventoryRepositoryImpl(inventoryDao)
    @Provides @Singleton fun provideInventoryItemRepo(iiDao: InventoryItemDao, productDao: ProductDao): InventoryItemRepository = InventoryItemRepositoryImpl(iiDao, productDao)
    @Provides @Singleton fun provideCsvExportRepo(app: Application): CsvExportRepository = CsvExportRepositoryImpl(app)
    @Provides @Singleton fun provideSettingsRepo(settingsDao: SettingsDao): SettingsRepository = SettingsRepositoryImpl(settingsDao)
    @Provides @Singleton fun provideProductListLayoutRepo(layoutDao: ProductListLayoutDao, categoryDao: CategoryDao): ProductListLayoutRepository = ProductListLayoutRepositoryImpl(layoutDao, categoryDao)
    @Provides @Singleton fun provideInventoryConfigRepo(
        configDao: InventoryConfigDao,
        fieldMappingDao: FieldMappingConfigDao,
        searchConfigDao: SearchConfigDao,
        cardLayoutDao: CardLayoutConfigDao,
        creationRulesDao: CreationRulesConfigDao
    ): InventoryConfigRepository = InventoryConfigRepositoryImpl(configDao, fieldMappingDao, searchConfigDao, cardLayoutDao, creationRulesDao)

    // UseCases
    @Provides fun provideLoginUC(userRepo: UserRepository) = LoginUserUseCase(userRepo)
    @Provides fun provideCreateInvUC(invRepo: InventoryRepository, userRepo: UserRepository) = CreateInventoryUseCase(invRepo, userRepo)
    @Provides fun provideAddItemUC(itemRepo: InventoryItemRepository, userRepo: UserRepository) = AddInventoryItemUseCase(itemRepo, userRepo)
    @Provides fun provideExportItemsUC(csvRepo: CsvExportRepository) = ExportInventoryItemsUseCase(csvRepo)
    @Provides fun provideFinishInvUC(invRepo: InventoryRepository, userRepo: UserRepository) = FinishInventoryUseCase(invRepo, userRepo)
    @Provides fun provideObserveProductsUC(productRepo: ProductRepository) = ObserveProductsUseCase(productRepo)
    @Provides fun provideUpsertProductUC(productRepo: ProductRepository) = UpsertProductUseCase(productRepo)
    @Provides fun provideDeleteProductUC(productRepo: ProductRepository) = DeleteProductUseCase(productRepo)
    @Provides fun provideImportProductsCsvUC(productRepo: ProductRepository) = ImportProductsCsvUseCase(productRepo)
    @Provides fun provideExportProductsCsvUC(productRepo: ProductRepository) = ExportProductsCsvUseCase(productRepo)
    @Provides fun provideExportProductsXlsxUC(productRepo: ProductRepository) = ExportProductsXlsxUseCase(productRepo)
    @Provides fun provideExportProductsXlsxToUriUC(productRepo: ProductRepository) = ExportProductsXlsxToUriUseCase(productRepo)
    @Provides fun providePrepareProductsXmlParserUC(productRepo: ProductRepository) = PrepareProductsXmlParserUseCase(productRepo)
    @Provides fun provideObserveCategoriesUC(catRepo: CategoryRepository) = ObserveCategoriesUseCase(catRepo)
    @Provides fun provideSetCategoryEnabledUC(catRepo: CategoryRepository) = SetCategoryEnabledUseCase(catRepo)
    @Provides fun provideSetAllCategoriesEnabledUC(catRepo: CategoryRepository) = SetAllCategoriesEnabledUseCase(catRepo)
    @Provides fun provideImportProductsXlsxUC(productRepo: ProductRepository) = ImportProductsXlsxUseCase(productRepo)
    @Provides fun provideGetProductAttributesUC(productRepo: ProductRepository) = GetProductAttributesUseCase(productRepo)
    @Provides fun provideGetProductListLayoutUC(layoutRepo: ProductListLayoutRepository) = GetProductListLayoutUseCase(layoutRepo)
    @Provides fun provideUpdateProductListLayoutUC(layoutRepo: ProductListLayoutRepository) = UpdateProductListLayoutUseCase(layoutRepo)
    @Provides fun provideGetAvailableLayoutKeysUC(layoutRepo: ProductListLayoutRepository) = GetAvailableLayoutKeysUseCase(layoutRepo)
    @Provides fun provideSuggestLayoutDefaultsUC(layoutRepo: ProductListLayoutRepository) = SuggestLayoutDefaultsUseCase(layoutRepo)
    @Provides fun provideSaveInventoryConfigUC(repo: InventoryConfigRepository) = SaveInventoryConfigUseCase(repo)
    @Provides fun provideLoadInventoryConfigUC(repo: InventoryConfigRepository) = LoadInventoryConfigUseCase(repo)
    @Provides fun provideDeleteInventoryConfigUC(repo: InventoryConfigRepository) = DeleteInventoryConfigUseCase(repo)
    @Provides fun provideObserveInventoryConfigsUC(repo: InventoryConfigRepository) = ObserveInventoryConfigsUseCase(repo)
}
