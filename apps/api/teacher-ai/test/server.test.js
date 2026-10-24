const assert = require('node:assert/strict');
const { once } = require('node:events');
const { after, before, test } = require('node:test');
const app = require('../src/server');

let server;
let base;
before(async () => {
  server = app.listen(0, '127.0.0.1');
  await once(server, 'listening');
  base = `http://127.0.0.1:${server.address().port}`;
});
after(async () => {
  server.closeAllConnections();
  await new Promise(resolve => server.close(resolve));
});

test('existing health and CORS contract is preserved without exposing credentials', async () => {
  const response = await fetch(`${base}/api/health`, { headers: { Origin: 'http://localhost:5173' } });
  assert.equal(response.status, 200);
  assert.equal(response.headers.get('access-control-allow-origin'), '*');
  const data = await response.json();
  assert.equal(data.status, 'ok');
  assert.equal(typeof data.hasApiKey, 'boolean');
  assert.equal(typeof data.model, 'string');
  assert.deepEqual(Object.keys(data).sort(), ['hasApiKey', 'model', 'status']);
});

test('subject catalog remains available from the same API path', async () => {
  const response = await fetch(`${base}/api/knowledge/subjects`);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.success, true);
  assert.ok(Array.isArray(body.data.subjects));
  assert.ok(Array.isArray(body.data.grades));
});

for (const path of ['/api/knowledge/generate', '/api/lesson-plan/generate', '/api/lesson-plan/generate-stream']) {
  test(`validates missing input before calling the model: ${path}`, async () => {
    const response = await fetch(`${base}${path}`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}' });
    assert.equal(response.status, 400);
    const body = await response.json();
    assert.equal(body.success, false);
    assert.match(body.error, /subject/);
  });
}

for (const path of ['/', '/index.html', '/api/missing']) {
  test(`does not serve a second frontend: ${path}`, async () => {
    const response = await fetch(`${base}${path}`);
    assert.equal(response.status, 404);
    assert.match(response.headers.get('content-type'), /application\/json/);
    assert.equal((await response.json()).success, false);
  });
}
