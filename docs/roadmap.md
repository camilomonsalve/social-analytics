# Roadmap

## Sprint 0 — Base del proyecto ✅ (este commit)
- Estructura del repositorio (backend, frontend, docker, docs)
- Docker Compose con PostgreSQL
- `GET /api/v1/health`
- Swagger disponible en `/swagger-ui.html`
- Angular consumiendo `/health` y mostrando el estado de conexión

## Sprint 1 — Módulo Profile
- Migración `V1__create_profile_table.sql` (ya incluida)
- `CsvImporter` para `datos.csv` (https://apoyaronaabelardo.org/)
- `Profile` entity, repository, service, mapper, DTO
- `GET /api/v1/profiles`, `GET /api/v1/profiles/{id}`, `GET /api/v1/categories`
- Angular: lista y detalle de perfiles

## Sprint 2 — Sincronización automática
- `Scheduler` que descarga el CSV periódicamente
- Detección de cambios (hash por perfil) y actualización incremental

## Sprint 3 — Provider de Instagram
- `SocialAccount` entity + migración
- `InstagramProvider` (vía instrack.app inicialmente)
- `MetricSnapshot` entity + migración

## Sprint 4 — Históricos y analítica
- Cálculo de tasas de crecimiento, medias móviles, tendencias
- Angular: gráficas de evolución (Apache ECharts)

## Sprint 5 — Dashboard, rankings y comparación
- `GET /api/v1/rankings`, `GET /api/v1/compare`
- Angular: ranking, comparación entre perfiles, filtros
- Exportación de gráficas a PNG/PDF
