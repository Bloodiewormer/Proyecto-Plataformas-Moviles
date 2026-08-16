# Glifo

## Documento de diseño y arquitectura

**Grupo X-Ray** — Brandon Brenes · David González · Felipe Ugalde
Desarrollo de Aplicaciones Móviles · II Ciclo 2026
Documento previo a la implementación · versión 1.0

---

## 0. Identidad

**Glifo.** Un glifo es la unidad mínima de la escritura: el signo individual trazado a mano. Es exactamente aquello que la aplicación lee, evalúa y decide si comprendió o no.

La identidad visual se construye sobre el **grifo mitológico** —cabeza de águila, cuerpo de león—, que aporta las dos ideas del producto: vista aguda para leer lo que cuesta leer, y guarda del contenido del estudiante.

> El glifo es la unidad de la escritura. El grifo, su guardián.

La paleta parte de un **azul pizarra con acento dorado**, tomados del cuerpo y del carácter heráldico del grifo, y se aparta deliberadamente de los tonos por defecto que generan las herramientas de diseño asistido. Se define en **dos modos completos, noche y día**: ambos declaran el mismo conjunto de tokens y solo cambian los valores, de modo que ningún componente necesita conocer el modo activo.

El dorado es el color de la acción y del estado activo, no un adorno de marca: si un elemento es dorado, se puede tocar. La identidad de marca descansa en el logotipo.

Los estados del mapa de confianza —verificado, reparado, escalado, incierto— se codifican con **color, forma y etiqueta textual simultáneamente**, de modo que la información siga siendo legible para personas con deficiencias en la percepción del color. Los valores completos están en `Glifo_Arquitectura_Estandares.md` §12.2 y §12.3.

---

## 1. El problema

El ciclo del enunciado —apuntes → conocimiento → brechas contra el temario → estudio dirigido— depende por completo de un primer paso: que la fotografía de un apunte manuscrito se convierta de forma fiel en contenido estructurado.

Ese primer paso es el punto frágil del proyecto, y el propio enunciado lo identifica como su riesgo principal. La mitigación planteada originalmente es una pantalla donde el estudiante corrige la transcripción antes de que la IA compare.

Nuestra observación es que esa mitigación no escala. En cursos como Cálculo, Álgebra Lineal o Física, el apunte real contiene integrales, matrices, subíndices, límites y diagramas. Un OCR de texto convencional no reconoce estructura matemática: devuelve una cadena de caracteres sin relación con lo escrito. Pedirle al estudiante que corrija eso a mano equivale a pedirle que retranscriba la página completa.

El resultado, si no se resuelve, es un apunte reconstruido que **parece** correcto, y flashcards y quizzes generados a partir de ese error, sin que el estudiante se entere.

---

## 2. Propuesta de valor

> Glifo digitaliza apuntes manuscritos —fórmulas y diagramas incluidos— escalando el procesamiento solo cuando hace falta, y muestra al estudiante qué entendió con certeza, qué tuvo que reparar y qué necesita revisión.

Tres compromisos concretos que se traducen en decisiones de arquitectura:

| Compromiso | Cómo se implementa |
|---|---|
| **Nada se inventa en silencio** | Cada fragmento del apunte lleva un nivel de confianza calculado y visible |
| **Ninguna foto se queda sin procesar** | Escalamiento automático al siguiente motor cuando el actual no alcanza el umbral |
| **El costo es una decisión de diseño** | Cada nivel del pipeline tiene un costo conocido; se registra qué nivel resolvió cada región |

---

## 3. Principios de diseño

1. **La IA no debe hacer todo.** Se maximiza el procesamiento local y determinista antes de invocar cualquier modelo. Preprocesamiento, OCR de texto, segmentación, cálculo de confianza, comparación de cobertura, corrección de quizzes, repetición espaciada y progreso no usan IA.
2. **Toda llamada de IA produce un resultado persistente y reutilizable.** Nunca se recalcula con IA lo que ya fue guardado.
3. **Nunca una llamada por ítem.** La generación de flashcards y quizzes, y la reparación de regiones de una misma página, viajan siempre en lote.
4. **Contexto mínimo.** El temario se procesa una sola vez a estructura persistente y nunca se reenvía completo a un modelo; solo se envía el fragmento relevante a cada llamada.
5. **La incertidumbre se muestra, no se oculta.** Si el sistema no pudo leer algo, lo marca; no lo completa.

---

## 4. Roles y permisos

| Rol | Responsabilidades |
|---|---|
| **Estudiante** | Capturar apuntes, revisar y corregir la reconstrucción, estudiar, consultar su cobertura y su consumo |
| **Docente** | Crear cursos, publicar el temario oficial, mantener el **glosario de notación canónica** del curso, consultar las brechas agregadas del grupo |
| **Administrador** | Gestión de usuarios, roles y privilegios; estado del sistema |

El rol docente no es un panel de consulta añadido para cumplir un requisito: **el glosario de notación canónica que mantiene alimenta directamente el motor de reconocimiento**, elevando la confianza sobre los símbolos propios de cada materia. Es una entrada funcional al pipeline.

Estructura de acceso: `users` · `roles` · `privileges` · `user_roles` · `role_privileges`.

---

## 5. Flujo principal

```text
DOCENTE
  crea curso → publica temario (PDF) → define glosario de notación
  → procesado una sola vez → persistido

ESTUDIANTE
  se une al curso mediante código
        │
        ▼
  CAPTURA (CameraX) → hash perceptual
        │            └─ página ya procesada → se reutiliza, sin llamadas
        ▼
  MOTOR DE INGESTA  (ver sección 6)
        │
        ▼
  REVISIÓN CON MAPA DE CONFIANZA
     └─ las correcciones del estudiante alimentan el glosario del curso
        │
        ▼
  COBERTURA CONTRA EL TEMARIO
     prefiltro local resuelve la mayoría de los temas
     └─ solo los casos ambiguos se envían a adjudicación semántica
        │
        ▼
  ESTADOS POR TEMA  ·  sólido / parcial / ausente / dudoso
  DELTA DE PROGRESO ·  variación de cobertura entre sesiones
        │
        ▼
  GENERACIÓN DE REFUERZO  ·  flashcards y quiz de los vacíos, en lote
        │
        ▼
  EVALUACIÓN LOCAL → repetición espaciada determinista
```

---

## 6. Motor de ingesta: escalera ramificada

Es el componente central del proyecto y la principal decisión de arquitectura.

```text
  ┌─────────────────────────────────────────────────────────┐
  │ N0  PREPROCESAMIENTO LOCAL (OpenCV)                     │
  │     corrección de perspectiva · deskew · contraste      │
  │     métricas de calidad: desenfoque, iluminación,       │
  │     reflejos                                            │
  │     SEGMENTACIÓN DE LA PÁGINA EN REGIONES               │
  │     └─ imagen irrecuperable → solicitar nueva captura   │
  │        indicando el motivo concreto                     │
  └───────────────────────────┬─────────────────────────────┘
                              ▼
  ┌─────────────────────────────────────────────────────────┐
  │ N1  OCR DE TEXTO LOCAL (ML Kit)                         │
  │     se ejecuta sobre TODAS las regiones                 │
  │     costo cero, sin conexión                            │
  └───────────────────────────┬─────────────────────────────┘
                              ▼
  ┌─────────────────────────────────────────────────────────┐
  │     CLASIFICADOR DE REGIÓN                              │
  │     combina geometría (densidad de trazos, relación de  │
  │     aspecto, líneas horizontales) con el comportamiento │
  │     de N1 sobre esa región                              │
  └───┬─────────────────┬───────────────────┬───────────────┘
      │                 │                   │
   TEXTO           MATEMÁTICA            DIBUJO
   resuelto            ▼                   │
   en N1     ┌──────────────────┐          │
             │ N1.5 OCR MATE-   │          │
             │ MÁTICO → LaTeX   │          │
             │ + confianza      │          │
             └────────┬─────────┘          │
                      │                    │
      ┌───────────────┴────────────────────┘
      ▼
  ┌─────────────────────────────────────────────────────────┐
  │     COMPUERTAS DE VALIDACIÓN (deterministas, locales)   │
  │     · confianza por debajo del umbral                   │
  │     · LaTeX que no compila (JLaTeXMath)                 │
  │     · diccionario y glosario del curso                  │
  └───────────────────────────┬─────────────────────────────┘
                              ▼
  ┌─────────────────────────────────────────────────────────┐
  │ N2  REPARACIÓN SELECTIVA POR VISIÓN                     │
  │     solo las regiones que no pasaron las compuertas,    │
  │     más los dibujos, RECORTADAS y EN UNA SOLA LLAMADA   │
  │     por página                                          │
  └───────────────────────────┬─────────────────────────────┘
                              ▼
  ┌─────────────────────────────────────────────────────────┐
  │ N3  PÁGINA COMPLETA A VISIÓN                            │
  │     último recurso, a solicitud del estudiante          │
  └───────────────────────────┬─────────────────────────────┘
                              ▼
       Lo que sigue sin resolverse se marca como INCIERTO.
                  No se completa por inferencia.
```

### Decisiones y su justificación

**El clasificador se ejecuta después de N1, no antes.** ML Kit es local y gratuito, así que conviene ejecutarlo sobre todas las regiones y usar su comportamiento como señal de clasificación: texto coherente con alta confianza indica texto; salida fragmentada con símbolos sueltos indica matemática; ausencia de salida indica dibujo. Clasificar únicamente por geometría es frágil; combinarlo con la reacción del OCR es más robusto y no añade costo.

**El escalamiento es por región, no por página.** Solo se envía a un modelo de visión el recorte que lo necesita, no la fotografía completa. Un recorte de una fórmula representa aproximadamente un orden de magnitud menos de datos que una página completa.

**Las regiones que escalan viajan en una sola llamada.** Fórmulas no resueltas y dibujos de una misma página se agrupan, cada uno con su instrucción específica.

**La imagen original nunca se descarta.** El recorte de cada región de fórmula se conserva junto al LaTeX generado. Si la transcripción es dudosa, el estudiante ve el original.

---

## 7. Motores de reconocimiento evaluados

| Motor | Tipo | Salida | Costo | Estado |
|---|---|---|---|---|
| **OpenCV** | Local | Imagen normalizada + regiones | Ninguno | Adoptado — N0 |
| **ML Kit Text Recognition** | Local, en dispositivo | Texto | Ninguno | Adoptado — N1 |
| **SimpleTex API** | Servicio externo | LaTeX + confianza numérica | Capa gratuita permanente | Adoptado — N1.5 |
| **Modelo de visión (LLM)** | Servicio externo | Texto / LaTeX / descripción | Por tokens | Adoptado — N2, N3 |
| **Mathpix** | Servicio externo | LaTeX | De pago | Descartado |
| **Tesseract** | Local | Texto | Ninguno | Descartado: débil en manuscrito |
| **ML Kit Digital Ink** | Local | Texto | Ninguno | Descartado: requiere trazos en pantalla, no fotografías |
| **pix2tex / LaTeX-OCR** | Autohospedado | LaTeX | Infraestructura propia | **Consulta abierta — sección 8** |

**Criterio de selección de N1.5.** Se priorizó que el motor entregue una **señal numérica de confianza derivada del modelo**, no autorreportada. SimpleTex expone un campo de confianza por respuesta, lo que permite decidir de forma automática si una región se acepta o escala. Un modelo generalista de visión puede describir por qué falló, pero su autoevaluación no es una medida calibrada.

**Diseño para el reemplazo.** N1.5 se define como una interfaz (`MathOcrEngine`) con implementaciones intercambiables. El motor concreto es un detalle de configuración, no una dependencia estructural. Esto permite sustituirlo si la latencia, la disponibilidad o la precisión medida no resultan aceptables.

---

## 8. Consulta al profesor: pix2tex autohospedado

Esta es la decisión que el equipo quiere plantear antes de cerrar la arquitectura.

**La propuesta.** Desplegar **pix2tex / LaTeX-OCR** como microservicio REST propio y consumirlo desde el backend como una implementación más de `MathOcrEngine`, en lugar de —o además de— un servicio externo.

**Argumentos a favor**
- Independencia total de proveedores externos: sin cuotas diarias, sin riesgo de cambio de términos, sin dependencia de la disponibilidad de un tercero.
- Los apuntes de los estudiantes no salen de infraestructura propia.
- El componente sería enteramente del equipo, con control sobre el modelo y sus umbrales.
- Sin costo por llamada.

**Argumentos en contra**
- Introduce un **segundo despliegue** en un stack que hasta ahora es un monolito Spring Boot.
- La variante estándar con PyTorch consume entre 600 MB y 1 GB de memoria, por encima del límite de las capas gratuitas de hosting. La variante **ONNX** reduce el consumo a un rango que sí encaja, y sería la que se propondría.
- **Arranque en frío:** las capas gratuitas suspenden el servicio por inactividad, y la carga inicial del modelo toma decenas de segundos. Es un riesgo directo para una demostración en vivo.
- Precisión reportada del orden del 85 % sobre manuscrito prolijo en papel blanco, pero entre 60 % y 70 % sobre fotografías tomadas rápidamente y con iluminación deficiente —que es precisamente el insumo real del proyecto.
- El modelo está diseñado para expresiones matemáticas aisladas y tiende a producir salida inventada cuando recibe una región que no lo es, lo que entra en conflicto con el principio de no completar lo ilegible.

**Preguntas concretas**

1. ¿Un microservicio Python independiente del monolito Spring Boot es admisible para el Laboratorio 6, o contradice el requisito de arquitectura monolítica?
2. ¿Autohospedar un modelo propio aporta valor en la evaluación frente a consumir un servicio externo gratuito?
3. ¿Existe infraestructura universitaria disponible para hospedarlo, o debe recurrirse a una capa gratuita comercial?
4. Considerando el arranque en frío de las capas gratuitas, ¿es un riesgo aceptable para la defensa final?

**Posición del equipo.** Implementar primero la vía de servicio externo, que es funcional de inmediato, y evaluar el autohospedaje como incorporación posterior. La interfaz `MathOcrEngine` permite añadir el segundo motor sin rehacer nada. Si se considera que el autohospedaje suma valor en la evaluación, se reordena la prioridad.

**Independientemente de la decisión**, pix2tex se ejecutará localmente sobre el conjunto de calibración para incorporarlo a la comparación de motores del artículo científico.

---

## 9. Política de uso de inteligencia artificial

| Identificador | Función | Disparador | Agrupación |
|---|---|---|---|
| **OCR-M** | Reconocimiento de fórmulas → LaTeX | Región clasificada como matemática | Por región |
| **IA-00** | Reparación selectiva por visión | Regiones que no superaron las compuertas de validación | **Una llamada por página** |
| **IA-01** | Reconstrucción del apunte a estructura JSON | Una vez por apunte | Lote |
| **IA-02** | Generación de flashcards y quizzes | Una vez por conjunto de temas | **Lote — nunca una llamada por ítem** |
| **IA-03** | Explicación bajo demanda de un concepto | A solicitud del estudiante | Resultado cacheado |
| **IA-05** | Adjudicación semántica de cobertura | Solo los temas que el prefiltro local no pudo clasificar | Lote |

**Procesos que no utilizan IA:** preprocesamiento de imagen, OCR de texto, segmentación y clasificación de regiones, deduplicación por hash perceptual, cálculo de confianza, validación de LaTeX, prefiltro de cobertura, cálculo del delta de progreso, corrección de quizzes, programación de repasos y cálculo de progreso.

Toda salida de IA se persiste. El sistema no reprocesa contenido ya generado.

---

## 10. Arquitectura técnica

```text
┌─── APLICACIÓN ANDROID ──────────────────────────────┐
│  Kotlin · Jetpack Compose · MVVM · Hilt              │
│                                                      │
│  CameraX                                             │
│  PipelineEngine                                      │
│    ├── ImagePreprocessor      (OpenCV)               │
│    ├── RegionSegmenter                               │
│    ├── RegionClassifier                              │
│    ├── TextOcrEngine          (ML Kit)               │
│    ├── ConfidenceScorer                              │
│    └── EscalationPolicy                              │
│  PerceptualHasher                                    │
│  CoverageEngine               (prefiltro local)      │
│  SrsScheduler                 (determinista)         │
│  Room                         (caché + cola)         │
│  Firebase Cloud Messaging                            │
└────────────────────────┬─────────────────────────────┘
                         │  REST · Retrofit + Gson
┌────────────────────────▼─────────────────────────────┐
│  BACKEND — Spring Boot (monolítico)                  │
│                                                      │
│  Spring Security + JWT                               │
│  Controllers → Services → Repositories (DTOs)        │
│                                                      │
│  AiOrchestrator                                      │
│    ├── MathOcrEngine          (interfaz)     OCR-M   │
│    ├── VisionRepairService                   IA-00   │
│    ├── ReconstructionService                 IA-01   │
│    ├── GenerationService                     IA-02   │
│    ├── ExplanationService                    IA-03   │
│    └── SemanticJudgeService                  IA-05   │
│  LatexValidator               (JLaTeXMath)           │
│  CostLedger                                          │
│                                                      │
│  PostgreSQL — relacional + JSONB                     │
└──────────────────────────────────────────────────────┘
```

Las credenciales de los servicios de IA residen exclusivamente en el backend. La aplicación cliente nunca las contiene.

---

## 11. Modelo de datos

Nomenclatura en inglés. `users` en plural. Aproximadamente veinte entidades.

**Control de acceso**
```
users · roles · privileges · user_roles · role_privileges
```

**Dominio académico**
```
courses          (id, name, code, owner_user_id, term)
enrollments      (user_id, course_id, status)
syllabi          (id, course_id, source_file, parsed_at)
syllabus_topics  (id, syllabus_id, parent_id, code, title, order_index)
course_glossary  (id, course_id, term, canonical_form, kind)
```

**Ingesta**
```
notes            (id, user_id, course_id, class_date, title, created_at)
note_pages       (id, note_id, perceptual_hash, storage_uri, page_index)
page_processing  (id, note_page_id, level_reached, overall_confidence,
                  quality_metrics JSONB, regions JSONB, processed_at)
note_contents    (id, note_id, content JSONB)
```

**Estudio**
```
topic_coverage      (user_id, syllabus_topic_id, state, score, updated_at)
coverage_snapshots  (user_id, course_id, coverage_pct, taken_at)
study_items         (id, course_id, syllabus_topic_id, kind, payload JSONB)
attempts            (id, user_id, study_item_id, response JSONB,
                     is_correct, answered_at)
review_schedule     (user_id, study_item_id, due_at, interval_days, ease)
```

**Operación**
```
ai_calls       (id, user_id, course_id, call_type, level, input_tokens,
                output_tokens, estimated_cost, latency_ms, created_at)
sync_queue     (id, user_id, entity_type, payload JSONB, attempts,
                last_error, status)
devices        (id, user_id, fcm_token, platform)
notifications  (id, user_id, kind, payload JSONB, sent_at, read_at)
```

### Criterio de uso de JSONB

Se aplicó de forma selectiva, no generalizada.

| Se usa JSONB | Justificación |
|---|---|
| `page_processing.regions` | El número y la forma de las regiones varía por página; normalizarlo no aporta consultabilidad |
| `note_contents.content` | El apunte estructurado es un documento, no una relación |
| `study_items.payload` | La estructura difiere según el tipo de ítem (flashcard, opción múltiple, verdadero/falso) |
| `attempts.response` | El formato de respuesta depende del tipo de pregunta |
| `quality_metrics` | Conjunto variable de métricas de diagnóstico |

| No se usa JSONB | Justificación |
|---|---|
| `topic_coverage` | Se filtra y agrega constantemente |
| `syllabus_topics`, `enrollments` | Relaciones con cardinalidad e integridad referencial |
| Bloque de control de acceso | Integridad referencial obligatoria |

La configuración de la aplicación se maneja mediante `SharedPreferences`, no como entidad persistida.

---

## 12. Almacenamiento local y sincronización

Corresponde al tema de investigación aplicada asignado al grupo, y es también un requisito funcional del producto: los apuntes se toman en aulas donde la conectividad no está garantizada.

**Diseño**

- **Room** como caché local de lectura. Todo el contenido ya generado —apuntes reconstruidos, flashcards, quizzes, cobertura y programación de repasos— es consultable sin conexión.
- **Cola de captura.** Las fotografías tomadas sin conexión se encolan localmente con su metadata y se procesan al restablecerse la red.
- **Sincronización unidireccional** en el MVP: el dispositivo envía al servidor. La resolución bidireccional de conflictos se declara como trabajo futuro.
- **Reintentos con retroceso exponencial e idempotencia.** Cada elemento de la cola lleva un identificador estable, de modo que un reenvío tras una falla parcial no duplica registros. La sincronización parcial es el punto débil habitual de este tipo de diseño y se aborda explícitamente.
- **Notificación push** al completarse el procesamiento de la cola, indicando cuántas capturas se resolvieron y en qué nivel del pipeline.
- **Deduplicación por hash perceptual:** una página ya procesada no vuelve a consumir recursos, ni siquiera si se fotografía de nuevo.

---

## 13. Alcance del MVP

Clasificación por prioridad, sin estimaciones de esfuerzo.

### Imprescindible

- Autenticación con JWT y dos roles operativos sobre la estructura de acceso completa
- Docente: creación de curso, publicación de temario, código de inscripción
- Captura con CameraX y preprocesamiento N0 con diagnóstico de calidad
- OCR de texto local N1 y clasificador de regiones
- OCR matemático N1.5 con salida LaTeX
- Reparación selectiva por visión N2, agrupada por página
- Cálculo de confianza y mapa de confianza visible, con corrección manual
- Validación determinista de LaTeX
- Reconstrucción del apunte a estructura persistente
- Temario procesado una vez; cobertura con prefiltro local y adjudicación semántica
- Estados por tema con definición operativa
- Generación en lote de flashcards y quizzes; evaluación local
- Repetición espaciada determinista
- Caché offline, cola de captura y sincronización con reintentos
- Notificaciones push reales
- Registro de llamadas de IA con nivel alcanzado
- Backend desplegado; distribución del APK

### Deseable

- Renderizado de LaTeX junto al recorte original
- Nivel N3 a solicitud del estudiante
- Glosario de notación canónica mantenido por el docente
- Delta de cobertura entre sesiones
- Deduplicación por hash perceptual
- Rol administrador
- Vista de brechas agregadas para el docente
- Pruebas unitarias del cálculo de confianza

### Trabajo futuro declarado

- Resolución bidireccional de conflictos de sincronización
- Comparación semántica mediante embeddings
- Detección de contradicciones entre apuntes de un mismo tema
- Planificación completa de estudio orientada a fechas de evaluación
- Presupuesto de consumo por curso con modo degradado

---

## 14. Riesgos técnicos

| Riesgo | Mitigación |
|---|---|
| Integración de OpenCV en Android (compilación, tamaño del artefacto) | Se aborda en la primera etapa, antes que cualquier otro componente; se incluyen únicamente los módulos necesarios |
| La segmentación de regiones no discrimina correctamente | Degradación a procesamiento de página completa: mayor costo, pero el sistema sigue operando |
| Calibración de los umbrales de confianza | Conjunto fijo de fotografías de calibración definido al inicio, sobre el cual se ajustan y se miden los pesos |
| Transcripción incorrecta de una fórmula | El recorte original se conserva y se muestra junto al LaTeX. La imagen nunca se descarta |
| Disponibilidad o latencia del servicio de OCR matemático | `MathOcrEngine` es intercambiable; existe una implementación de respaldo sobre el modelo de visión ya integrado |
| Error de clasificación de una región | Si el motor asignado devuelve baja confianza, la región escala igualmente. Un error de ruteo cuesta latencia, no corrección |
| Sincronización parcial ante fallas de red | Reintentos con retroceso exponencial e identificadores idempotentes |
| Despliegue en la nube | Se aborda en el laboratorio previo al que lo exige, no en el mismo |

---

## 15. Cumplimiento de requisitos del curso

| Requisito | Estado en el diseño |
|---|---|
| Mínimo dos roles | Tres roles definidos: estudiante, docente, administrador |
| Tablas `users`, `roles`, `privileges` y puentes | Incorporadas con el esquema indicado |
| Nomenclatura en inglés, `users` en plural | Aplicado en todo el modelo |
| PostgreSQL como base principal | Adoptado |
| Uso de campos JSON | Aplicado de forma selectiva, con criterio documentado (sección 11) |
| Modelo de datos acotado | Aproximadamente veinte entidades |
| Menú de navegación | Contemplado |
| Notificaciones push reales | Firebase Cloud Messaging |
| Modo offline real | Room en dispositivo, con cola de captura y sincronización |
| Autenticación conforme al framework | Spring Security con JWT |
| Arquitectura monolítica | Backend Spring Boot monolítico |
| Configuración de plataforma | `SharedPreferences` |
| Nombre propio de aplicación | **Glifo** — ver sección 0 |
| Paleta de color propia | Definida por el equipo, en dos modos completos — ver sección 0 |

---

## 16. Síntesis

El ciclo funcional de Glifo —apuntes, conocimiento, brechas contra el temario, estudio dirigido— es el del enunciado.

La contribución propia del proyecto está en **garantizar el primer paso de ese ciclo**: convertir de forma verificable una fotografía de apuntes manuscritos, incluyendo notación matemática, en contenido estructurado; escalando el costo de procesamiento únicamente sobre las regiones que lo requieren, y explicitando al estudiante el nivel de certeza de cada fragmento en lugar de presentar como cierto lo que no pudo leerse.
