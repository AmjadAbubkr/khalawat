# Concerns & Technical Debt — Khalawat

## Security & Privacy

### ✅ Strengths
- **No logging**: VPN only intercepts DNS. No HTTP/HTTPS inspection.
- **No external services**: Zero analytics, crash reporting, accounts, or cloud storage.
- **Local blocklist**: All filtering happens on-device. No data leaves the device.
- **Local intervention server**: NanoHTTPD binds to `127.0.0.1` only.
- **Open source**: Code is auditable.

### ⚠️ Concerns

1. **SharedPreferences stores PIN in plaintext**
   - `KhalawatPreferences` stores `companion_pin` as a plain string
   - **Risk**: Rooted devices can read SharedPreferences XML files
   - **Mitigation**: Use Android Keystore + EncryptedSharedPreferences for PIN storage
   - **Priority**: Medium — Companion PIN is optional; only affects parent-installed devices

2. **Blocklist is visible in APK assets**
   - `blocklist.txt` is bundled as a plain text asset
   - **Risk**: Anyone can extract and view the full list of blocked domains
   - **Mitigation**: Obfuscate or encrypt the blocklist in production builds
   - **Priority**: Low — the domains are publicly known; obscurity is not security

3. **No certificate pinning for upstream DNS**
   - DNS queries forwarded to `8.8.8.8` via plain UDP
   - **Risk**: DNS spoofing on the local network
   - **Mitigation**: Consider DNS-over-HTTPS or DNS-over-TLS for upstream resolution
   - **Priority**: Low — standard DNS behavior; users already accept this risk without Khalawat

4. **NanoHTTPD has no auth**
   - Intervention server on `127.0.0.1:8080` has no authentication
   - **Risk**: Other apps on the device could access the server
   - **Mitigation**: Port is localhost-only; risk is limited to co-resident malicious apps
   - **Priority**: Low — standard Android sandboxing limits inter-app access

## Architecture Risks

1. **VPN permission revocation**
   - Android allows users to revoke VPN permission from Settings at any time
   - **Risk**: Protection silently disabled without going through anti-tamper flow
   - **Mitigation**: Detect VPN disconnect and show reminder notification
   - **Gap**: No persistent re-prompt on disconnect (only a single notification)

2. **Android process death**
   - Android may kill the VPN service under memory pressure
   - **Mitigation**: `START_STICKY` ensures restart; Room DB persists escalation state
   - **Gap**: Brief window of no protection between kill and restart

3. **No AIDL/IPC between VPN service and app UI**
   - PRD mentions AIDL for VPN↔UI communication
   - **Current state**: No AIDL implementation; app and service don't communicate beyond `startService()`
   - **Gap**: Dashboard cannot show real-time escalation state from the VPN service
   - **Priority**: High for Issue #13 (end-to-end integration)

4. **Single upstream DNS (8.8.8.8)**
   - Hardcoded in `KhalawatVpnService`
   - **Risk**: If 8.8.8.8 is unreachable, all non-blocked DNS fails
   - **Mitigation**: Fall back to device's default DNS resolver
   - **Gap**: No fallback implementation

## Missing Features (Post-MVP)

| Feature | PRD Section | Priority |
|---------|-------------|----------|
| AIDL VPN↔UI communication | §6.7 | High — needed for real-time dashboard |
| Notification channel + daily reminder | §8 | Medium — Issue #12 |
| End-to-end integration testing | §11 | High — Issue #13 |
| EncryptedSharedPreferences for PIN | §6.6 | Medium |
| DNS-over-HTTPS bypass prevention | §7 | Low — DoH domains in blocklist |
| Night Guard (scheduled blocking) | §13 | Post-MVP |
| iOS version | §4 | Post-MVP |
| Extended blocklist (gambling, dating) | §13 | Post-MVP |
| Custom escalation durations | §13 | Post-MVP |
| Usage patterns dashboard | §13 | Post-MVP |
| Custom spiritual content | §13 | Post-MVP |

## Known Issues

1. **Icon dependency**: `material-icons-extended:1.7.6` is a large dependency (~20MB) for 3 icons. Should migrate to individual icon imports or custom drawables.

2. **Arabic text encoding**: Unicode escape sequences used in source files to avoid Windows codepage corruption. This reduces readability for Arabic-literate developers.

3. **No ProGuard rules for NanoHTTPD**: May need keep rules for reflection-based HTTP serving.

4. **Blocklist is static**: Updated only via app updates. No remote update mechanism (by design, for privacy).

5. **No instrumentation tests**: `RoomSessionRepository` and Compose UI screens are not covered by unit tests. Room-backed implementation needs Android Context.

## Technical Debt

| Item | Description | Effort |
|------|-------------|--------|
| Replace `material-icons-extended` | Use individual icon modules or custom vector drawables | Low |
| Implement AIDL/IPC | VPN service ↔ app UI real-time communication | Medium |
| EncryptedSharedPreferences | Secure PIN storage instead of plain SharedPreferences | Low |
| DNS fallback | Fall back to device default DNS if 8.8.8.8 unreachable | Low |
| ProGuard rules for NanoHTTPD | Add keep rules for release builds | Low |
| Compose UI tests | Add `ui-test-junit4` tests for screens | Medium |
| Instrumented Room tests | Test `RoomSessionRepository` on device/emulator | Medium |
| Blocklist encryption | Obfuscate blocklist in APK for production | Medium |
