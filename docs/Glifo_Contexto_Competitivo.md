# Glifo — Contexto competitivo

Referencia sobre las propuestas de los nueve equipos del curso. Sirve para evitar solapamientos y preparar la ronda de preguntas.

**Marco:** el docente asignó el mismo enunciado a todos los grupos —*"hago el mismo proyecto… es más sencillo para que ustedes se comparen"*—. La similitud entre proyectos es intencional. La diferenciación disponible no es de dominio, sino de ejecución técnica.

Está expresamente permitido inspirarse en las propuestas ajenas: *"No hay ningún problema, esa es la idea, por eso es que es en grupo."*

---

## 1. Los nueve equipos

### Alfa — Julio Araya · Ismael Rojas · José León
Apuntes y quizzes con **capa social y de motivación**. Tres roles; inscripción por código; rachas; comparación con el resto del grupo, anónima u opcional; el docente ve qué temas está flojeando la clase; preguntas escritas evaluadas por IA.

Tesis del equipo: sin factor social, este tipo de aplicaciones se abandona. Es la justificación de producto más articulada de la cohorte.

### Bravo — Isaac Araya · José Fernández · Catherine · Derek
La ejecución más limpia del enunciado base. Barra de cobertura por curso; foto o galería → confirmación → comparación semántica contra el temario → estados dominado / parcial / ausente → porcentaje de cobertura → quiz de los vacíos → **delta**: cuánta cobertura había antes y qué temas cambiaron de estado. Gráfica de progreso semanal. Vista docente con vacíos frecuentes del grupo.

Es el competidor más fuerte por foco y claridad, no por innovación.

### Charlie — Dave Navarro · Keilor Vargas · Kenneth Jara
Estudiante y administrador; decidieron no incluir rol docente. Flashcards con autoevaluación; modo offline con descarga de temarios y quizzes; **panel administrativo con llamadas a la API, costo y suspensión por consumo excesivo**.

Autores de NotaViva, el enunciado guía del curso. Único otro equipo que aborda el costo de IA: ellos lo **monitorean**, Glifo lo **reduce por arquitectura**.

### Delta — Nicole Masis · Brenda Serrano · Mariana Madrigal
**La carta del estudiante**: suben el documento oficial del curso y la IA extrae el temario y la **rúbrica de evaluación con ponderaciones**, de ahí llenan un calendario con fechas de exámenes. Fallback manual para rubros que la IA no extraiga.

Única propuesta con un motivador extrínseco auténtico: conecta el estudio con la nota y la fecha.

### Echo — Josh Gámez · Andrel Ramírez · Josué Ulloa — «Modus»
La propuesta conceptualmente más ambiciosa. Analiza apuntes de varios estudiantes para construir un **perfil del docente**: detectar el método y estilo de evaluación de cada profesor. Modelo de datos con patrones, versiones de patrón, evidencia y contribuciones.

Único equipo que redefine el problema. Requiere masa crítica de usuarios aportando material, lo que hace difícil demostrarlo con pocos participantes.

### Sierra — Sebastián Benavides · Mishelle Rojas · Johnny
El docente crea cursos y sube contenido; los estudiantes se inscriben por **código QR con aprobación pendiente**. Cobertura, vacíos, material reforzado. La IA declara si pudo entender los apuntes. **Capturas en cola sin conexión**. Notificaciones espaciadas configurables por el usuario.

### Tango — Andrey Solís · Ariana Quirós · Javier Garita
El más extenso: tres roles completos, centro de repaso, calendario de estudio, historial, logros y rachas, mapa de aprendizaje, compartir apuntes, filtros y búsqueda, resumen semanal por correo. Único equipo que mencionó patrones de diseño explícitamente. Modelo de datos con un orquestador de IA de tres servicios.

Recibió la indicación de enfocarse: tienen más funciones de las que pueden implementar.

### Zulu — Fernán Mesén · Sharon Araya · Alice Fajardo · Dayron — «Apuntes IA»
Autenticación con Google; registro manual de materias; carga del programa del curso en PDF, manual o desde galería. Captura con metadatos de clase y fecha. **La taxonomía de estados más clara de la cohorte**: sólido, débil y faltante, con criterios explícitos. Refuerzo generado solo para temas débiles o faltantes.

### X-Ray — Brandon Brenes · David González · Felipe Ugalde — «Glifo»
Motor de ingesta resiliente. Ver `Glifo_Alcance.md`.

---

## 2. Mapa de saturación

Cuántos de los nueve equipos incorporan cada elemento:

| Funcionalidad | Equipos | ¿Diferencia? |
|---|---|---|
| Foto → IA → apunte estructurado | 9 / 9 | No, es el enunciado |
| Quiz generado por IA | 9 / 9 | No |
| Comparación contra temario y cobertura | 8 / 9 | No |
| Estados por tema | 6 / 9 | No |
| Rol o vista docente con vacíos del grupo | 6 / 9 | No |
| Flashcards | 5 / 9 | No |
| Notificaciones o recordatorios | 5 / 9 | No |
| Inscripción por código | 4 / 9 | No |
| Modo offline | 3 / 9 | No |
| Rachas, logros o comparación social | 2 / 9 | Ocupado |
| Repetición espaciada | 2 / 9 | Ocupado |
| Costo de IA | 2 / 9 | Sí, articulando monitorear frente a reducir |
| Rúbrica y calendario de exámenes | 1 / 9 | De Delta |
| Perfil del docente | 1 / 9 | De Echo |
| **Preprocesamiento de imagen y OCR local** | **1 / 9 — solo X-Ray** | **Sí** |
| **Respaldo OCR → visión** | **0 / 9** | **Libre** |
| **Fórmulas y diagramas manuscritos** | **0 / 9** | **Libre** |
| **Superficie de incertidumbre para el usuario** | 1 parcial | **Casi libre** |

Las tres últimas filas son el territorio de Glifo, y son la misma idea desde ángulos distintos.

---

## 3. Elementos adoptados de otros equipos

| Elemento | Origen | Aporte |
|---|---|---|
| Delta de cobertura entre sesiones | Bravo | La mejor comunicación de valor de la cohorte, sin costo de IA |
| Estados con definición operativa | Zulu | Vocabulario con criterio explícito |
| Fallback manual del temario | Delta | Elimina un punto de falla; el docente lo valoró en clase |
| Cola de captura offline | Sierra | Núcleo del tema de investigación aplicada |
| Autoevaluación en flashcards | Charlie | Entrada barata a la repetición espaciada |
| Inscripción por código | Alfa, Tango, Sierra | Habilita el segundo rol sin flujo de aprobación |
| Orquestador de IA explícito en el diagrama | Tango | Hace visible la decisión arquitectónica |
| Instantáneas y gráfica de progreso | Bravo | Aprovecha datos que ya existen |

---

## 4. Diferenciación frente a cada equipo

| Equipo | Su fuerte | Separación de Glifo |
|---|---|---|
| Alfa | Motivación social | Glifo compite en fidelidad del insumo, no en retención |
| Bravo | Ejecución limpia y medición | Misma medición, sobre datos que Glifo garantiza y Bravo asume |
| Charlie | Offline y panel de costo | Charlie mide el gasto; Glifo lo decide por región en tiempo real |
| Delta | Rúbrica y calendario | Delta optimiza el cuándo estudiar; Glifo, el qué está realmente escrito |
| Echo | Perfil del docente | Echo necesita muchos usuarios; Glifo funciona con una sola foto |
| Sierra | Cobertura, cola offline, QR | Sierra dice si entendió; Glifo dice qué parte, con qué confianza y cómo la reparó |
| Tango | Amplitud y patrones | Tango es ancho; Glifo es profundo en un punto |
| Zulu | Taxonomía clara de estados | Glifo adopta el vocabulario y añade el eje de confianza |

---

## 5. Temas de investigación aplicada

Cada equipo expone su tema el 11 de noviembre, con ronda de preguntas evaluada en ambas direcciones.

**Tema de X-Ray:** almacenamiento local y sincronización de datos.

### Preguntas previsibles hacia X-Ray

| Origen probable | Pregunta |
|---|---|
| Nube e imágenes | ¿Por qué procesar en el dispositivo si la nube lo hace mejor? |
| NoSQL | ¿Por qué JSONB y no un motor documental? |
| Analítica | ¿Cómo saben que su sincronización funciona? ¿Qué miden? |
| Push y FCM | ¿Cómo avisan cuando la sincronización termina en segundo plano? |
| Pruebas automatizadas | ¿Cómo prueban la resolución de conflictos? |
| Biometría | Los apuntes locales son datos sensibles: ¿cómo los protegen en reposo? |
| Multiplataforma | Room es solo Android: ¿cómo portarían esto? |
| APIs REST | ¿Qué pasa si el backend responde a medias durante una sincronización? |

Las dos críticas son la primera —ahí se juega la tesis del proyecto— y la última, porque **la sincronización parcial es el punto débil real de cualquier diseño offline**.

### Técnicas de otros temas aplicables a Glifo

| Tema | Aplicación |
|---|---|
| Analítica y monitoreo | Eventos personalizados para medir la escalera; Crashlytics por las bibliotecas nativas |
| Procesamiento de imágenes en la nube | Compresión, resolución y formato antes de subir recortes a N2 |
| Notificaciones push con FCM | Aviso al completarse la cola de sincronización |
| Bases de datos NoSQL | Posición defendible sobre JSONB frente a un motor documental |
| Pruebas automatizadas | El ConfidenceScorer es función pura y el conjunto de calibración es su fixture |
| Multiplataforma híbrido | Argumento de por qué Glifo es nativo por necesidad: OpenCV, ML Kit y CameraX |
