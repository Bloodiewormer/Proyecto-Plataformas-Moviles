# Cómo contribuir

## Ramas y pull requests

- `main` siempre compila y es lo que despliega Render.
- Cada cambio va en su propia rama (`feat/...`, `fix/...`, `docs/...`, `chore/...`) y entra a `main` por pull request.
- Un PR que toca el backend y la app a la vez debe poder desplegarse en orden: primero la migración de base, después el backend y al final el APK (ver [la guía de despliegue](docs/Guia_Despliegue.md#53-orden-cuando-cambian-el-backend-y-la-app-a-la-vez)).

## Commits

[Conventional Commits](https://www.conventionalcommits.org/), en inglés:

```
<tipo>(<alcance>): <descripción corta>

tipo: feat | fix | docs | refactor | test | chore
alcance: android | backend | coverage | sync | auth | ...
```

Ejemplos: `feat(android): bottom navigation by role`, `fix(backend): return page_count in the note list`.

## Antes de abrir un PR

- **Backend:** `cd backend && ./gradlew test` contra la base `glifo_test` (ver la [guía](docs/Guia_Despliegue.md#23-pruebas)).
- **Android:** `cd android && ./gradlew :app:testDebugUnitTest`.
- **Cambios en una entidad JPA:** agregar el script `.psql` incremental en `backend/docs/db/`. El perfil `cloud` valida el esquema y no arranca sin la migración.
- **Cambios en una entidad Room:** subir la versión de `GlifoDatabase` y agregar la `Migration`.
- **Pipeline de IA:** se respeta el principio de que la IA no debe hacer todo. Primero se procesa en el teléfono, la llamada a IA es el último recurso y su resultado se guarda.
- **Decisiones de diseño:** se registran en `docs/Glifo_Bitacora_Decisiones.md`. Si una reemplaza a otra, la anterior se archiva, no se borra.

## Secretos

Nunca se suben claves, contraseñas, keystores ni archivos `.env`. Los valores de producción viven en Render y en el gestor de contraseñas del mantenedor. Las plantillas (`backend/.env.example`, `backend/render.env.example`) solo llevan los nombres de las variables.
