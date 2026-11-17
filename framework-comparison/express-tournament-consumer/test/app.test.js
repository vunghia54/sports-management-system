'use strict';

const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { createApp } = require('../src/app');

let provider;
let providerUrl;
let mode = 'ok';

const quietLog = () => {};

function listen(server) {
  return new Promise((resolve) => server.listen(0, '127.0.0.1', () => resolve(server.address().port)));
}

async function startConsumer(overrides = {}) {
  const config = {
    port: 0,
    providerBaseUrl: providerUrl,
    timeoutMs: 300,
    maxAttempts: 3,
    retryDelayMs: 10,
    ...overrides
  };
  const server = http.createServer(createApp(config, quietLog));
  const port = await listen(server);
  return { server, base: `http://127.0.0.1:${port}` };
}

before(async () => {
  provider = http.createServer((req, res) => {
    const send = (status, body, headers = {}) => {
      res.writeHead(status, { 'Content-Type': 'application/json', ...headers });
      res.end(JSON.stringify(body));
    };
    if (mode === 'slow') {
      setTimeout(() => send(200, { id: 1 }), 1000);
      return;
    }
    if (mode === 'unavailable') {
      send(503, { status: 503, message: 'down' });
      return;
    }
    if (req.url === '/api/v1/tournaments/1') {
      send(200, { id: 1, name: 'Giai Bong Da TH2', status: 'UPCOMING', location: 'Ha Noi',
        startDate: '2026-12-01', endDate: '2026-12-10' }, { 'X-Instance-Id': 'tournament-service:8081' });
      return;
    }
    if (req.url === '/api/v1/competitions?tournamentId=1') {
      send(200, [{ id: 10, name: 'Bong da nam', status: 'OPEN_REGISTRATION', tournamentId: 1 }]);
      return;
    }
    send(404, { status: 404, message: 'Tournament not found' });
  });
  providerUrl = `http://127.0.0.1:${await listen(provider)}`;
});

after(() => provider.close());

test('200: tong hop giai dau tu Provider, co instance va so noi dung', async () => {
  mode = 'ok';
  const { server, base } = await startConsumer();
  try {
    const res = await fetch(`${base}/api/v1/tournament-summaries/1`);
    assert.equal(res.status, 200);
    const body = await res.json();
    assert.equal(body.tournamentId, 1);
    assert.equal(body.competitionCount, 1);
    assert.equal(body.handledByProviderInstance, 'tournament-service:8081');
    assert.ok(res.headers.get('x-request-id'));
  } finally {
    server.close();
  }
});

test('400: ma giai dau khong hop le', async () => {
  const { server, base } = await startConsumer();
  try {
    const res = await fetch(`${base}/api/v1/tournament-summaries/abc`);
    assert.equal(res.status, 400);
    const body = await res.json();
    assert.equal(body.path, '/api/v1/tournament-summaries/abc');
  } finally {
    server.close();
  }
});

test('404: Provider khong tim thay giai dau', async () => {
  mode = 'ok';
  const { server, base } = await startConsumer();
  try {
    const res = await fetch(`${base}/api/v1/tournament-summaries/999999`);
    assert.equal(res.status, 404);
    assert.equal((await res.json()).attempts, 1);
  } finally {
    server.close();
  }
});

test('503: Provider tat (connection refused) -> retry 3 lan roi tra fallback', async () => {
  const closed = http.createServer();
  const deadPort = await listen(closed);
  await new Promise((resolve) => closed.close(resolve));
  const { server, base } = await startConsumer({ providerBaseUrl: `http://127.0.0.1:${deadPort}` });
  try {
    const res = await fetch(`${base}/api/v1/tournament-summaries/1`);
    assert.equal(res.status, 503);
    const body = await res.json();
    assert.equal(body.code, 'TEMPORARILY_UNAVAILABLE');
    assert.equal(body.attempts, 3);
  } finally {
    server.close();
  }
});

test('503: Provider tra 503 lien tuc -> retry co gioi han', async () => {
  mode = 'unavailable';
  const { server, base } = await startConsumer();
  try {
    const res = await fetch(`${base}/api/v1/tournament-summaries/1`);
    assert.equal(res.status, 503);
    assert.equal((await res.json()).attempts, 3);
  } finally {
    server.close();
  }
});

test('504: Provider cham hon timeout -> tra 504 trong thoi gian gioi han', async () => {
  mode = 'slow';
  const { server, base } = await startConsumer({ timeoutMs: 200 });
  try {
    const started = Date.now();
    const res = await fetch(`${base}/api/v1/tournament-summaries/1`);
    const elapsed = Date.now() - started;
    assert.equal(res.status, 504);
    assert.ok(elapsed < 900, `phai tra loi som, mat ${elapsed}ms`);
  } finally {
    server.close();
    mode = 'ok';
  }
});
