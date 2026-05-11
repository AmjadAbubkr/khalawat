# Integrations — Khalawat

## Android System Integrations

### VpnService (`android.net.VpnService`)
- **Purpose**: Intercepts DNS traffic at the network level via a TUN interface
- **Configuration**: DNS-only routing (no HTTP/HTTPS inspection)
- **Lifecycle**: `START_STICKY` — auto-restarts if Android kills the service
- **Foreground Service**: Required notification with "Khalawat is active"
- **Permission**: User must grant VPN permission via system dialog
- **Entry point**: `KhalawatVpnService.kt` — thin shell; all logic delegated to `DnsResolverCoordinator`

### SharedPreferences (`android.content.SharedPreferences`)
- **Purpose**: Persistent app preferences
- **Keys stored**:
  - `onboarding_complete` (Boolean)
  - `companion_pin` (String?)
  - `selected_language` (String)
  - `vpn_active` (Boolean)
  - `parent_message` (String?)
- **Wrapper**: `KhalawatPreferences.kt`

### Room Database (`androidx.room`)
- **Purpose**: Persist escalation state across process death
- **Database**: `AppDatabase` (singleton, version 1)
- **Tables**:
  - `escalation_state` — current stage, timestamps
  - `override_log` — domain, stage, timestamp
- **TypeConverters**: `Converters.kt` — `EscalationStage` enum ↔ String
- **DAO**: `EscalationStateDao.kt`
- **Repository**: `RoomSessionRepository.kt` (implements `SessionRepository`)

### NotificationChannel
- **Channel ID**: `khalawat_vpn`
- **Name**: "Khalawat VPN Protection"
- **Importance**: LOW (non-intrusive)
- **Created in**: `KhalawatVpnService.onCreate()`

### ActivityResultContracts
- **Purpose**: Modern replacement for deprecated `startActivityForResult`
- **Used for**: VPN permission request (`VpnService.prepare()`)
- **Location**: `MainActivity.kt`

## Internal Module Integrations

### DnsResolverCoordinator ↔ DnsProxy + EscalationEngine + SessionRepository
The coordinator is the central orchestrator:
- Receives DNS queries from `KhalawatVpnService`
- Sends queries to `DnsProxy.resolve()` for blocklist check
- If blocked → calls `EscalationEngine.onBlockedRequest(domain)`
- Persists state via `SessionRepository.saveState()`
- Logs overrides via `SessionRepository.logOverride()`
- Returns `DnsResult` (FORWARD or REDIRECT) to the VPN service

### KhalawatVpnService ↔ InterventionServer
- VPN service starts `InterventionServer` on `127.0.0.1:8080`
- Blocked DNS queries redirect to `127.0.0.1`
- Browser loads intervention HTML from the embedded server

### EscalationEngine ↔ InterventionServer
- Escalation stage determines which HTML page the server serves:
  - STAGE_1 → `stage1.html` (15s countdown + ayah)
  - STAGE_2 → `stage2.html` (breathing + dhikr)
  - STAGE_3 → `stage3.html` (2-min lock)
  - COOLING → `stage3.html` (same as Stage 3)

### OnboardingState ↔ KhalawatPreferences
- When onboarding completes, `KhalawatPreferences` stores:
  - `onboarding_complete = true`
  - `companion_pin` (if set)
  - `selected_language`
  - `parent_message`

### AntiTamperState ↔ KhalawatPreferences
- Reads companion PIN from `KhalawatPreferences` to determine if PIN gate is needed
- Tracks disconnect count for reminder notification logic

### BlocklistStore ↔ DnsProxy
- `DnsProxyImpl` uses `BlocklistStore.isBlocked(domain)` to check each query
- Blocklist loaded from `assets/blocklist.txt`

### SpiritualContent ↔ InterventionServer
- `InterventionServer` uses `SpiritualContent.nextAyah()` / `nextHadith()` to populate intervention pages
- Content rotated without replacement until all items shown

## External Network Integrations

### Upstream DNS (8.8.8.8)
- **Purpose**: Forward non-blocked DNS queries
- **Address**: `8.8.8.8:53` (Google Public DNS)
- **Protocol**: UDP
- **Privacy note**: Only clean (non-blocked) queries reach this server. Blocked queries never leave the device.

### NanoHTTPD (embedded)
- **Purpose**: Serve intervention HTML pages to redirected browsers
- **Bind**: `127.0.0.1:8080`
- **Scope**: Local only — no external network access
- **Library version**: 2.3.1

## No External Services
Khalawat has **zero** external service dependencies:
- No analytics
- No crash reporting
- No user accounts / authentication
- No cloud storage
- No remote configuration
- No ad networks
- No third-party DNS (clean queries go to 8.8.8.8, which is user-visible and replaceable)
