/**
 * Smoke test for DigiWear Relay Server
 */
const http = require('http');
const { spawn } = require('child_process');

console.log('[Test] Starting relay server test process...');

const env = Object.assign({}, process.env, {
    PORT: '8999',
    HOST: '127.0.0.1',
    DIGIWEAR_SERVER_KEY: 'test-secret-key-123'
});

const srv = spawn('node', ['server.js'], { cwd: __dirname, env });

srv.stdout.on('data', (d) => process.stdout.write(d.toString()));
srv.stderr.on('data', (d) => process.stderr.write(d.toString()));

setTimeout(() => {
    // 1. Test health check
    http.get('http://127.0.0.1:8999/health', (res) => {
        let body = '';
        res.on('data', c => body += c);
        res.on('end', () => {
            console.log('[Test] Health response:', body);
            try {
                const parsed = JSON.parse(body);
                if (parsed.status === 'online' && parsed.authRequired === true) {
                    console.log('[Test] SUCCESS: Server health check passed with auth enabled!');
                } else {
                    console.error('[Test] FAIL: Unexpected health payload');
                    process.exit(1);
                }
            } catch (err) {
                console.error('[Test] FAIL: Parse error', err);
                process.exit(1);
            } finally {
                srv.kill();
                process.exit(0);
            }
        });
    }).on('error', (err) => {
        console.error('[Test] FAIL: Request error', err);
        srv.kill();
        process.exit(1);
    });
}, 1200);
