# Tech Stack — Khalawat

## Language & Runtime
- **Kotlin 2.1.0** — primary language for all source
- **Android SDK 35** (compileSdk / targetSdk)
- **minSdk 24** — covers 99%+ of active devices

## UI Framework
- **Jetpack Compose** with **Material3**
- **Compose BOM 2024.12.01** — aligned version catalog
- **Compose Runtime** — `mutableStateOf`, `mutableFloatStateOf`, `mutableIntStateOf`, `derivedStateOf` for reactive state in state machines
- **material-icons-extended:1.7.6** — Shield, Warning, Check icons (large dep, debt item)

## Networking / VPN
- **Android VpnService** — TUN-based DNS interception
- **NanoHTTPD 2.3.1** — embedded HTTP server for intervention pages
- **java.net.DatagramSocket** — upstream DNS forwarding to 8.8.8.8

## Persistence
- **Room 2.6.1** (KSP processor) — escalation state persistence
- **SharedPreferences** — app preferences (onboarding, PIN, language, VPN state)

## Build
- **Gradle Kotlin DSL** — `build.gradle.kts`
- **KSP** — annotation processing (Room, not kapt)
- **ProGuard** — enabled for release

## Testing
- **JUnit 4** — test runner
- **Google Truth** — fluent assertions
- **kotlinx.coroutines.test** — coroutine testing
- **compose-runtime** (pure JVM) — enables `mutableStateOf` in unit tests without Android emulator

## Key Dependencies Detail

| Dependency | Version | Purpose |
|-----------|---------|---------|
| compose-bom | 2024.12.01 | Aligned Compose versions |
| compose-runtime | (BOM) | `mutableStateOf`, `derivedStateOf` for state machines |
| material3 | (BOM) | UI components |
| material-icons-extended | 1.7.6 | Shield, Warning, Check icons |
| room-runtime | 2.6.1 | Escalation state persistence |
| room-compiler (KSP) | 2.6.1 | Annotation processing |
| nanohttpd | 2.3.1 | Local intervention HTTP server |
| junit | 4.13.2 | Test runner |
| truth | 1.4.4 | Assertions |
