# Glifo — Biblioteca de componentes

**Grupo X-Ray** — Brandon Brenes · David González · Felipe Ugalde
Especificación para Figma · versión 2.0 — paleta «piedra y ámbar», doble modo

> Se construye **antes** que cualquier pantalla. Los cuatro componentes marcados como
> núcleo aparecen en 20 de las 39 pantallas; rehacerlos después obliga a retocar todo.
>
> **Cambio respecto a la versión 1.0:** paleta nueva y soporte de modo día. La versión
> anterior (fondo azul profundo, acento turquesa, dorado heráldico) queda derogada.

---

## 0. Configuración del archivo

| Parámetro | Valor |
|---|---|
| Marco base | **360 × 800** (Android compacto) |
| Unidad | 1 px de Figma = 1 dp |
| Rejilla de columnas | 4 columnas · margen 16 · canalón 16 |
| Rejilla de línea base | 4 dp |
| Modos de color | **Noche** (predeterminado) y **día** |

En Figma los dos modos se implementan como **variable modes** sobre una única
colección de variables de color. Cada componente se dibuja una sola vez; el modo se
cambia desde el panel de la página. Si la versión de Figma disponible no admite
variable modes, se usan dos conjuntos de estilos con el mismo nombre y prefijo
`n/` y `d/`, y se duplica solo la página de pantallas finales.

**Páginas del archivo**

```
00 · Tokens
01 · Componentes
02 · T1 — Acceso y navegación         (19 ago · 11 pantallas)
03 · T2 — Captura y confianza          (26 ago ·  7 pantallas)
04 · T3 — Escalera y sincronización     ( 9 sep ·  4 pantallas)
05 · T4 — Cobertura y temario           (14 oct ·  5 pantallas)
06 · T5 — IA, estudio y administración  (28 oct · 12 pantallas)
07 · Flujos                              (prototipo enlazado)
```

**Nomenclatura de capas.** `<ID>/<nombre>` — `E2/Mapa de confianza`. El ID es el
mismo del inventario, de modo que un frame de Figma, una ruta de navegación y una
fila de la tabla de alcance se refieren siempre igual.

---

## 1. Concepto de la paleta

**Piedra y ámbar.** Azul pizarra, hueso, gris, ámbar y óxido. El pizarra y el hueso
**intercambian papeles entre modos**: en noche el pizarra es la superficie y el hueso
es el texto; en día el hueso es el fondo y el pizarra es el texto. Es lo que hace que
los dos modos se lean como la misma marca y no como una inversión mecánica.

Cinco colores base. Cada uno aparece de forma **nativa** —con su valor exacto— en al
menos un modo.

| Base | Papel en noche | Papel en día |
|---|---|---|
| `#2E3B4B` pizarra | Superficie | Texto principal |
| `#D7D1B9` hueso | Texto principal | Campo y superficie hundida |
| `#FFD372` ámbar | Acción y estado activo | Relleno de acción |
| `#B94117` óxido | Aclarado a `#E0693A` | Error y escalado |
| `#959595` gris | Texto secundario e incierto | Oscurecido a `#63666A` |

**Sobre la nomenclatura.** La paleta no se nombra por su referencia de origen en
ningún entregable. El documento de arquitectura ya restringe el uso de iconografía de
terceros y no conviene contradecirlo en la misma página.

---

## 2. Tokens de color

### 2.1 Superficie y texto

| Token | Noche | Día | Uso |
|---|---|---|---|
| `background` | `#161E27` | `#EDEAE0` | Fondo de aplicación |
| `surface` | **`#2E3B4B`** | `#F7F4EC` | Tarjetas, hojas, barra superior |
| `surfaceHigh` | `#3B4A5C` | **`#D7D1B9`** | Campos, miniaturas, superficie hundida |
| `border` | `#4A5A6E` | `#C4BCA3` | Divisores y bordes |
| `textPrimary` | **`#D7D1B9`** | **`#2E3B4B`** | Texto principal |
| `textSecondary` | **`#959595`** | `#63666A` | Texto secundario y etiquetas |

**El gris base no sirve como texto en modo día.** `#959595` sobre `#EDEAE0` da 2.6 : 1,
por debajo del mínimo de 4.5 : 1. Se oscurece a `#63666A`, que da 5.4 : 1. Es el único
punto donde la paleta base no alcanza por sí sola.

### 2.2 Acción

| Token | Noche | Día | Uso |
|---|---|---|---|
| `accent` | **`#FFD372`** | **`#FFD372`** | Relleno de botón primario, interruptor activo, FAB |
| `onAccent` | `#1A1206` | `#2E3B4B` | Texto sobre el relleno de acento |
| `accentText` | `#FFD372` | `#8A6210` | Acento como **texto, icono o trazo** |
| `accentSoft` | ámbar 16 % | oro oscuro 20 % | Fondo de chip, ítem activo del menú |
| `accentLine` | ámbar 42 % | oro oscuro 50 % | Borde de campo enfocado |

**Dos tokens de acento, no uno.** El ámbar funciona como relleno en ambos modos, pero
como texto sobre fondo hueso da 1.6 : 1 y es ilegible. `accent` rellena, `accentText`
escribe. Confundirlos es el error más probable al construir la biblioteca.

### 2.3 Estados de confianza

Escala funcional. **Cada estado lleva codificación no cromática además de color — cerca del 8 % de los hombres presenta alguna deficiencia en la percepción del color, y el par verde/ámbar es el peor discriminado. Dentro de texto corrido: subrayado sólido o punteado, relleno y etiqueta textual. En listas y encabezados: chip con forma propia.**

| Estado | Noche | Día | Forma | Nivel |
|---|---|---|---|---|
| Verificado | `#5FA88C` | `#2F7D62` | Círculo lleno | N1 |
| Reparado | `#8FB7DC` | `#3E6E9E` | Cuadrado | N1.5 |
| Escalado | `#F59E0B` | `#D97706` | Triángulo | N2 |
| Incierto | **`#959595`** | `#63666A` | Círculo punteado | — |

**Escalado y `alert` NO comparten valor (D-12).** Una revisión anterior los unificó en el óxido, con el argumento de que escalar cuesta dinero y el triángulo ya avisa. El argumento no sobrevivió al discurso del producto: **escalar a visión es funcionamiento normal del pipeline, no una falla**, y pintarlo con el color del error contradice la tesis central del proyecto. `escalated` tiene ámbar propio —`#F59E0B` noche, `#D97706` día—, distinguible de `verified` y de `alert` en ambos modos. El óxido (`#E0693A` / `#B94117`) queda exclusivamente para lo destructivo.

**Consecuencia que hay que aceptar.** Con `escalated` en ámbar, el par verde/ámbar —el peor discriminado por quienes tienen deficiencia en la percepción del color— vuelve a estar dentro del mapa de confianza. Por eso la doble codificación deja de ser un refuerzo y pasa a ser obligatoria: dentro de texto corrido el portador es el subrayado (sólido contra punteado) más el relleno y la etiqueta textual; la forma geométrica sobrevive solo como chip compacto en listas y encabezados, donde hay sitio para ella.

### 2.4 Derivados de superficie

| Token | Regla |
|---|---|
| `<estado>Soft` | El color del estado al 14–20 % · fondo de chip y resalte |
| `<estado>Faint` | Al 7–10 % · fondo de tarjeta de aviso |
| `<estado>Line` | Al 38–45 % · borde de tarjeta de aviso |
| `neutralSoft` | Gris al 14–18 % · insignias sin estado |
| `scrim` | Noche `rgba(8,12,17,.72)` · día `rgba(46,59,75,.5)` |

---

## 3. Tokens de tipografía

Familia **Inter**. Si no está disponible en el dispositivo, la del sistema.

| Estilo | Tamaño | Peso | Interlineado | Espaciado | Uso |
|---|---|---|---|---|---|
| `display` | 28 | 600 | 34 | −0.5 | Solo splash y marca |
| `title` | 22 | 500 | 28 | −0.2 | Título de pantalla |
| `subtitle` | 18 | 500 | 24 | 0 | Encabezado de sección, título de tarjeta |
| `body` | 16 | 400 | 24 | 0 | Texto corrido, campos |
| `bodyStrong` | 16 | 500 | 24 | 0 | Énfasis dentro de texto |
| `secondary` | 14 | 400 | 20 | 0 | Etiquetas, metadatos |
| `caption` | 12 | 500 | 16 | +0.4 | Insignias, contadores |
| `mono` | 14 | 400 | 20 | 0 | LaTeX crudo, códigos de curso |

`mono` — JetBrains Mono o Roboto Mono. Se usa en dos sitios y solo dos: el LaTeX sin
renderizar y el código de inscripción del curso. Ambos son cadenas que el usuario
copia o transcribe, y ahí el ancho fijo evita confundir `0`/`O` y `1`/`l`.

---

## 4. Espaciado, radio y elevación

| Escala de espaciado | 4 · 8 · 12 · 16 · 24 · 32 |
|---|---|
| Margen lateral de pantalla | 16 |
| Separación entre tarjetas | 12 |
| Relleno interno de tarjeta | 16 |
| Separación entre secciones | 24 |

| Radio | Valor | Aplica a |
|---|---|---|
| `radiusControl` | 8 | Botones, campos, insignias |
| `radiusCard` | 12 | Tarjetas, hojas inferiores |
| `radiusFab` | 18 | Botón de acción flotante |
| `radiusFull` | 999 | Chips, avatar, indicadores |

**Elevación.** No se usan sombras en ninguno de los dos modos. La jerarquía se expresa
por superficie (`background` → `surface` → `surfaceHigh`) y por borde de 1 dp. En modo
noche una sombra no se percibe sobre el pizarra; en modo día ensuciaría el hueso.

| Altura de toque | Valor |
|---|---|
| Objetivo mínimo | 48 × 48 |
| Botón primario | 48 |
| Campo de texto | 56 |
| Ítem de lista | 72 mínimo · 56 en variante compacta |
| Barra superior | 56 |
| Barra de gestos de Android 14 | 26 · píldora de 118 × 4 |

---

## 5. Componentes núcleo

### 5.1 `ChipConfianza` — el componente firma

Aparece en el mapa de confianza, la lista de apuntes, la escalera en proceso, la
cobertura y el detalle de región.

| Propiedad | Tipo | Valores |
|---|---|---|
| `estado` | Variante | `verificado` · `reparado` · `escalado` · `incierto` |
| `tamaño` | Variante | `punto` (12) · `chip` (24) · `etiqueta` (28 con texto) |
| `mostrarNivel` | Booleano | Añade `N1` / `N1.5` / `N2` a la derecha |

**Construcción de la forma** — vectorial, no fuente de iconos:

| Estado | Geometría |
|---|---|
| Verificado | Círculo ⌀ 12, relleno sólido |
| Reparado | Cuadrado 11 × 11, radio 2, relleno sólido |
| Escalado | Triángulo equilátero, lado 13, radio de vértice 1.5, relleno sólido |
| Incierto | Círculo ⌀ 12, sin relleno, borde 1.5 discontinuo, guion 2 / hueco 2 |

**Variante `etiqueta`:** forma + texto en `caption`, relleno horizontal 8, altura 28,
radio `radiusFull`, fondo `<estado>Soft`.

> Prueba de aceptación: exportar el chip en escala de grises **en los dos modos**. Los
> cuatro estados deben seguir siendo distinguibles por forma. Con esta paleta la forma
> hace más trabajo que antes, así que la prueba dejó de ser opcional.

### 5.2 `BarraCobertura`

| Propiedad | Tipo | Valores |
|---|---|---|
| `variante` | Variante | `compacta` (altura 6) · `detallada` (altura 12 con leyenda) |
| `sólido` / `parcial` / `ausente` / `dudoso` | Número | Porcentaje por segmento |

Segmentos contiguos, sin separación, radio `radiusFull` solo en los extremos de la
barra completa. Reutiliza los cuatro colores de confianza: es deliberado, la escala de
certeza es la misma idea aplicada a dos objetos distintos.

### 5.3 `TarjetaCurso`

| Propiedad | Tipo | Valores |
|---|---|---|
| `rol` | Variante | `estudiante` · `docente` |
| `estado` | Variante | `normal` · `presionada` · `sinTemario` |

Estructura vertical, relleno 16, radio `radiusCard`, fondo `surface`, borde 1.

| Zona | Estudiante | Docente |
|---|---|---|
| Línea 1 | Nombre del curso · `subtitle` | Nombre del curso · `subtitle` |
| Línea 2 | Código · `mono` · `textSecondary` | Código · `mono` + nº de inscritos |
| Cuerpo | `BarraCobertura` compacta + % | Estado del temario + brechas del grupo |
| Pie | Delta desde la última sesión | Código de inscripción copiable |

El estado `sinTemario` sustituye el cuerpo por un aviso sobre `alertFaint` con borde
`alertLine` y la acción **Publicar temario**. Un curso sin temario no calcula
cobertura, y eso tiene que verse en la tarjeta, no descubrirse al entrar.

### 5.4 `ItemLista`

| Propiedad | Tipo | Valores |
|---|---|---|
| `tipo` | Variante | `apunte` · `tema` · `usuario` · `colaSync` |
| `estado` | Variante | `normal` · `presionado` · `deshabilitado` |

Altura mínima 72, relleno vertical 12, horizontal 16, divisor inferior 1 en `border`.

| Tipo | Izquierda | Centro | Derecha |
|---|---|---|---|
| `apunte` | Miniatura 48 × 48 radio 8 | Título + fecha de clase | Tira de `ChipConfianza` + `InsigniaNivel` |
| `tema` | `ChipConfianza` chip | Código + título del tema | Puntaje en `mono` |
| `usuario` | Avatar 40 | Nombre + correo | Chip de rol |
| `colaSync` | Miniatura 48 | Fecha + estado | Indicador o barra de progreso |

---

## 6. Componentes de estructura

### 6.1 `BarraSuperior`

Altura 56, fondo `surface`, borde inferior 1.

| Propiedad | Valores |
|---|---|
| `izquierda` | `menú` · `atrás` · `cerrar` |
| `título` | Texto en `title` o **conmutador de curso** |
| `derecha` | Hasta 2 acciones de 24 dp |

**Conmutador de curso activo** — reemplaza el título en las pantallas dependientes de
curso. Muestra el nombre en `subtitle` con cheurón; al tocar abre la hoja inferior de
selección. El curso elegido persiste en `SharedPreferences`, y es el único punto donde
ese requisito se manifiesta visualmente.

### 6.2 `Drawer`

Ancho 296, fondo `surface`, deslizante desde la izquierda sobre `scrim`.

| Zona | Contenido |
|---|---|
| Cabecera | Avatar 56 · nombre · correo · **chip de rol** |
| Curso activo | Nombre + acción de cambiar |
| Cuerpo | `ItemDrawer` filtrados por privilegio |
| Pie | Perfil · Ajustes · Cerrar sesión |

`ItemDrawer`: altura 48, icono 24, etiqueta en `body`, estado `activo` con fondo
`accentSoft`, texto e icono en `accentText` y barra izquierda de 3 dp en `accent`.

**El chip de rol no es decorativo.** El contenido del cuerpo cambia por completo entre
estudiante y docente: solo Perfil y Ajustes se repiten. Que los dos menús se vean
distintos al abrirlos es la demostración más barata de que hay dos roles reales.

### 6.3 `Boton`

| Jerarquía | Relleno | Texto | Borde |
|---|---|---|---|
| Primario | `accent` | `onAccent` | — |
| Secundario | Transparente | `accentText` | 1 en `accentText` |
| Texto | Transparente | `accentText` | — |
| Destructivo | Transparente | `alert` | 1 en `alert` |

Altura 48 (`sm` 40), radio `radiusControl`, texto en `bodyStrong`. Deshabilitado al
38 % de opacidad. Cargando: indicador de 20 dp, el texto permanece para que el ancho
no salte.

### 6.4 `CampoTexto`

Altura mínima 56, fondo `surfaceHigh`, radio `radiusControl`, borde 1 en `border`.

| Estado | Borde | Etiqueta |
|---|---|---|
| Reposo | `border` | `textSecondary` |
| Enfocado | 2 en `accentLine` | `accentText` |
| Error | 2 en `alert` | `alert` + mensaje debajo en `secondary` |
| Deshabilitado | `border` al 38 % | `textSecondary` al 38 % |

El mensaje de error dice qué pasó y cómo se arregla, en la voz de la interfaz:
«Las dos contraseñas deben coincidir», no «Campo inválido».

### 6.5 `EstadoVacio`

Glifo o ilustración 96, título en `subtitle`, descripción en `secondary`, y **siempre
una acción**. Una pantalla vacía es una invitación a actuar, no un aviso.

| Pantalla | Título | Acción |
|---|---|---|
| C1 sin cursos | Todavía no estás en ningún curso | Unirme con un código |
| E1 sin apuntes | Aquí van tus apuntes de clase | Tomar la primera foto |
| G1 sin pendientes | No hay repasos para hoy | Ver todos los temas |
| H1 cola vacía | Todo está sincronizado | — |

### 6.6 `InsigniaNivel`

Muestra el nivel del pipeline que resolvió una región o página: `N0` `N1` `N1.5` `N2`
`N3` `—`. Altura 20, relleno horizontal 6, radio `radiusControl`, `caption`, fondo
`<estado>Soft` correspondiente.

### 6.7 `BannerSincronizacion`

Franja de 40 dp bajo la barra superior. Estados: `sinConexion` (`alertSoft`), `enCola`
(`accentSoft`, con contador), `sincronizando` (`accentSoft`, indeterminado), `error`
(`alertSoft`, con acción **Reintentar**). Se oculta cuando no hay nada pendiente.

### 6.8 `PasoEscalera`

Exclusivo de la pantalla de procesamiento. Riel vertical de 24 dp con nodo circular y
línea de conexión.

| Estado del nodo | Relleno | Línea siguiente |
|---|---|---|
| Completado | `verified`, marca en `background` | `verified` |
| En curso | Borde `accent`, texto `accentText` | `border` |
| Pendiente | Borde `border`, número en `textSecondary` | `border` |

---

## 7. Orden de construcción

| Paso | Qué | Depende de |
|---|---|---|
| 1 | Colección de variables de color con los dos modos | — |
| 2 | Estilos de texto | — |
| 3 | `ChipConfianza` | 1 |
| 4 | `Boton`, `CampoTexto` | 1, 2 |
| 5 | `BarraCobertura` | 3 |
| 6 | `ItemLista`, `TarjetaCurso` | 3, 5 |
| 7 | `BarraSuperior`, `Drawer` | 4 |
| 8 | `EstadoVacio`, `InsigniaNivel`, `BannerSincronizacion`, `PasoEscalera` | 3, 4 |
| 9 | Tandas T1 a T5 | 1–8 |

Los pasos 3 y 4 son paralelizables entre dos personas. El 9 no arranca antes de
terminar el 8: cada frame dibujado con un componente provisional es un frame que hay
que rehacer, y ahora son 39.

---

## 8. Verificación antes de dar la biblioteca por cerrada

- [ ] Los cuatro estados de confianza se distinguen en escala de grises, **en los dos modos**
- [ ] Ningún texto por debajo de 4.5 : 1 de contraste sobre su fondo, **en los dos modos**
- [ ] `accent` no se usa nunca como color de texto; para eso está `accentText`
- [ ] Ningún objetivo de toque menor de 48 × 48
- [ ] Cada componente con variante de estado presionado
- [ ] Cada lista con estado vacío, cargando y error
- [ ] Ningún valor de espaciado fuera de la escala 4 · 8 · 12 · 16 · 24 · 32
- [ ] Ningún morado en el archivo — el docente lo desaconsejó explícitamente
- [ ] Ningún color de la versión 1.0 sobreviviente (`#3FC5C0`, `#C9A227`, `#0B1420`…)
