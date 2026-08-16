# Guía de contribución — Grupo X-Ray

## Flujo de ramas

- `main` — siempre estable, es lo que se defiende en clase.
- `feature/<frente>-<descripcion>` — una rama por tarea, ej: `feature/android-camerax-captura`, `feature/backend-auth-jwt`, `feature/cv-preprocesamiento-opencv`.
- Nada se hace commit directo a `main`. Todo entra vía Pull Request, aunque sea auto-aprobado por el mismo equipo (deja registro de qué se hizo y cuándo).

## Convención de commits

```
<tipo>(<frente>): <descripción corta>

tipo: feat | fix | docs | refactor | test | chore
frente: android | backend | cv | docs
```

Ejemplos:
- `feat(android): captura de foto con CameraX`
- `feat(backend): endpoint de creación de nota estructurada`
- `docs(arquitectura): resuelve conflicto de tokens escalated/alert`

## Asignación de tareas

Se usan **GitHub Issues** con las plantillas en `.github/ISSUE_TEMPLATE/`. Cada tarea nueva:

1. Se crea como Issue usando la plantilla correspondiente (`Tarea Android`, `Tarea Backend`, `Tarea CV/Pipeline`, `Tarea Documentación`).
2. Se asigna a la persona responsable (`Assignees`).
3. Se etiqueta con la tanda/lab correspondiente si aplica (ej. `lab-3`).
4. Se referencia en el PR que la resuelve con `Closes #<numero>`.

Opcional: usar **GitHub Projects** (tablero Kanban del repo) para visualizar To Do / In Progress / Done por frente.

## Antes de un PR

- Verificar que el código compila (Android Studio / IntelliJ) sin warnings nuevos.
- Si se toca el pipeline de IA, confirmar que se respeta el principio "la IA no debe hacer todo": procesamiento local primero, llamada a IA es último recurso, resultado persistido.
- Si se toca el esquema de base de datos, validar con `@dbml/core` y actualizar `docs/Glifo_Diseno_Arquitectura.md` si aplica.
- Actualizar `docs/Glifo_Bitacora_Decisiones.md` si la tarea implica una decisión de diseño nueva o cambia una existente (archivar el valor anterior, no borrarlo).

## Documentos de referencia obligatorios

Antes de implementar UI o estilos, revisar `docs/Glifo_Arquitectura_Estandares.md` (especialmente §4, §4.1, §4.3) para tokens de color, tipografía y componentes. El prototipo (`prototype/`) tiene prioridad sobre la documentación cuando divergen — si eso pasa, se actualiza el documento, no el prototipo.
