'use strict';

const { createApp, loadConfig } = require('./app');

const startedAt = process.hrtime.bigint();
const config = loadConfig();
const app = createApp(config);

app.listen(config.port, () => {
  const ms = Number(process.hrtime.bigint() - startedAt) / 1e6;
  console.log(
    `${new Date().toISOString()} INFO [express-tournament-consumer] Started on port ${config.port} in ${ms.toFixed(0)} ms ` +
    `(provider=${config.providerBaseUrl}, timeout=${config.timeoutMs}ms, maxAttempts=${config.maxAttempts})`
  );
});
