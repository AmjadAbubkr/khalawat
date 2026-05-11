# Conventions — Khalawat

## Package Structure

All code lives under `com.khalawat.android`. Each module has its own package:

- `blocklist` — domain blocklist
- `dns` — DNS proxy and packet parsing
- `escalation` — escalation state machine
- `content` — spiritual content rotation
- `server` — intervention HTTP server
- `persistence` — Room DB, DAOs, repositories
- `vpn` — VPN service + coordinator
- `onboarding` — first-run flow (state + UI)
- `antitamper` — disable friction (state + UI)
- `ui` — dashboard and theme
- Root package — `MainActivity`, `KhalawatPreferences`

## Naming Conventions

### Files
- Interface + impl pair: `BlocklistStore.kt` (interface) + `BlocklistStoreImpl.kt` (implementation)
- State machines: `*State.kt` (e.g., `OnboardingState.kt`, `AntiTamperState.kt`)
- Compose screens: `*Screen.kt` or `*Flow.kt`
- Tests: `*Test.kt` matching the class under test
- Entities: `Entities.kt` (all Room entities in one file for small DB)
- DAOs: `*Dao.kt`

### Classes & Interfaces
- Interfaces: noun (e.g., `DnsProxy`, `BlocklistStore`, `EscalationEngine`)
- Implementations: interface name + `Impl` (e.g., `DnsProxyImpl`)
- Data classes: noun phrase (e.g., `EscalationState`, `DnsQuery`, `DnsResult`)
- Sealed classes: for result types (e.g., `DnsResponse.Blocked`, `DnsResponse.Forwarded`)
- Enums: for stages, screens, languages (e.g., `EscalationStage`, `OnboardingScreen`, `Language`)

### Functions
- Public interface methods: verb or verb phrase (e.g., `onBlockedRequest()`, `override()`, `resolve()`)
- Private helpers: descriptive (e.g., `persistState()`, `extractDomain()`, `escapeHtml()`)

- Compose-local state: `by remember { mutableStateOf(...) }` (e.g., `holdElapsed`, `pinInput`)

## Architecture Patterns

### Interface-Implementation Separation

```kotlin
interface DnsProxy {
    fun resolve(query: DnsQuery): DnsResponse
}
```
<
Android-dependent code is split into:
1. **Thin Android shell** — handles lifecycle, permissions, system APIs (e.g., `KhalawatVpnService`)
2. **Pure logic core** — fully unit-testable (e.g., `DnsResolverCoordinator`)



### Repository Pattern
- `SessionRepository` interface for persistence
- `RoomSessionRepository` for production (Room-backed)
- `FakeSessionRepository` for unit tests (in-memory)

### Singleton Pattern (Database)
`AppDatabase` uses a companion object `getInstance(context)` to ensure single Room instance.

### HTML Template Safety
All dynamic content inserted into HTML templates must be escaped via `escapeHtml()` to prevent XSS. This applies to both spiritual content (from `SpiritualContent`) and user-controlled query parameters (from `IHTTPSession.parms`).

## Kotlin Style

### Null Safety
- Use `?` for nullable types explicitly
- Prefer `?.let {}` over `!!` or null checks
- `Instant?` for optional timestamps (e.g., `cooldownEndTime`, `lastOverrideTime`)

### Coroutines
- Room DAO methods use `suspend` modifier
- `kotlinx.coroutines.test` for testing coroutine code
- `delay()` used in `LaunchedEffect` for UI timers (e.g., hold progress)

### Data Classes
- Prefer `data class` for immutable state objects
- Use `copy()` for state transitions

### Enums
- Use `entries` (not `values()`) for Kotlin 2.1.0
- Each enum value has a comment documenting its purpose

### TypeConverters
- Room `@TypeConverter` functions are in a standalone `Converters.kt` file
- Enum ↔ String conversion via `name` / `valueOf`

## Compose UI Conventions

### Screen Structure

@Composable
fun DashboardScreen(
    isVpnActive: Boolean,
    currentStage: EscalationStage,
    overrideCountToday: Int,
    onToggleVpn: () -> Unit,
    onShowDisable: () -> Unit
)
```

### State Hoisting
- UI state is hoisted to the caller (`MainActivity`)
- Event callbacks passed as lambda parameters
- State machines owned by `MainActivity`, not inside composables
- Compose-local state (e.g., `holdElapsed`, `pinInput`) uses `remember { mutableStateOf(...) }`

### Timers in Compose
- Use `LaunchedEffect(key)` to start/stop timers
- `delay(intervalMs)` inside a `while` loop for periodic updates


### Theming
- Islamic green color scheme defined in `Color.kt` and `Theme.kt`
- Uses Material3 dynamic theming where available
- **Known issue**: Color constant names (`Purple80`, `Pink80`) don't match actual values (green) — cosmetic debt

## Build Conventions

### Gradle
- Single module: `app`
- Version catalogs not used (dependencies inline in `build.gradle.kts`)
- KSP (not kapt) for annotation processing

### ProGuard
- Enabled for release builds (`isMinifyEnabled = true`)
- Standard Android ProGuard rules + custom rules in `proguard-rules.pro`

### CI
- GitHub Actions on every push
- Runs `testDebugUnitTest` + `assembleDebug`
- APK uploaded as workflow artifact
