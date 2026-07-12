# Arquitectura

```
                         +----------------------+
                         |     Angular App      |
                         +----------+-----------+
                                    |
                                 REST API
                                    |
                    +---------------+---------------+
                    |        Spring Boot API        |
                    +---------------+---------------+
                                    |
      +---------------+-------------+--------------+----------------+
      |               |                            |                |
 Profile Module   Analytics Module          Synchronization    Provider Module
                                             Module
      |                                        |                |
      |                              +---------+--------+       |
      |                              |                  |       |
      |                         Importers          Schedulers   |
      |                              |                  |       |
      |                     CSV / JSON / API            |       |
      |                                                 |       |
      +-------------------------+-----------------------+-------+
                                |
                          PostgreSQL
```

## Principios

- **Monolito modular** organizado por dominio (`profile`, `analytics`,
  `synchronization`, `provider`, `common`, `configuration`), no por capas técnicas.
- **El frontend nunca conoce** el CSV ni ninguna red social directamente. Solo
  consume la API REST versionada (`/api/v1/...`) y renderiza.
- **Los providers están completamente desacoplados.** Cada uno solo conoce su
  plataforma e implementa una interfaz `SocialProvider` común. Ningún provider
  accede directamente al repositorio; siempre pasan por `SynchronizationService`.
- **Los importadores son intercambiables** (`Importer<T>`): CSV hoy, podría ser
  JSON o una API mañana sin tocar el resto del sistema.
- **DTOs separados de entidades JPA.** Nunca se expone una entidad directamente
  en la API.
- **IDs como UUID**, no enteros autoincrementales.
- **Analytics calcula**, no solo reenvía: tasas de crecimiento, medias móviles,
  tendencias. Angular únicamente pinta lo que la API le devuelve.

## Flujo de datos

```
CSV → Importer → Profile → SocialAccount → Synchronization → Provider →
MetricSnapshot → Analytics → REST API → Angular
```

## Modelo de dominio (visión general)

- **Profile**: entidad observada (persona, empresa, medio, partido). Categorías:
  `artistas`, `empresas`, `medios`, `politica`.
- **SocialAccount**: cuenta de una red social asociada a un `Profile`.
- **MetricSnapshot**: fotografía de métricas de una `SocialAccount` en un instante,
  usada para construir históricos y gráficas sin depender de que la red social
  externa conserve historial.
