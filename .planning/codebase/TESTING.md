# Testing — Khalawat

## Strategy

Khalawat uses **strict TDD (RED→GREEN→REFACTOR)** per module. Every module is built:
1. Write failing tests against the public interface
2. Implement minimum code to pass
3. Refactor while keeping tests green

Android-dependent code (VPN service, Compose UI) is tested via thin shells that delegate to pure-logic cores.

## Test Suite Overview

| Test Suite | File | Tests | What's Tested |
|------------|------|-------|---------------|
| **BlocklistStore** | `BlocklistStoreTest.kt` | 9 | Load blocklist, isBlocked(), size(), edge cases |
| **DnsProxy** | `DnsProxyTest.kt` | 8 | DNS packet parsing, blocklist integration, forwarding |
| **EscalationEngine** | `EscalationEngineTest.kt` | 11 | Stage progression, override, cooling, idle reset, sliding window |
| **SpiritualContent** | `SpiritualContentTest.kt` | 10 | Content loading, rotation, language support, reset |
| **InterventionServer** | `InterventionServerTest.kt` | 6 | HTML serving per stage, language, content injection |
| **SessionRepository** | `SessionRepositoryTest.kt` | 8 | Save/load state, override logging, count queries, clear |
| **DnsResolverCoordinator** | `DnsResolverCoordinatorTest.kt` | 14 | Blocked→redirect, allowed→forward, override, reset, persistence |
| **OnboardingState** | `OnboardingStateTest.kt` | 22 | Screen navigation, PIN validation, language, VPN permission, completion |
| **AntiTamperState** | `AntiTamperStateTest.kt` | 21 | Hold progress, completion, PIN gate, disconnect tracking, reminders |
| **Total** | **9 files** | **109** | |

## Test Doubles

### FakeSessionRepository
- **Location**: `SessionRepositoryTest.kt` (inner class)
- **Purpose**: In-memory implementation for unit tests (Room needs Android Context)
- **Methods**: `loadState()`, `saveState()`, `clearState()`, `logOverride()`, `getOverrideCountSince()`, `clearOverrideLogs()`

### FakeDnsProxy
- **Location**: `DnsResolverCoordinatorTest.kt` (inner class)
- **Purpose**: Configurable blocked/forwarded responses
- **Behavior**: Returns `DnsResponse.Blocked` for domains matching a set, `DnsResponse.Forwarded` otherwise

### FakeEscalationEngine
- **Location**: `DnsResolverCoordinatorTest.kt` (inner class)
- **Purpose**: Tracks `onBlockedRequest` calls, returns configurable stages
- **Features**: Configurable current stage, cooldown, last request time

### FakeSessionRepoForVpn
- **Location**: `DnsResolverCoordinatorTest.kt` (inner class)
- **Purpose**: Simplified session repository for coordinator tests
- **Features**: In-memory state + override log

## What Each Suite Tests

### BlocklistStoreTest (9 tests)
- Loading from file path
- `isBlocked()` for exact and subdomain matches
- Empty/nonexistent blocklist handling
- `size()` after loading

### DnsProxyTest (8 tests)
- DNS packet construction and parsing
- Domain extraction from query packets
- Blocked domain returns redirect IP
- Non-blocked domain returns forwarded response
- Malformed packet handling

### EscalationEngineTest (11 tests)
- Initial state = STAGE_1
- Stage progression: STAGE_1 → STAGE_2 → STAGE_3 → UNLOCKED
- Override at each stage
- COOLING period: 10 min, all blocked → STAGE_3
- 30-min idle reset: one level down
- 10-min post-override: no reset during cooling
- `reset()` returns to STAGE_1

### SpiritualContentTest (10 tests)
- Load content from JSON path
- Rotation without replacement
- `resetRotation()` restarts from beginning
- All 6 languages (AR, EN, UR, MS, TR, FR)
- Content types: ayah, hadith, dhikr
- Exhaustion: wraps around after all items shown

### InterventionServerTest (6 tests)
- Serve stage1.html for STAGE_1 requests
- Serve stage2.html for STAGE_2 requests
- Serve stage3.html for STAGE_3 and COOLING requests
- Language parameter in URL selects content language
- Spiritual content injection into HTML templates

### SessionRepositoryTest (8 tests)
- Save and load escalation state
- Clear state
- Log override and query count since timestamp
- Clear override logs
- Null state on first load (no saved state)

### DnsResolverCoordinatorTest (14 tests)
- Blocked domain → REDIRECT action + correct stage
- Allowed domain → FORWARD action
- Override persists state + logs override
- onStage3Complete transitions state
- reset() clears engine + repository
- Multiple blocked requests escalate stages
- Coordinator properly delegates to all three dependencies

### OnboardingStateTest (22 tests)
- Screen navigation: next(), back()
- Boundary conditions (can't go back from first, can't go next from last)
- Companion PIN: enable/disable, 4-digit validation, invalid PINs rejected
- Parent message setting
- Language selection
- VPN permission grant → completion
- isComplete only true after VPN permission

### AntiTamperStateTest (21 tests)
- Hold: start, progress update, completion
- Hold: release resets progress
- Hold: progress clamped to [0, 1]
- Spiritual reminder shown during hold, null when inactive
- Reminders rotate on successive hold attempts
- Companion PIN: set, verify (correct + incorrect)
- Disconnect tracking: increment count, reset count
- shouldShowDisconnectReminder: true after 3+ disconnects
- Hold duration is configurable (default 30s)

## Running Tests

```bash
# All unit tests
gradlew.bat testDebugUnitTest

# Specific test class
gradlew.bat testDebugUnitTest --tests "com.khalawat.android.escalation.EscalationEngineTest"

# With verbose output
gradlew.bat testDebugUnitTest --info

# Instrumented tests (requires device/emulator)
gradlew.bat connectedAndroidTest
```

## CI Integration
GitHub Actions workflow (`.github/workflows/build.yml`) runs `testDebugUnitTest` on every push. Tests must pass before PR merge.

## Test Coverage Gaps

| Area | Gap | Reason |
|------|-----|--------|
| `KhalawatVpnService` | No unit tests | Android VpnService requires emulator; logic is in `DnsResolverCoordinator` |
| `RoomSessionRepository` | No unit tests | Room needs Android Context; tested via instrumented tests |
| `MainActivity` | No unit tests | Compose UI; needs instrumented testing |
| Compose screens | No unit tests | UI layer; state machines (OnboardingState, AntiTamperState) are fully tested |
| `KhalawatPreferences` | No unit tests | SharedPreferences wrapper; thin delegation to Android API |

These gaps are acceptable for MVP — all business logic is tested through pure unit tests. Android shells are thin delegation layers.
