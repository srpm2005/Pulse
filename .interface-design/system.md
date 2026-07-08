# Interface Design System

## Intent & Domain
**Verb:** Track prices. Assess positions. Trigger alerts.
**Feel:** Terminal-chic, precise, calm, and unmistakably data-focused.
**Domain Colors:** Phosphor green (#10b981), Stark red (#ef4444), Amber warning (#f59e0b).
**Depth Strategy:** High-contrast pure black surfaces offset by absolute 1px slate-grey borders. No shadows, no gradients.

## Typography (Dense Structure)
- **Base font:** `Inter` mapped through strictly tabular metrics (`tabular-nums`).
- **Base size:** `13px` for all standard views. `11px` for muted metadata. Small but incredibly legible due to contrast.
- **Weights:** Heavy use of 600/700 for distinct symbols and integers.

## Density
- **Spacing unit:** 4px.
- Tight layout bounds: `<div class="card">` utilizes `16px 20px` padding (reduced from 24px) for extreme visual packing. Standard inputs use `10px 12px`.
- High information packing in the navigation, shrinking vertical rhythms drastically.

## Layout & Layering
- **Backgrounds:** Canvas is `#000000`. Content cards are `#0a0a0a`. Strict 1-level elevation hierarchy.
- **Borders:** `#222222`. Completely dry rendering, invisible unless focused.
- **Corner Radii:** Rigid `4px` maximum to emphasize structural integrity.

## Motion Guidelines
- Clicks trigger `scale(0.97)` instantly in 120ms to provide tactile terminal-style feedback. 
- Overlays leverage modern CSS `@starting-style` with strict GPU-only `transform: scale(0.95)` masking without relying on heavy visual sliding. 
