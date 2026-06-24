# Darcy Vet UI/UX Style Guide

## Visual position

Darcy Vet uses a restrained clinical desktop interface with glassmorphism and spatial UI principles. The product should feel like a precise local clinic workstation, not a generic technology demo.

The interface should communicate: calm, reliable, layered, clinical, tactile.

## Strict visual bans

The following are not allowed:

- Purple or blue futuristic gradients.
- Neon button glows.
- Orb, beam, magic, hologram, or chatbot-style decoration.
- Decorative gradient hero sections.
- Generic SaaS cards that do not map to a clinic workflow.
- Shiny filler visuals with no operational substance.
- Low-contrast controls where affordance is only visible through shadow.
- Random accent colors per module.

## Color system

Base surfaces must use absolute neutral families:

- Zinc 950: app background.
- Zinc 900 / Zinc 850: primary glass surfaces.
- Zinc 800 / Slate 800: elevated panels and dividers.
- Zinc 100 / Zinc 300 / Zinc 400: text hierarchy.

The app uses one primary accent only:

- Clinical amber: primary action, selected navigation, active state, focus indication.

Do not introduce a second brand accent without explicit design review. Semantic red is allowed only for error or destructive messaging.

## Glassmorphism rules

Glassmorphism must be functional, not decorative:

- Use translucent neutral panels over a dark neutral canvas.
- Every glass surface needs a subtle border and elevation.
- Do not use glass as a substitute for layout hierarchy.
- Keep text contrast high.
- Avoid stacking more than three visible depth levels.

Recommended hierarchy:

1. Base canvas: Zinc 950.
2. Navigation rail: translucent Zinc 900.
3. Content panels/cards: translucent Zinc 900/850 with neutral border.
4. Primary actions: amber, solid, high contrast.

## Spatial UI rules

Spatial UI means depth and location should help users understand workflow state.

- Sidebar is a fixed spatial anchor.
- Dashboard, appointment board, records, and billing are separate workspace zones.
- Cards float above the base canvas but remain aligned to a grid.
- Important workflow context should remain visible: selected patient, selected appointment, selected invoice.
- Use spacing to separate workflow regions.
- Motion may be added later, but it must clarify state changes, not decorate.

## Typography

Use default system typography for now. Prioritize readability over personality.

- Page titles: bold, large, clinical labels.
- Section headings: direct nouns, not marketing copy.
- Body text: short operational descriptions.
- Empty states: specific next action.

## Component rules

### Buttons

- Primary buttons use clinical amber.
- Destructive actions require confirmation state before mutation.
- No glow, gradient, pulse, shimmer, or neon state.

### Cards

- Cards must represent real clinic information: appointment row, invoice summary, patient alert, dashboard metric.
- Avoid abstract cards such as assistant insight or productivity claims.

### Alerts

- Patient alerts and destructive warnings may use semantic red.
- Routine status labels should stay neutral unless a state truly requires attention.

## Copywriting rules

Write like clinic software:

- Use “Schedule appointment.”
- Use “Patient alert: allergies recorded.”
- Use “No appointments scheduled for this date.”

## Implementation note

The Compose shell should source colors from shared theme tokens. Hardcoded purple, blue, neon, or gradient colors should be treated as style regressions.
