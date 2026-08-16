# Glifo — Alcance

**Grupo X-Ray** — Brandon Brenes · David González · Felipe Ugalde
Desarrollo de Aplicaciones Móviles · II Ciclo 2026

> **Documentos relacionados**
> · `Glifo_Arquitectura_Estandares.md` — arquitectura, paquetes, clases, convenciones
> · `Glifo_Diseno_Arquitectura.md` — versión para consulta con el docente
> · `Glifo_Contexto_Competitivo.md` — propuestas de los demás equipos
> · `Glifo_Bitacora_Decisiones.md` — histórico de decisiones y alternativas descartadas
> · `Contexto_Curso.md` — marco del curso

---

## 1. Identidad

**Glifo.** Un glifo es la unidad mínima de la escritura: el signo individual trazado a mano. Es exactamente aquello que la aplicación lee, evalúa y decide si comprendió.

La identidad visual se construye sobre el **grifo mitológico** —cabeza de águila, cuerpo de león—, que aporta las dos ideas del producto: vista aguda para leer lo que cuesta leer, y guarda del contenido del estudiante.

> El glifo es la unidad de la escritura. El grifo, su guardián.

### Paleta base

Glifo define **dos modos completos** —noche y día—, no un tema oscuro con una variante clara. Ambos comparten la misma estructura de tokens; solo cambian los valores. El azul pizarra viene del cuerpo del grifo; el dorado, de su carácter heráldico.

| Token | Noche | Día | Uso |
|---|---|---|---|
| `background` | `#161E27` | `#EDEAE0` | Fondo |
| `surface` | `#2E3B4B` | `#F7F4EC` | Tarjetas y superficies |
| `surfaceHigh` | `#3B4A5C` | `#D7D1B9` | Relleno interno, pistas de barras |
| `border` | `#4A5A6E` | `#C4BCA3` | Contorno de tarjeta y separadores |
| `accent` | `#FFD372` | `#FFD372` | Acción primaria y estado activo |
| `textPrimary` | `#D7D1B9` | `#2E3B4B` | Texto principal |
| `textSecondary` | `#959595` | `#63666A` | Texto secundario |
| `alert` | `#E0693A` | `#B94117` | Errores y destructivos |

El juego completo de tokens derivados —`accentText`, `onAccent`, las variantes `Soft`, `Faint` y `Line`, `btnSecBorder`, `btnSecText` y `scrim`— está en `Glifo_Arquitectura_Estandares.md` §12.2.

**El dorado es el color de la acción, no el de la marca.** No se usa como decoración: si aparece, el elemento se puede tocar o está activo. La identidad de marca descansa en el logotipo, no en un color reservado.

### Estados de confianza

Escala funcional del mapa de confianza. Cada estado lleva **codificación no cromática además del color**: cerca del 8 % de los hombres presenta alguna deficiencia en la percepción del color, y el par verde/ámbar es el peor discriminado.

| Estado | Noche | Día | Codificación no cromática | Nivel |
|---|---|---|---|---|
| Verificado | `#5FA88C` | `#2F7D62` | Subrayado sólido, sin relleno | N1 |
| Reparado | `#8FB7DC` | `#3E6E9E` | Subrayado sólido + relleno suave | N1.5 |
| Escalado | `#E0693A` | `#B94117` | Subrayado sólido + relleno tenue | N2 |
| Incierto | `#959595` | `#63666A` | Subrayado punteado + relleno suave | — |

Además, todo fragmento no resuelto en N1 lleva una **etiqueta textual** con su estado y el nivel que lo resolvió (`REPARADO · N1.5`). El color nunca es el único portador de la información.

---

## 2. Problema y propuesta

El ciclo del enunciado —apuntes, cobertura contra el temario, refuerzo dirigido— depende de un primer paso: que la fotografía de un apunte manuscrito se convierta de forma fiel en contenido estructurado.

En cursos con notación matemática ese paso falla. Un OCR de texto no reconoce fracciones, exponentes, matrices ni límites: devuelve una cadena de caracteres sin relación con lo escrito. El resultado es un apunte reconstruido que parece correcto, y material de estudio generado sobre ese error.

> **Glifo digitaliza apuntes manuscritos —fórmulas y diagramas incluidos— escalando el procesamiento solo cuando hace falta, y muestra al estudiante qué entendió con certeza, qué tuvo que reparar y qué necesita revisión.**

| Compromiso | Implementación |
|---|---|
| Nada se inventa en silencio | Cada fragmento lleva un nivel de confianza calculado y visible |
| Ninguna foto se queda sin procesar | Escalamiento automático cuando el motor actual no alcanza el umbral |
| El costo es una decisión de diseño | Cada nivel tiene costo conocido; se registra cuál resolvió cada región |

**Encuadre:** Glifo no es «local en vez de nube». Es **local primero, nube cuando hace falta, y solo en la región que la necesita.**

---

## 3. Principios

1. **La IA no hace todo.** Se maximiza el procesamiento local y determinista antes de invocar cualquier modelo.
2. **Toda llamada de IA produce un resultado persistente.** Nunca se recalcula lo ya guardado.
3. **Nunca una llamada por ítem.** Generación y reparación viajan siempre en lote.
4. **Contexto mínimo.** El temario se procesa una vez y nunca se reenvía completo.
5. **La incertidumbre se muestra.** Si el sistema no pudo leer algo, lo marca; no lo completa.

---

## 4. Roles

| Rol | Responsabilidades |
|---|---|
| **Estudiante** | Capturar, revisar y corregir la reconstrucción, estudiar, consultar cobertura y consumo |
| **Docente** | Crear cursos, publicar el temario, mantener el **glosario de notación canónica**, consultar brechas del grupo |
| **Administrador** | Usuarios, roles y privilegios; estado del sistema |

El glosario que mantiene el docente alimenta directamente el cálculo de confianza del pipeline. Es una entrada funcional, no un panel de consulta.

---

## 5. Flujo principal

```text
DOCENTE
  crea curso → publica temario (PDF) → define glosario de notación
  → procesado una sola vez → persistido

ESTUDIANTE
  se une por código
        │
        ▼
  CAPTURA (CameraX) → hash perceptual
        │            └─ página ya procesada → se reutiliza, sin llamadas
        ▼
  ╔═══════ ESCALERA RAMIFICADA ═══════════════════════╗
  ║ N0  OpenCV local                                   ║
  ║     deskew · perspectiva · contraste                ║
  ║     métricas de calidad · segmentación en regiones  ║
  ║     └─ irrecuperable → repetir CON MOTIVO           ║
  ║                        │                            ║
  ║ N1  ML Kit OCR local sobre TODAS las regiones       ║
  ║                        │                            ║
  ║          CLASIFICADOR DE REGIÓN (enrutador)         ║
  ║          │             │              │             ║
  ║       TEXTO        MATEMÁTICA      DIBUJO           ║
  ║      resuelto          ▼              │             ║
  ║       en N1     N1.5 OCR MATEMÁTICO   │             ║
  ║          │        → LaTeX + conf      │             ║
  ║          └─────────────┬──────────────┘             ║
  ║                        ▼                            ║
  ║     COMPUERTAS: confianza bajo umbral               ║
  ║                 o LaTeX que no compila              ║
  ║                        ▼                            ║
  ║ N2  REPARACIÓN SELECTIVA POR VISIÓN                 ║
  ║     recortes · UNA SOLA LLAMADA POR PÁGINA          ║
  ║                        ▼                            ║
  ║ N3  Página completa · a solicitud del estudiante    ║
  ╚═════════════════════════════════════════════════════╝
        │
        ▼
  IA-01 RECONSTRUCCIÓN → JSON + confianza por fragmento
        │
        ▼
  REVISIÓN CON MAPA DE CONFIANZA
     └─ las correcciones alimentan el glosario del curso
        │
        ▼
  COBERTURA
     prefiltro local resuelve la mayoría
     └─ zona gris → IA-05 adjudicación semántica en lote
        │
        ▼
  ESTADOS POR TEMA + DELTA DE PROGRESO
        │
        ▼
  IA-02 flashcards + quiz de los vacíos · 1 llamada por lote
        │
        ▼
  EVALUACIÓN LOCAL → repetición espaciada → los fallos se reprograman
```

---

## 6. Funcionalidades

### 6.1 Base del curso

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| Autenticación, registro, navegación, listas | 2 | Imprescindible |
| Menú de navegación | 2 | Imprescindible |
| `users` · `roles` · `privileges` + puentes | 4 | Imprescindible |
| Rol estudiante y rol docente | 2 | Imprescindible |
| Rol administrador | 6 | Deseable |
| Hilt | 3 | Imprescindible |
| Retrofit, Gson, manejo de errores | 3 | Imprescindible |
| Consumo de API simulada | 3 | Imprescindible |
| Spring Boot: Repository, Services, DTOs | 4–5 | Imprescindible |
| PostgreSQL con JSONB | 4 | Imprescindible |
| Colección de Postman | 5 | Imprescindible |
| JWT y Spring Security | 6 | Imprescindible |
| Despliegue en la nube | 6 | Imprescindible |
| Firebase App Distribution | Final | Imprescindible |
| Notificaciones push | 6 | Imprescindible |
| Encuesta a cinco personas externas | Final | Imprescindible |
| Artículo científico | — | Imprescindible |
| Investigación aplicada | 11 nov | Imprescindible |

### 6.2 Captura e ingesta — núcleo del proyecto

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| CameraX y pantalla de captura | 2 | Imprescindible |
| **N0** preprocesamiento OpenCV | 2 | Imprescindible |
| Diagnóstico de calidad con motivo concreto | 2 | Imprescindible |
| Segmentación en regiones | 3 | Imprescindible |
| **Clasificador de región como enrutador** | 3 | Imprescindible |
| **N1** OCR de texto local | 3 | Imprescindible |
| **ConfidenceScorer** y calibración | 3 | Imprescindible |
| **Mapa de confianza en la interfaz** | 3 | Imprescindible |
| Corrección manual de fragmento | 3 | Imprescindible |
| **N1.5** OCR matemático a LaTeX | 6 | Imprescindible |
| **N2** reparación selectiva por visión | 6 | Imprescindible |
| Validación de LaTeX como señal de confianza | 6 | Deseable |
| Renderizado LaTeX con recorte original al lado | 6 | Deseable |
| **N3** página completa a solicitud | 6 | Deseable |
| Deduplicación por hash perceptual | 4 | Deseable |
| Glosario de notación canónica | 5 | Deseable |
| Importar desde galería | 2 | Opcional |
| Apunte multipágina | — | Opcional |
| Consenso entre motores como señal | 6 | Opcional |

### 6.3 Conocimiento y cobertura

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| Cursos e inscripción por código | 5 | Imprescindible |
| Temario procesado una vez y persistido | 5 | Imprescindible |
| **IA-01** reconstrucción a JSON | 6 | Imprescindible |
| Prefiltro local de cobertura | 5 | Imprescindible |
| **IA-05** adjudicación semántica | 6 | Imprescindible |
| Estados con definición operativa | 5 | Imprescindible |
| Detección automática de tema | 5 | Deseable |
| Fallback manual del temario | 5 | Deseable |
| **Delta de cobertura** entre sesiones | 5 | Deseable |
| Instantáneas históricas | 5 | Opcional |
| Gráfica de progreso semanal | 6 | Opcional |

### 6.4 Estudio

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| **IA-02** flashcards y quiz en lote | 6 | Imprescindible |
| Evaluación local determinista | 6 | Imprescindible |
| Repetición espaciada determinista | 6 | Imprescindible |
| Reprogramación de ítems fallados | 6 | Deseable |
| Autoevaluación en flashcards | 6 | Deseable |
| Métricas de dominio | 6 | Deseable |
| Historial de quizzes | 6 | Opcional |
| **IA-03** explicación bajo demanda | 6 | Opcional |
| Fecha de examen y priorización | — | Opcional |
| Flashcards creadas por el usuario | — | Opcional |

### 6.5 Offline y sincronización

Corresponde al tema de investigación aplicada del equipo.

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| Room como caché de lectura | 4 | Imprescindible |
| Consumo offline de todo lo generado | 4 | Imprescindible |
| Cola de captura offline | 4 | Imprescindible |
| Sincronización unidireccional | 5 | Imprescindible |
| **Reintentos con backoff e idempotencia** | 5 | Imprescindible |
| Indicador de estado de sincronización | 5 | Deseable |
| Notificación push al completar la cola | 6 | Deseable |

### 6.6 Telemetría y pruebas

| Funcionalidad | Lab | Prioridad |
|---|---|---|
| Registro `ai_calls` con nivel alcanzado | 6 | Imprescindible |
| Firebase Analytics con eventos propios | 6 | Deseable |
| Crashlytics | 3 | Deseable |
| Pruebas unitarias del ConfidenceScorer | 5 | Deseable |
| Vista de consumo para estudiante y docente | 6 | Opcional |
| Pruebas de la política de sincronización | 5 | Opcional |

---

## 7. Fuera de alcance

Se declaran explícitamente como trabajo futuro:

- Resolución bidireccional de conflictos de sincronización
- Comparación semántica mediante embeddings
- Detección de contradicciones entre apuntes
- Planificación completa de estudio orientada a evaluaciones
- Presupuesto de consumo por curso con modo degradado
- Rachas, logros y comparación social
- Compartir apuntes por canales externos
- Autenticación biométrica
- Búsqueda semántica avanzada, texto a voz, calendario integrado

El registro completo de alternativas evaluadas y sus motivos está en `Glifo_Bitacora_Decisiones.md`.

---

## 8. Llamadas de IA

| ID | Función | Disparador | Agrupación |
|---|---|---|---|
| **OCR-M** | Fórmula → LaTeX (N1.5), gratuito | Región clasificada como matemática | Por región |
| **IA-00** | Reparación selectiva por visión | Regiones que no superan las compuertas | **Una llamada por página** |
| **IA-01** | Reconstrucción a estructura JSON | Una vez por apunte | Lote |
| **IA-02** | Flashcards y quizzes | Una vez por conjunto de temas | **Lote** |
| **IA-03** | Explicación bajo demanda | A solicitud | Resultado cacheado |
| **IA-05** | Adjudicación semántica de cobertura | Solo la zona gris del prefiltro | Lote |

**Sin IA:** preprocesamiento, OCR de texto, segmentación, clasificación, deduplicación, cálculo de confianza, validación de LaTeX, prefiltro de cobertura, delta de progreso, corrección de quizzes, repetición espaciada y progreso.

---

## 9. Arquitectura y modelo de datos

Detallados en `Glifo_Arquitectura_Estandares.md`. Resumen:

- **Cliente Android** en Kotlin, Compose y MVVM con Hilt, en tres capas más el paquete `pipeline`.
- **Backend Spring Boot monolítico** con Spring Security y JWT, organizado por dominio, con `AiOrchestrator` como fachada de las llamadas de IA.
- **PostgreSQL** como base principal con JSONB selectivo; **Room** como caché y cola.
- Aproximadamente veinte entidades, nomenclatura en inglés, `users` en plural.
- Las credenciales de servicios externos residen únicamente en el backend.

---

## 10. Calendario

Todos los laboratorios se defienden en vivo en la clase del miércoles. **La fecha operativa es la última clase antes del cierre oficial.**

| Defensa | Lab | Exigencia del curso | Trabajo de Glifo en paralelo |
|---|---|---|---|
| **19 ago** | Corrección Lab 1 | Tablas de acceso · JSON razonado · modelo acotado | Rol docente en el prototipo · pantallas de la escalera |
| **26 ago** | Lab 2 | Menú, autenticación, navegación, listas · dos flujos de rol · paleta y nombre | OpenCV compilando · CameraX · **N0** · rechazo con motivo |
| **9 sep** | Lab 3 | Hilt · Retrofit · errores · API simulada | **N1** · segmentación · **ConfidenceScorer** · **mapa de confianza** · Crashlytics |
| **23 sep** | Lab 4 | Backend · PostgreSQL · Repository | Esquema completo · **Room y cola offline** · deduplicación |
| **14 oct** | Lab 5 | Services · REST · DTOs · Postman | Temario · prefiltro de cobertura · **sincronización con reintentos** · glosario · pruebas |
| **28 oct** | Lab 6 | JWT · Spring Security · **API de IA** · nube | **N1.5 y N2** · IA-01 · IA-02 · IA-05 · LaTeX · `ai_calls` · Analytics · push |
| 1–10 nov | — | Sin entregas abiertas | Integración · encuesta · artículo · ensayo de defensa |
| **11 nov** | Final | APK · JWT · encuesta | Defensa e investigación aplicada con ronda de preguntas |

**El Laboratorio 2 tiene dos semanas efectivas**, una compartida con la corrección del Laboratorio 1.
**Semana crítica: 23 de septiembre** — defensa del Laboratorio 4 con los Laboratorios 5 y 6 ya abiertos.

---

## 11. Capacidad y reparto

Tres integrantes · siete horas semanales de estudio independiente cada uno · trece semanas y media ≈ **285 horas de equipo**, incluidos el artículo científico, las tareas de investigación y las evaluaciones cortas.

| Bloque | Horas |
|---|---|
| Base obligatoria del curso | ~165 |
| Captura e ingesta | ~106 |
| Conocimiento y cobertura | ~52 |
| Estudio | ~42 |
| Offline y sincronización | ~33 |
| Telemetría y pruebas | ~24 |
| **Catálogo completo** | **~422** |

El catálogo completo no cabe. Aplicando el filtro de prioridades:

| Nivel | Horas |
|---|---|
| Imprescindible | ~223 |
| Deseable | ~61 |
| **Imprescindible + deseable** | **~284** |
| Opcional | ~38 |

Queda sin margen. **Para recuperar colchón se bajan a opcional, en este orden:** N3 a solicitud, instantáneas históricas, indicador de sincronización, autoevaluación en flashcards, fallback manual del temario. Eso devuelve unas trece horas sin tocar el motor.

Tres personas no rinden una vez y media lo de dos: la coordinación cuesta, y el despliegue en la nube no sale a la primera.

### Frentes de trabajo

| Frente | Alcance | Labs |
|---|---|---|
| **A — Cliente** | Compose, navegación, roles en interfaz, Hilt, Retrofit, mapa de confianza | 2, 3 |
| **B — Backend** | Spring Boot, PostgreSQL, Repository, DTOs, JWT, Postman, despliegue | 4, 5, 6 |
| **C — Motor** | OpenCV, ML Kit, escalera, Room, cola, sincronización, push, **investigación aplicada** | 2, 3, 4 · 11 nov |

Los frentes definen responsabilidad principal, no exclusividad. La nota es individual y hay dos defensas orales: todos revisan el trabajo de todos.

---

## 12. Producto mínimo

Debe funcionar de extremo a extremo el 11 de noviembre:

1. Autenticación con dos roles reales sobre la estructura de acceso completa y JWT
2. Docente: crear curso, publicar temario, código de inscripción. Estudiante: unirse
3. Captura con **escalera N0 → N1 → N1.5 → N2** y nivel alcanzado visible
4. **Mapa de confianza** con corrección manual
5. **Al menos una fórmula manuscrita** reconocida y renderizada
6. Cobertura con cuatro estados y **delta** entre sesiones
7. Flashcards y quiz en lote, corregidos localmente
8. **Cola offline**: capturar sin conexión, sincronizar al volver, notificar por push
9. `ai_calls` visible: llamadas, nivel y ahorro
10. Backend desplegado y APK distribuido

---

## 13. Riesgos

| Riesgo | Mitigación |
|---|---|
| Integración de OpenCV en Android | Primera tarea del Laboratorio 2, antes que cualquier otra. Solo los módulos necesarios |
| La segmentación no discrimina bien | Degradar a página completa: mayor costo, el sistema sigue operando |
| Calibración de umbrales | Conjunto fijo de fotografías definido en la primera semana; ajustar y medir contra él |
| Transcripción incorrecta de fórmula | El recorte original se conserva y se muestra junto al LaTeX |
| Disponibilidad o latencia del OCR matemático | Probar en el Laboratorio 5. `MathOcrEngine` es intercambiable, con respaldo sobre el modelo de visión |
| Error de clasificación de región | Si el motor devuelve baja confianza, la región escala igual. Cuesta latencia, no corrección |
| Sincronización parcial | Reintentos con backoff e identificadores idempotentes |
| Despliegue en la nube | Abordarlo en el Laboratorio 5, no en el 6 |
| Sobrealcance | El filtro de prioridades no se renegocia a mitad de camino |
| Fallo del demo en vivo | Tres fotografías preparadas: buena (N1), regular (N1.5), mala (N2) |

---

## 14. Guion de defensa

| Min | Contenido |
|---|---|
| 0:00 | Los nueve proyectos asumen que la foto de un apunte se lee bien. El enunciado señala el OCR manuscrito como su riesgo principal. Ese riesgo es nuestro proyecto |
| 0:30 | Foto buena → procesada localmente, **cero llamadas** |
| 1:15 | Foto con una integral manuscrita → el clasificador enruta esa región al motor matemático mientras el resto se resuelve localmente → aparece la fórmula en LaTeX, con el recorte original al lado |
| 2:15 | Foto mala → escalamiento a visión, o rechazo **con motivo concreto** |
| 3:00 | Mapa de confianza. *Nunca completamos lo que no pudimos leer* |
| 3:45 | Cobertura, delta y quiz — rápido, es lo que todos tienen |
| 4:15 | Contador: *cinco páginas, tres motores gratuitos, dos llamadas al modelo de pago* |
| 4:45 | Los dos roles y el glosario del docente |

---

## 15. Síntesis

El ciclo funcional de Glifo es el del enunciado y lo comparten los nueve equipos.

La contribución propia está en **garantizar el primer paso de ese ciclo**: convertir de forma verificable una fotografía de apuntes manuscritos, notación matemática incluida, en contenido estructurado; escalando el costo únicamente sobre las regiones que lo requieren, y explicitando el nivel de certeza de cada fragmento en lugar de presentar como cierto lo que no pudo leerse.
