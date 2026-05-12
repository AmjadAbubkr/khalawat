---
name: Khalawat
colors:
  surface: '#fcf8f7'
  surface-dim: '#ddd9d8'
  surface-bright: '#fcf8f7'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f7f3f2'
  surface-container: '#f1edec'
  surface-container-high: '#ebe7e6'
  surface-container-highest: '#e5e2e1'
  on-surface: '#1c1b1b'
  on-surface-variant: '#454843'
  inverse-surface: '#313030'
  inverse-on-surface: '#f4f0ef'
  outline: '#757873'
  outline-variant: '#c5c7c1'
  surface-tint: '#5d5f5b'
  primary: '#5d5f5b'
  on-primary: '#ffffff'
  primary-container: '#f5f5f0'
  on-primary-container: '#6f706c'
  inverse-primary: '#c6c7c2'
  secondary: '#4d644d'
  on-secondary: '#ffffff'
  secondary-container: '#cfeacc'
  on-secondary-container: '#536a53'
  tertiary: '#735c00'
  on-tertiary: '#ffffff'
  tertiary-container: '#fff4de'
  on-tertiary-container: '#886c00'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e3e3de'
  primary-fixed-dim: '#c6c7c2'
  on-primary-fixed: '#1a1c19'
  on-primary-fixed-variant: '#454744'
  secondary-fixed: '#cfeacc'
  secondary-fixed-dim: '#b4cdb1'
  on-secondary-fixed: '#0b200e'
  on-secondary-fixed-variant: '#364c36'
  tertiary-fixed: '#ffe088'
  tertiary-fixed-dim: '#e9c349'
  on-tertiary-fixed: '#241a00'
  on-tertiary-fixed-variant: '#574500'
  background: '#fcf8f7'
  on-background: '#1c1b1b'
  surface-variant: '#e5e2e1'
typography:
  display:
    fontFamily: Newsreader
    fontSize: 48px
    fontWeight: '600'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Newsreader
    fontSize: 32px
    fontWeight: '500'
    lineHeight: 40px
  headline-md:
    fontFamily: Newsreader
    fontSize: 24px
    fontWeight: '500'
    lineHeight: 32px
  body-lg:
    fontFamily: Newsreader
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Newsreader
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-lg:
    fontFamily: Newsreader
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Newsreader
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.03em
  headline-lg-mobile:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '500'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 8px
  container-max: 1120px
  gutter: 24px
  margin-mobile: 20px
  margin-desktop: 64px
---

## Brand & Style

The design system is anchored in a meditative and spiritual philosophy, prioritizing stillness over stimulation. The aesthetic is rooted in **Modern Minimalism** with a focus on tactile luxury through high-contrast typography and metallic highlights. It aims to evoke a sense of reverence and clarity, making it suitable for contemplative practices, scholarly reading, or spiritual journaling.

The interface acts as a silent companion, using generous white space (breathable room) to reduce cognitive load. Interaction patterns should be deliberate and calm, avoiding aggressive animations or cluttered layouts. The style balances the timeless authority of classical manuscripts with the clean functionalism of contemporary high-end lifestyle design.

## Colors

This design system utilizes a high-contrast, limited palette to maintain focus and dignity.

- **Primary Surface (#F5F5F0):** A warm, parchment-like off-white used for backgrounds to reduce eye strain compared to pure white.
- **Deep Forest Green (#0A1F0D):** The primary color for text, iconography, and structural borders. It provides a grounded, organic contrast to the off-white.
- **Metallic Gold (#D4AF37):** Reserved exclusively for primary actions, progress indicators, and significant highlights. It represents value and enlightenment.

**Dark Mode Logic:** The hierarchy flips to a "Forest Night" theme. The Deep Forest Green becomes the primary background. Off-white is used for primary text to maintain legibility, and Gold remains the primary accent color for interactive elements.

## Typography

The design system exclusively uses **Newsreader**, a serif typeface designed for continuous reading and editorial excellence. The use of a single font family reinforces the minimalist discipline of the system.

- **Headlines:** Use medium weights with slightly tighter tracking for a sophisticated, "published" look.
- **Body Text:** Set with generous line height to ensure a comfortable, meditative reading pace. 
- **Labels:** Small caps or increased letter-spacing should be used for metadata and labels to differentiate them from body content without switching typefaces.
- **Optical Sizing:** Ensure Newsreader's optical sizing features are enabled to maintain stroke integrity at smaller sizes.

## Layout & Spacing

The layout follows a **Fixed Grid** philosophy on desktop to preserve intentional whitespace and contain content within a readable "well." 

- **Desktop:** A 12-column grid centered on the page with a maximum width of 1120px. Gutters are fixed at 24px to provide clear separation without breaking the visual flow.
- **Mobile:** A 4-column fluid grid with 20px side margins. 
- **Rhythm:** Spacing follows an 8px base unit. Vertical rhythm is critical; use large "Stage Spacing" (64px+) between sections to allow the eye to rest. Elements should feel uncrowded, as if placed on a gallery wall.

## Elevation & Depth

To maintain a respectful and grounded aesthetic, the design system avoids heavy drop shadows and floating effects. 

- **Tonal Layers:** Depth is communicated through subtle color shifts. A secondary surface might be 2% darker or lighter than the base surface.
- **Low-Contrast Outlines:** Instead of shadows, use fine 1px borders in a muted version of the Forest Green (opacity 10-15%) to define containers.
- **The "Gold Thread":** A 1px gold top-border can be used on primary cards or headers to denote elevation and hierarchy without physical volume.
- **Focus:** Interactive states are signaled by a slight increase in the stroke weight of the border or a subtle shift in the background color, rather than "lifting" the element off the page.

## Shapes

The design system utilizes **Soft, Subtle Curves (ROUND_EIGHT)**. 

- **Standard Radius:** 8px (0.5rem) for cards, input fields, and buttons. This softens the high-contrast professional look, making it feel more approachable and organic.
- **Large Radius:** 16px (1rem) for major containers or bottom sheets.
- **Consistency:** Avoid pill-shaped buttons; the 8px radius provides a more "architectural" and stable feel, which aligns with the spiritual and meditative tone.

## Components

- **Buttons:** 
    - *Primary:* Filled Gold (#D4AF37) with Forest Green text. No shadow, 8px radius.
    - *Secondary:* Forest Green outline (1px) with Forest Green text.
- **Inputs:** Forest Green 1px borders with the Off-White background. Labels are always positioned above the field in Label-Small style.
- **Cards:** Use a fine 1px Forest Green border at low opacity (10%). For featured content, add a 2px Gold top-accent.
- **Lists:** Separated by thin, horizontal dividers (1px) in muted Forest Green. High vertical padding (16px+) for each list item.
- **Progress Indicators:** Use a thin Gold line for progress bars and a simple circular stroke for loading states.
- **Dividers:** Use a "fading" divider (transparent to Forest Green to transparent) for a more graceful section transition.
- **Spiritual Additions:** A "Prayer/Meditation" card component featuring centered serif text, extra-wide margins, and a single Gold icon highlight.