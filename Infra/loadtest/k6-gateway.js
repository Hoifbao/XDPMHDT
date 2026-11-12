// Load test k6 qua API Gateway (Bảo - Sprint 6, XDPMHDT-41).
// Mô phỏng du khách dùng app: xem chi tiết hiện vật, tìm di tích gần, tải registry beacon, đăng nhập.
// Tăng dần tới 500 người dùng ảo (VU) để đối chiếu yêu cầu: p95 <= 2 giây với 500 người dùng đồng thời.
//
// Chạy bằng Docker (ở thư mục gốc repo, PowerShell):
//   Thử nhanh 30 giây, 5 VU (nên chạy trước để kiểm tra dữ liệu và đường dẫn):
//     docker run --rm -i -e PROFILE=smoke -v ${PWD}/Infra/loadtest:/scripts grafana/k6:0.54.0 run /scripts/k6-gateway.js
//   Chạy đủ (khoảng 9 phút, tới 500 VU):
//     docker run --rm -i -v ${PWD}/Infra/loadtest:/scripts grafana/k6:0.54.0 run /scripts/k6-gateway.js
//
// Biến môi trường (thêm -e TEN=giatri vào lệnh docker run):
//   BASE_URL      mặc định http://host.docker.internal:8080 (Gateway trên máy, nhìn từ trong Docker)
//   PROFILE       full (mặc định) | smoke
//   MAX_VUS       số VU tối đa, mặc định 500 (máy yếu thì hạ xuống 200, ghi rõ trong báo cáo)
//   ARTIFACT_IDS  danh sách id hiện vật, mặc định 1001-1025 (dữ liệu Seed_SmartHeritage_v1.sql). Ví dụ: 1,2,3
//   LOGIN_USER / LOGIN_PASS  tài khoản đăng nhập, mặc định admin / Test@1234 (giống traffic-sim.ps1)
//
// Chỉ gọi API đọc và đăng nhập: không tạo, sửa, xoá dữ liệu.

import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = (__ENV.BASE_URL || 'http://host.docker.internal:8080').replace(/\/$/, '');
const PROFILE = (__ENV.PROFILE || 'full').toLowerCase();
const MAX_VUS = parseInt(__ENV.MAX_VUS || '500', 10);
const LOGIN_USER = __ENV.LOGIN_USER || 'admin';
const LOGIN_PASS = __ENV.LOGIN_PASS || 'Test@1234';

// "1001-1025" hoặc "1,2,3" -> mảng số
function parseIds(raw) {
  const ids = [];
  for (const part of raw.split(',')) {
    const p = part.trim();
    if (!p) continue;
    const m = p.match(/^(\d+)-(\d+)$/);
    if (m) {
      for (let i = parseInt(m[1], 10); i <= parseInt(m[2], 10); i++) ids.push(i);
    } else {
      ids.push(parseInt(p, 10));
    }
  }
  return ids;
}
const ARTIFACT_IDS = parseIds(__ENV.ARTIFACT_IDS || '1001-1025');

// Toạ độ các di tích trong dữ liệu seed (site_db.heritage_sites), lệch nhẹ ngẫu nhiên khi gọi nearby
const SITE_POINTS = [
  [16.4698, 107.5786], // Đại Nội Huế
  [15.8771, 108.3262], // Phố cổ Hội An
  [10.788, 106.705],   // Bảo tàng Lịch sử TP. HCM
  [21.0404, 105.7985], // Bảo tàng Dân tộc học
  [12.2654, 109.1953], // Tháp Bà Ponagar
  [20.2856, 105.9065], // Cố đô Hoa Lư
];

const stagesFull = [
  { duration: '1m', target: Math.round(MAX_VUS * 0.1) }, // khởi động
  { duration: '2m', target: Math.round(MAX_VUS * 0.4) },
  { duration: '2m', target: MAX_VUS },                   // tăng tới tối đa
  { duration: '3m', target: MAX_VUS },                   // giữ tải tối đa: đọc p95 ở đoạn này
  { duration: '1m', target: 0 },                         // hạ tải
];
const stagesSmoke = [
  { duration: '10s', target: 5 },
  { duration: '20s', target: 5 },
];

export const options = {
  scenarios: {
    du_khach: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: PROFILE === 'smoke' ? stagesSmoke : stagesFull,
      gracefulRampDown: '30s',
    },
  },
  // Ngưỡng đạt/không đạt (in ✓/✗ cuối bài chạy). Theo yêu cầu: p95 <= 2 giây; tỉ lệ lỗi < 1%.
  // Có ngưỡng theo từng nhóm API để k6 in p95 riêng từng nhóm.
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:artifact_detail}': ['p(95)<2000'],
    'http_req_duration{endpoint:nearby}': ['p(95)<2000'],
    'http_req_duration{endpoint:registry}': ['p(95)<2000'],
    'http_req_duration{endpoint:login}': ['p(95)<2000'],
    checks: ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function artifactDetail() {
  const id = pick(ARTIFACT_IDS);
  // name gom mọi id vào 1 dòng thống kê, tránh mỗi id 1 dòng
  const res = http.get(`${BASE_URL}/api/artifacts/${id}`, {
    tags: { endpoint: 'artifact_detail', name: 'GET /api/artifacts/{id}' },
  });
  check(res, { 'chi tiết hiện vật 200': (r) => r.status === 200 });
}

function nearby() {
  const [lat, lng] = pick(SITE_POINTS);
  const dLat = (Math.random() - 0.5) * 0.02; // lệch khoảng 1 km
  const dLng = (Math.random() - 0.5) * 0.02;
  const url = `${BASE_URL}/api/heritage-sites/nearby?lat=${(lat + dLat).toFixed(5)}&lng=${(lng + dLng).toFixed(5)}&radius=5`;
  const res = http.get(url, { tags: { endpoint: 'nearby', name: 'GET /api/heritage-sites/nearby' } });
  check(res, { 'di tích gần 200': (r) => r.status === 200 });
}

function registry() {
  const res = http.get(`${BASE_URL}/api/ibeacons/registry`, {
    tags: { endpoint: 'registry', name: 'GET /api/ibeacons/registry' },
  });
  check(res, { 'registry beacon 200': (r) => r.status === 200 });
}

function login() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ username: LOGIN_USER, password: LOGIN_PASS }),
    { headers: { 'Content-Type': 'application/json' }, tags: { endpoint: 'login', name: 'POST /api/auth/login' } },
  );
  check(res, { 'đăng nhập 200': (r) => r.status === 200 });
}

// Kiểm tra trước khi chạy: Gateway sống và 4 API trả 200. Sai thì dừng luôn, không chạy tải vô ích.
export function setup() {
  const probes = [
    ['GET', `/api/artifacts/${ARTIFACT_IDS[0]}`, null],
    ['GET', `/api/heritage-sites/nearby?lat=${SITE_POINTS[0][0]}&lng=${SITE_POINTS[0][1]}&radius=5`, null],
    ['GET', '/api/ibeacons/registry', null],
    ['POST', '/api/auth/login', JSON.stringify({ username: LOGIN_USER, password: LOGIN_PASS })],
  ];
  for (const [method, path, body] of probes) {
    const res = http.request(method, `${BASE_URL}${path}`, body, {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'setup' },
    });
    if (res.status !== 200) {
      throw new Error(`Kiểm tra trước thất bại: ${method} ${path} trả ${res.status}. `
        + 'Xem service tương ứng đã chạy chưa, ARTIFACT_IDS có đúng dữ liệu không, tài khoản đăng nhập có đúng không.');
    }
  }
  console.log(`OK: ${BASE_URL}, profile=${PROFILE}, max VU=${PROFILE === 'smoke' ? 5 : MAX_VUS}, ${ARTIFACT_IDS.length} hiện vật`);
}

// Mỗi vòng = 1 thao tác của du khách, tỉ lệ: 50% chi tiết hiện vật, 25% di tích gần,
// 20% registry beacon, 5% đăng nhập. Nghỉ 1-3 giây giữa 2 thao tác như người dùng thật.
export default function () {
  const r = Math.random() * 100;
  if (r < 50) artifactDetail();
  else if (r < 75) nearby();
  else if (r < 95) registry();
  else login();
  sleep(1 + Math.random() * 2);
}
