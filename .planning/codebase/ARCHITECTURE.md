# Architecture — Khalawat

## System Overview

Khalawat is a voluntary digital self-discipline app for Muslims. It uses Android's `VpnService` to intercept DNS requests at the network level, check them against a local blocklist, and either forward clean queries to the device's default resolver or redirect blocked queries to a local intervention server that serves staged spiritual content.

```
┌──────────────────────────────────────────────────────────────┐
│                       Android Device                         │
│                                                              │
│  ┌──────────┐    DNS query    ┌──────────────────────────┐   │
│  │ Browser  │ ─────────────► │    KhalawatVpnService     │   │
│  │ or App   │                │    (TUN interface)        │   │
│  └──────────┘                └─────────┬────────────────┘   │
│                                        │                    │
│                           ┌────────────▼──────────┐         │
│                           │ DnsResolverCoordinator │         │
│                           │    (logic core)       │         │
│                           └──┬──────────────┬─────┘         │
│                              │              │               │
│               ┌──────────────▼──┐  ┌────────▼────────┐      │
│               │    DnsProxy     │  │ EscalationEngine │      │
│               │  (blocklist     │  │ (stage machine)  │      │
│               │   check)        │  └────────┬─────────┘      │
│               └──┬─────────┬───┘           │                │
│                  │         │    ┌──────────▼──────────┐      │
│  ┌───────────────▼──┐ ┌───▼──┐ │ SessionRepository   │      │
│  │ Intervention     │ │8.8.8│ │ (Room persistence)   │      │
│  │ Server           │ │ .8  │ └─────────────────────┘      │
│  │ (NanoHTTPD)      │ └─────┘                              │
│  └──────────────────┘                                      │
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
  → AntiTamperState.isHoldActive = true (mutableStateOf → Compose observes)
  → LaunchedEffect(state.isHoldActive) key changes → timer launches
  → Timer ticks every 100ms → updateHoldProgress(elapsed)
  → Spiritual reminders rotate during hold
  → Progress reaches 100% after 30 seconds → isHoldComplete = true
  → If Companion PIN set → PIN entry required
  → If PIN correct or no PIN → VPN disabled
  → "Go Back" at any point → releaseHold(), progress resets
```

### 5. Onboarding → Dashboard Transition
```
App launch → KhalawatApp composable
  → derivedStateOf(!isOnboardingComplete && !onboardingState.isComplete)
  → If fresh install: showOnboarding = true → OnboardingFlow
  → User completes 5 screens, grants VPN permission
  → onboardingState.grantVpnPermission() sets isComplete = true
  → derivedStateOf re-evaluates → showOnboarding = false
  → DashboardScreen shown (reactive, no imperative flag-setting)
  → If returning user (prefs.isOnboardingComplete = true):
     showOnboarding = false immediately → DashboardScreen
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
| **OnboardingState** | `onboarding` | 5-screen first-run state machine (mutableStateOf) |
| **OnboardingFlow** | `onboarding` | Compose UI for onboarding |
| **AntiTamperState** | `antitamper` | 30-sec hold + PIN gate + disconnect tracking (mutableStateOf) |
| **DisableScreen** | `antitamper` | Compose UI for disable flow (LaunchedEffect timer) |
| **DashboardScreen** | `ui` | Main dashboard Compose UI |
| **KhalawatPreferences** | (root) | SharedPreferences wrapper |
| **MainActivity** | (root) | Navigation: onboarding → dashboard → disable (derivedStateOf) |

## Key Design Decisions

1. **DNS-only VPN**: Zero impact on non-DNS traffic. Privacy promise holds — we never see browsing data.
2. **No third-party DNS**: Clean queries go to 8.8.8.8, not our own resolver.
3. **Local intervention server**: NanoHTTPD embedded in VPN process. Works even if app UI is killed.
4. **Plain HTML intervention pages**: No JavaScript. XSS-safe via `escapeHtml()`. Works in WebView without concerns.
5. **Room persistence**: Escalation state survives process death. KSP (not annotationProcessor) for annotation processing.
6. **Compose mutableStateOf for state machines**: Both `OnboardingState` and `AntiTamperState` use `mutableStateOf`/`mutableFloatStateOf`/`mutableIntStateOf` for Compose observability. `compose-runtime` is a pure-JVM library — classes remain unit-testable without Android dependencies.
7. **derivedStateOf for navigation**: `showOnboarding` in `KhalawatApp` uses `derivedStateOf(!isOnboardingComplete && !onboardingState.isComplete)` — reactively hides onboarding when VPN permission is granted, with zero imperative flag-setting and zero race conditions.
8. **LaunchedEffect for timers**: `DisableScreen` uses `LaunchedEffect(state.isHoldActive)` as the key. Since `isHoldActive` is now `mutableStateOf`, Compose properly relaunches the effect when `startHold()` or `releaseHold()` is called.
9. **No AIDL yet**: VPN service and UI communicate only via `startService()` intents and SharedPreferences. AIDL planned for Issue #13.
10. **ActivityResultContracts**: Replaces deprecated `startActivityForResult`/`onActivityResult` pattern.

## Compose State Strategy

| Component | State Class | Compose-Observable? | Pattern |
|-----------|-------------|---------------------|---------|
| OnboardingFlow | `OnboardingState` | ✅ `mutableStateOf` | Direct observation |
| DisableScreen | `AntiTamperState` | ✅ `mutableStateOf`/`mutableFloatStateOf`/`mutableIntStateOf` | Direct observation + LaunchedEffect timer |
| KhalawatApp navigation | (local) | ✅ `derivedStateOf` | Reactive derivation from OnboardingState |
| DashboardScreen | (props) | N/A | Stateless, receives data from parent |
