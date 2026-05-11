# Architecture — Khalawat

## System Overview

Khalawat is a voluntary digital self-discipline app for Muslims. It uses Android's `VpnService` to intercept DNS requests at the network level, check them against a local blocklist, and either forward clean queries to the device's default resolver or redirect blocked queries to a local intervention server that serves staged spiritual content.

```
┌──────────────────────────────────────────────────────────────┐
│ Android Device                                               │
│                                                              │
│  ┌──────────┐    DNS query    ┌──────────────────────────┐   │
│  │  Browser  │ ─────────────► │  KhalawatVpnService      │   │
│  │  or App   │                │  (TUN interface)          │   │
│  └──────────┘                └─────────┬────────────────┘   │
│                                         │                    │
│                              ┌──────────▼──────────┐        │
│                              │ DnsResolverCoordinator│        │
│                              │ (logic core)         │        │
│                              └──┬──────────────┬───┘        │
│                                 │              │             │
│                    ┌────────────▼──┐    ┌──────▼────────┐   │
│                    │  DnsProxy      │    │ EscalationEngine│  │
│                    │  (blocklist    │    │ (stage machine) │  │
│                    │   check)       │    └──────┬─────────┘  │
│                    └──┬─────────┬──┘           │             │
│                       │         │    ┌─────────▼─────────┐  │
│              ┌────────▼──┐ ┌───▼────┐│ SessionRepository  │  │
│              │Intervention│ │Default ││ (Room persistence) │  │
│              │Server      │ │Resolver│└───────────────────┘  │
│              │(NanoHTTPD) │ │(8.8.8.8)│                      │
│              └───────────┘ └────────┘                        │
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
    ↓ 10-min cooling period
COOLING (all blocked domains → STAGE_3 directly)
```

### 3. Sliding Window Reset
- 30 minutes idle (no blocked request) → reset one level
- 10 minutes after any Stage 3 override → no reset (cooling is absolute)

### 4. Anti-Tamper Flow
```
User presses "I want to disable"
    → 30-second hold button activates
    → Spiritual reminders rotate during hold
    → Hold completes
        → If Companion PIN set → PIN required
        → If no PIN → VPN disabled
```

## Module Boundaries

| Module | Package | Responsibility |
|--------|---------|----------------|
| **BlocklistStore** | `blocklist` | Load + query offline domain blocklist |
| **DnsProxy** | `dns` | DNS packet parsing, blocklist check, forwarding |
| **EscalationEngine** | `escalation` | 3-stage + cooling state machine |
| **SpiritualContent** | `content` | Rotating ayat/hadith/dhikr in 6 languages |
| **InterventionServer** | `server` | NanoHTTPD serving HTML intervention pages |
| **SessionRepository** | `persistence` | Room DB persistence for escalation state |
| **DnsResolverCoordinator** | `vpn` | Testable logic core between TUN and network |
| **KhalawatVpnService** | `vpn` | Android VpnService shell (TUN, packet loop) |
| **OnboardingState** | `onboarding` | 5-screen first-run state machine |
| **OnboardingFlow** | `onboarding` | Compose UI for onboarding |
| **AntiTamperState** | `antitamper` | 30-sec hold + PIN gate + disconnect tracking |
| **DisableScreen** | `antitamper` | Compose UI for disable flow |
| **DashboardScreen** | `ui` | Main dashboard Compose UI |
| **KhalawatPreferences** | (root) | SharedPreferences wrapper |
| **MainActivity** | (root) | Navigation: onboarding → dashboard → disable |

## Key Design Decisions

1. **DNS-only VPN**: Zero impact on non-DNS traffic. Privacy promise holds — we never see browsing data.
2. **No third-party DNS**: Clean queries go to 8.8.8.8, not our own resolver.
3. **Local intervention server**: NanoHTTPD embedded in VPN process. Works even if app UI is killed.
4. **Plain HTML intervention pages**: No dependency on app UI layer. Served from `127.0.0.1:8080`.
5. **DnsResolverCoordinator**: Extracted as testable logic core. KhalawatVpnService is a thin Android shell.
6. **FakeSessionRepository**: Pure unit tests use fakes; Room-backed implementation tested via instrumented tests.
7. **KSP over kapt**: Room annotation processing via Kotlin Symbol Processing for faster builds.
8. **ActivityResultContracts**: Modern API replaces deprecated `startActivityForResult`.
