# DigiWear Live Multiplayer Relay Server

An ultra-lightweight (<20MB RAM, zero database), hardened WebSocket relay server for real-time remote PvP duels between DigiWear tamers.

---

## Features

- **Microscopic Footprint**: Pure Node.js event-loop relay requiring < 20MB RAM and zero external database. Runs effortlessly on a Raspberry Pi Zero, VPS, home server, NAS, or free-tier container.
- **Pre-Shared Key Security**: Support for `DIGIWEAR_SERVER_KEY` authentication via query parameter (`?key=...`) or HTTP header (`X-DigiWear-Key`). Unauthenticated sockets are rejected at the HTTP upgrade stage with constant-time comparison.
- **Zero-Trust E2EE Friendly**: The relay blindly forwards encrypted payloads (`ENCRYPTED_PAYLOAD`) between matched tamers without inspecting or persisting any battle secrets or fighter states.
- **DDoS & Flood Protection**: Per-IP concurrent connection throttling and per-socket packet-rate limiting (40 msgs/sec).
- **Auto-Pruning Rooms**: Temporary 6-character room codes (`ABCDEF`) that auto-clean upon client disconnect or 30-minute inactivity.

---

## Quick Start (Docker / Podman)

### 1. Run with Docker Compose
```bash
# Clone or copy relay-server directory
cd relay-server

# Optional: define your secret key
export DIGIWEAR_SERVER_KEY="my-secret-tamer-key-12345"

# Launch in background
docker compose up -d --build
```

### 2. Run with Bare Docker
```bash
docker build -t digiwear-relay .
docker run -d --name digiwear-relay -p 8080:8080 -e DIGIWEAR_SERVER_KEY="my-secret-tamer-key-12345" --memory="32m" digiwear-relay
```

---

## Running with Node.js Directly

```bash
cd relay-server
npm install
DIGIWEAR_SERVER_KEY="my-secret-tamer-key-12345" PORT=8080 npm start
```

---

## Health Check Endpoint

```bash
curl http://localhost:8080/health
```

Response:
```json
{
  "status": "online",
  "service": "digiwear-live-relay",
  "version": "1.0.0",
  "authRequired": true,
  "activeRooms": 1,
  "activeDuels": 1,
  "memoryUsageBytes": 16777216
}
```

---

## Companion App Configuration

In the DigiWear Companion App (`Battle Hub` -> `Live Duel Lobby` -> `Server Settings`):
- **Relay URL**: `wss://relay.yourdomain.com` (or `ws://192.168.1.100:8080` for local LAN)
- **Server Key**: `my-secret-tamer-key-12345` (leave blank if server auth is disabled)
