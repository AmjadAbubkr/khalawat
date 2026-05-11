<p align="center">
  <img src="https://img.shields.io/badge/Khalawat-v1.0.0-2E7D32?style=for-the-badge&labelColor=1B5E20&color=4CAF50" alt="Khalawat">
  <img src="https://img.shields.io/badge/tests-109%20passing-4CAF50?style=for-the-badge&labelColor=2E7D32" alt="Tests">
  <img src="https://img.shields.io/badge/platform-Android%207.0%2B-3DDC84?style=for-the-badge&labelColor=1B5E20&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/license-MIT-blue?style=for-the-badge&labelColor=1565C0" alt="License">
</p>

# خَلَاوَة — Khalawat

**Voluntary digital self-discipline for Muslims.**

A local VPN that intercepts haram content requests and replaces them with a staged spiritual intervention — not shame, just friction and redirection.

> *"Whoever fears Allah, He will make for him a way out."* — Quran 65:2

---

## 🕌 What is Khalawat?

Khalawat (خَلَاوَة, "privacy/solitude") stands between you and what harms your soul — with enough delay and spiritual interrupt to let you choose differently. It's **voluntary**, **private**, and **free**.

- **No logging** — we never see your traffic
- **No accounts** — no sign-up, no cloud, no analytics
- **No premium** — 100% free, all features included (sadaqah mission)
- **Open source** — auditable by anyone

## 🛡️ How It Works

When you try to access a blocked domain, Khalawat doesn't just block it. It provides a **3-stage spiritual escalation**:

| Stage | Duration | What Happens |
|-------|----------|--------------|
| **Stage 1** — Soft Intercept | 15 seconds | Quranic ayah + countdown. "Go Back" (primary) or "I still want to proceed" (muted) |
| **Stage 2** — Active Deflection | 30 seconds | 4-7-8 breathing exercise + dhikr counter. "I've calmed down" (primary) or "Override" (text link) |
| **Stage 3** — Hard Lock | 2 minutes | Full lock — no override. Suggestions: make wudu, leave the room, pray 2 rak'ah |
| **Cooling** | 10 minutes | After Stage 3, all blocked domains go directly to Stage 3 |

**Sliding window reset**: 30 minutes idle → reset one level. This rewards self-restraint.

## 🔧 Architecture

```
Browser/App → DNS query → TUN Interface → KhalawatVpnService
    → DnsResolverCoordinator (logic core)
        → DnsProxy (blocklist check)
            → Blocked? → EscalationEngine → InterventionServer (127.0.0.1:8080)
            → Allowed? → Forward to 8.8.8.8
```

### Key Design Decisions

- **DNS-only VPN** — zero impact on non-DNS traffic, privacy promise holds
- **Local intervention server** — NanoHTTPD embedded in VPN process, serves HTML pages
- **Plain HTML/CSS/JS** — intervention pages bundled in APK, no UI dependency
- **Testable logic core** — `DnsResolverCoordinator` separates VPN shell from testable logic
- **Room persistence** — escalation state survives process death

## 📱 Features

### Core
- ✅ DNS-level content filtering via local VPN
- ✅ 3-stage spiritual escalation (not a blunt block)
- ✅ Quranic ayat, hadith, and dhikr in 6 languages (AR, EN, UR, MS, TR, FR)
- ✅ 4-7-8 breathing exercise + dhikr counter
- ✅ Sliding window reset (rewards self-restraint)
- ✅ 10-minute cooling period after Stage 3
- ✅ Session persistence across process death (Room DB)

### Protection
- ✅ Anti-tamper: 30-second hold to disable + spiritual reminders
- ✅ Optional Companion PIN (for parents)
- ✅ Disconnect tracking + reminder notification
- ✅ DNS-over-HTTPS bypass prevention (DoH domains in blocklist)

### Onboarding
- ✅ 5-screen first-run flow: Welcome → Purpose → How It Works → Companion PIN → VPN Permission
- ✅ Optional Companion PIN setup with parent message
- ✅ Language selection

### Dashboard
- ✅ VPN protection status indicator
- ✅ Daily intervention count
- ✅ Current escalation stage display
- ✅ Toggle protection on/off

## 🧪 Testing

**109 unit tests, 0 failures** — strict TDD (RED→GREEN→REFACTOR) per module.

| Module | Tests | What's Tested |
|--------|-------|---------------|
| AntiTamperState | 21 | Hold progress, PIN gate, disconnect tracking |
| OnboardingState | 22 | Screen navigation, PIN validation, language, completion |
| DnsResolverCoordinator | 14 | Blocked→redirect, allowed→forward, override, persistence |
| EscalationEngine | 11 | Stage progression, cooling, idle reset |
| SpiritualContent | 10 | Content loading, rotation, 6 languages |
| SessionRepository | 8 | Save/load state, override logging |
| DnsProxy | 8 | DNS parsing, blocklist integration |
| BlocklistStore | 9 | Load, query, size, edge cases |
| InterventionServer | 6 | HTML serving per stage + language |

```bash
# Run all tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

## 🏗️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material3 |
| VPN | Android VpnService (DNS-only TUN) |
| DNS Proxy | Custom Kotlin implementation |
| HTTP Server | NanoHTTPD 2.3.1 |
| Database | Room 2.6.1 (KSP) |
| Coroutines | Kotlinx Coroutines 1.9.0 |
| Build | Gradle 8.11.1 + GitHub Actions |
| Min SDK | 24 (Android 7.0+) |
| Target SDK | 35 |

## 📂 Project Structure

```
app/src/main/java/com/khalawat/android/
├── MainActivity.kt              # Entry: wires onboarding→dashboard→disable
├── KhalawatPreferences.kt       # SharedPreferences wrapper
├── antitamper/                  # 30-sec hold + PIN gate
├── blocklist/                   # Domain blocklist (load + query)
├── content/                     # Spiritual content rotation (6 languages)
├── dns/                         # DNS proxy + packet parsing
├── escalation/                  # 3-stage + cooling state machine
├── onboarding/                  # 5-screen first-run flow
├── persistence/                 # Room DB + session repository
├── server/                      # NanoHTTPD intervention server
├── ui/                          # Dashboard + theme
└── vpn/                         # VPN service + coordinator
```

## 🔒 Privacy Guarantees

| Guarantee | How |
|-----------|-----|
| No traffic logged | VPN only intercepts DNS. No HTTP/HTTPS inspection. |
| No data sent anywhere | Blocklist is local. DNS forwarding uses 8.8.8.8. |
| No user accounts | No sign-up. No cloud. No analytics. |
| Source auditable | Open source. Anyone can verify. |

## 🗺️ Roadmap

- [ ] AIDL VPN↔UI real-time communication
- [ ] Notification channel + daily spiritual reminder
- [ ] End-to-end integration testing on real device
- [ ] EncryptedSharedPreferences for PIN storage
- [ ] Night Guard (scheduled blocking hours)
- [ ] Extended blocklist (gambling, dating)
- [ ] Custom escalation durations
- [ ] iOS version (Screen Time API)
- [ ] Usage patterns dashboard (local-only)
- [ ] Custom spiritual content

## 🤝 Contributing

This is a sadaqah project. Contributions are welcome:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing`)
3. Write tests first (TDD)
4. Make your changes
5. Ensure all 109 tests pass (`./gradlew testDebugUnitTest`)
6. Open a Pull Request

## 📄 License

MIT License — use it, share it, improve it. May Allah accept it as sadaqah jariyah.

---

*خَلَاوَة — Because the hardest sins to quit are the private ones, and you deserve a tool that respects your privacy while helping you choose differently.*
