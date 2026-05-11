# Project Structure — Khalawat

## File Tree

```
C:\projects\khalawat\
├── .github\workflows\
│   └── build.yml                   # GitHub Actions CI: tests + debug APK
├── .planning\codebase\             # Codebase documentation
│   ├── ARCHITECTURE.md
│   ├── CONCERNS.md
│   ├── CONVENTIONS.md
│   ├── INTEGRATIONS.md
│   ├── STACK.md
│   ├── STRUCTURE.md
│   └── TESTING.md
├── app\
│   ├── build.gradle.kts            # App module: dependencies, SDK config, KSP
│   ├── proguard-rules.pro          # ProGuard rules for release builds
│   └── src\
│       ├── main\
│       │   ├── AndroidManifest.xml # VpnService + foreground permissions
│       │   ├── assets\
│       │   │   ├── blocklist.txt   # Blocked domains (porn, gambling, DoH)
│       │   │   └── intervention\
│       │   │       ├── stage1.html # 15s countdown + ayah
│       │   │       ├── stage2.html # 4-7-8 breathing + dhikr counter
│       │   │       └── stage3.html # 2-min lock + suggestions
│       │   ├── java\com\khalawat\android\
│       │   │   ├── MainActivity.kt           # Entry: wires onboarding→dashboard→disable
│       │   │   ├── KhalawatPreferences.kt    # SharedPreferences wrapper
│       │   │   ├── antitamper\
│       │   │   │   ├── AntiTamperState.kt    # 30-sec hold + PIN gate logic
│       │   │   │   └── DisableScreen.kt      # Compose UI for disable flow
│       │   │   ├── blocklist\
│       │   │   │   ├── BlocklistStore.kt     # Interface + domain lookup
│       │   │   │   └── BlocklistStoreImpl.kt
│       │   │   ├── content\
│       │   │   │   ├── SpiritualContent.kt   # Interface + ContentItem/Language
│       │   │   │   └── SpiritualContentImpl.kt
│       │   │   ├── dns\
│       │   │   │   ├── DnsProxy.kt           # Interface + DnsQuery/DnsResponse
│       │   │   │   ├── DnsProxyImpl.kt
│       │   │   │   └── DnsPacketParser.kt    # DNS wire format parsing
│       │   │   ├── escalation\
│       │   │   │   ├── EscalationEngine.kt   # Interface + EscalationStage/State
│       │   │   │   └── EscalationEngineImpl.kt
│       │   │   ├── onboarding\
│       │   │   │   ├── OnboardingState.kt    # 5-screen state machine (mutableStateOf)
│       │   │   │   └── OnboardingFlow.kt     # Compose UI for all 5 screens
│       │   │   ├── persistence\
│       │   │   │   ├── Entities.kt           # EscalationStateEntity, OverrideLogEntity
│       │   │   │   ├── EscalationStateDao.kt
│       │   │   │   ├── Converters.kt         # Room TypeConverter for EscalationStage
│       │   │   │   ├── AppDatabase.kt        # Room DB singleton
│       │   │   │   ├── SessionRepository.kt  # Interface + PersistentEscalationState
│       │   │   │   └── RoomSessionRepository.kt
│       │   │   ├── server\
│       │   │   │   └── InterventionServer.kt # NanoHTTPD on port 8080 (XSS-safe)
│       │   │   ├── ui\
│       │   │   │   ├── DashboardScreen.kt    # Main dashboard Compose UI
│       │   │   │   └── theme\
│       │   │   │       ├── Color.kt
│       │   │   │       └── Theme.kt          # Islamic green color scheme
│       │   │   └── vpn\
│       │   │       ├── DnsResolverCoordinator.kt # Testable logic core
│       │   │       └── KhalawatVpnService.kt     # Android VpnService shell (RFC 791 checksum)
│       │   └── res\
│       │       ├── drawable\ic_launcher_foreground.xml
│       │       ├── mipmap-anydpi-v26\ic_launcher.xml
│       │       └── values\ (colors.xml, strings.xml, themes.xml)
│       └── test\java\com\khalawat\android\
│           ├── antitamper\AntiTamperStateTest.kt      # 21 tests
│           ├── blocklist\BlocklistStoreTest.kt        # 9 tests
│           ├── content\SpiritualContentTest.kt        # 10 tests
│           ├── dns\DnsProxyTest.kt                    # 8 tests
│           ├── escalation\EscalationEngineTest.kt     # 11 tests
│           ├── onboarding\OnboardingStateTest.kt      # 22 tests
│           ├── persistence\SessionRepositoryTest.kt   # 8 tests
│           ├── server\InterventionServerTest.kt       # 12 tests
│           └── vpn\DnsResolverCoordinatorTest.kt      # 14 tests
├── build.gradle.kts                # Root: AGP + Kotlin + KSP + Compose plugins
├── settings.gradle.kts             # Single module: app
├── gradle.properties               # JVM args, AndroidX, Parcelize flags
├── gradle\wrapper\
│   └── gradle-wrapper.properties   # Gradle 8.11.1
├── gradlew / gradlew.bat
├── .gitignore
├── PRD.md                          # Full product requirements document
└── README.md                       # Project overview and setup guide
```

## Line Counts (approximate)

| Category | Lines | Notes |
|----------|-------|-------|
| Main source (Kotlin) | ~1,900 | 27 source files |
| Test source (Kotlin) | ~1,400 | 9 test files, 115 tests |
| HTML intervention pages | ~250 | 3 stage pages |
| Build configs (Gradle) | ~130 | Root + app |
| Assets (blocklist) | ~50 | Domain list |
| Documentation (.planning) | ~960 | 7 markdown files |
| README.md | ~182 | Project overview |
| PRD.md | ~300 | Product requirements |
| **Total** | **~5,200** | |
