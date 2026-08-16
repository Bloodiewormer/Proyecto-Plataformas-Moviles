# NotaViva — Contexto del Curso EIF411

> **Propósito:** documento de entrada neutral. Registra el marco fijo en el que se desarrolla el proyecto — calendario, entregables obligatorios, stack exigido y tiempo disponible — más el enunciado de NotaViva tal como fue planteado y seleccionado.
>
> No contiene decisiones de diseño, arquitectura ni alcance del equipo. Esas viven en el documento de visión del proyecto, que se mantiene por separado.
>
> **Última actualización:** 13 de agosto de 2026.

---

## 1. Identificación del curso

| Campo | Valor |
|---|---|
| Curso | Diseño y Programación de Plataformas Móviles |
| Código | EIF 411 |
| Carrera | Ingeniería de Sistemas de Información (ISIN) — Bachillerato |
| Universidad | Universidad Nacional (UNA), Escuela de Informática, Facultad de Ciencias Exactas y Naturales |
| Nivel | III nivel, bachillerato |
| Periodo | II Ciclo 2026 |
| Modalidad | Presencial remoto |
| Naturaleza | Teórico-práctico |
| Créditos | 4 |
| Horas semanales | 11 (2 teoría · 2 laboratorio · 7 estudio independiente) |
| Sesión | Miércoles 18:00 – 21:20 |
| Consulta | Martes 17:00 – 19:00 |
| Docente | Maikol Guzmán Alán — maikol.guzman.alan@una.cr |
| Requisito | EIF209 Programación IV |
| Correquisito | No aplica |

**Equipo:** X-Ray — **Brandon Brenes, David González y Felipe Ugalde (3 personas)**, en todos los rubros incluido el artículo científico.

---

## 2. Evaluación

| Rubro | Valor |
|---|---|
| Laboratorios en clase | 60 % |
| Artículo científico (en inglés, grupal — X-Ray lo hace entre los 3) | 10 % |
| Proyecto final | 30 % |
| **Total** | **100 %** |

- El proyecto final es trabajo en grupo con **calificación individual**, según el aporte de cada estudiante.
- Nota mínima de aprobación: **70 %**.
- No hay examen extraordinario; la nota de aprovechamiento es la suma directa de los rubros.

### Reglas de entrega

1. Todo entregable que no se presente a tiempo, no compile o no tenga funcionalidad mínima se califica con **0**.
2. **Funcionalidad mínima = al menos el 40 %** de la funcionalidad solicitada, funcionando al momento de la ejecución.
3. El plagio en código, documentación o algoritmos activa el reglamento interno de la UNA.
4. **Los laboratorios NO se suben a ninguna plataforma.** Se revisan en vivo, en clase, en modo defensa. El docente fue explícito: *"yo no reviso nada en el aula virtual, nada, absolutamente nada"*. El aula virtual se usa únicamente para publicar la nota.
5. El **código sí se sube a GitHub Classroom**.
6. **Ciclo de corrección:** las observaciones de un laboratorio se corrigen y se vuelven a presentar en vivo la semana siguiente, al final de la clase. El docente devuelve el 100 % de la nota si la corrección está completa: *"si me lo enseñan, les devuelvo el 100 % la nota"*. Si no se presenta la corrección, la nota queda como está.

### Consecuencia práctica sobre las fechas

Como todo se defiende en clase y la sesión es el **miércoles 18:00–21:20**, la fecha operativa de cada laboratorio es **la última clase del miércoles anterior al cierre**, no la fecha de cierre del calendario. Ver §4.1.

---

## 3. Contenido del programa

### Objetivo general
Introducir al estudiante en el tema de los dispositivos móviles contemplando conceptos, arquitectura, características, categorías y alcance del paradigma.

### Objetivos específicos
1. Conceptos, arquitecturas y categorías de dispositivos móviles.
2. Elementos de GUI y animación.
3. Programación distribuida mediante sockets e hilos.
4. Almacenamiento en dispositivos móviles como sistema persistente.
5. Visualización en emulador y ejecución en dispositivo.
6. Análisis de tecnologías móviles dominantes del mercado.
7. Integración de servicios de IA (modelos de lenguaje, agentes y protocolos como MCP) como componentes funcionales en aplicaciones móviles nativas.

### Módulos

| Módulo | Semanas | Temas |
|---|---|---|
| **1. Patrones de diseño** | 2 | MVC, MVP, MVVM, Singleton, Observer, Iterator · Fundamentos de Kotlin |
| **2. Introducción a plataformas móviles** | 2 | Arquitecturas iOS/Android/Windows Phone · Historia de smartphones y tablets · Frameworks · Configuraciones y perfiles · Emuladores (mín. 2) · Generación de APK |
| **3. GUI y prototipado asistido por IA** | 4 | Prototipado en papel y mockups · Layouts XML (legado) y Jetpack Compose · Activities, Widgets, Eventos · Usabilidad, internacionalización, accesibilidad, estilos |
| **4. Programación distribuida y redes** | 3 | Tareas asíncronas · Comunicación entre procesos · Sockets, HTTP/HTTPS, push, XML/JSON, NFC, Bluetooth, WiFi · Media (cámara, audio, video, localización) · Front-end ↔ back-end |
| **5. Tipos de almacenamiento** | 2 | Archivos locales (binarios y texto) · Bases de datos locales y remotas · Almacenamiento en la nube |
| **6. Temas de actualidad** | 2 | Seguridad (cuentas, permisos, firma, roles, datos sensibles) · Rendimiento (memoria, batería, fugas, bitácoras) · Publicación en tiendas · Integración de IA generativa |

### Eje transversal declarado

El programa incorpora la IA generativa **como componente estructural** de las aplicaciones desarrolladas, incluyendo integración de LLMs y protocolos de interoperabilidad como **MCP (Model Context Protocol)**.

Submódulo 6.4:
- IA generativa embebida en apps (asistentes, generación de contenido, recomendación).
- Consumo de APIs de LLMs (OpenAI, Claude, Gemini) desde Android con Kotlin.
- MCP: concepto y casos de uso para conectar apps con herramientas externas.
- Consideraciones de costo, privacidad y manejo de datos sensibles al integrar IA.

### Estrategia metodológica
1. Laboratorios grupales — X-Ray los trabaja entre los 3 integrantes.
2. Artículo científico en inglés, grupal — los 3 integrantes.
3. Proyecto final de desarrollo móvil.

Cada una de las 15 sesiones incluye laboratorio, tarea de investigación y evaluación corta.

---

## 4. Calendario de entregables

### 4.1 Tabla maestra

| # | Entregable | Apertura | Cierre oficial | **Defensa real (miércoles)** | Estado |
|---|---|---|---|---|---|
| Lab 1 | Wireframes (Figma) + diagrama de entidades/clases | mié 29 jul | jue 13 ago | **defendido mié 12 ago** | **85** — corrección el **mié 19 ago** |
| Lab 2 | Activities en Android/Kotlin | sáb 8 ago | sáb 29 ago | **mié 26 ago** | abierto |
| Lab 3 | Manejo de errores, Hilt, Retrofit, consumo de APIs de prueba | mié 26 ago | dom 13 sep | **mié 9 sep** | — |
| Lab 4 | Fundamentos del backend y persistencia de datos | mié 9 sep | sáb 26 sep | **mié 23 sep** | — |
| Lab 5 | Servicios, Web API, DTO y testing | mié 23 sep | sáb 17 oct | **mié 14 oct** | — |
| Lab 6 | Seguridad, Inteligencia Artificial y Cloud | mié 30 sep | sáb 31 oct | **mié 28 oct** | — |
| — | Investigación aplicada | mié 11 nov | dom 15 nov | **mié 11 nov** | — |
| — | Proyecto Final | mié 11 nov | dom 15 nov | **mié 11 nov** | — |

> La columna de defensa real adelanta cada entrega entre 1 y 4 días respecto al cierre oficial. El caso más apretado es el **Lab 2: 2 semanas efectivas, no 3**, y una de ellas compartida con la corrección del Lab 1 (19 ago).

### 4.2 Solapamientos

```
AGO        |13|    |20|    |27|      SEP |03| |10| |17| |24|   OCT |01| |08| |15| |22| |29|  NOV |05| |12|15
Lab 1  ====X
Lab 2      ===============X
Lab 3              ==================X
Lab 4                        =================X
Lab 5                                  =========================X
Lab 6                                      =================================X
Inv/Final                                                                             ==X
```

- **26 ago – 13 sep:** Lab 2 (cierre) y Lab 3 abiertos.
- **23 – 26 sep:** Labs 4, 5 y 6 abiertos simultáneamente.
- **1 – 10 nov:** único periodo del semestre sin entregables abiertos.

### 4.3 Tiempo disponible

- Del 13 de agosto al 15 de noviembre: **~13.5 semanas**.
- Estudio independiente asignado por el programa: 7 h/semana por persona → **~95 h por persona**, **~285 h de equipo** (3 integrantes) en el semestre.
- Ese total incluye el artículo científico, las tareas de investigación y las evaluaciones cortas de cada sesión, no solo el desarrollo.
- Advertencia de capacidad: 3 personas no rinden 1.5× de 2. La coordinación tiene costo, y en las semanas de solapamiento (ver §4.2) la capacidad efectiva baja.

---

## 5. Detalle de cada laboratorio

### Lab 1 — Wireframes de la aplicación
**29 jul – 13 ago · defendido el 12 ago · nota 85**

Objetivo: desarrollar el prototipo de la aplicación utilizando **Figma** como herramienta de diseño, y el **diagrama de relación de entidades/clases (base de datos)**.

**Resultado obtenido — única deducción:**

| Rubro | Puntaje | Descriptor |
|---|---|---|
| Diagrama UML de Objetos de BD | **1.5 / 3** | *Diagrama presente pero con relaciones o entidades incompletas* |

Observaciones del docente sobre ese rubro:
- **Ocupan las tablas `privilege`, `roles`.**
- **Explorar el uso de JSON.**

Todos los demás rubros quedaron en su valor máximo. La observación que el docente hizo al cierre de la sesión sobre el segundo rol de usuario (*"al grupo X-Ray me faltó ver el rol diferente… es casi que pierden 50 % del curso"*) **no fue una deducción del Lab 1**: es un requisito hacia adelante que impacta el Lab 2, donde login, registro, menú y navegación deben existir para dos roles distintos.

**Corrección a presentar el miércoles 19 de agosto** para recuperar los 15 puntos.

### Lab 2 — Activities en Android
**8 ago – 29 ago**

Objetivo: desarrollar en Android con Kotlin las pantallas más importantes definidas en el prototipo. Elementos que el enunciado exige contemplar:
- Menú
- Login
- Registro
- Main
- Navegación
- Listas

### Lab 3 — Manejo de errores, Hilt, Retrofit y consumo de APIs de prueba
**26 ago – 13 sep**

Objetivo: evaluar la implementación de una arquitectura moderna en Android mediante:
- Inyección de dependencias con **Hilt**.
- Consumo de servicios web con **Retrofit** (GET, POST según corresponda).
- Deserialización automática con **Gson** o **kotlinx.serialization**.
- Buenas prácticas en el manejo de errores durante las llamadas a servicios.
- Integración con una **API simulada (mock)** para simular escenarios reales de consumo de datos.

### Lab 4 — Fundamentos del backend y persistencia de datos
**9 sep – 26 sep**

Objetivo: asegurar comprensión de conceptos base de backend y persistencia. Temas:
- Introducción al backend.
- Programación distribuida (conceptos y ejemplos básicos).
- Capa de datos: diseño de base de datos (modelado relacional, claves primarias y foráneas).
- Relaciones entre entidades.
- **Patrón Repositorio.**

### Lab 5 — Servicios, Web API, DTO y testing
**23 sep – 17 oct**

Objetivo: validar la implementación de lógica de negocio y exposición de servicios por medio de una API REST. Temas:
- Capa de servicios.
- Capa de WebAPI (controladores REST).
- DTOs de entrada y salida.
- Pruebas con **Postman** (endpoints, headers, status codes).

### Lab 6 — Seguridad, Inteligencia Artificial y Cloud
**30 sep – 31 oct**

Objetivo: evaluar aspectos avanzados como seguridad, integración con servicios externos e implementación en la nube. Temas:
- **Seguridad con JWT usando Spring Security.**
- **Integración con APIs externas** (ChatGPT, Claude, Gemini, etc.).
- **Cloud deployment** (Render, Railway, Heroku, AWS).

### Investigación aplicada
**11 nov – 15 nov**

Objetivo: integrar al proyecto un tema adicional, seleccionado al inicio del curso, que permita profundizar en aspectos complementarios del desarrollo de software. Se evalúa la **creatividad y efectividad** con que se apliquen los conocimientos investigados.

Requisitos de presentación:
- Introducción clara y concisa al tema, comprensible para compañeros no familiarizados con él.
- **Ronda de preguntas obligatoria:** cada grupo que no expone debe formular al menos una pregunta técnica o crítica al grupo expositor. Se puede designar un portavoz rotativo. Se evalúa la pertinencia (relación con el tema, comprensión del contenido) y el nivel de profundidad (superficial vs. reflexiva; detalles técnicos, implicaciones o aplicaciones).

**Tema asignado a X-Ray:** *Implementación de almacenamiento local y sincronización de datos.*

**Estado de los demás temas:**

| Tema | Cupo |
|---|---|
| Notificaciones push con Firebase Cloud Messaging | Lleno |
| Técnicas en la nube para procesamiento de imágenes | Lleno |
| **APIs / MCP REST públicas para obtener y mostrar datos** | **Libre (0/1)** |
| Bases de datos NoSQL (Firebase, MongoDB) | Lleno |
| Análisis y monitoreo (Firebase Analytics, Crashlytics, AppCenter) | Lleno |
| **Pruebas automatizadas (unitarias, integración y UI)** | **Libre (0/1)** |
| Autenticación biométrica | Lleno |
| Desarrollo multiplataforma (React Native, Flutter, Ionic) | Lleno |
| MCP Server | Lleno |
| Almacenamiento local y sincronización de datos | Asignado a X-Ray |
| Localización y soporte multiidioma | Lleno |

Los dos temas con cupo libre siguen disponibles para cambio.

### Proyecto Final
**11 nov – 15 nov**

Objetivo: culminar el desarrollo del prototipo integrando todos los laboratorios del curso. La entrega debe incluir:
1. Versión final de la aplicación distribuida mediante **Firebase App Distribution**.
2. Pantallas de inicio de sesión integradas con el sistema de seguridad basado en **JWT** y conectadas al **API del backend**.
3. Todos los ajustes necesarios para asegurar funcionamiento correcto y estable.
4. **Encuesta de satisfacción a al menos 5 personas externas al grupo**, considerando:
   - Aspectos generales de usabilidad y funcionalidad.
   - Recomendaciones de mejora.
   - Identificación de errores o fallos detectados durante la prueba.

---

## 6. Stack exigido por el curso

Cada elemento aparece evaluado en un laboratorio específico. Es el conjunto mínimo obligatorio, independientemente de las decisiones técnicas propias del equipo.

| Capa | Tecnología | Exigido en |
|---|---|---|
| Lenguaje / IDE | Kotlin, Android Studio | Todo el curso |
| Arquitectura | MVVM (entre los patrones del Módulo 1) | Módulo 1 |
| UI | Jetpack Compose (o layouts XML legado) | Módulo 3 / Lab 2 |
| Prototipado | Figma | Lab 1 |
| Inyección de dependencias | Hilt | Lab 3 |
| Cliente HTTP | Retrofit + Gson / kotlinx.serialization | Lab 3 |
| Persistencia local | Base de datos local | Módulo 5 / Investigación aplicada |
| Backend | Spring Boot (implícito por el uso de Spring Security) | Labs 4–6 |
| Patrones de datos | Repositorio, DTOs | Labs 4–5 |
| Seguridad | JWT + Spring Security | Lab 6 / Proyecto Final |
| Despliegue | Render, Railway, Heroku o AWS | Lab 6 |
| Distribución | Firebase App Distribution | Proyecto Final |
| Pruebas de API | Postman | Lab 5 |
| IA generativa | API externa de LLM (OpenAI, Claude, Gemini) | Lab 6 / Módulo 6 |
| Emuladores | Al menos 2 configurados | Módulo 2 |

---

## 7. Enunciado del proyecto: NotaViva

*Fuente: matriz How–Now–Wow de la cohorte, formulario cerrado el 29 de julio de 2026.*

### 7.1 Definición

**NotaViva** — propuesta original del Grupo Charlie (Keneth Jara Herrera), planteada con GPT-4o-mini (visión + texto). Fue **seleccionada como aplicación guía del curso**, por lo que los nueve grupos desarrollan variantes de ella.

> El estudiante fotografía sus apuntes, la IA los reconstruye como texto estructurado, los audita contra el temario oficial del curso y genera quizzes sobre los vacíos detectados. El ciclo cierra: **organizar → auditar → reforzar**.

### 7.2 Evaluación obtenida

Ponderación de la matriz: IA central 35 % · Viabilidad 30 % · Innovación 20 % · Cobertura del curso 15 %.

| Eje | NotaViva |
|---|---|
| IA central | 9.5 |
| Viabilidad | 8.0 |
| Innovación | 8.5 |
| Cobertura del curso | 9.5 |
| **Total** | **8.83 — 1.º de 9, cuadrante WOW** |

### 7.3 Justificación de la selección

Es la única propuesta que cubre los cinco ejes técnicos del curso sin forzarlos:
- Cámara como entrada indispensable, no decorativa
- Autenticación
- CRUD real
- Tres roles
- Notificaciones push
- Tres llamadas de IA con propósitos distintos: **visión, comparación semántica y generación**

Ventaja pedagógica señalada: el caso de uso es el propio curso — se puede cargar el temario de EIF411 y probar la app contra el material real de las clases.

### 7.4 Riesgo identificado en el enunciado

> **Riesgo:** el OCR sobre letra manuscrita de baja calidad es el punto frágil.
>
> **Mitigación planteada en la propuesta original:** pantalla de revisión donde el estudiante corrige la transcripción antes de que la IA compare. Reduce la dependencia del OCR sin desmontar el flujo.

### 7.5 Señales de la cohorte aplicables al proyecto

| Señal | Contenido |
|---|---|
| **Colisión de alcance** | NotaViva (Charlie) y cUNA (Bravo) proponen el mismo mecanismo: comparar apuntes del estudiante contra material oficial para detectar vacíos. La observación recomienda asignar territorios distintos antes de arrancar. |
| **API key en el cliente** | Aplica a las nueve propuestas: la llave del modelo no puede vivir en el APK. Se señala la necesidad de definir un backend intermedio mínimo antes de la primera llamada al modelo. |
| **Nivel de la cohorte** | Ningún equipo planteó un chatbot pegado al final; 6 de 9 propuestas describen la IA operando dentro del flujo, con entrada y salida definidas. |
| **Cobertura de sensores** | Tres propuestas de la cohorte quedaron sin capacidades nativas. NotaViva no está entre ellas: la cámara es entrada obligatoria del flujo. |

### 7.6 Nota sobre el equipo X-Ray

La propuesta original de X-Ray fue **Simula** (David González): entrenador conversacional con IA. Obtuvo la puntuación más alta de la cohorte en IA central (10.0) y la más baja en cobertura del curso (4.0), por ser una aplicación exclusivamente de texto, sin cámara, GPS ni permisos.

Al desarrollar NotaViva, el equipo trabaja sobre un enunciado con cobertura de curso 9.5.

---

## 8. Requisitos obligatorios consolidados

Lista de verificación derivada únicamente de los enunciados oficiales, sin decisiones de diseño:

- [ ] Wireframes en Figma
- [ ] Diagrama de relación de entidades/clases
- [ ] Menú, login, registro, main, navegación y listas en Kotlin
- [ ] Hilt (inyección de dependencias)
- [ ] Retrofit + deserialización automática
- [ ] Manejo de errores en llamadas a servicios
- [ ] Consumo de API mock
- [ ] Base de datos con modelado relacional y relaciones entre entidades
- [ ] Patrón Repositorio
- [ ] Capa de servicios
- [ ] Controladores REST con DTOs de entrada y salida
- [ ] Pruebas de endpoints con Postman
- [ ] JWT + Spring Security
- [ ] Integración con API externa de LLM
- [ ] Backend desplegado en la nube
- [ ] Al menos 2 emuladores configurados
- [ ] APK distribuido por Firebase App Distribution
- [ ] Encuesta de satisfacción a 5 personas externas
- [ ] Investigación aplicada presentada, con ronda de preguntas
- [ ] Artículo científico en inglés

### 8.1 Requisitos transversales dictados en clase (sesión 12 ago)

Aplican a los nueve grupos y no aparecen en los enunciados escritos:

- [ ] **Mínimo 2 roles de usuario** — el docente lo calificó de mandatorio: *"si no tienen dos, es casi que pierden 50 % del curso"*. El enunciado de NotaViva contempla 3.
- [ ] **Tablas `users` · `roles` · `privileges` + puentes `user_roles` · `role_privileges`**, con el esquema exacto dictado por el docente
- [ ] **`users` en plural** — `user` es palabra reservada en Postgres
- [ ] **Todos los nombres de entidades en inglés**
- [ ] **PostgreSQL obligatorio para todos los grupos** como base de datos principal
- [ ] **Menú de navegación obligatorio** en la app
- [ ] **Si se implementan notificaciones, deben ser push reales** — no simuladas
- [ ] **Si se implementa modo offline, debe ser real**, con base de datos en el dispositivo; si no, declararlo explícitamente como funcionalidad futura
- [ ] **Autenticación conforme al estándar del framework** (Spring Security)
- [ ] **Nombre de aplicación distinto** al de los demás grupos
- [ ] **Paleta de color propia** — el docente desaconsejó explícitamente el morado por defecto de las herramientas de IA
- [ ] **Evaluar campos JSON/JSONB** para quizzes, preguntas, opciones y flashcards, en lugar de normalizarlo todo
- [ ] **Reducir el tamaño del modelo de datos** — *"bases de datos muy grandes se vuelven hiper complejas de manejar"*
- [ ] **`SharedPreferences`** para configuración de la app, no una tabla de settings
- [ ] **Documentar el ejercicio de diálogo con IA** sobre el modelo de datos — el docente lo pidió a todos los grupos y forma parte de su enfoque del curso
- [ ] Arquitectura **monolítica** en esta etapa; elementos serverless más adelante
- [ ] Se permite **fragmentar** la persistencia: la principal es relacional, pero una sección concreta puede moverse a NoSQL
- [ ] Está **explícitamente permitido inspirarse en las propuestas de los otros grupos**

---

## 9. Bibliografía del curso

- AlMulla, B., Assi, M., & Hassan, S. (2026). *Understanding the Challenges and Opportunities of Generative AI Apps: An Empirical Study.* arXiv:2506.16453
- Anthropic (2024). *Introducing the Model Context Protocol.*
- Model Context Protocol (2024). Especificación oficial y documentación técnica.
- McWherter, J., & Gowell, S. (2012). *Professional mobile application development.* John Wiley & Sons.
- Miguel, R. M. (2012). *Desarrollo de aplicaciones para Android.* Grupo Editorial RA-MA.
- Robledo, D. (2016). *Desarrollo de aplicaciones para Android I.* Ministerio de Educación, Cultura y Deporte.
- Lecheta, R. R. (2017). *Android Essencial con Kotlin.* Novatec Editora.
- Fonseca Camargo, Y., Pertuz Toscano, K., & Martelo López, E. (2018). *Aplicaciones nativas vs. aplicaciones híbridas en el desarrollo de aplicaciones móviles para Android e iOS.*
- Sánchez Rueda, F. (2020). *Comparativa Kotlin y Java en desarrollo Android.*
