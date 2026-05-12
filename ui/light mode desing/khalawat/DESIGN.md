---
name: Khalawat
colors:
  surface: '#131313'
  surface-dim: '#131313'
  surface-bright: '#393939'
  surface-container-lowest: '#0e0e0e'
  surface-container-low: '#1c1b1b'
  surface-container: '#201f1f'
  surface-container-high: '#2a2a2a'
  surface-container-highest: '#353534'
  on-surface: '#e5e2e1'
  on-surface-variant: '#d0c5af'
  inverse-surface: '#e5e2e1'
  inverse-on-surface: '#313030'
  outline: '#99907c'
  outline-variant: '#4d4635'
  surface-tint: '#e9c349'
  primary: '#f2ca50'
  on-primary: '#3c2f00'
  primary-container: '#d4af37'
  on-primary-container: '#554300'
  inverse-primary: '#735c00'
  secondary: '#b4cdb8'
  on-secondary: '#203527'
  secondary-container: '#364c3c'
  on-secondary-container: '#a3bba7'
  tertiary: '#c6cee8'
  on-tertiary: '#283044'
  tertiary-container: '#abb2cc'
  on-tertiary-container: '#3d455a'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#ffe088'
  primary-fixed-dim: '#e9c349'
  on-primary-fixed: '#241a00'
  on-primary-fixed-variant: '#574500'
  secondary-fixed: '#d0e9d4'
  secondary-fixed-dim: '#b4cdb8'
  on-secondary-fixed: '#0b2013'
  on-secondary-fixed-variant: '#364c3c'
  tertiary-fixed: '#dae2fd'
  tertiary-fixed-dim: '#bec6e0'
  on-tertiary-fixed: '#131b2e'
  on-tertiary-fixed-variant: '#3f465c'
  background: '#131313'
  on-background: '#e5e2e1'
  surface-variant: '#353534'
typography:
  display-lg:
    fontFamily: Newsreader
    fontSize: 40px
    fontWeight: '600'
    lineHeight: 48px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Newsreader
    fontSize: 28px
    fontWeight: '500'
    lineHeight: 36px
  headline-sm:
    fontFamily: Newsreader
    fontSize: 22px
    fontWeight: '500'
    lineHeight: 28px
  body-lg:
    fontFamily: Manrope
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Manrope
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: Manrope
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.05em
  quote-arabic:
    fontFamily: Noto Serif
    fontSize: 24px
    fontWeight: '400'
    lineHeight: 40px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 8px
  container-padding-mobile: 20px
  container-padding-desktop: 40px
  gutter: 16px
  section-gap: 32px
---

## Brand & Style

This design system is built on the core metaphor of **Shadow to Light**. It addresses the deeply personal and often difficult journey of self-discipline with an atmosphere of sacred privacy, spiritual growth, and non-judgmental guidance. The brand personality is serene, solemn yet hopeful, and profoundly focused.

The chosen design style is **Modern Minimalist with Tonal Layering**. It prioritizes extreme legibility and mental "breathing room" to reduce cognitive load during moments of temptation or reflection. By utilizing deep, atmospheric backgrounds and subtle, luminous accents, the UI mimics the transition from the "shadow" of isolation to the "light" of spiritual presence.

The user experience should feel like entering a quiet prayer hall at night—cool, stable, and intentionally separated from the chaotic noise of the digital world. Visual elements are whisper-quiet, ensuring that the user's intention remains the primary focus.

## Colors

The palette is rooted in the "Shadow to Light" principle, moving from dense, protective darks to warm, illuminating golds.

- **Primary (Light/Guidance):** A soft Metallic Gold (#D4AF37). Used sparingly for calls to action, active states, and spiritual highlights. It represents the "Nuur" (light) that guides the user.
- **Secondary (Growth/Privacy):** Forest Green (#1B3022). Deep and muted, used for success states, progress indicators, and areas denoting personal cultivation.
- **Tertiary (Atmosphere):** Midnight Blue (#0F172A). Provides a sense of vastness and calm, used for subtle gradients and secondary containers.
- **Neutral (Foundation):** Charcoal and Rich Black (#121212). The base surface color, ensuring the app remains unobtrusive and private.
- **Intervention (Warmth):** Instead of harsh reds, use a **Warm Amber (#C2780E)** for alerts or moments requiring friction. This signals "caution" and "reflection" rather than "failure" or "punishment."

## Typography

The typography strategy creates a dialogue between the modern UI and timeless spiritual wisdom.

- **UI & Interaction:** **Manrope** is used for all functional elements. Its balanced, modern grotesque letterforms provide a sense of stability and clarity.
- **Spiritual & Literary:** **Newsreader** is employed for headers and Quranic translations. Its traditional serif structure evokes the authority of a printed manuscript.
- **Arabic Script:** When displaying original Arabic text, use a refined, high-contrast Naskh style via **Noto Serif Arabic** to maintain elegance and readability.
- **Hierarchy:** High contrast between serif headings and sans-serif body text helps distinguish between "content to be reflected upon" and "tools to be used."

## Layout & Spacing

The layout philosophy is based on **Centered Focus** and **Generous Breathing Room**. 

- **Grid:** A 4-column fluid grid for mobile and 12-column for larger displays. Content is often constrained to a narrow central column (max 680px) to simulate a focused, meditative reading experience.
- **Rhythm:** An 8px linear scale is used. However, vertical rhythm is intentionally expanded around spiritual quotes to create a "sacred void," preventing the UI from feeling cluttered.
- **Margins:** Wider-than-average side margins (20px+) reinforce the feeling of a private, contained space.

## Elevation & Depth

This design system avoids traditional drop shadows in favor of **Tonal Luminosity** and **Layered Opacity**.

- **Shadow to Light:** Depth is achieved by "lifting" elements toward the light. Higher-level elements (like active cards) use slightly lighter charcoal fills and a subtle **Gold Inner Glow** (0.5px border or 2px soft blur) rather than a dark outer shadow.
- **Glassmorphism:** Use backdrop blurs (20px+) on top navigation bars and bottom sheets to maintain a sense of the "ambient background" while providing a surface for interaction.
- **Scrims:** Use deep radial gradients (Midnight Blue to Black) to draw the eye toward the center of the screen, mimicking the light fall-off seen in the reference image.

## Shapes

The shape language combines geometric precision with soft, organic edges.

- **Corners:** A base radius of 8px (Rounded) is used for most UI containers. For spiritual highlights or "moments of peace" (like a finished habit), use 16px or 24px (rounded-lg/xl) to soften the visual impact.
- **The Arch:** Subtle use of the pointed arch (Sasanian or pointed horseshoe) in iconography or container clipping links the modern UI to traditional Islamic architecture without being decorative or overbearing.
- **Icons:** Minimalist, single-weight strokes (1.5px) with slightly rounded terminals. Avoid filled icons unless they represent an active, "illuminated" state.

## Components

- **The Intervention Shield:** A full-screen modal triggered during "moments of struggle." It features a deep Amber gradient and a single, large-type serif prompt. The primary button is "Back to Peace," and the secondary is "Proceed with Awareness."
- **Focus Cards:** Used for daily goals. They feature a dark charcoal background with a very thin (1px) border that transitions from Dark Green to Gold as progress is made.
- **Spiritual Reminders:** Elegant, borderless containers with center-aligned Newsreader typography. These should appear to float on the background.
- **The "Nuur" Button:** The primary CTA. A solid Gold fill with dark charcoal text. When pressed, it should emit a soft, localized outer glow.
- **Subtle Inputs:** Text fields should not have boxes. Use a single bottom border that illuminates (Gold) only when focused, minimizing visual noise when idle.
- **Progress Halos:** Instead of linear bars, use thin circular strokes (halos) to represent habit completion, echoing the celestial motifs of the moon and stars.