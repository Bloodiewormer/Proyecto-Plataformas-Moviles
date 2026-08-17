# Prototipo

Prototipo interactivo generado con **Figma Make** (React 19 + Vite + Tailwind CSS 4). Cubre las 39 pantallas del prototipo distribuidas en 5 tandas (T1-T5), más la paleta de modos día/noche.

**[Demo en vivo (GitHub Pages)](https://bloodiewormer.github.io/Proyecto-Plataformas-Moviles/)**

## Estructura

```
prototype/
├── src/
│   ├── App.tsx                          # Entry point de la app React
│   ├── main.tsx
│   ├── index.css
│   └── imports/
│       ├── Glifo_T1_Wireframes.html     # Tanda 1 (11 frames / 10 pantallas)
│       ├── Glifo_T2_Wireframes.html     # Tanda 2
│       ├── Glifo_T3_Wireframes.html     # Tanda 3
│       ├── Glifo_T4_Wireframes.html     # Tanda 4
│       ├── Glifo_T5_Wireframes.html     # Tanda 5
│       ├── Glifo_Paleta_Modos.html      # Paleta de colores día/noche
│       ├── Glifo_Prototipo.dc.html      # Documento de control del prototipo
│       ├── Glifo_Biblioteca_Componentes.md  # Tokens, tipografía, spacing, componentes
│       └── pasted_text/design-decision-d12.md
├── index.html
├── package.json
├── vite.config.ts
└── tsconfig.json
```

## Cómo correrlo localmente

Requiere Node.js y pnpm (o npm/yarn adaptando los comandos).

```bash
cd prototype
pnpm install
pnpm dev
```

Esto levanta un servidor local (Vite) donde se puede navegar el prototipo interactivo.

Para build de producción (por ejemplo, para publicar como demo estática):
```bash
pnpm build
pnpm preview
```

## Relación con la documentación

- Los tokens de color (`surfaceHigh`, `border`, etc.) definidos aquí son la fuente de verdad — están canonizados en `docs/Glifo_Arquitectura_Estandares.md`.
- **El prototipo tiene prioridad sobre la documentación cuando divergen.** Si encuentran una discrepancia, se actualiza el documento en `docs/`, no se revierte el prototipo.
- Issue conocido pendiente: los tokens `escalated` y `alert` comparten el mismo valor hex — contradice que la escalación es operación normal, no error. Ver `docs/Glifo_Bitacora_Decisiones.md`.
