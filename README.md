# Social Analytics

Plataforma que visualiza la evolución de perfiles sociales (artistas, empresas,
medios, política) a partir de datos normalizados, independientemente de la
plataforma de origen.

- **backend/** — Spring Boot 3 (Java 21). Sincroniza un CSV fuente, consulta
  proveedores de redes sociales y expone una API REST versionada.
- **frontend/** — Angular. Consume la API y construye gráficas; no conoce
  ninguna red social ni el CSV directamente.
- **docker/**, **docker-compose.yml** — PostgreSQL + pgAdmin para desarrollo local.
- **docs/** — arquitectura, modelo de base de datos, contrato de API y decisiones (ADRs).

## Levantar el entorno (Sprint 0)

1. Base de datos:
   ```bash
   docker compose up -d
   ```

2. Backend (desde `backend/`):
   ```bash
   ./mvnw spring-boot:run
   ```
   Verifica: http://localhost:8080/api/v1/health → `{"status":"UP"}`
   Swagger: http://localhost:8080/swagger-ui.html

3. Frontend (desde `frontend/`):
   ```bash
   npm install
   npm start
   ```
   Abre http://localhost:4200 — debería mostrar "✅ Backend conectado".

## Estado

Sprint 0 completado cuando los tres pasos anteriores funcionan de punta a punta.
El siguiente paso es el módulo `profile`: importar `datos.csv` y exponer
`GET /api/v1/profiles`.
