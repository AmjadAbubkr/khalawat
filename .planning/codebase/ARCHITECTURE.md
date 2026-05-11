# Architecture — Khalawat

## System Overview

Khalawat is a voluntary digital self-discipline app for Muslims. It uses Android's `VpnService` to intercept DNS requests at the network level, check them against a local blocklist, and either forward clean queries to the device's default resolver or redirect blocked queries to a local intervention server that serves staged spiritual content.

```
┌──────────────────────────────────────────────────────────────┐
│                       Android Device                          │
│                                                               │
│  ┌──────────┐   DNS query   ┌──────────────────────────┐     │
│  │ Browser  │ ─────────────► │   KhalawatVpnService     │     │
│  │ or App   │               │   (TUN interface)        │     │
│  └──────────┘               └─────────┬────────────────┘     │
│                                        │                      │
│                             ┌──────────▼──────────┐           │
│                             │DnsResolverCoordinator│           │
│                             │   (logic core)      │           │
│                             └──┬──────────────┬───┘           │
│                                │              │               │
│                    ┌────────────▼──┐  ┌───────▼────────┐     │
│                    │   DnsProxy    │  │EscalationEngine│     │
│                    │ (blocklist    │  │ (stage machine)│     │
│                    │   check)      │  └───────┬────────┘     │
│                    └──┬────────┬──┘          │               │
│                       │        │    ┌────────▼─────────┐     │
│              ┌────────▼──┐  ┌──▼──┐│ SessionRepository │     │
│              │Intervention│  │8.8.8││ (Room persistence)│     │
│              │  Server   │  │ .8  │└──────────────────┘     │
│              │(NanoHTTPD)│  └─────┘                          │
│              └───────────┘                                    │
└──────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. DNS Interception Flow

```
App/Browser → DNS query → TUN interface → KhalawatVpnService
  → DnsResolverCoordinator.handleDnsPacket()
    → DnsProxy.resolve(query)
      → if blocked: EscalationEngine.onBlockedRequest(domain)
        → Returns DnsResult(REDIRECT, stage, domain, 127.0.0.1)
      → if allowed: Returns DnsResult(FORWARD)
```

### 2. Escalation Flow

```
STAGE_1 (15s soft intercept + ayah)
  ↓ user clicks "I still want to proceed"
STAGE_2 (30s breathing + dhikr)
  ↓ user clicks "Override"
STAGE_3 (2-min hard lock, no override)
  ↓ timer completes
UNLOCKED → domain accessible
  ↓ next blocked request within 10 min
COOLING (all blocked domains → STAGE_3 directly, 10-min duration)
  ↓ cooling expires → full reset to STAGE_1
```

### 3. Sliding Window Reset

- 30 minutes idle (no blocked request) → reset one level
- 10 minutes after any Stage 3 override → no reset (cooling is absolute)
- Cooling expiry → full reset to STAGE_1

### 4. Anti-Tamper Flow

```
User presses "I want to disable Khalawat"
  → startHold() called on AntiTamperState
  → LaunchedEffect timer ticks every 100ms
  → updateHoldProgress(elapsed) advances hold progress
  → Spiritual reminders rotate during hold
  → Progress reaches 100% after 30 seconds → isHoldComplete = true
  → If Companion PIN set → PIN entry required
  → If PIN correct or no PIN → VPN disabled
  → "Go Back" at any point → releaseHold(), progress resets
```

## Module Boundaries

| Module | Package | Responsibility |
|--------|---------|----------------|
| **BlocklistStore** | `blocklist` | Load + query offline domain blocklist |
| **DnsProxy** | `dns` | DNS packet parsing, blocklist check, forwarding |
| **EscalationEngine** | `escalation` | 3-stage + UNLOCKED + cooling state machine |
| **SpiritualContent** | `content` | Rotating ayat/hadith/dhikr in 6 languages |
| **InterventionServer** | `server` | NanoHTTPD serving HTML intervention pages with XSS escaping |
| **SessionRepository** | `persistence` | Room DB persistence for escalation state |
| **DnsResolverCoordinator** | `vpn` | Testable logic core between TUN and network |
| **KhalawatVpnService** | `vpn` | Android VpnService shell (TUN, packet loop, IPv4 checksum) |
| **OnboardingState** | `onboarding` | 5-screen first-run state machine |
| **OnboardingFlow** | `onboarding` | Compose UI for onboarding |
| **AntiTamperState** | `antitamper` | 30-sec hold + PIN gate + disconnect tracking |
| **DisableScreen** | `antitamper` | Compose UI for disable flow (LaunchedEffect timer) |
| **DashboardScreen** | `ui` | Main dashboard Compose UI |
| **KhalawatPreferences** | (root) | SharedPreferences wrapper |
| **MainActivity** | (root) | Navigation: onboarding → dashboard → disable |

## Key Design Decisions

1. **DNS-only VPN**: Zero impact on non-DNS traffic. Privacy promise holds — we never see browsing data.
2. **No third-party DNS**: Clean queries go to 8.8.8.8, not our own resolver.
3. **Local intervention server**: NanoHTTPD embedded in VPN process. Works even if app UI is killed.
4. **Plain HTML intervention pages**: No dependency on app UI layer. Served from `127.0.0.1:8080`.
5. **XSS prevention**: All dynamic template replacements are HTML-escaped via `escapeHtml()` before insertion.
6. **DnsResolverCoordinator**: Extracted as testable logic core. KhalawatVpnService is a thin Android shell.
7. **IPv4 header checksum**: `buildResponsePacket()` computes valid RFC 791 one's-complement checksum for synthetic DNS responses.
8. **FakeSessionRepository**: Pure unit tests use fakes; Room-backed implementation tested via instrumented tests.
9. **KSP over kapt**: Room annotation processing via Kotlin Symbol Processing for faster builds.
10. **ActivityResultContracts**: Modern API replaces deprecated `startActivityForResult`.
11. **LaunchedEffect timer**: DisableScreen uses Compose `LaunchedEffect` to drive hold progress at 100ms intervals, keeping UI logic in the Composable and state logic in `AntiTamperState`.
12. **Plain vars in state machines**: `OnboardingState` and `AntiTamperState` use plain `var` properties (not `mutableStateOf`) to keep them pure-logic with zero Compose dependencies. Recomposition is triggered by parent state changes in the consuming Composables.
