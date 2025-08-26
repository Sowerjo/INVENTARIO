package com.mobitech.inventario.domain.model

enum class UserRole { OPERATOR, CHECKER, SUPERVISOR }
enum class InventoryStatus { ONGOING, FINISHED, CHECKED }
enum class ConferenceStatus { CONFERIDO, DIVERGENTE, PENDENTE }
enum class PaymentMethod { PIX, CARTAO, DINHEIRO, OUTRO }
enum class BackupType { CSV_EXPORT, EXCEL_EXPORT, DB_BACKUP, DB_RESTORE }
enum class ConferenceMode { CEGA, ORIENTADA }

// Novos enums para configurações de inventário
enum class UpdateMode { OVERWRITE, DELTA }
enum class InputMode { ACCUMULATE, OVERWRITE }
