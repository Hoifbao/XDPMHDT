import { writeFile } from 'node:fs/promises';
import { performance } from 'node:perf_hooks';

const args = Object.fromEntries(process.argv.slice(2).reduce((pairs, value, i, all) => {
  if (value.startsWith('--')) pairs.push([value.slice(2), all[i + 1]]);
  return pairs;
}, []));
const url = args.url ?? 'http://localhost:18083/api/artifacts/1?lang=vi';
const users = Number(args.users ?? 20);
const seconds = Number(args.seconds ?? 10);
if (!Number.isInteger(users) || users < 1 || users > 500 || seconds < 1 || seconds > 120) {
  throw Error('Use 1..500 users and 1..120 seconds');
}
const warm = await fetch(url, { signal: AbortSignal.timeout(10000) });
if (warm.status !== 200) throw Error(`Warmup failed: HTTP ${warm.status}`);
await warm.arrayBuffer();
const times = [];
let errors = 0;
let pendingMax = 0;
const polls = [];
const metricUrl = new URL('/actuator/prometheus', url);
const timer = setInterval(() => polls.push((async () => {
  try {
    const text = await (await fetch(metricUrl, { signal: AbortSignal.timeout(2000) })).text();
    for (const match of text.matchAll(/^hikaricp_connections_pending(?:\{[^\n]*\})? ([\d.]+)/gm)) {
      pendingMax = Math.max(pendingMax, Number(match[1]));
    }
  } catch {}
})()), 500);
const start = performance.now();
const deadline = start + seconds * 1000;
try {
  await Promise.all(Array.from({ length: users }, async () => {
    while (performance.now() < deadline) {
      const sent = performance.now();
      try {
        const response = await fetch(url, { signal: AbortSignal.timeout(10000) });
        await response.arrayBuffer();
        if (response.status !== 200) errors++;
      } catch { errors++; }
      times.push(performance.now() - sent);
    }
  }));
} finally { clearInterval(timer); }
await Promise.allSettled(polls);
times.sort((a, b) => a - b);
const percentile = p => Math.round((times[Math.min(times.length - 1, Math.ceil(times.length * p) - 1)] ?? 0) * 100) / 100;
const elapsed = performance.now() - start;
const result = {
  note: 'Local isolated content endpoint probe; does not replace the team 9-minute mixed-service k6 acceptance.',
  url, users, requestedSeconds: seconds, requests: times.length, errors,
  elapsedSeconds: Math.round(elapsed / 10) / 100,
  requestsPerSecond: Math.round(times.length / elapsed * 100000) / 100,
  p50Ms: percentile(.5), p95Ms: percentile(.95), p99Ms: percentile(.99),
  sampledHikariPendingMax: pendingMax, measuredAt: new Date().toISOString(),
};
if (args.output) await writeFile(args.output, JSON.stringify(result, null, 2) + '\n');
console.log(JSON.stringify(result, null, 2));
process.exitCode = errors ? 1 : 0;
