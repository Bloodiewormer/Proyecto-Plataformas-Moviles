
Necesito aplicar una decisión de diseño ya cerrada (D-12) en todo el proyecto. **No rediseñes nada, no cambies layouts, no toques ningún otro token.** Es una sustitución de valores más la reescritura de dos párrafos que defendían la decisión anterior.

## Contexto

El token `escalated` (estado del mapa de confianza: la región requirió escalar a un modelo de visión, nivel N2) comparte actualmente el mismo valor hexadecimal que `alert` (error y acciones destructivas). Eso es un error: **escalar a visión es funcionamiento normal del pipeline, no una falla**, y pintarlo con el color del error contradice la tesis central del producto. `escalated` pasa a tener un ámbar propio.

## Cambio 1 — Valores de token

En **todos** los archivos que declaren tokens (`src/App.tsx` en las constantes `NIGHT` y `DAY`, y los bloques `.night{}` / `.day{}` de `src/imports/Glifo_Paleta_Modos.html`, `Glifo_Prototipo.dc.html`, `Glifo_T1_Wireframes.html`, `Glifo_T2_Wireframes.html`, `Glifo_T3_Wireframes.html`, `Glifo_T4_Wireframes.html`, `Glifo_T5_Wireframes.html`):

**Modo noche**

| Token | Valor actual | Valor nuevo |
|---|---|---|
| `escalated` | `#E0693A` | `#F59E0B` |
| `escalatedSoft` | `rgba(224,105,58,.18)` | `rgba(245,158,11,.18)` |
| `escalatedFaint` | `rgba(224,105,58,.09)` | `rgba(245,158,11,.09)` |
| `escalatedLine` | `rgba(224,105,58,.45)` | `rgba(245,158,11,.45)` |

**Modo día**

| Token | Valor actual | Valor nuevo |
|---|---|---|
| `escalated` | `#B94117` | `#D97706` |
| `escalatedSoft` | `rgba(185,65,23,.14)` | `rgba(217,119,6,.14)` |
| `escalatedFaint` | `rgba(185,65,23,.07)` | `rgba(217,119,6,.07)` |
| `escalatedLine` | `rgba(185,65,23,.42)` | `rgba(217,119,6,.42)` |

**Crítico:** `alert`, `alertSoft`, `alertFaint` y `alertLine` **no cambian**. Siguen en `#E0693A` (noche) y `#B94117` (día) con sus mismas rgba. Los dos grupos usaban literalmente los mismos números rgba, así que sustituye únicamente donde el nombre de la propiedad empiece por `escalated`.

## Cambio 2 — Corregir la narrativa en `Glifo_Paleta_Modos.html`

1. En el swatch de modo día del token `alert`, la descripción dice `Error, destructivo y escalado`. Debe decir `Error y destructivo`.
2. En el bloque «Escala de confianza», el swatch **Escalado** muestra `#B94117 · nativo` con la leyenda `Triángulo · N2 · = alert`. Debe mostrar el color `#F59E0B`, la etiqueta `#F59E0B · ámbar propio` y la leyenda `Triángulo · N2 · ≠ alert`.
3. El párrafo `<p class="note">` que empieza con **«Escalado y alert comparten token.»** debe reemplazarse por:

> **Escalado y alert ya no comparten token (D-12).** Una revisión anterior los unificó en el rust, con el argumento de que escalar cuesta dinero y el triángulo ya avisa. El argumento se cayó al revisar el discurso del producto: *escalar a visión es funcionamiento normal del pipeline, no una falla*, y presentarlo con el color del error contradice la tesis central del proyecto. **Escalado** pasa a un ámbar propio —`#F59E0B` en noche, `#D97706` en día—, distinguible de *verified* y de *alert* en ambos modos; el rust queda exclusivamente para lo destructivo. El verde salvia sigue siendo la única incorporación ajena a la paleta: sin él, «lo leí bien» y «me costó dinero» caerían en la misma familia cálida, que es justamente la distinción que sostiene el proyecto.

## Cambio 3 — Corregir `src/imports/Glifo_Biblioteca_Componentes.md`

1. En la tabla de la sección **2.3 Estados de confianza**, la fila `Escalado` debe quedar: `| Escalado | \`#F59E0B\` | \`#D97706\` | Triángulo | N2 |`
2. El párrafo que empieza con **«Escalado y `alert` comparten valor.»** se reemplaza por:

> **Escalado y `alert` NO comparten valor (D-12).** Una revisión anterior los unificó en el óxido, con el argumento de que escalar cuesta dinero y el triángulo ya avisa. El argumento no sobrevivió al discurso del producto: **escalar a visión es funcionamiento normal del pipeline, no una falla**, y pintarlo con el color del error contradice la tesis central del proyecto. `escalated` tiene ámbar propio —`#F59E0B` noche, `#D97706` día—, distinguible de `verified` y de `alert` en ambos modos. El óxido (`#E0693A` / `#B94117`) queda exclusivamente para lo destructivo.

3. El párrafo que empieza con **«Consecuencia que hay que aceptar.»** se reemplaza por:

> **Consecuencia que hay que aceptar.** Con `escalated` en ámbar, el par verde/ámbar —el peor discriminado por quienes tienen deficiencia en la percepción del color— vuelve a estar dentro del mapa de confianza. Por eso la doble codificación deja de ser un refuerzo y pasa a ser obligatoria: dentro de texto corrido el portador es el subrayado (sólido contra punteado) más el relleno y la etiqueta textual; la forma geométrica sobrevive solo como chip compacto en listas y encabezados, donde hay sitio para ella.

4. El encabezado de la sección 2.3 dice «Cada estado lleva **forma propia** además de color». Debe decir «Cada estado lleva **codificación no cromática** además de color — cerca del 8 % de los hombres presenta alguna deficiencia en la percepción del color, y el par verde/ámbar es el peor discriminado. Dentro de texto corrido: subrayado sólido o punteado, relleno y etiqueta textual. En listas y encabezados: chip con forma propia.»

## Lo que NO debes cambiar

- La codificación geométrica del componente `Chip` (círculo, cuadrado, triángulo, círculo punteado). Sigue siendo válida en listas y encabezados; solo se retiró como portador único dentro de texto corrido, y el prototipo ya usa subrayado + relleno + etiqueta ahí.
- Ningún otro token: `background`, `surface`, `surfaceHigh`, `border`, `accent`, `accentText`, `onAccent`, `textPrimary`, `textSecondary`, `verified`, `repaired`, `uncertain`, `alert*`, `btnSec*`, `neutralSoft`, `scrim`.
- Layouts, tipografías, espaciados, textos de interfaz, navegación, ni el contenido de ninguna pantalla.

## Verificación al terminar

Reporta: cuántos archivos modificaste, y confirma que una búsqueda de `escalated` seguida de `#E0693A`, `#B94117`, `rgba(224,105,58` o `rgba(185,65,23` devuelve **cero** resultados, mientras que `alert` seguido de esos mismos valores sigue devolviendo resultados.
