'use strict';

// Provider gia lap phan hoi cham, dung de minh hoa timeout (TH2 - Bai 4.2) ma khong can sua service that.
// Chay: node scripts/slow-provider.js  (cong 8099, tre 10 giay)
const http = require('node:http');

const port = Number(process.env.SLOW_PORT || 8099);
const delayMs = Number(process.env.SLOW_DELAY_MS || 10000);

http.createServer((req, res) => {
  setTimeout(() => {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ id: 1, name: 'Slow tournament', status: 'UPCOMING' }));
  }, delayMs);
}).listen(port, () => {
  console.log(`Slow provider listening on ${port}, delay=${delayMs}ms`);
});
