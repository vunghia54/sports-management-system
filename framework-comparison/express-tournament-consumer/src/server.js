'use strict';

const { createApp, loadConfig } = require('./app');

const startedAt = process.hrtime.bigint();
const config = loadConfig();
const app = createApp(config);

app.listen(config.port, () => {
  // Tinh tu luc tien trinh Node khoi dong (giong "Started ... in" cua Spring tinh tu luc JVM khoi dong)
  const ms = process.uptime() * 1000;
  const appMs = Number(process.hrtime.bigint() - startedAt) / 1e6;
  console.log(
    `${new Date().toISOString()} INFO [express-tournament-consumer] Started on port ${config.port} in ${ms.toFixed(0)} ms (app init ${appMs.toFixed(0)} ms) ` +
    `(provider=${config.providerBaseUrl}, timeout=${config.timeoutMs}ms, maxAttempts=${config.maxAttempts})`
  );
});
