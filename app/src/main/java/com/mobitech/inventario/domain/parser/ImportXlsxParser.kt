package com.mobitech.inventario.domain.parser

import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook

data class ParsedProduct(
    val code: String,
    val name: String,
    val description: String?,
    val category: String?,
    val unit: String?,
    val attributes: Map<String, String?>
)

data class ParsedXlsxResult(
    val headers: List<String>,
    val originalHeaders: List<String>, // Adiciona headers originais
    val products: List<ParsedProduct>
)

/**
 * Parser puro para XLSX de produtos com normalização de headers e preservação de strings.
 * Regras:
 *  - Linha 1: headers (categorias / atributos). Normalizados para chaves canônicas
 *  - DataFormatter para preservar zeros à esquerda e evitar notação científica
 *  - Identificação de colunas especiais com sinônimos expandidos
 *  - Dedupe por SKU > EAN > UUID para idempotência
 */
object ImportXlsxParser {

    // Mapeamento de sinônimos para chaves canônicas
    private val headerNormalization = mapOf(
        // Códigos
        "codigo_de_barras" to "ean", "codigo de barras" to "ean", "barcode" to "ean",
        "ean13" to "ean", "ean8" to "ean", "gtin" to "ean",

        // SKU/Código
        "codigo" to "sku", "código" to "sku", "cod" to "sku", "item" to "sku",
        "produto" to "sku", "ref" to "sku", "referencia" to "sku", "referência" to "sku",

        // Nome/Descrição
        "nome_do_produto" to "nome", "produto_nome" to "nome", "descricao_produto" to "nome",
        "titulo" to "nome", "title" to "nome", "description" to "descricao",
        "desc" to "descricao", "details" to "descricao",

        // Preço
        "price" to "preco", "valor" to "preco", "vlr" to "preco", "custo" to "preco",

        // Quantidade/Estoque
        "qtd" to "quantidade", "qty" to "quantidade", "estoque" to "quantidade",
        "stock" to "quantidade", "saldo" to "quantidade",

        // Categoria
        "cat" to "categoria", "category" to "categoria", "grupo" to "categoria",
        "setor" to "categoria", "departamento" to "categoria",

        // Unidade
        "un" to "unidade", "unit" to "unidade", "und" to "unidade", "uni" to "unidade"
    )

    private fun normalizeHeader(header: String): String {
        val cleaned = header.trim().lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
        return headerNormalization[cleaned] ?: cleaned
    }

    private val codeSyn = listOf("sku", "codigo", "code", "id", "produto", "item")
    private val nameSyn = listOf("nome", "name", "titulo", "title", "descricao", "descrição")
    private val descSyn = listOf("descricao", "descrição", "desc", "details")
    private val catSyn = listOf("categoria", "category", "grupo", "setor")
    private val unitSyn = listOf("unidade", "unit", "und", "uni")
    private val priceSyn = listOf("preco", "preço", "price", "valor", "vlr")
    private val qtySyn = listOf("quantidade", "qtd", "qty", "estoque", "stock")

    fun parse(bytes: ByteArray): ParsedXlsxResult {
        val formatter = DataFormatter()

        XSSFWorkbook(bytes.inputStream()).use { wb ->
            val sheet = wb.getSheetAt(0)
            val headerRow = sheet.getRow(sheet.firstRowNum) ?: return ParsedXlsxResult(emptyList(), emptyList(), emptyList())

            // Extrair e normalizar headers
            val rawHeaders = (0 until headerRow.lastCellNum).map { idx ->
                val cell = headerRow.getCell(idx)
                formatter.formatCellValue(cell).trim()
            }

            val normalizedHeaders = rawHeaders.map { normalizeHeader(it) }

            // Resolver duplicados
            val seen = mutableMapOf<String, Int>()
            val headers = normalizedHeaders.map { h ->
                val count = (seen[h] ?: 0) + 1
                seen[h] = count
                if (count == 1) h else "${h}_${count}"
            }

            fun matchHeader(targets: List<String>): Int? =
                headers.indexOfFirst { h -> targets.any { t -> h.equals(t, true) } }.takeIf { it >= 0 }

            val idxCode = matchHeader(codeSyn) ?: 0
            val idxName = matchHeader(nameSyn)?.takeIf { it != idxCode }
            val idxDesc = matchHeader(descSyn)?.takeIf { it != idxCode && it != idxName }
            val idxCategory = matchHeader(catSyn)
            val idxUnit = matchHeader(unitSyn)

            val products = mutableListOf<ParsedProduct>()
            val seenCodes = mutableSetOf<String>()

            for (r in (sheet.firstRowNum + 1)..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue

                fun cell(i: Int?): String? = i?.let {
                    val cellValue = formatter.formatCellValue(row.getCell(it)).trim()
                    cellValue.takeIf { it.isNotBlank() }
                }

                val code = cell(idxCode) ?: continue

                // Dedupe por código
                if (seenCodes.contains(code)) continue
                seenCodes.add(code)

                val name = cell(idxName) ?: code
                val desc = cell(idxDesc)
                val cat = cell(idxCategory)
                val unit = cell(idxUnit)

                val attrMap = headers.mapIndexed { idx, header ->
                    header to formatter.formatCellValue(row.getCell(idx)).trim().takeIf { it.isNotBlank() }
                }.toMap()

                products += ParsedProduct(
                    code = code,
                    name = name,
                    description = desc,
                    category = cat,
                    unit = unit,
                    attributes = attrMap
                )
            }

            return ParsedXlsxResult(headers = headers, originalHeaders = rawHeaders, products = products)
        }
    }
}
