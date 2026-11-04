# Contrato de la API REST — Lab 6 (IA, Cobertura, Telemetría y Visión)

Este documento describe el contrato exacto (rutas, privilegios, JSON de entrada/salida en `snake_case` o según DTO real, formato de imágenes y códigos de error) de los endpoints del Lab 6 consumidos por la aplicación Android.

Todas las respuestas exitosas usan la envoltura estándar `ApiResponse<T>`:
```json
{
  "data": { ... },
  "meta": { ... }
}
```
Todas las respuestas de error usan `ApiError`:
```json
{
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Descripción en español para el usuario",
  "timestamp": "2026-10-28T20:00:00Z",
  "path": "/api/v1/..."
}
```

---

## 1. Cobertura del Temario e IA-04 (`#43`)

Controlador: `CoverageController` (`/api/v1/courses/{courseId}/coverage`)
Privilegio requerido en los 3 endpoints: **`COVERAGE_READ_OWN`** (`ROLE_STUDENT` y `ROLE_TEACHER`).
Además, el usuario autenticado debe estar matriculado en el curso (`ACTIVE`) o ser el docente dueño; de lo contrario responde `404 Not Found`.

### 1.1 `GET /api/v1/courses/{courseId}/coverage`
Devuelve el estado actual de cobertura del temario para el usuario autenticado.
- Los temas padre (`parent_id == null` que tienen subtemas) traen `state: null` y su `score` es el promedio de sus subtemas hoja.
- Los temas hoja sin fila en `topic_coverage` se devuelven con `state: "ABSENT"`, `score: 0.0` y `updated_at: null`.
- `state_counts` siempre incluye las 4 claves: `SOLID`, `PARTIAL`, `ABSENT`, `UNCERTAIN`.

**Response `200 OK`:**
```json
{
  "data": {
    "course_id": 1,
    "coverage_pct": 43.8,
    "state_counts": {
      "SOLID": 1,
      "PARTIAL": 1,
      "ABSENT": 1,
      "UNCERTAIN": 1
    },
    "topics": [
      {
        "topic_id": 100,
        "parent_id": null,
        "code": "U1",
        "title": "Límites y continuidad",
        "state": null,
        "score": 0.4375,
        "updated_at": "2026-10-28T18:30:00Z"
      },
      {
        "topic_id": 101,
        "parent_id": 100,
        "code": "U1.1",
        "title": "Definición formal de límite",
        "state": "SOLID",
        "score": 0.9,
        "updated_at": "2026-10-28T18:30:00Z"
      },
      {
        "topic_id": 102,
        "parent_id": 100,
        "code": "U1.2",
        "title": "Límites laterales",
        "state": "PARTIAL",
        "score": 0.85,
        "updated_at": "2026-10-27T15:00:00Z"
      },
      {
        "topic_id": 103,
        "parent_id": 100,
        "code": "U1.3",
        "title": "Continuidad en un punto",
        "state": "UNCERTAIN",
        "score": 0.0,
        "updated_at": "2026-10-28T18:30:00Z"
      },
      {
        "topic_id": 104,
        "parent_id": 100,
        "code": "U1.4",
        "title": "Teorema del valor intermedio",
        "state": "ABSENT",
        "score": 0.0,
        "updated_at": null
      }
    ]
  },
  "meta": null
}
```

**Códigos de error:**
- `400 Bad Request` (`VALIDATION_FAILED` / `TYPE_MISMATCH`): `courseId <= 0` o no numérico.
- `401 Unauthorized` (`UNAUTHORIZED`): sin JWT o JWT expirado/inválido.
- `403 Forbidden` (`FORBIDDEN`): rol sin `COVERAGE_READ_OWN` (ej. `ROLE_ADMIN`).
- `404 Not Found` (`NOT_FOUND`): curso no existe o el usuario no pertenece al curso.

---

### 1.2 `POST /api/v1/courses/{courseId}/coverage/evaluate`
Ejecuta el pre-filtro determinista local sobre los apuntes `READY` (`notes.content`) del estudiante en el curso y envía únicamente los temas en zona gris (máximo 25 por lote) a IA-04 (`SemanticJudgeService`).
- No recibe body.
- Devuelve en `data` el mismo objeto `CoverageResponse` que `GET /coverage` y en `meta` el desglose de la evaluación.

**Response `200 OK`:**
```json
{
  "data": {
    "course_id": 1,
    "coverage_pct": 55.0,
    "state_counts": {
      "SOLID": 2,
      "PARTIAL": 0,
      "ABSENT": 1,
      "UNCERTAIN": 1
    },
    "topics": [
      {
        "topic_id": 101,
        "parent_id": 100,
        "code": "U1.1",
        "title": "Definición formal de límite",
        "state": "SOLID",
        "score": 0.9,
        "updated_at": "2026-10-28T18:35:00Z"
      }
    ]
  },
  "meta": {
    "prefilter_resolved": 3,
    "ai_adjudicated": 1,
    "uncertain": 1,
    "ai_called": true
  }
}
```

**Códigos de error:**
- `400 Bad Request`: `courseId <= 0`.
- `401 Unauthorized`: sin token o expirado.
- `403 Forbidden`: sin `COVERAGE_READ_OWN`.
- `404 Not Found`: curso inexistente o ajeno.
- `409 Conflict`:
  - `COVERAGE_EVALUATION_IN_PROGRESS`: ya hay una evaluación en curso para este usuario y curso.
  - `COVERAGE_EVALUATION_COOLDOWN`: se ejecutó una evaluación con IA hace menos de 1 minuto.
- `502 Bad Gateway` (`AI_PROVIDER_ERROR`): fallo del proveedor OpenAI al adjudicar zona gris.

---

### 1.3 `GET /api/v1/courses/{courseId}/coverage/delta?since={ISO-8601}`
Devuelve los temas hoja cuya cobertura cambió (`updated_at > since`).
- Parámetro query obligatorio: `since` en formato ISO-8601 UTC (ej. `2026-10-21T00:00:00Z`). No puede ser una fecha futura.

**Response `200 OK`:**
```json
{
  "data": {
    "course_id": 1,
    "since": "2026-10-21T00:00:00Z",
    "changed_counts": {
      "SOLID": 1,
      "PARTIAL": 1,
      "ABSENT": 0,
      "UNCERTAIN": 0
    },
    "changed_topics": [
      {
        "topic_id": 101,
        "parent_id": 100,
        "code": "U1.1",
        "title": "Definición formal de límite",
        "state": "SOLID",
        "score": 0.9,
        "updated_at": "2026-10-28T18:35:00Z"
      },
      {
        "topic_id": 102,
        "parent_id": 100,
        "code": "U1.2",
        "title": "Límites laterales",
        "state": "PARTIAL",
        "score": 0.72,
        "updated_at": "2026-10-27T15:00:00Z"
      }
    ]
  },
  "meta": null
}
```

**Códigos de error:**
- `400 Bad Request`: `since` ausente (`MISSING_PARAMETER`), formato inválido (`TYPE_MISMATCH`), o fecha en el futuro (`BAD_REQUEST`: `"La fecha 'since' no puede estar en el futuro"`).
- `401 Unauthorized`, `403 Forbidden`, `404 Not Found`.

---

## 2. Uso y Costo de IA (`#46`)

Controlador: `AiUsageController`
Solo devuelve totales agregados (nunca filas individuales de `ai_calls`).
- Un valor `null` en `input_tokens`, `output_tokens`, `estimated_cost_usd` o `average_latency_ms` significa **desconocido** (ninguna llamada informó ese dato o no había tarifa configurada), nunca `0`.
- `estimated_cost_usd` se envía como `String?` decimal con 8 decimales (ej. `"0.00035000"`) para no perder precisión.
- `call_type` puede ser: `OCR_M` (N1.5), `IA_00` (N2), `IA_01` (reconstrucción), `IA_02` (ítems de estudio), `IA_03` (explicación), `IA_04` (juez de cobertura).

### 2.1 `GET /api/v1/ai-usage/me?courseId={opcional}`
- Privilegio requerido: **`USAGE_READ_OWN`** (`ROLE_STUDENT` y `ROLE_TEACHER`).
- Query opcional: `courseId` (entero positivo). Si se omite, agrega todas las llamadas del usuario autenticado (`course_id: null`).

**Response `200 OK`:**
```json
{
  "data": {
    "scope": "USER",
    "course_id": null,
    "currency": "USD",
    "totals": {
      "total_calls": 4,
      "successful_calls": 3,
      "failed_calls": 1,
      "rejected_outputs": 0,
      "input_tokens": 2450,
      "output_tokens": 310,
      "unknown_token_calls": 1,
      "estimated_cost_usd": "0.00055350",
      "unknown_cost_calls": 1,
      "average_latency_ms": 620,
      "avoided_calls": 3
    },
    "by_call_type": [
      {
        "call_type": "OCR_M",
        "usage": {
          "total_calls": 1,
          "successful_calls": 1,
          "failed_calls": 0,
          "rejected_outputs": 0,
          "input_tokens": 600,
          "output_tokens": 20,
          "unknown_token_calls": 0,
          "estimated_cost_usd": "0.00010200",
          "unknown_cost_calls": 0,
          "average_latency_ms": 340,
          "avoided_calls": 2
        }
      },
      {
        "call_type": "IA_01",
        "usage": {
          "total_calls": 3,
          "successful_calls": 2,
          "failed_calls": 1,
          "rejected_outputs": 0,
          "input_tokens": 1850,
          "output_tokens": 290,
          "unknown_token_calls": 1,
          "estimated_cost_usd": "0.00045150",
          "unknown_cost_calls": 1,
          "average_latency_ms": 760,
          "avoided_calls": 1
        }
      }
    ]
  },
  "meta": null
}
```

**Códigos de error:**
- `400 Bad Request` (`VALIDATION_FAILED`): `courseId <= 0`.
- `401 Unauthorized`: sin token.
- `403 Forbidden`: sin `USAGE_READ_OWN` (ej. `ROLE_ADMIN`).

---

### 2.2 `GET /api/v1/courses/{courseId}/ai-usage`
- Privilegio requerido: **`USAGE_READ_COURSE`** (`ROLE_TEACHER` y `ROLE_ADMIN`), **y además el usuario autenticado debe ser el docente dueño del curso** (`course.owner.id == user.userId`).
- Devuelve la misma estructura `AiUsageResponse` con `"scope": "COURSE"` y `"course_id": <courseId>`.

**Códigos de error:**
- `400 Bad Request`: `courseId <= 0`.
- `401 Unauthorized`: sin token.
- `403 Forbidden`: estudiante sin `USAGE_READ_COURSE`, o usuario con `USAGE_READ_COURSE` que **no es el dueño** del curso.
- `404 Not Found`: el curso no existe.

---

## 3. OCR Matemático N1.5 (`#38`) y Reparación por Visión N2 (`#41`)

Controlador: `NoteController`
Privilegios requeridos en ambos endpoints: **`NOTE_WRITE_OWN` y `AI_VISION_INVOKE`** (`@PreAuthorize("hasAuthority('NOTE_WRITE_OWN') and hasAuthority('AI_VISION_INVOKE')")`).
Formatos de imagen aceptados (validados por Content-Type **y** firma binaria/magic bytes en `RegionCrop`):
- `image/png` (firma `89 50 4E 47 0D 0A 1A 0A`)
- `image/jpeg` (firma `FF D8 FF`)
- `image/webp` (firma `RIFF....WEBP`)
Límites de tamaño multipart (Spring Boot default):
- **Máximo por archivo/parte (`max-file-size`):** `1 MB` (`1,048,576` bytes).
- **Máximo total de la petición (`max-request-size`):** `10 MB`.
- **Máximo de regiones por llamada en `/repair`:** `8` recortes (`VisionRepairService.MAX_CROPS = 8`).
- **Máximo de intentos por región:** `3` intentos (`PageRegionStore.MAX_ATTEMPTS = 3`).

> **Nota sobre nombres de campos JSON en `#38` y `#41`:** A diferencia de `coverage` y `ai-usage`, los DTOs `MathOcrResponse`, `PageRepairResponse` y `RepairedRegion` serializan sus propiedades en **`camelCase`** (`pageIndex`, `regionId`, `latexCompiles`, `resolvedAt`, `escalationReasons`, `levelReached`, `isUncertain`) para coincidir directamente con las claves de `note_pages.regions` (JSONB).

### 3.1 `POST /api/v1/notes/{id}/pages/{pageIndex}/regions/{regionId}/math`
Envía el recorte de una región `MATH` a N1.5 (`VisionMathEngine`) y aplica la compuerta de validación `LatexValidator` + `FormulaEscalationPolicy` (`#39`).
- **Content-Type:** `multipart/form-data`
- **Parte multipart requerida:** nombre `"crop"` (`@RequestPart("crop") crop: MultipartFile`), con `Content-Type` `image/png`, `image/jpeg` o `image/webp`.
- Validaciones de ruta: `id > 0`, `pageIndex >= 0`, `regionId` de 1 a 64 caracteres (`[A-Za-z0-9_-]{1,64}`).

**Response `200 OK`:**
```json
{
  "data": {
    "pageIndex": 0,
    "regionId": "r2",
    "latex": "\\lim_{x \\to a} f(x) = L",
    "confidence": 0.92,
    "latexCompiles": true,
    "status": "KEEP_N1_5",
    "resolvedAt": "N1_5",
    "escalationReasons": [],
    "engine": "VisionMath",
    "cached": false
  },
  "meta": null
}
```
- `status`: `"KEEP_N1_5"` (aceptado y guardado en la región) o `"NEEDS_N2"` (no superó la compuerta; `resolvedAt` permanece `"UNRESOLVED"` y `escalationReasons` indica p. ej. `["LOW_CONFIDENCE"]` o `["LATEX_DOES_NOT_COMPILE"]`).
- Si la región ya estaba resuelta en `N1_5` con LaTeX guardado, devuelve `200 OK` con `"cached": true` sin llamar al motor (y registra una llamada evitada en `ai_avoided_calls`).

**Códigos de error:**
- `400 Bad Request`: recorte vacío, formato no soportado o firma binaria inválida, `regionId` inválido o repetido en la página.
- `401 Unauthorized`: sin JWT.
- `403 Forbidden`: falta `NOTE_WRITE_OWN` o `AI_VISION_INVOKE`.
- `404 Not Found`: apunte no existe o no pertenece al usuario, página no existe, o la región no existe / no es de tipo `MATH`.
- `409 Conflict`:
  - `REGION_ALREADY_RESOLVED`: la región ya fue resuelta en otro nivel (`N1` o `N2`).
  - `AI_ATTEMPTS_EXCEEDED`: la región ya alcanzó el límite de 3 intentos (`mathOcrAttempts >= 3`).
  - `RESOURCE_LOCKED`: otra petición está modificando la misma página.
- `413 Payload Too Large`: el recorte supera el límite multipart (1 MB).
- `502 Bad Gateway` (`AI_PROVIDER_ERROR`): fallo del proveedor OpenAI.
- `503 Service Unavailable` (`AI_NOT_CONFIGURED`): `OPENAI_API_KEY` no configurada en el servidor.

---

### 3.2 `POST /api/v1/notes/{id}/pages/{pageIndex}/repair`
Repara en **una sola llamada multimodal (N2 / IA-00)** hasta 8 regiones pendientes (`resolvedAt == "UNRESOLVED"`) de una misma página (`TEXT`, `MATH` o `DRAWING`).
- **Content-Type:** `multipart/form-data`
- **Partes multipart:** una parte de archivo por cada región a reparar, donde el **`name` de la parte es exactamente el `regionId`** (por ejemplo, parte `"r1"` con `r1.jpg`, parte `"r2"` con `r2.png`).
  - Mínimo 1 parte, máximo 8 partes (`MAX_CROPS = 8`).
  - Cada `regionId` debe enviarse una sola vez por petición.

**Response `200 OK`:**
```json
{
  "data": {
    "pageIndex": 0,
    "levelReached": "N2",
    "repaired": 2,
    "uncertain": 1,
    "regions": [
      {
        "regionId": "r1",
        "kind": "TEXT",
        "value": "para todo ε > 0 existe δ > 0",
        "confidence": 0.94,
        "resolvedAt": "N2",
        "isUncertain": false
      },
      {
        "regionId": "r2",
        "kind": "MATH",
        "value": "\\frac{a}{b}",
        "confidence": 0.91,
        "resolvedAt": "N2",
        "isUncertain": false
      },
      {
        "regionId": "r3",
        "kind": "TEXT",
        "value": null,
        "confidence": 0.35,
        "resolvedAt": "UNRESOLVED",
        "isUncertain": true
      }
    ]
  },
  "meta": null
}
```
- Si una región supera la compuerta (`confidence >= 0.75` y, si es `MATH`, compila con `LatexValidator`), `value` trae el texto/LaTeX/caption reparado, `resolvedAt` pasa a `"N2"` e `isUncertain` pasa a `false`.
- Si no supera la compuerta o es ilegible, `value` es `null`, `resolvedAt` sigue en `"UNRESOLVED"` e `isUncertain` es `true`.
- Si todas las regiones enviadas ya estaban resueltas (`N1`, `N1_5` o `N2`), devuelve `200 OK` con `repaired: 0` y su estado actual sin llamar a OpenAI (registrando 1 llamada evitada `IA_00` en `ai_avoided_calls`).

**Códigos de error:**
- `400 Bad Request`: sin partes, más de 8 partes, parte repetida para la misma región, `regionId` inexistente en la página, mezcla de regiones pendientes con regiones ya resueltas, o imagen vacía/inválida.
- `401 Unauthorized`: sin JWT.
- `403 Forbidden`: falta `NOTE_WRITE_OWN` o `AI_VISION_INVOKE`.
- `404 Not Found`: apunte o página no existe o no pertenece al usuario.
- `409 Conflict`: `AI_ATTEMPTS_EXCEEDED` (alguna región ya tiene `visionRepairAttempts >= 3`) o `RESOURCE_LOCKED`.
- `413 Payload Too Large`: tamaño multipart excedido.
- `502 Bad Gateway` (`AI_PROVIDER_ERROR`): fallo de OpenAI.
- `503 Service Unavailable` (`AI_NOT_CONFIGURED`): `OPENAI_API_KEY` no configurada.
