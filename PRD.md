# Khalawat — Product Requirements Document

> **Voluntary digital self-discipline for Muslims.**  
> A local VPN that intercepts haram content requests and replaces them with a staged spiritual intervention — not shame, just friction and redirection.

---

## 1. Problem Statement

The hardest sins to quit are the private ones — precisely because there's no social accountability. Muslims who want to avoid accessing haram content (pornography, etc.) in private moments have no tool that respects their privacy while creating meaningful behavioral friction. Parental controls spy on you. Browser extensions are trivially bypassed. Nothing addresses the *urge window* — the 1–3 minutes where a different choice can be made.

## 2. Vision

Khalawat is a voluntary, self-imposed spiritual accountability tool. You install it for yourself. It never logs, never reports, never sends data anywhere. It simply stands between you and what harms your soul — with enough delay and spiritual interrupt to let you choose differently.

## 3. Target Users

| User | Scenario |
|------|----------|
| **Self-user** | A Muslim who wants to protect their own private browsing. Installs it, sets it up, chooses to keep it running. |
| **Parent** | A parent installing on a child's device. Uses Companion PIN to prevent the child from disabling protection. |

## 4. Platform

**Android-only MVP.** Android's `VpnService` API provides full DNS interception capability. iOS uses a restricted Screen Time API — that's a future phase, with an intentionally different (more hands-off) experience.

## 5. Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                    Android Device                    │
│                                                     │
│  ┌──────────┐    DNS query     ┌──────────────────┐ │
│  │  Browser  │ ──────────────► │  VpnService       │ │
│  │  or App   │                 │  (TUN interface)  │ │
│  └──────────┘                  └────────┬─────────┘ │
│                                         │           │
│                                ┌────────▼─────────┐ │
│                                │   DnsProxy        │ │
│                                │                   │ │
│                                │  Blocklist match? │ │
│                                │                   │ │
│                                │  YES → 127.0.0.1 │ │
│                                │  NO  → Forward    │ │
│                                └──┬──────────┬────┘ │
│                                   │          │      │
│                          ┌────────▼──┐  ┌───▼──────┐│
│                          │Intervention│  │Default   ││
│                          │Server      │  │Resolver  ││
│                          │(NanoHTTPD) │  │(ISP DNS) ││
│                          └───────────┘  └──────────┘│
└─────────────────────────────────────────────────────┘
```

### Key Technical Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| VPN tunnel scope | DNS-only | Zero impact on non-DNS traffic. Privacy promise holds — we never see browsing data. |
| DNS resolution | Local proxy, blocklist-first check, clean queries forwarded to phone's default resolver | No new third-party DNS dependency. No privacy regression. |
| Intervention delivery | NanoHTTPD embedded in VPN service, serves HTML pages | Self-contained in the VPN process. Works even if the main app is killed. |
| Intervention page format | Plain HTML/CSS/JS bundled in APK | No dependency on the app UI layer. Served from `127.0.0.1`. |
| Blocklist storage | Compressed file bundled in APK assets | Fully offline. Updated via Google Play app updates. |
| Escalation state | Room DB for persistence, AIDL for real-time VPN↔UI communication | Survives process death. Real-time for intervention triggers. |
| Service lifecycle | `START_STICKY` + foreground notification | Auto-restarts if Android kills the service. |

## 6. Deep Modules

These are the isolated, testable components with clear interfaces:

### 6.1 DnsProxy

**Responsibility:** Intercepts DNS packets, checks against blocklist, returns redirect IP or forwards to default resolver.

| Input | Output |
|-------|--------|
| DNS query packet (domain name) | If blocked: `127.0.0.1` (redirect to intervention server) |
| | If not blocked: forward to default resolver, return response |

**Interface:**
```kotlin
interface DnsProxy {
    fun resolve(query: DnsQuery): DnsResponse
}

sealed class DnsResponse {
    data class Blocked(val redirectIp: InetAddress) : DnsResponse()
    data class Forwarded(val answer: ByteArray) : DnsResponse()
}
```

**Testability:** ✅ High — pure logic. Feed domain, get blocked/forwarded.

### 6.2 BlocklistStore

**Responsibility:** Loads and queries the offline domain blacklist from bundled assets.

| Input | Output |
|-------|--------|
| Domain name (e.g., `example.com`) | `true` if blocked, `false` if not |

**Interface:**
```kotlin
interface BlocklistStore {
    fun isBlocked(domain: String): Boolean
    fun loadBlocklist(assetPath: String)
    fun size(): Int
}
```

**Performance requirement:** Lookup against 100k+ domains in <5ms.

**Testability:** ✅ High — load list, query domains, verify results.

### 6.3 EscalationEngine

**Responsibility:** Manages intervention stages, session state, sliding window resets, and cooling periods.

**State Machine:**
```
                    ┌──────────────┐
                    │   STAGE_1    │  Soft Intercept (15s countdown)
                    │              │
                    └──────┬───────┘
                           │ User clicks "I still want to proceed"
                           ▼
                    ┌──────────────┐
                    │   STAGE_2    │  Active Deflection (30s breathing + dhikr)
                    │              │
                    └──────┬───────┘
                           │ User clicks "Override"
                           ▼
                    ┌──────────────┐
                    │   STAGE_3    │  Hard Lock (2 min, no override)
                    │              │
                    └──────┬───────┘
                           │ Timer completes
                           ▼
                    ┌──────────────┐
                    │  UNLOCKED    │  Domain accessible
                    └──────┬───────┘
                           │ 10-min cooling period active
                           │ Next blocked domain → skip to STAGE_3
                           ▼
                    ┌──────────────┐
                    │ COOLING      │  All blocked domains → STAGE_3 directly
                    │ (10 minutes) │
                    └──────────────┘
```

**Sliding Window Reset:**
- 30 minutes idle (no blocked request) → reset one level (Stage 3 → Stage 2, Stage 2 → Stage 1)
- 10 minutes after any Stage 3 override → no reset, cooling period is absolute

**Interface:**
```kotlin
interface EscalationEngine {
    fun onBlockedRequest(domain: String): EscalationState
    fun getCurrentStage(): EscalationStage
    fun getCooldownEndTime(): Instant?
    fun getLastRequestTime(): Instant
    fun reset()
}

enum class EscalationStage { STAGE_1, STAGE_2, STAGE_3, COOLING }
```

**Testability:** ✅ High — time-based state machine. Inject a clock for deterministic testing.

### 6.4 SpiritualContent

**Responsibility:** Loads and rotates ayat, hadith, and dhikr from bundled JSON. Supports 6 languages. Rotation without replacement until all items are shown.

| Input | Output |
|-------|--------|
| Content type (ayat / hadith / dhikr) + language code | Next content item in rotation |

**Interface:**
```kotlin
interface SpiritualContent {
    fun nextAyah(language: Language): ContentItem
    fun nextHadith(language: Language): ContentItem
    fun nextDhikr(language: Language): ContentItem
    fun resetRotation()
}

data class ContentItem(
    val arabic: String,
    val translation: String,
    val source: String  // e.g., "Quran 24:21", "Muslim 2657"
)

enum class Language { AR, EN, UR, MS, TR, FR }
```

**MVP content counts:** 20 ayat, 15 hadith, 10 dhikr per language.

**Testability:** ✅ High — load JSON, query, verify rotation order and language.

### 6.5 InterventionServer

**Responsibility:** NanoHTTPD server embedded in VPN service. Serves HTML intervention pages based on escalation stage and language.

| Input | Output |
|-------|--------|
| HTTP request from redirected browser | HTML page for the current escalation stage |

**Pages served:**

**Stage 1 — Soft Intercept:**
- Single Quranic ayah or hadith (randomized from rotation)
- 15-second countdown timer — "Back" disabled, "Continue Anyway" greyed out
- After 15s: "Go Back" (primary) + "I still want to proceed" (small, muted)

**Stage 2 — Active Deflection:**
- 4-7-8 breathing exercise (inhale 4s, hold 7s, exhale 8s)
- Dhikr counter (سُبْحَانَ الله, الْحَمْدُ لِلَّه, اَللَّهُ اَكْبَر) — tap to count
- 30-second minimum before exit option
- After 30s: "I've calmed down — go back" (primary) + "Override" (text link only)

**Stage 3 — Hard Lock:**
- Full screen lock for 2 minutes — no override
- Message: *"You chose this lock for yourself. It's not punishment — it's protection."*
- Suggestions: make wudu, leave the room, pray 2 rak'ah
- After 2 min: lock lifts

**Testability:** ✅ Medium — verify correct HTML served for stage + language.

### 6.6 CompanionPin

**Responsibility:** Optional PIN set by a parent/guardian during onboarding. When enabled, PIN is required to disable protection or change settings.

**Interface:**
```kotlin
interface CompanionPin {
    fun isSet(): Boolean
    fun setPin(pin: String)
    fun verifyPin(pin: String): Boolean
    fun removePin(currentPin: String): Boolean
}
```

**Testability:** ✅ Medium — set, verify, remove PIN logic.

### 6.7 VpnService

**Responsibility:** Android `VpnService` — configures TUN interface, routes DNS to DnsProxy, manages lifecycle.

- `START_STICKY` — auto-restart on kill
- Foreground notification: "Khalawat is active"
- Detects VPN disconnect → sends reminder notification

**Testability:** ❌ Low — requires Android runtime.

### 6.8 OnboardingFlow

**Responsibility:** 5-screen first-run setup.

| Screen | Content |
|--------|---------|
| 1 — Intent | *"Khalawat protects you in your private moments. You set it up. You control it. No one watches."* |
| 2 — How it works | *"When you try to access something you've decided to avoid, Khalawat steps in — not to shame you, but to give you a moment to choose differently."* |
| 3 — Language | العربية \| English \| اردو \| Bahasa Melayu \| Türkçe \| Français |
| 4 — Permission | *"Khalawat needs VPN permission to check website addresses before they load. It never sees your traffic, never logs anything, never sends data anywhere."* → [Enable Protection] |
| 5 — Companion PIN (optional) | *"For yourself: Skip this. For a child: Set a PIN only you know."* + parental message |

**Parental message (Screen 5):**
> *"Your child's screen is their private space — but you have the right to protect them. Khalawat doesn't spy, doesn't log, and doesn't report. It simply stands between them and what harms their soul. The PIN you set here means only you can turn it off."*

**Testability:** ❌ Low — UI flow, requires Android runtime.

### 6.9 SessionPersistence

**Responsibility:** Room DB — stores escalation state, cooldown timestamps. Survives process death.

| Table | Fields |
|-------|--------|
| `escalation_state` | `domain`, `stage`, `last_request_time`, `cooldown_end_time` |
| `override_log` | `domain`, `timestamp`, `stage_reached` |

**Testability:** ✅ Medium — DB read/write, TTL logic.

## 7. Anti-Tamper

| Escape vector | Defense |
|---------------|---------|
| **Turn off VPN from notification** | 30-second hold button + spiritual reminder before disabling |
| **Uninstall app** | Cannot prevent. Android allows it. |
| **Settings → VPN disconnect** | Same as notification disconnect — in-app friction |
| **DNS-over-HTTPS bypass** | Known DoH server domains added to blocklist (Chrome "secure DNS", Cloudflare 1.1.1.1, etc.) |
| **In-app disable** | If Companion PIN is set → requires PIN. If not → 30-second hold. |
| **VPN disconnect detected** | Reminder notification: *"Khalawat was protecting you. Re-enable to stay guarded."* |

## 8. Notifications

| Notification | When | Style |
|-------------|------|-------|
| **Foreground service** | VPN running | Persistent, minimal: "Khalawat is active" with shield icon |
| **Disconnect reminder** | VPN stopped unexpectedly | "Khalawat was protecting you. Re-enable to stay guarded." |
| **Daily spiritual reminder** | Once daily (optional toggle) | Rotating ayah from SpiritualContent |

## 9. Business Model

**100% free. All features included. No premium tier.**

This is sadaqah — no paywall between a person and their protection. All features including Night Guard scheduling, extended blocklist, stronger escalation, and usage patterns are available to everyone.

## 10. CI/CD

- **GitHub Actions** workflow on every push
- Runs unit tests (DnsProxy, BlocklistStore, EscalationEngine, SpiritualContent)
- Builds debug APK
- APK downloadable from workflow artifacts

## 11. Testing Strategy

| Layer | Scope | When |
|-------|-------|------|
| **Unit tests** | DnsProxy, BlocklistStore, EscalationEngine, SpiritualContent | Every push (CI) |
| **Manual validation** | Full flow on one real Android device | Before release |

**Manual validation checklist:**
- [ ] Install APK on real device
- [ ] Enable VPN → open blocked domain in Chrome → intervention page shows
- [ ] Click through all 3 stages → escalation works
- [ ] Wait 30 minutes → stage resets one level
- [ ] Kill the app → VPN restarts via `START_STICKY`
- [ ] Turn off VPN from notification → reminder notification appears
- [ ] 30-second hold to disable works
- [ ] Companion PIN set → cannot disable without PIN
- [ ] Language switch → intervention pages show correct language

## 12. Privacy Guarantees

| Guarantee | How |
|-----------|-----|
| No traffic logged | VPN only intercepts DNS. No HTTP/HTTPS inspection. |
| No data sent anywhere | Blocklist is local. DNS forwarding uses phone's existing resolver. |
| No user accounts | No sign-up. No cloud. No analytics. |
| No third-party DNS | Clean queries go to the phone's default resolver, not ours. |
| Source auditable | Open source. Anyone can verify. |

## 13. Future (Post-MVP)

| Feature | Notes |
|---------|-------|
| iOS version | Screen Time API — restricted, more hands-off by necessity |
| Night Guard | Scheduled blocking hours (e.g., 10pm–6am) |
| Extended blocklist | More categories: gambling, dating apps |
| Custom escalation | User-configurable stage durations |
| Usage patterns | Local-only dashboard: "You triggered intervention 3 times this week" |
| Custom spiritual content | User can add their own ayat, dua, or reminders |

## 14. Tech Stack Summary

| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| VPN | Android VpnService (DNS-only TUN) |
| DNS Proxy | Custom Kotlin implementation |
| HTTP Server | NanoHTTPD (embedded in VPN service) |
| Intervention Pages | Plain HTML/CSS/JS bundled in APK |
| Database | Room (escalation state, session persistence) |
| IPC | AIDL (VPN service ↔ app UI) |
| Build | Gradle + GitHub Actions |
| Min SDK | 24 (Android 7.0+) |
| Target SDK | 35 |

---

*Document generated: 2026-05-09*  
*Status: Design complete — ready for implementation*
