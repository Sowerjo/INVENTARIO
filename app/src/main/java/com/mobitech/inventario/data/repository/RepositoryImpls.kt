package com.mobitech.inventario.data.repository

import android.content.Context
import android.net.Uri
import at.favre.lib.crypto.bcrypt.BCrypt
import com.mobitech.inventario.data.local.*
import com.mobitech.inventario.domain.common.Result
import com.mobitech.inventario.domain.common.resultOf
import com.mobitech.inventario.domain.model.*
import com.mobitech.inventario.domain.repository.*
import com.mobitech.inventario.domain.parser.ImportXlsxParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File
import java.time.Instant

class UserRepositoryImpl(private val userDao: UserDao): UserRepository {
    @Volatile private var current: UserEntity? = null
    override suspend fun login(username: String, password: String): Result<UserEntity> = resultOf {
        val user = userDao.findByUsername(username) ?: error("Usuário não encontrado")
        val verify = BCrypt.verifyer().verify(password.toCharArray(), user.passwordHash)
        if(!verify.verified) error("Senha inválida")
        current = user
        user
    }
    override suspend fun getCurrent(): UserEntity? = current
    override suspend fun logout() { current = null }
}

class CategoryRepositoryImpl(private val categoryDao: CategoryDao): CategoryRepository {
    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    override suspend fun setEnabled(id: Long, enabled: Boolean): Result<Unit> = resultOf { categoryDao.setEnabled(id, enabled); Unit }
    override suspend fun ensure(name: String): Result<Long> = resultOf {
        val existing = categoryDao.findByName(name)
        if (existing != null) existing.id else categoryDao.insert(CategoryEntity(name = name, enabled = false))
    }
    override suspend fun setAllEnabled(enabled: Boolean): Result<Unit> = resultOf { categoryDao.setAllEnabled(enabled); Unit }
}

class ProductRepositoryImpl(private val productDao: ProductDao, private val context: Context, private val categoryDao: CategoryDao, private val productAttributeDao: ProductAttributeDao): ProductRepository {
    private fun baseDir(): File {
        val dir = File(context.filesDir, "inventario")
        dir.mkdirs()
        File(dir, "import").mkdirs()
        File(dir, "export").mkdirs()
        return dir
    }

    override fun observeProducts(): Flow<List<ProductEntity>> = productDao.observeAll()
    override suspend fun upsert(product: ProductEntity): Result<Long> = resultOf { productDao.upsert(product) }
    override suspend fun findByCode(code: String): ProductEntity? = productDao.findByCode(code)
    override suspend fun delete(product: ProductEntity): Result<Unit> = resultOf { productDao.delete(product); Unit }
    override suspend fun importCsv(content: String, overwrite: Boolean): Result<Int> = resultOf {
        val raw = content.replace('\r', '\n')
        val linesAll = raw.lineSequence().map { it.trimEnd() }.filter { it.isNotBlank() }.toList()
        if (linesAll.isEmpty()) return@resultOf 0
        fun detectDelim(sample: String): Char = when {
            sample.contains(';') -> ';'
            sample.contains('\t') -> '\t'
            sample.contains(',') -> ','
            else -> ';'
        }
        val firstLine = linesAll.first()
        val delim = detectDelim(firstLine)
        val norm: (String) -> String = { s ->
            s.trim().trim('"', '\'',' ').lowercase()
                .replace("ç", "c")
                .replace("á", "a").replace("à", "a").replace("ã", "a")
                .replace("é", "e").replace("ê", "e")
                .replace("í", "i").replace("ó", "o").replace("õ", "o").replace("ú", "u")
        }
        val headerTokensRaw = firstLine.split(delim).map { norm(it) }
        val synonyms = mapOf(
            "code" to listOf("code","codigo","código","cod"),
            "name" to listOf("name","nome"),
            "description" to listOf("description","descricao","descrição","desc"),
            "category" to listOf("category","categoria","cat"),
            "unit" to listOf("unit","unidade","uni","und")
        )
        fun findIndex(key: String): Int? = synonyms[key]?.firstNotNullOfOrNull { syn -> headerTokensRaw.indexOf(syn).takeIf { it >= 0 } }
        val headerLooksValid = listOf("code","name").all { k -> synonyms[k]!!.any { headerTokensRaw.contains(it) } }
        val dataLines = if (headerLooksValid) linesAll.drop(1) else linesAll
        if (overwrite) productDao.clearAll()
        var count = 0
        val catSet = mutableSetOf<String>()
        val idxCode = if (headerLooksValid) findIndex("code") else 0
        val idxName = if (headerLooksValid) findIndex("name") else 1
        val idxDesc = if (headerLooksValid) findIndex("description") else 2
        val idxCat  = if (headerLooksValid) findIndex("category") else 3
        val idxUnit = if (headerLooksValid) findIndex("unit") else 4
        dataLines.forEach { line ->
            val parts = line.split(delim).map { it.trim().trim('"', '\'') }
            val code = idxCode?.let { parts.getOrNull(it) }?.takeIf { !it.isNullOrBlank() } ?: return@forEach
            val name = idxName?.let { parts.getOrNull(it) }?.takeIf { !it.isNullOrBlank() } ?: code
            val desc = idxDesc?.let { parts.getOrNull(it) }?.takeUnless { it.isNullOrBlank() }
            val cat = idxCat?.let { parts.getOrNull(it) }?.takeUnless { it.isNullOrBlank() }
            val unit = idxUnit?.let { parts.getOrNull(it) }?.takeUnless { it.isNullOrBlank() }
            productDao.upsert(ProductEntity(code = code, name = name, description = desc, category = cat, unit = unit))
            cat?.let { catSet += it }
            count++
        }
        // garantir categorias
        catSet.forEach { c ->
            val existing = categoryDao.findByName(c)
            if (existing == null) categoryDao.insert(CategoryEntity(name = c, enabled = false))
        }
        // salva o arquivo original em inventario/import para histórico
        val ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val importFile = File(baseDir(), "import/product_import_$ts.csv")
        importFile.writeText(content)
        count
    }
    override suspend fun exportCsv(): Result<String> = resultOf {
        val list = productDao.observeAll().first()
        val ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val file = File(baseDir(), "export/products_$ts.csv")
        file.printWriter().use { out ->
            out.println("code;name;description;category;unit")
            list.forEach { p ->
                out.println(listOf(p.code, p.name, p.description ?: "", p.category ?: "", p.unit ?: "").joinToString(";"))
            }
        }
        file.absolutePath
    }
    override suspend fun prepareXmlParser(): Result<Unit> = Result.Success(Unit) // futuro: preparar libs/parsers
    override suspend fun importXlsx(bytes: ByteArray, overwrite: Boolean, onProgress: ((Int, Int) -> Unit)?): Result<Int> = resultOf {
        val parsed = ImportXlsxParser.parse(bytes)
        val total = parsed.products.size
        if (overwrite) {
            productDao.clearAll()
            productAttributeDao.clearAll()
            categoryDao.deleteAll()
        }

        // Recriamos as categorias na ordem exata dos headers ORIGINAIS
        val categoriesInOrder = parsed.originalHeaders.mapIndexed { index, headerName ->
            CategoryEntity(name = headerName, originalName = headerName, enabled = false, orderIndex = index)
        }
        categoryDao.insertAll(categoriesInOrder)

        val seen = mutableSetOf<String>()
        parsed.products.forEachIndexed { idx, p ->
            productDao.upsert(ProductEntity(code = p.code, name = p.name, description = p.description, category = p.category, unit = p.unit))
            // Usar os headers originais para os atributos
            val attrs = parsed.originalHeaders.mapIndexedNotNull { index, originalHeader ->
                val normalizedHeader = parsed.headers[index]
                val value = p.attributes[normalizedHeader]
                if (value != null) ProductAttributeEntity(productCode = p.code, key = originalHeader, value = value) else null
            }
            productAttributeDao.upsertAll(attrs)
            seen += p.code
            if (idx % 100 == 0) onProgress?.invoke(idx+1, total)
        }
        onProgress?.invoke(total, total)
        // salva arquivo
        val ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val importFile = File(baseDir(), "import/product_import_$ts.xlsx")
        importFile.writeBytes(bytes)
        seen.size
    }

    override suspend fun getAttributesFor(codes: List<String>, keys: List<String>): Result<Map<String, Map<String, String?>>> = resultOf {
        if (codes.isEmpty() || keys.isEmpty()) return@resultOf emptyMap<String, Map<String,String?>>()
        val attrs = productAttributeDao.findByProductsAndKeys(codes, keys)
        attrs.groupBy { it.productCode }.mapValues { (_, list) -> list.associate { it.key to it.value } }
    }

    override suspend fun exportXlsx(): Result<String> = resultOf {
        val list = productDao.observeAll().first()
        val categories = categoryDao.observeAll().first().sortedBy { it.orderIndex }.map { it.originalName }  // Usar originalName ordenado por orderIndex
        val ts = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val file = File(baseDir(), "export/products_$ts.xlsx")

        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Produtos")

        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerStyle.setFont(headerFont)

        // Usar APENAS as categorias do arquivo original, sem campos básicos predefinidos
        val allHeaders = categories

        val headerRow = sheet.createRow(0)
        for (i in allHeaders.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(allHeaders[i])
            cell.cellStyle = headerStyle
        }

        // Buscar todos os atributos de produtos para as categorias usando originalName
        val productCodes = list.map { it.code }
        val attributesMap = if (categories.isNotEmpty() && productCodes.isNotEmpty()) {
            productAttributeDao.findByProductsAndKeys(productCodes, categories)
                .groupBy { it.productCode }
                .mapValues { (_, attrs) -> attrs.associate { it.key to it.value } }
        } else {
            emptyMap()
        }

        // Adicionar dados dos produtos
        var rowIndex = 1
        for (p in list) {
            val row = sheet.createRow(rowIndex++)

            // Valores das categorias (todos os campos vêm dos atributos dinâmicos)
            val productAttrs = attributesMap[p.code] ?: emptyMap()
            for (i in categories.indices) {
                val categoryValue = productAttrs[categories[i]] ?: ""
                row.createCell(i).setCellValue(categoryValue)
            }
        }

        // Definir larguras das colunas
        for (i in categories.indices) {
            sheet.setColumnWidth(i, 3500)
        }

        file.outputStream().use { workbook.write(it) }
        workbook.close()
        file.absolutePath
    }

    override suspend fun exportXlsxToUri(uri: Uri): Result<Unit> = resultOf {
        val list = productDao.observeAll().first()
        val categories = categoryDao.observeAll().first().sortedBy { it.orderIndex }.map { it.originalName }  // Usar originalName ordenado por orderIndex

        val workbook = org.apache.poi.xssf.usermodel.XSSFWorkbook()
        val sheet = workbook.createSheet("Produtos")

        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerStyle.setFont(headerFont)

        // Usar APENAS as categorias do arquivo original, sem campos básicos predefinidos
        val allHeaders = categories

        val headerRow = sheet.createRow(0)
        for (i in allHeaders.indices) {
            val cell = headerRow.createCell(i)
            cell.setCellValue(allHeaders[i])
            cell.cellStyle = headerStyle
        }

        // Buscar todos os atributos de produtos para as categorias usando originalName
        val productCodes = list.map { it.code }
        val attributesMap = if (categories.isNotEmpty() && productCodes.isNotEmpty()) {
            productAttributeDao.findByProductsAndKeys(productCodes, categories)
                .groupBy { it.productCode }
                .mapValues { (_, attrs) -> attrs.associate { it.key to it.value } }
        } else {
            emptyMap()
        }

        // Adicionar dados dos produtos
        var rowIndex = 1
        for (p in list) {
            val row = sheet.createRow(rowIndex++)

            // Valores das categorias (todos os campos vêm dos atributos dinâmicos)
            val productAttrs = attributesMap[p.code] ?: emptyMap()
            for (i in categories.indices) {
                val categoryValue = productAttrs[categories[i]] ?: ""
                row.createCell(i).setCellValue(categoryValue)
            }
        }

        // Definir larguras das colunas
        for (i in categories.indices) {
            sheet.setColumnWidth(i, 3500)
        }

        // Escrever para o URI escolhido pelo usuário
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            workbook.write(outputStream)
        } ?: error("Não foi possível abrir o arquivo para escrita")

        workbook.close()
        Unit
    }
}

class InventoryRepositoryImpl(private val inventoryDao: InventoryDao): InventoryRepository {
    override fun observeInventories(): Flow<List<InventoryEntity>> = inventoryDao.observeAll()
    override suspend fun create(name: String, note: String?, userId: Long): Result<Long> = resultOf {
        inventoryDao.insert(InventoryEntity(name = name, note = note, createdByUserId = userId))
    }
    override suspend fun finalize(id: Long, userId: Long): Result<Unit> = resultOf {
        val now = Instant.now().toEpochMilli()
        inventoryDao.updateStatus(id, InventoryStatus.FINISHED, now)
        Unit
    }
    override suspend fun delete(id: Long, userId: Long): Result<Unit> = resultOf {
        // Simplificado: não implementado delete real (exigiria DAO extra) -> TODO
        Unit
    }
    override suspend fun getById(id: Long): InventoryEntity? = inventoryDao.getById(id)
}

class InventoryItemRepositoryImpl(private val inventoryItemDao: InventoryItemDao, private val productDao: ProductDao): InventoryItemRepository {
    override fun observeItems(inventoryId: Long): Flow<List<InventoryItemEntity>> = inventoryItemDao.observeItems(inventoryId)
    override suspend fun addItem(inventoryId: Long, productCode: String, qty: Double, userId: Long): Result<Long> = resultOf {
        val product = productDao.findByCode(productCode) ?: error("Produto não existe")
        inventoryItemDao.insert(InventoryItemEntity(inventoryId = inventoryId, productCode = product.code, qtyCounted = qty, countedByUserId = userId))
    }
}

class CsvExportRepositoryImpl(private val context: Context): CsvExportRepository {
    private fun baseExportDir(): File {
        val dir = File(context.filesDir, "inventario/export")
        dir.mkdirs()
        return dir
    }
    override suspend fun exportInventoryItems(inventory: InventoryEntity, items: List<InventoryItemEntity>): Result<String> = resultOf {
        val file = File(baseExportDir(), "inventario_${inventory.id}.csv")
        file.printWriter().use { out ->
            out.println("inventoryId;productCode;qtyCounted;countedAt")
            items.forEach { i -> out.println("${i.inventoryId};${i.productCode};${i.qtyCounted};${i.countedAt}") }
        }
        file.absolutePath
    }
}

class ConferenceRepositoryImpl: ConferenceRepository { // Stubs
    override suspend fun recount(inventoryItemId: Long, qty: Double, userId: Long): Result<Long> = Result.Error("TODO")
    override suspend fun markStatus(conferenceItemId: Long, status: ConferenceStatus): Result<Unit> = Result.Error("TODO")
    override suspend fun buildDivergenceReport(inventoryId: Long): Result<String> = Result.Error("TODO")
}

class PaymentRepositoryImpl: PaymentRepository { // Stubs
    override fun observeByInventory(inventoryId: Long): Flow<List<PaymentEntity>> = throw NotImplementedError()
    override suspend fun register(payment: PaymentEntity): Result<Long> = Result.Error("TODO")
}

class BackupRepositoryImpl(private val context: Context): BackupRepository { // Stubs
    override suspend fun backupDb(): Result<String> = Result.Error("TODO")
    override suspend fun restoreDb(path: String): Result<Unit> = Result.Error("TODO")
}

class SettingsRepositoryImpl(private val settingsDao: SettingsDao): SettingsRepository {
    override suspend fun get(): SettingsEntity = settingsDao.get() ?: SettingsEntity().also { settingsDao.save(it) }
    override suspend fun save(settings: SettingsEntity): Result<Unit> = resultOf { settingsDao.save(settings); Unit }
}

class ProductListLayoutRepositoryImpl(
    private val layoutDao: ProductListLayoutDao,
    private val categoryDao: CategoryDao
): ProductListLayoutRepository {

    override suspend fun get(): ProductListLayoutEntity =
        layoutDao.get() ?: ProductListLayoutEntity().also { layoutDao.save(it) }

    override suspend fun save(layout: ProductListLayoutEntity): Result<Unit> = resultOf {
        layoutDao.save(layout)
    }

    override suspend fun updateLine1Key(key: String): Result<Unit> = resultOf {
        layoutDao.updateLine1Key(key)
    }

    override suspend fun updateLine2Keys(keys: List<String>): Result<Unit> = resultOf {
        layoutDao.updateLine2Keys(keys.joinToString(","))
    }

    override suspend fun updateLine3Keys(keys: List<String>): Result<Unit> = resultOf {
        layoutDao.updateLine3Keys(keys.joinToString(","))
    }

    override suspend fun updateQtyKey(key: String): Result<Unit> = resultOf {
        layoutDao.updateQtyKey(key)
    }

    override suspend fun getAvailableKeys(): Result<List<String>> = resultOf {
        val categories = categoryDao.observeAll().first()
        categories.map { it.name }.distinct().sorted()
    }

    override suspend fun suggestDefaults(availableKeys: List<String>): ProductListLayoutEntity {
        val line1Key = when {
            availableKeys.contains("nome") -> "nome"
            availableKeys.contains("sku") -> "sku"
            availableKeys.contains("ean") -> "ean"
            else -> availableKeys.firstOrNull() ?: "nome"
        }

        val line2Keys = listOf("ean", "sku").filter { availableKeys.contains(it) }.take(2)
        val line3Keys = listOf("preco", "categoria").filter { availableKeys.contains(it) }.take(2)

        val qtyKey = when {
            availableKeys.contains("quantidade") -> "quantidade"
            availableKeys.contains("estoque") -> "estoque"
            availableKeys.contains("qtd") -> "qtd"
            else -> "quantidade"
        }

        return ProductListLayoutEntity(
            line1Key = line1Key,
            line2Keys = line2Keys.joinToString(","),
            line3Keys = line3Keys.joinToString(","),
            qtyKey = qtyKey
        )
    }
}
