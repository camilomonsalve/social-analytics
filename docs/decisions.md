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

## ADR-006: Spring Boot 4.1 sobre Java 25
Se actualizó de Spring Boot 3.3 / Java 21 a Spring Boot 4.1 (basado en Spring
Framework 7) sobre Java 25, antes de que existiera código de dominio que migrar.
Requiere `springdoc-openapi` 3.x (la línea 2.x apunta a Spring Boot 3) y Lombok
1.18.42+ (soporte de JDK 25).

## ADR-007: Angular 22, Node.js 24 LTS y PostgreSQL 18
En la misma línea que ADR-006, se actualizó el resto del stack a sus versiones
estables más recientes antes de escribir código de dominio:

- **Angular 22** (requiere TypeScript 6.0+ y Node 22.12+/24+) en lugar de
  Angular 20.
- **Node.js 24**, la línea LTS activa, en lugar de Node 16 (ya sin soporte).
- **PostgreSQL 18** en lugar de 16. Desde la versión 18, la imagen oficial de
  Docker espera un único mount en `/var/lib/postgresql` (ya no en
  `/var/lib/postgresql/data`), para soportar `pg_upgrade --link`.

Consecuencia en el código: Angular 22 cambia el valor por defecto de
`changeDetection` a `OnPush` para componentes sin estrategia explícita. Los
componentes que actualizan estado dentro de callbacks (por ejemplo, un
`subscribe()` de HTTP) deben usar `signal()` en lugar de propiedades planas
para que la vista se actualice correctamente.