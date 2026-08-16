# Glifo — Bitácora de decisiones

Registro histórico del proyecto. **Este archivo no describe el proyecto**: describe cómo se llegó a él.

Los documentos de trabajo (`Glifo_Alcance.md`, `Glifo_Arquitectura_Estandares.md`) están escritos en presente y describen únicamente lo que se va a construir. Cualquier decisión revertida, alternativa descartada o versión anterior vive aquí y **no debe reintroducirse** en aquellos.

---

## Índice de decisiones

| ID | Decisión | Estado |
|---|---|---|
| D-01 | Dirección estratégica: motor de ingesta resiliente | Cerrada |
| D-02 | Nombre del proyecto: Glifo | Cerrada |
| D-03 | Tres roles de usuario | Cerrada |
| D-04 | PostgreSQL principal, Room como caché | Cerrada |
| D-05 | Escalera ramificada de procesamiento | Cerrada |
| D-06 | Motor de OCR matemático: SimpleTex | Cerrada |
| D-07 | pix2tex autohospedado | **Abierta** — pendiente de consulta |
| D-08 | Comparación de cobertura: híbrida | Cerrada |
| D-09 | JSONB selectivo, no generalizado | Cerrada |
| D-10 | Offline y sincronización como pilar | Cerrada |
| D-11 | Telemetría propia + Firebase Analytics | Cerrada |
| D-12 | Paleta: dos modos, dorado como acento | Cerrada — un punto abierto |

---

## D-01 · Dirección estratégica

**Decisión.** Glifo conserva el dominio del enunciado —apuntes, cobertura contra temario, refuerzo— pero desplaza su eje de producto: de «app de apuntes que ahorra tokens» a **motor de ingesta resiliente para apuntes manuscritos**.

**Alternativas consideradas y descartadas**

| Opción | Motivo del descarte |
|---|---|
| Mantener la propuesta original sin cambios | El diferenciador (preprocesamiento y economía de tokens) era invisible para el usuario y no respondía a la fragilidad del OCR |
| Replanteamiento total del problema (autopsia de exámenes, banco colaborativo de apuntes, motor de estudio sin cámara) | El enunciado es común a los nueve grupos; cambiar el problema se sale del marco asignado y descarta el trabajo previo |

**Fundamento.** El propio enunciado identifica el OCR sobre manuscrito de baja calidad como su riesgo principal, y propone como mitigación que el estudiante corrija a mano. Esa mitigación no escala en cursos con notación matemática. El territorio de la ingesta fiable no estaba ocupado por ningún otro grupo.

---

## D-02 · Nombre

**Decisión.** El proyecto se llama **Glifo**.

**Contexto.** No podía conservar el nombre NotaViva: es el del enunciado guía del curso, propuesto por Keneth Jara (Grupo Charlie), y el docente pidió nombres distintos por equipo.

**Alternativas evaluadas**

| Candidato | Resultado |
|---|---|
| **Glifo** | **Elegido.** Unidad mínima de la escritura; describe exactamente lo que la app procesa |
| Grifo | Descartado como nombre: en buena parte del ámbito hispanohablante significa «llave de agua». Se conserva como **base de la identidad visual** |
| Runa | Descartado: buen significado, pero común en tiendas de aplicaciones y pierde el juego glifo/grifo |
| Calco, Trazo, Nitidez | Descartados por menor precisión semántica |

---

## D-03 · Roles

**Decisión.** Tres roles: estudiante, docente, administrador.

**Motivo del cambio.** El diseño inicial contemplaba un único rol y una versión previa listaba explícitamente el panel del docente entre las funciones eliminadas. El docente calificó el mínimo de dos roles como mandatorio y señaló su ausencia en la presentación del equipo. El enunciado guía contempla tres.

**Elemento diferenciador incorporado.** El rol docente no es un panel de consulta: mantiene el **glosario de notación canónica** del curso, que alimenta directamente el cálculo de confianza del pipeline.

---

## D-04 · Persistencia

**Decisión.** PostgreSQL como base principal; Room como caché local y cola de sincronización.

**Motivo del cambio.** El diseño inicial era local-first con Room como almacenamiento principal. PostgreSQL es obligatorio para los nueve grupos, y la credencial del servicio de IA no puede residir en el APK, lo que exige un backend intermedio de todos modos.

---

## D-05 · Escalera de procesamiento

**Decisión.** Pipeline ramificado: N0 preprocesamiento → N1 OCR local sobre todas las regiones → clasificador como enrutador → N1.5 OCR matemático para fórmulas → N2 reparación selectiva por visión para lo no resuelto y para dibujos → N3 página completa a solicitud.

**Evolución del diseño**

1. Versión inicial: OCR único sin plan alternativo. El docente señaló la ausencia de un mecanismo de respaldo como riesgo crítico.
2. Segunda versión: escalera lineal N0→N1→N2→N3.
3. **Versión vigente:** escalera ramificada. El clasificador de regiones pasó de etiquetar a enrutar, lo que elevó su prioridad a imprescindible.

**Decisión de orden.** El clasificador se ejecuta **después** de N1, no antes: ML Kit es local y gratuito, y su comportamiento sobre una región es mejor señal de clasificación que la geometría por sí sola.

---

## D-06 · Motor de OCR matemático

**Decisión.** SimpleTex como implementación primaria de `MathOcrEngine`, con respaldo automático sobre el modelo de visión.

**Restricción del equipo:** ninguna solución de pago. Si igual se cobra por imagen, un motor dedicado solo se justifica siendo gratuito, marcadamente más barato o marcadamente más preciso.

**Alternativas evaluadas**

| Motor | Resultado |
|---|---|
| **SimpleTex** | **Elegido.** Gratuito con cuota diaria amplia; devuelve confianza numérica derivada del modelo; soporta manuscrito |
| Modelo de visión con instrucción de LaTeX | **Respaldo.** Admite contexto del curso y puede declarar que no entendió, pero su autoevaluación no es una medida calibrada |
| Mathpix | Descartado: de pago, sin capa gratuita permanente |
| Tesseract | Descartado: débil en manuscrito, que es el caso relevante |
| ML Kit Digital Ink | Descartado: opera sobre trazos en pantalla, no sobre fotografías |
| pix2tex, Surya, texify autohospedados | Ver D-07 |

**Criterio decisivo.** Se priorizó que el motor entregue una señal de confianza **derivada del modelo**, no autorreportada, para poder decidir automáticamente si una región se acepta o escala.

---

## D-07 · pix2tex autohospedado — **abierta**

**Estado.** Sin resolver. Pendiente de consulta con el docente.

**Propuesta.** Desplegar pix2tex como microservicio REST propio y consumirlo como una implementación más de `MathOcrEngine`.

**A favor:** independencia de proveedores externos; los apuntes no salen de infraestructura propia; sin cuotas; mayor peso académico.

**En contra:** introduce un segundo despliegue; la variante con PyTorch excede la memoria de las capas gratuitas de hosting —la variante ONNX sí encaja—; el arranque en frío de decenas de segundos es un riesgo para la demostración en vivo; precisión reportada entre 60 % y 70 % sobre fotografías con iluminación deficiente, que es el insumo real; tiende a producir salida inventada sobre regiones que no son fórmulas.

**Preguntas planteadas al docente**

1. ¿Un microservicio Python independiente es admisible, o contradice el requisito de arquitectura monolítica?
2. ¿Autohospedar aporta valor en la evaluación frente a consumir un servicio externo gratuito?
3. ¿Existe infraestructura universitaria disponible?
4. ¿Es aceptable el arranque en frío para la defensa final?

**Independientemente del resultado**, pix2tex se ejecuta localmente sobre el conjunto de calibración para la comparación de motores del artículo científico. Esa medición no requiere desplegar nada.

---

## D-08 · Comparación de cobertura

**Decisión.** Prefiltro local determinista resuelve la mayoría de los temas; solo los casos ambiguos se envían a adjudicación semántica por IA.

**Motivo del cambio.** El diseño previo hacía la comparación enteramente local. El enunciado guía justifica la selección del proyecto, entre otras razones, por incorporar tres llamadas de IA con propósitos distintos: visión, comparación semántica y generación. Eliminar la comparación semántica suprimía uno de esos tres pilares.

El enfoque híbrido conserva la llamada y aplica la misma política de escalamiento del pipeline a un segundo problema.

---

## D-09 · JSONB

**Decisión.** Aplicación selectiva, con criterio documentado, no generalizada.

**Contexto.** El docente recomendó evaluar campos JSON a los nueve grupos, y fue una de las dos observaciones sobre el modelo de datos en la primera entrega.

**Criterio adoptado:** JSONB donde la estructura es variable y no se consulta por campo interno; relacional donde hay integridad referencial o agregación frecuente.

---

## D-10 · Offline y sincronización

**Decisión.** Pilar del producto, no funcionalidad secundaria.

**Motivo.** Corresponde al tema de investigación aplicada asignado al equipo, evaluado por separado y con ronda de preguntas de los demás grupos. Una versión previa de la planificación lo había reducido a prioridad menor; se revirtió.

**Alcance:** consumo offline, cola de captura, sincronización unidireccional con reintentos e idempotencia. La resolución bidireccional de conflictos queda declarada como trabajo futuro.

---

## D-11 · Telemetría

**Decisión.** Registro propio de llamadas de IA (`ai_calls`) más eventos personalizados de Firebase Analytics y Crashlytics.

**Motivo.** El argumento central del proyecto es cuantitativo: la proporción de regiones resueltas en cada nivel del pipeline es un dato que debe producirse, no afirmarse. La misma instrumentación alimenta el artículo científico y la ronda de preguntas de investigación aplicada.

Crashlytics se incorpora porque OpenCV y ML Kit son bibliotecas nativas y sus fallos no se reproducen en el emulador.

---

## D-12 · Paleta

**Decisión.** Dos modos completos —noche y día— con la misma estructura de tokens, y el **dorado como color de acción**. Se adopta la paleta materializada en el prototipo de las cinco tandas.

**Motivo del cambio.** La paleta original nació en documento y nunca se probó sobre pantallas reales. Al producir los 39 frames se vio que el acento turquesa `#3FC5C0` y el dorado heráldico `#C9A227` competían: dos colores saturados de familias distintas peleando por la atención en la misma pantalla, sin que ninguno de los dos tuviera un dominio claro. Unificar en el dorado dejó una sola señal de interacción y liberó el turquesa.

**Consecuencia.** El token `heraldic` desaparece. El dorado ya no puede usarse como adorno de marca: si aparece, el elemento se puede tocar o está activo. La identidad de marca pasa a descansar enteramente en el logotipo del grifo.

**Elementos incorporados.** `surfaceHigh` y `border` como tokens de primer nivel; el juego de variantes `Soft` / `Faint` / `Line` sistematizado sobre todo color semántico; `accentText` y `onAccent` para resolver contraste del dorado en ambos modos.

**Valores anteriores — no reintroducir**

| Token | Versión descartada |
|---|---|
| `background` | `#0B1420` |
| `surface` | `#132234` |
| `accent` | `#3FC5C0` |
| `heraldic` | `#C9A227` |
| `textPrimary` | `#E8E4D9` |
| `textSecondary` | `#8FA3B8` |
| `alert` | `#D85A30` |
| Verificado | `#5DCAA5` |
| Reparado | `#85B7EB` |
| Escalado | `#EF9F27` |
| Incierto | `#F0997B` |

También se descarta la codificación por figura geométrica —círculo lleno, cuadrado, triángulo, círculo punteado—, que no sobrevivió al contacto con texto en línea: no hay dónde poner un triángulo dentro de un párrafo. La sustituye la combinación de subrayado, relleno y etiqueta textual descrita en `Glifo_Arquitectura_Estandares.md` §12.3, que cumple la misma función de doble codificación.

**Punto abierto.** `escalated` y `alert` comparten valor (`#E0693A` / `#B94117`). El escalamiento a visión es funcionamiento normal, no una falla, y el argumento central del proyecto se debilita si se presenta con el color de error. Pendiente: separar `escalated` hacia un ámbar propio, distinguible de `verified` y de `alert` en ambos modos. **Plazo: antes del Laboratorio 3 (9 de septiembre)**, que es cuando el mapa de confianza se defiende en clase.

---

## Funcionalidades evaluadas y excluidas del alcance

Registro de lo que **no** se construye, para evitar que reaparezca en planificaciones posteriores.

| Funcionalidad | Motivo |
|---|---|
| Detección de contradicciones entre apuntes | Alto riesgo de falsos positivos; señalar una contradicción inexistente destruye la confianza en el producto |
| Comparación semántica mediante embeddings | Requiere modelo adicional y almacenamiento vectorial; el prefiltro léxico con glosario y adjudicación por IA da resultado comparable |
| Detección automática de materia | El flujo natural es seleccionar el curso antes de capturar |
| Resolución bidireccional de conflictos de sincronización | Proyecto en sí mismo; declarada como trabajo futuro |
| Presupuesto de consumo por curso con modo degradado | El registro de llamadas basta para sostener el argumento de costo |
| Planificación completa de estudio orientada a evaluaciones | Reducida a fecha de examen manual con ordenamiento por prioridad |
| Rachas, logros y comparación social entre estudiantes | Ocupado por otros equipos; añade multiusuario y consideraciones de privacidad sin aportar diferenciación |
| Compartir apuntes por mensajería o correo | Superficie adicional sin relación con el eje del proyecto |
| Preguntas abiertas evaluadas por IA | Exige una llamada por respuesta; contradice la política de agrupación |
| Rúbrica de evaluación completa con ponderaciones | Territorio de otro equipo; no toca el motor |
| Inscripción por código QR con aprobación docente | Añade estados y flujo de trabajo; un código de curso cumple la misma función |
| Panel administrativo con suspensión por consumo | Requiere política de cuotas y telemetría en tiempo real |
| Resumen semanal por correo | Canal adicional que mantener |
| Autenticación biométrica | Marginal para el alcance |
| Segunda base de datos NoSQL | JSONB cubre lo documental sin añadir un despliegue |
| Búsqueda semántica avanzada, texto a voz, calendario integrado | Fuera del alcance desde el inicio |

---

## Correcciones al marco del curso

Registro de datos que se corrigieron tras verificación, para que no se reintroduzcan.

| Dato | Versión incorrecta | Versión verificada |
|---|---|---|
| Tamaño del equipo | 2 integrantes; «máximo 2 personas» por grupo | **3 integrantes.** El límite de 2 aplica a laboratorios individuales y al artículo científico, no a los equipos de proyecto |
| Modo de entrega | Entrega por aula virtual | **Defensa en vivo en clase.** No se sube nada; el aula virtual solo publica la nota |
| Fechas efectivas | Fechas de cierre del calendario | **La última clase del miércoles antes del cierre** |
| Duración del Laboratorio 2 | 3 semanas | **2 semanas efectivas**, una compartida con la corrección del Laboratorio 1 |
| Alcance del Laboratorio 2 | Implementación completa | Solo Activities: menú, autenticación, navegación y listas. La IA no interviene hasta el Laboratorio 6 |

---

## Resultado del Laboratorio 1

Nota obtenida: **85**. Deducción única en el rubro de diagrama UML de objetos de base de datos (1.5 de 3), con dos observaciones: incorporar las tablas de roles y privilegios, y evaluar el uso de JSON. Ambas resueltas en D-03 y D-09.

La observación del docente sobre el segundo rol de usuario no formó parte de esa deducción; corresponde a un requisito aplicable a las entregas siguientes.
