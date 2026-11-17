'use strict';

const express = require('express');
const crypto = require('node:crypto');

const SERVICE_NAME = 'express-tournament-consumer';

/**
 * Doc cau hinh tu bien moi truong (TH2 - Muc 1: khong hard-code URL cua Provider).
 */
function loadConfig(env = process.env) {
  const positive = (value, fallback) => {
    const n = Number(value);
    return Number.isFinite(n) && n > 0 ? n : fallback;
  };
  return {
    port: positive(env.PORT, 8095),
    providerBaseUrl: String(env.TOURNAMENT_PROVIDER_URL || 'http://localhost:8080').replace(/\/+$/, ''),
    timeoutMs: positive(env.PROVIDER_TIMEOUT_MS, 3000),
    maxAttempts: Math.min(positive(env.PROVIDER_MAX_ATTEMPTS, 3), 3),
    retryDelayMs: positive(env.PROVIDER_RETRY_DELAY_MS, 200)
  };
}

/** Loi co kiem soat khi goi Provider; status la HTTP status tra ve cho client. */
class ProviderError extends Error {
  constructor(status, error, message, extra = {}) {
    super(message);
    this.status = status;
    this.error = error;
    this.extra = extra;
  }
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function isTimeout(err) {
  return Boolean(err) && (err.name === 'TimeoutError' || err.name === 'AbortError');
}

/**
 * Goi GET toi Provider, tra ve { body, instance, attempts }.
 * - 404 tu Provider            -> ProviderError 404 (khong retry)
 * - Khong ket noi duoc / 502/503 -> retry toi da maxAttempts lan, sau do 503 (fallback)
 * - Qua timeout                -> ProviderError 504 (khong retry de client khong cho qua lau)
 */
async function getFromProvider(config, path, requestId, log) {
  const url = `${config.providerBaseUrl}${path}`;
  let lastReason = '';
  for (let attempt = 1; attempt <= config.maxAttempts; attempt++) {
    let response;
    try {
      response = await fetch(url, {
        method: 'GET',
        headers: { Accept: 'application/json', 'X-Request-Id': requestId },
        signal: AbortSignal.timeout(config.timeoutMs)
      });
    } catch (err) {
      if (isTimeout(err)) {
        log('WARN', requestId, `Provider timeout sau ${config.timeoutMs}ms: GET ${path}`);
        throw new ProviderError(504, 'Gateway Timeout',
          `Tournament service khong phan hoi trong ${config.timeoutMs}ms`, { attempts: attempt });
      }
      lastReason = (err.cause && err.cause.code) || err.message;
      log('WARN', requestId, `Lan thu ${attempt}/${config.maxAttempts} that bai: GET ${path} (${lastReason})`);
      if (attempt < config.maxAttempts) {
        await sleep(config.retryDelayMs);
      }
      continue;
    }

    if (response.status === 404) {
      throw new ProviderError(404, 'Not Found', `Khong tim thay tai nguyen tai Provider: ${path}`, { attempts: attempt });
    }
    if (response.status === 502 || response.status === 503) {
      lastReason = `HTTP ${response.status}`;
      log('WARN', requestId, `Lan thu ${attempt}/${config.maxAttempts}: Provider tra ${response.status} cho GET ${path}`);
      if (attempt < config.maxAttempts) {
        await sleep(config.retryDelayMs);
      }
      continue;
    }
    if (!response.ok) {
      throw new ProviderError(502, 'Bad Gateway',
        `Provider tra ve HTTP ${response.status} khong mong doi cho ${path}`, { attempts: attempt });
    }

    let body;
    try {
      body = await response.json();
    } catch (err) {
      throw new ProviderError(502, 'Bad Gateway', `Provider tra ve JSON khong hop le cho ${path}`, { attempts: attempt });
    }
    return {
      body,
      instance: response.headers.get('x-instance-id'),
      attempts: attempt
    };
  }

  log('ERROR', requestId, `Provider khong kha dung sau ${config.maxAttempts} lan thu: ${lastReason}`);
  throw new ProviderError(503, 'Service Unavailable', 'Tournament service is currently unavailable', {
    code: 'TEMPORARILY_UNAVAILABLE',
    attempts: config.maxAttempts
  });
}

function defaultLogger(level, requestId, message) {
  const line = `${new Date().toISOString()} ${level} [${SERVICE_NAME}] [${requestId}] ${message}`;
  if (level === 'ERROR' || level === 'WARN') {
    console.error(line);
  } else {
    console.log(line);
  }
}

function createApp(config = loadConfig(), log = defaultLogger) {
  const app = express();

  // Ma theo doi request, dong bo voi X-Request-Id cua he thong Spring Boot
  app.use((req, res, next) => {
    const incoming = req.get('X-Request-Id');
    req.requestId = incoming && /^[A-Za-z0-9._-]{1,64}$/.test(incoming) ? incoming : crypto.randomUUID();
    res.set('X-Request-Id', req.requestId);
    next();
  });

  const sendError = (req, res, status, error, message, extra = {}) => {
    res.status(status).json({
      timestamp: new Date().toISOString(),
      status,
      error,
      message,
      path: req.originalUrl,
      ...extra
    });
  };

  app.get('/health', (req, res) => {
    res.json({ status: 'UP' });
  });

  app.get('/api/v1/hello', (req, res) => {
    res.json({ message: 'Welcome to Express Tournament Consumer', framework: 'Node.js + Express' });
  });

  app.get('/api/v1/info', (req, res) => {
    res.json({
      service: SERVICE_NAME,
      framework: 'Node.js + Express',
      version: '1.0',
      status: 'UP',
      provider: config.providerBaseUrl,
      timeoutMs: config.timeoutMs,
      maxAttempts: config.maxAttempts
    });
  });

  // Consumer: tong hop thong tin giai dau + noi dung thi dau tu tournament-service (Spring Boot)
  app.get('/api/v1/tournament-summaries/:id', async (req, res) => {
    const id = req.params.id;
    if (!/^[1-9][0-9]{0,17}$/.test(id)) {
      return sendError(req, res, 400, 'Bad Request', 'Ma giai dau phai la so nguyen duong');
    }
    try {
      const tournament = await getFromProvider(config, `/api/v1/tournaments/${id}`, req.requestId, log);
      const competitions = await getFromProvider(config, `/api/v1/competitions?tournamentId=${id}`, req.requestId, log);
      const list = Array.isArray(competitions.body) ? competitions.body : [];
      log('INFO', req.requestId, `Tong hop giai dau #${id}: ${list.length} noi dung, instance=${tournament.instance || 'n/a'}`);
      return res.json({
        tournamentId: tournament.body.id,
        name: tournament.body.name,
        status: tournament.body.status,
        location: tournament.body.location,
        startDate: tournament.body.startDate,
        endDate: tournament.body.endDate,
        competitionCount: list.length,
        competitions: list.map((c) => ({ id: c.id, name: c.name, status: c.status })),
        handledByProviderInstance: tournament.instance || null,
        consumer: SERVICE_NAME,
        attempts: tournament.attempts + competitions.attempts - 1
      });
    } catch (err) {
      if (err instanceof ProviderError) {
        return sendError(req, res, err.status, err.error, err.message, err.extra);
      }
      log('ERROR', req.requestId, `Loi khong mong doi: ${err.message}`);
      return sendError(req, res, 500, 'Internal Server Error', 'Loi he thong khi tong hop du lieu giai dau');
    }
  });

  app.use((req, res) => {
    sendError(req, res, 404, 'Not Found', 'Khong ton tai endpoint');
  });

  return app;
}

module.exports = { createApp, loadConfig, getFromProvider, ProviderError };
