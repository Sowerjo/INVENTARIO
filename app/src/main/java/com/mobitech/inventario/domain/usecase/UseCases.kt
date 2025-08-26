package com.mobitech.inventario.domain.usecase

import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.*
import com.mobitech.inventario.domain.repository.*

class LoginUserUseCase(private val userRepo: UserRepository) {
    suspend operator fun invoke(username: String, password: String) = userRepo.login(username, password)
}

class CreateInventoryUseCase(private val repo: InventoryRepository, private val userRepo: UserRepository) {
    suspend operator fun invoke(name: String, note: String?): Result<Long> {
        val user = userRepo.getCurrent() ?: return Result.Error("Sem usuário logado")
        return repo.create(name, note, user.id)
    }
}

class FinishInventoryUseCase(private val repo: InventoryRepository, private val userRepo: UserRepository) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        val user = userRepo.getCurrent() ?: return Result.Error("Sem usuário logado")
        if (user.role != UserRole.SUPERVISOR) return Result.Error("Sem permissão")
        return repo.finalize(id, user.id)
    }
}

class AddInventoryItemUseCase(private val repo: InventoryItemRepository, private val userRepo: UserRepository) {
    suspend operator fun invoke(inventoryId: Long, productCode: String, qty: Double) =
        userRepo.getCurrent()?.let { repo.addItem(inventoryId, productCode, qty, it.id) }
            ?: Result.Error("Sem usuário logado")
}

class ExportInventoryItemsUseCase(private val exportRepo: CsvExportRepository) {
    suspend operator fun invoke(inv: InventoryEntity, items: List<InventoryItemEntity>) =
        exportRepo.exportInventoryItems(inv, items)
}

class ObserveProductsUseCase(private val repo: ProductRepository) {
    operator fun invoke() = repo.observeProducts()
}

class UpsertProductUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(p: ProductEntity) = repo.upsert(p)
}

class DeleteProductUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(p: ProductEntity) = repo.delete(p)
}

class ImportProductsCsvUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(content: String, overwrite: Boolean) = repo.importCsv(content, overwrite)
}

class ExportProductsCsvUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke() = repo.exportCsv()
}

class PrepareProductsXmlParserUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke() = repo.prepareXmlParser()
}

class ObserveCategoriesUseCase(private val repo: CategoryRepository) {
    operator fun invoke() = repo.observeAll()
}

class SetCategoryEnabledUseCase(private val repo: CategoryRepository) {
    suspend operator fun invoke(id: Long, enabled: Boolean) = repo.setEnabled(id, enabled)
}

class SetAllCategoriesEnabledUseCase(private val repo: CategoryRepository) {
    suspend operator fun invoke(enabled: Boolean) = repo.setAllEnabled(enabled)
}

class ImportProductsXlsxUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(bytes: ByteArray, overwrite: Boolean, onProgress: ((Int, Int) -> Unit)? = null) =
        repo.importXlsx(bytes, overwrite, onProgress)
}

class GetProductAttributesUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(codes: List<String>, keys: List<String>) = repo.getAttributesFor(codes, keys)
}

class GetProductListLayoutUseCase(private val repo: ProductListLayoutRepository) {
    suspend operator fun invoke() = repo.get()
}

class UpdateProductListLayoutUseCase(private val repo: ProductListLayoutRepository) {
    suspend operator fun invoke(layout: ProductListLayoutEntity) = repo.save(layout)
}

class GetAvailableLayoutKeysUseCase(private val repo: ProductListLayoutRepository) {
    suspend operator fun invoke() = repo.getAvailableKeys()
}

class SuggestLayoutDefaultsUseCase(private val repo: ProductListLayoutRepository) {
    suspend operator fun invoke(availableKeys: List<String>) = repo.suggestDefaults(availableKeys)
}

class ExportProductsXlsxUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke() = repo.exportXlsx()
}

class ExportProductsXlsxToUriUseCase(private val repo: ProductRepository) {
    suspend operator fun invoke(uri: android.net.Uri) = repo.exportXlsxToUri(uri)
}

class SaveInventoryConfigUseCase(private val repo: InventoryConfigRepository) {
    suspend operator fun invoke(
        name: String,
        isDefault: Boolean,
        idKey: String,
        expectedQtyField: String,
        updateQtyField: String,
        updateMode: UpdateMode,
        scanKey: String,
        filterKeys: List<String>,
        normalizeSearch: Boolean,
        incrementOnScan: Boolean,
        line1Key: String,
        line2Keys: List<String>,
        line3Keys: List<String>,
        qtyKey: String,
        showExpected: Boolean,
        quickSteps: List<Int>,
        scopeCategory: String?,
        inputMode: InputMode
    ): Result<Long> {
        val config = InventoryConfigEntity(
            name = name,
            isDefault = isDefault
        )

        val fieldMapping = FieldMappingConfigEntity(
            configId = 0L, // Será preenchido no repositório
            idKey = idKey,
            expectedQtyField = expectedQtyField,
            updateQtyField = updateQtyField,
            updateMode = updateMode
        )

        val searchConfig = SearchConfigEntity(
            configId = 0L,
            scanKey = scanKey,
            filterKeys = filterKeys.joinToString(","),
            normalizeSearch = normalizeSearch,
            incrementOnScan = incrementOnScan
        )

        val cardLayout = CardLayoutConfigEntity(
            configId = 0L,
            line1Key = line1Key,
            line2Keys = line2Keys.joinToString(","),
            line3Keys = line3Keys.joinToString(","),
            qtyKey = qtyKey,
            showExpected = showExpected,
            quickSteps = quickSteps.joinToString(",")
        )

        val creationRules = CreationRulesConfigEntity(
            configId = 0L,
            scopeCategory = scopeCategory,
            inputMode = inputMode
        )

        return repo.save(config, fieldMapping, searchConfig, cardLayout, creationRules)
    }
}

class LoadInventoryConfigUseCase(private val repo: InventoryConfigRepository) {
    suspend operator fun invoke(id: Long) = repo.load(id)
}

class DeleteInventoryConfigUseCase(private val repo: InventoryConfigRepository) {
    suspend operator fun invoke(id: Long) = repo.delete(id)
}

class ObserveInventoryConfigsUseCase(private val repo: InventoryConfigRepository) {
    operator fun invoke() = repo.observeAll()
}
