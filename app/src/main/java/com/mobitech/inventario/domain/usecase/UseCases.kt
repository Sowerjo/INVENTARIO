package com.mobitech.inventario.domain.usecase

import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.model.InventoryEntity
import com.mobitech.inventario.domain.model.InventoryItemEntity
import com.mobitech.inventario.domain.model.UserRole
import com.mobitech.inventario.domain.model.ProductEntity
import com.mobitech.inventario.domain.model.CategoryEntity
import com.mobitech.inventario.domain.repository.*
import com.mobitech.inventario.domain.model.ProductListLayoutEntity

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
