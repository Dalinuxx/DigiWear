/**
 * DigiWear Multiplayer Live Relay Server
 * 
 * Ultra-lightweight (<20MB RAM), zero-database, authenticated WebSocket relay
 * designed for self-hosting on Raspberry Pis, home servers, VPS, or Docker.
 */

const http = require('http');
const crypto = require('crypto');
const { WebSocketServer, WebSocket } = require('ws');

const PORT = parseInt(process.env.PORT || '8080', 10);
const HOST = process.env.HOST || '0.0.0.0';
const SERVER_KEY = process.env.DIGIWEAR_SERVER_KEY ? process.env.DIGIWEAR_SERVER_KEY.trim() : '';
const RELAY_PATH = (process.env.RELAY_PATH || '').trim();
const MAX_PAYLOAD_BYTES = 32 * 1024; // 32 KB
const MAX_CLIENTS_PER_IP = parseInt(process.env.MAX_CLIENTS_PER_IP || '15', 10);
const PING_INTERVAL_MS = 25000;

// State in RAM
const rooms = new Map(); // roomId -> { hostWs, guestWs, createdAt, lastActivity }
const ipCounts = new Map(); // ip -> count

function generateRoomCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // Base32 unambiguous
    let code = '';
    for (let i = 0; i < 6; i++) {
        code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
}

const server = http.createServer((req, res) => {
    // Enable CORS for web inspection if needed
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Headers', '*');

    if (req.method === 'GET' && (req.url === '/' || req.url === '/health')) {
        let totalRooms = rooms.size;
        let totalActiveDuels = 0;
        for (const room of rooms.values()) {
            if (room.hostWs && room.guestWs) totalActiveDuels++;
        }
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            status: 'online',
            service: 'digiwear-live-relay',
            version: '1.0.0',
            authRequired: SERVER_KEY.length > 0,
            activeRooms: totalRooms,
            activeDuels: totalActiveDuels,
            memoryUsageBytes: process.memoryUsage().rss
        }));
        return;
    }

    res.writeHead(404);
    res.end('Not Found');
});

const wss = new WebSocketServer({ noServer: true, maxPayload: MAX_PAYLOAD_BYTES });

// Handle HTTP -> WebSocket Upgrade with Security Verification
server.on('upgrade', (request, socket, head) => {
    const remoteIp = request.headers['x-forwarded-for']?.split(',')[0].trim() || socket.remoteAddress || 'unknown';

    // 0. Secret URL Path check (stealth against automated web crawlers)
    if (RELAY_PATH.length > 0) {
        const urlObj = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
        if (!urlObj.pathname.startsWith(RELAY_PATH)) {
            socket.write('HTTP/1.1 404 Not Found\r\n\r\n');
            socket.destroy();
            return;
        }
    }

    // 1. Check IP connection rate limit
    const currentIpCount = ipCounts.get(remoteIp) || 0;
    if (currentIpCount >= MAX_CLIENTS_PER_IP) {
        socket.write('HTTP/1.1 429 Too Many Requests\r\n\r\n');
        socket.destroy();
        return;
    }

    // 2. Pre-Shared Key Authentication check
    if (SERVER_KEY.length > 0) {
        const urlObj = new URL(request.url, `http://${request.headers.host || 'localhost'}`);
        const queryKey = urlObj.searchParams.get('key');
        const headerKey = request.headers['x-digiwear-key'];
        const authHeader = request.headers['authorization'];
        const bearerKey = authHeader?.startsWith('Bearer ') ? authHeader.slice(7).trim() : null;

        const providedKey = (queryKey || headerKey || bearerKey || '').trim();

        // Constant-time comparison to prevent timing attacks
        const expectedBuf = Buffer.from(SERVER_KEY, 'utf-8');
        const providedBuf = Buffer.from(providedKey, 'utf-8');

        if (expectedBuf.length !== providedBuf.length || !crypto.timingSafeEqual(expectedBuf, providedBuf)) {
            socket.write('HTTP/1.1 401 Unauthorized\r\nContent-Type: text/plain\r\n\r\nInvalid DigiWear Server Key\r\n');
            socket.destroy();
            return;
        }
    }

    wss.handleUpgrade(request, socket, head, (ws) => {
        ipCounts.set(remoteIp, (ipCounts.get(remoteIp) || 0) + 1);
        ws.remoteIp = remoteIp;
        wss.emit('connection', ws, request);
    });
});

wss.on('connection', (ws) => {
    ws.isAlive = true;
    ws.roomId = null;
    ws.role = null;
    ws.msgCountSec = 0;
    ws.lastRateReset = Date.now();

    ws.on('pong', () => {
        ws.isAlive = true;
    });

    ws.on('message', (data) => {
        // Simple rate limiting: max 40 messages per second per client
        const now = Date.now();
        if (now - ws.lastRateReset > 1000) {
            ws.msgCountSec = 0;
            ws.lastRateReset = now;
        }
        ws.msgCountSec++;
        if (ws.msgCountSec > 40) {
            ws.send(JSON.stringify({ type: 'ERROR', code: 'RATE_LIMIT_EXCEEDED', message: 'Too many packets' }));
            return;
        }

        let msg;
        try {
            msg = JSON.parse(data.toString());
        } catch {
            ws.send(JSON.stringify({ type: 'ERROR', code: 'BAD_JSON', message: 'Malformed JSON envelope' }));
            return;
        }

        handleMessage(ws, msg);
    });

    ws.on('close', () => {
        // Decrement IP count
        const count = ipCounts.get(ws.remoteIp) || 1;
        if (count <= 1) {
            ipCounts.delete(ws.remoteIp);
        } else {
            ipCounts.set(ws.remoteIp, count - 1);
        }

        cleanupSocketFromRoom(ws);
    });

    ws.on('error', () => {
        cleanupSocketFromRoom(ws);
    });

    // Send connection greeting
    ws.send(JSON.stringify({
        type: 'CONNECTED',
        server: 'DigiWear Live Relay',
        version: '1.0.0',
        timestamp: Date.now()
    }));
});

function handleMessage(ws, msg) {
    const type = msg.type;

    switch (type) {
        case 'CREATE_ROOM': {
            cleanupSocketFromRoom(ws);
            let code = (msg.roomId || '').trim().toUpperCase();
            if (!code || code.length !== 6 || rooms.has(code)) {
                do {
                    code = generateRoomCode();
                } while (rooms.has(code));
            }

            const room = {
                roomId: code,
                hostWs: ws,
                guestWs: null,
                createdAt: Date.now(),
                lastActivity: Date.now()
            };
            rooms.set(code, room);
            ws.roomId = code;
            ws.role = 'HOST';

            ws.send(JSON.stringify({
                type: 'ROOM_CREATED',
                roomId: code,
                role: 'HOST'
            }));
            break;
        }

        case 'JOIN_ROOM': {
            cleanupSocketFromRoom(ws);
            const code = (msg.roomId || '').trim().toUpperCase();
            const room = rooms.get(code);

            if (!room) {
                ws.send(JSON.stringify({ type: 'ERROR', code: 'ROOM_NOT_FOUND', message: 'Room code does not exist' }));
                return;
            }

            if (room.guestWs && room.guestWs.readyState === WebSocket.OPEN) {
                ws.send(JSON.stringify({ type: 'ERROR', code: 'ROOM_FULL', message: 'Room already has two active tamers' }));
                return;
            }

            room.guestWs = ws;
            room.lastActivity = Date.now();
            ws.roomId = code;
            ws.role = 'GUEST';

            ws.send(JSON.stringify({
                type: 'ROOM_JOINED',
                roomId: code,
                role: 'GUEST'
            }));

            // Notify Host
            if (room.hostWs && room.hostWs.readyState === WebSocket.OPEN) {
                room.hostWs.send(JSON.stringify({
                    type: 'PEER_JOINED',
                    role: 'HOST',
                    peerRole: 'GUEST'
                }));
            }
            break;
        }

        case 'RELAY': {
            // Forward payload to peer in the same room
            if (!ws.roomId || !rooms.has(ws.roomId)) {
                ws.send(JSON.stringify({ type: 'ERROR', code: 'NOT_IN_ROOM', message: 'Socket is not attached to any room' }));
                return;
            }

            const room = rooms.get(ws.roomId);
            room.lastActivity = Date.now();
            const recipient = (ws === room.hostWs) ? room.guestWs : room.hostWs;

            if (recipient && recipient.readyState === WebSocket.OPEN) {
                recipient.send(JSON.stringify({
                    type: 'RELAY',
                    fromRole: ws.role,
                    payload: msg.payload
                }));
            } else {
                ws.send(JSON.stringify({ type: 'ERROR', code: 'PEER_NOT_CONNECTED', message: 'Opponent tamer is currently disconnected' }));
            }
            break;
        }

        case 'LEAVE_ROOM': {
            cleanupSocketFromRoom(ws);
            ws.send(JSON.stringify({ type: 'ROOM_LEFT' }));
            break;
        }

        default: {
            ws.send(JSON.stringify({ type: 'ERROR', code: 'UNKNOWN_TYPE', message: `Unknown action ${type}` }));
            break;
        }
    }
}

function cleanupSocketFromRoom(ws) {
    if (!ws.roomId) return;
    const room = rooms.get(ws.roomId);
    if (!room) return;

    const isHost = (room.hostWs === ws);
    const peer = isHost ? room.guestWs : room.hostWs;

    if (isHost) {
        room.hostWs = null;
    } else {
        room.guestWs = null;
    }

    if (peer && peer.readyState === WebSocket.OPEN) {
        peer.send(JSON.stringify({
            type: 'PEER_DISCONNECTED',
            role: isHost ? 'HOST' : 'GUEST'
        }));
    }

    // If both left, prune room from memory
    if (!room.hostWs && !room.guestWs) {
        rooms.delete(ws.roomId);
    }

    ws.roomId = null;
    ws.role = null;
}

// Keepalive ping loop & dead room cleaner
const interval = setInterval(() => {
    const now = Date.now();

    // 1. Ping active sockets
    wss.clients.forEach((ws) => {
        if (!ws.isAlive) return ws.terminate();
        ws.isAlive = false;
        ws.ping();
    });

    // 2. Prune abandoned rooms older than 30 minutes
    for (const [code, room] of rooms.entries()) {
        if (now - room.lastActivity > 30 * 60 * 1000) {
            if (room.hostWs) room.hostWs.terminate();
            if (room.guestWs) room.guestWs.terminate();
            rooms.delete(code);
        }
    }
}, PING_INTERVAL_MS);

wss.on('close', () => {
    clearInterval(interval);
});

server.listen(PORT, HOST, () => {
    console.log(`[DigiWear Relay] Listening on ${HOST}:${PORT}`);
    console.log(`[DigiWear Relay] Auth: ${SERVER_KEY.length > 0 ? 'ENABLED (DIGIWEAR_SERVER_KEY set)' : 'DISABLED (Public Open Mode)'}`);
});
