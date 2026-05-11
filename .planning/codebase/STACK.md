# Tech Stack — Khalawat

## Languages
- **Kotlin 2.1.0** — primary language for all app logic, UI, and VPN service

## Build System
- **Gradle 8.11.1** (wrapper)
- **Android Gradle Plugin (AGP) 8.7.3**
- **KSP 2.1.0-1.0.29** — Kotlin Symbol Processing (replaces kapt for Room)
- **Kotlin Compose Compiler Plugin 2.1.0**

## Android SDK
| Config | Value |
|--------|-------|
| compileSdk | 35 |
| minSdk | 24 (Android 7.0+) |
| targetSdk | 35 |
| JVM target | 17 |

## Frameworks & Libraries

### UI
| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.12.01 | Compose version management |
| Compose UI | (BOM) | Declarative UI toolkit |
| Compose UI Graphics | (BOM) | Graphics primitives |
| Compose UI Tooling Preview | (BOM) | @Preview support |
| Material3 | (BOM) | Material Design 3 components |
| Material Icons Extended | 1.7.6 | Extended icon set (Shield, Warning, etc.) |
| Activity Compose | 1.9.3 | Compose integration with Activity |
| Lifecycle Runtime KTX | 2.8.7 | Lifecycle-aware components |

### Core
| Library | Version | Purpose |
|---------|---------|---------|
| Core KTX | 1.15.0 | Kotlin extensions for Android APIs |
| Kotlin Coroutines Android | 1.9.0 | Async programming on Android |

### Data
| Library | Version | Purpose |
|---------|---------|---------|
| Room Runtime | 2.6.1 | Local SQLite database |
| Room KTX | 2.6.1 | Coroutines support for Room |
| Room Compiler | 2.6.1 | Annotation processor (via KSP) |

### Networking
| Library | Version | Purpose |
|---------|---------|---------|
| NanoHTTPD | 2.3.1 | Embedded HTTP server for intervention pages |

## Test Dependencies
| Library | Version | Purpose |
|---------|---------|---------|
| JUnit 4 | 4.13.2 | Unit test framework |
| Kotlin Coroutines Test | 1.9.0 | Coroutines test support |
| Google Truth | 1.4.4 | Fluent assertions |
| org.json | 20240303 | JSON parsing in tests |
| AndroidX Test JUnit | 1.2.1 | Instrumented test extensions |
| Espresso Core | 3.6.1 | UI instrumented tests |
| Compose UI Test JUnit4 | (BOM) | Compose UI testing |

## CI/CD
- **GitHub Actions** — builds on every push to `goose` branch
- Workflow: `.github/workflows/build.yml`
- Runs unit tests + builds debug APK

## Android APIs Used
| API | Purpose |
|-----|---------|
| `android.net.VpnService` | DNS-only TUN interface for traffic interception |
| `android.content.SharedPreferences` | App preferences (onboarding state, PIN, language) |
| `android.app.NotificationChannel` | Foreground service notification for VPN |
| `android.app.ForegroundService` | VPN service runs as foreground service |
| `ActivityResultContracts` | Modern permission/activity result handling |
