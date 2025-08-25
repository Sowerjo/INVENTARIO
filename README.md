# Inventario (Offline)

App Android (Compose + Room + Hilt) para gestão de inventários offline com exportação CSV local.

## Principais Recursos (MVP atual)
- Login offline (seed: admin / admin123)
- Cadastro básico de produtos (estrutura pronta - UI futura)
- Criação de inventário e adição de itens (estrutura inicial de casos de uso e repositórios)
- Exportação CSV (itens de inventário) para diretório interno
- Arquitetura Clean (domain/data/ui) + MVVM + Hilt
- Room Database com seed inicial (usuário admin + settings)

## Estrutura de Pastas
```
app/src/main/java/com/mobitech/inventario/
  data/ (Room, repositórios impl)
  domain/ (modelos, repositórios interfaces, use cases)
  ui/ (Compose, navegação, viewmodels)
  di/ (módulos Hilt)
```

## Credenciais Seed
```
Usuário: admin
Senha:   admin123
Perfil:  SUPERVISOR
```

## Requisitos de Build
- JDK 17
- Android Studio Iguana ou superior

## Rodando
```
./gradlew assembleDebug
```
Instalar APK localizado em `app/build/outputs/apk/debug/`.

## Próximos Passos (TODO)
- Implementar telas de Produtos, Inventário detalhado, Conferência
- Marcar status de conferência e relatório de divergências
- Exportação avançada (pagamentos, divergências)
- Importação CSV/XML de produtos
- Backup / Restore via SAF
- Scanner de código de barras (feature flag)
- Criptografia de banco (opcional)

## Exportação CSV
Arquivo gerado: `files/exports/inventario_<id>.csv` no storage interno da aplicação.

## Testes
Exemplo inicial em `LoginUseCaseTest`.

## Licença
Uso interno / educacional. Ajustar conforme necessidade.

