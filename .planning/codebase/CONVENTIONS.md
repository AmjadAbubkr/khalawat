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
- Private helpers: descriptive (e.g., `persistState()`, `extractDomain()`)

### Variables
- Private mutable state: prefixed with underscore (e.g., `_isHoldActive`, `_holdProgress`)
- Public read-only access: no underscore (e.g., `isHoldActive`, `holdProgress`)

## Architecture Patterns

### Interface-Implementation Separation
Every module exposes a pure Kotlin interface with no Android dependencies:
```kotlin
interface DnsProxy {
    fun resolve(query: DnsQuery): DnsResponse
}
```
Implementation classes depend on Android where needed; interfaces stay testable.

### Testable Logic Core
Android-dependent code is split into:
1. **Thin Android shell** — handles lifecycle, permissions, system APIs (e.g., `KhalawatVpnService`)
2. **Pure logic core** — fully unit-testable (e.g., `DnsResolverCoordinator`)

### State Machines
UI state is driven by pure-logic state machines with no Compose dependency:
- `OnboardingState` — drives `OnboardingFlow` Compose UI
- `AntiTamperState` — drives `DisableScreen` Compose UI
- `EscalationEngine` — drives intervention server responses

### Repository Pattern
- `SessionRepository` interface for persistence
- `RoomSessionRepository` for production (Room-backed)
- `FakeSessionRepository` for unit tests (in-memory)

### Singleton Pattern (Database)
`AppDatabase` uses a companion object `getInstance(context)` to ensure single Room instance.

## Kotlin Style

### Null Safety
- Use `?` for nullable types explicitly
- Prefer `?.let {}` over `!!` or null checks
- `Instant?` for optional timestamps (e.g., `cooldownEndTime`, `lastOverrideTime`)

### Coroutines
- Room DAO methods use `suspend` modifier
- `kotlinx.coroutines.test` for testing coroutine code

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
Each screen is a `@Composable` function:
```kotlin
@Composable
fun DashboardScreen(
    isVpnActive: Boolean,
    interventionCount: Int,
    currentStage: String,
    onToggleVpn: () -> Unit,
    onDisable: () -> Unit
)
```

### State Hoisting
- UI state is hoisted to the caller (`MainActivity`)
- Event callbacks passed as lambda parameters
- State machines owned by `MainActivity`, not inside composables

### Theming
- Islamic green color scheme defined in `Color.kt` and `Theme.kt`
- Uses Material3 dynamic theming where available

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
