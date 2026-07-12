# Architecture Decision Records

## ADR-001: PostgreSQL como base de datos
Permite relaciones complejas y consultas analíticas sobre históricos de métricas.

## ADR-002: Monolito modular en lugar de microservicios desde el día 0
El dominio del proyecto (perfiles sociales + sus métricas) es único. Empezar
como monolito modular, organizado por dominio, simplifica el desarrollo inicial
y no impide extraer módulos a microservicios más adelante si hace falta escalar
alguno de forma independiente.

## ADR-003: Angular no conoce ninguna red social ni el CSV
El frontend solo consume el modelo unificado que expone la API REST. Esto
permite añadir nuevas plataformas (Threads, Bluesky, etc.) sin tocar el frontend.

## ADR-004: UUID como identificador de entidades
Más seguros de exponer públicamente, más fáciles de fusionar entre entornos, y
mejor preparados si el sistema se divide en varios servicios en el futuro.

## ADR-005: Flyway para versionar el esquema
Cada cambio de base de datos queda registrado como una migración incremental
(`V1__...`, `V2__...`), nunca se modifican migraciones ya aplicadas.
