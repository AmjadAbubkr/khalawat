# Architecture — Khalawat

## System Overview

Khalawat is a voluntary digital self-discipline app for Muslims. It uses Android's `VpnService` to intercept DNS requests at the network level, check them against a local blocklist, and either forward clean queries to the device's default resolver or redirect blocked queries to a local intervention server that serves staged spiritual content.

```
