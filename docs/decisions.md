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

## ADR-008: GitHub Flow para gestión de branches
Se adopta GitHub Flow en lugar de GitFlow simplificado. El flujo es:

- **main**: rama principal, siempre desplegable
- **feature/***: branches temporales para nuevas funcionalidades
- Cada feature se mergea a main vía Pull Request
- No hay rama develop ni release branches

Ventajas para este proyecto:
- Equipo pequeño → menos overhead
- Despliegues continuos posibles
- Simplicidad: solo dos tipos de branches
- Encaja con sprints cortos y releases frecuentes

## ADR-009: Detección de cambios basada en hash por perfil
En lugar de comparar snapshots sucesivos del archivo CSV completo, cada perfil
almacena un `content_hash` (SHA-256 de sus campos clave: nombre, descripción,
foto, categoría). Al importar, se calcula el hash del perfil entrante y se
compara con el almacenado. Si coinciden, se trata como `UNCHANGED` y no se
escribe en la base de datos (sin `updated_at` bump).

Razones:
- Más simple: no requiere mantener copias de archivos CSV anteriores
- Sin estado compartido de filesystem: funciona bien si el sistema se escala
  horizontalmente
- Reutiliza el mismo camino de upsert tanto para importaciones manuales como
  programadas

## ADR-010: Tabla de auditoría `SyncJob` con propagación REQUIRES_NEW
La tabla `sync_job` registra el resultado de cada ejecución de sincronización
(created/updated/unchanged/failed counts, timing, errores). Los métodos
`startJob`, `finishJobSuccess` y `finishJobFailed` usan
`@Transactional(propagation = Propagation.REQUIRES_NEW)` para que cada escritura
de bookkeeping sea su propia transacción independiente, comprometida
inmediatamente.

Razón: asegura que el registro auditivo sobreviva independientemente del éxito
o fallo de la operación que está registrando. Sin esto, si algo fallara más
adelante en el método, el `SyncJob` podría hacer rollback junto con todo lo demás,
dejando sin rastro de que la sincronización se ejecutó.

## ADR-011: Manejo de errores por fila en sincronizaciones programadas
Para sincronizaciones programadas (no atendidas), cada fila del CSV se procesa
en su propia llamada a `upsertProfile` dentro de un try-catch. Si una fila falla,
se incrementa `profilesFailed` y se continúa con las demás. Esto es
intencionalmente diferente del importador manual de Sprint 1, que envuelve todo
el lote en una transacción y aborta en el primer error.

Razón: en un trabajo no atendido, una sola fila malformada no debería descartar
silenciosamente 499 actualizaciones válidas. El usuario humano que sube un archivo
manualmente quiere feedback inmediato ("algo está mal"), pero el scheduler
automático debe ser resiliente y procesar lo que pueda.

## ADR-012: Expansión del conjunto de categorías tras inspección de datos reales
El conjunto original de categorías (`artistas`, `empresas`, `medios`, `politica`) se
definió antes de inspeccionar el archivo CSV real en apoyaronaabelardo.org/datos.csv.
Al realizar QA manual contra el archivo de producción, se descubrieron tres
categorías adicionales legítimas: `partidos`, `lideres` y `sindicatos`. Estas
categorías son coherentes con el dominio del sitio (seguimiento de apoyo político:
partidos políticos, líderes individuales, sindicatos) y no son datos basura.

Acción tomada: se expandió `VALID_CATEGORIES` en `CsvImporter` para incluir las
tres categorías adicionales, y se actualizaron todos los documentos (architecture.md,
README.md) para reflejar el conjunto completo de siete categorías.

Lección: las suposiciones sobre el dominio deben validarse contra datos reales
antes de codificar restricciones. Este ADR documenta una corrección posterior a
la implementación inicial, no una decisión tomada a priori.

## ADR-013: Visibilidad de filas omitidas en sincronización
Originalmente, `CsvImporter.importFromCsv` devolvía solo las filas parseadas con
éxito (`List<Profile>`), filtrando silenciosamente las filas con `categoria`
inválida u otros errores de parsing. Esto significaba que una sincronización podía
reportar `profilesFailed: 0` mientras descartaba datos reales sin dejar rastro.

Acción tomada:
- Se introdujo `CsvImportResult` record con campos `profiles` y `skippedCount`
- `importFromCsv` ahora cuenta filas omitidas (nulls de parseRecord + excepciones)
- Se agregó columna `profiles_skipped_invalid` a tabla `sync_job`
- `SyncJobTracker.finishJobSuccess` acepta y persiste este contador
- `SyncJobResponse` DTO expone el campo para visibilidad en la API

Distinción semántica: `profilesFailed` se reserva para filas que parsearon
correctamente pero fallaron durante `upsertProfile` (error de persistencia).
`profilesSkippedInvalid` cuenta filas que no llegaron a parsearse (error de
datos de entrada). Esta distinción permite a operadores distinguir entre
problemas de calidad de datos vs. problemas de infraestructura.