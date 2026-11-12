// =====================================================================
// Dashboard thong ke: lay so lieu that tu GET /api/statistics/overview qua Gateway 8080
// (tuan 5: analytics-service 8087, doc analytics_db do NiFi dong bo).
// Loc theo di tich va khoang ngay (tuan 5). Tuan 6: hang "Hom nay" lay GET /api/statistics/realtime
// (so lieu tu su kien RabbitMQ) va tu tai lai moi 15 giay.
// =====================================================================
const API_BASE = (function () {
    try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
    catch (e) { return 'http://localhost:8080'; }
})();

// Canvas goc cua 2 bieu do: giu lai de ve lai sau khi o bieu do da bi thay bang thong bao
const CHART_BOXES = Array.from(document.querySelectorAll('.chart-box'))
    .map(box => ({ box, canvas: box.querySelector('canvas') }));

// Bo trong o nao thi khong gui tham so do (Backend hieu la khong loc)
function readFilters() {
    const params = new URLSearchParams({ top: '5' });
    const values = {
        siteId: document.getElementById('site-filter').value,
        from: document.getElementById('from-filter').value,
        to: document.getElementById('to-filter').value
    };
    Object.entries(values).forEach(([key, value]) => { if (value) params.set(key, value); });
    return params;
}

async function loadOverview(params) {
    const response = await fetch(API_BASE + '/api/statistics/overview?' + params);
    const body = await response.json().catch(() => null);
    if (!response.ok || !body || !body.success) {
        throw new Error((body && body.message) || ('Máy chủ trả lỗi HTTP ' + response.status));
    }
    return body.data;
}

// 185.5 giay -> "3:06"
function formatDuration(seconds) {
    if (seconds === null || seconds === undefined) return '—';
    const total = Math.round(seconds);
    return Math.floor(total / 60) + ':' + String(total % 60).padStart(2, '0');
}

const STATUS_LABEL = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' };

// Dung textContent (khong dung innerHTML) vi sau nay du lieu den tu nguoi dung qua API.
function el(tag, text, className) {
    const node = document.createElement(tag);
    if (text !== undefined) node.textContent = text;
    if (className) node.className = className;
    return node;
}

// Danh sach di tich lay tu GET /api/heritage-sites (cong khai). Loi thi van loc duoc theo ngay.
async function renderFilters() {
    const select = document.getElementById('site-filter');
    select.append(new Option('Tất cả di tích', ''));
    try {
        const response = await fetch(API_BASE + '/api/heritage-sites');
        const body = await response.json();
        if (!response.ok || !body.success) throw new Error(body.message);
        body.data.forEach(site => select.append(new Option(site.name, site.id)));
    } catch (e) {
        select.title = 'Không tải được danh sách di tích';
    }
    ['site-filter', 'from-filter', 'to-filter']
        .forEach(id => document.getElementById(id).addEventListener('change', refresh));
    // So lieu hom nay chi loc theo di tich, khong theo khoang ngay
    document.getElementById('site-filter').addEventListener('change', refreshRealtime);
}

function renderKpis(data) {
    const box = document.getElementById('kpis');
    box.replaceChildren();
    const kpis = [
        { label: 'Tổng khách', value: data.totalVisitors },
        { label: 'Lượt tương tác', value: data.totalViews },
        // Chi tinh feedback da duyet (APPROVED), con "So phan hoi" dem moi trang thai
        { label: 'Điểm đánh giá TB (đã duyệt)', value: data.averageRatingApproved ?? '—' },
        { label: 'Số phản hồi', value: data.totalFeedbackCount },
        { label: 'Thời gian dừng TB', value: formatDuration(data.averageDurationSeconds) }
    ];
    kpis.forEach(k => {
        const card = el('div', undefined, 'card stat');
        card.append(el('strong', k.value.toLocaleString('vi-VN')), el('span', k.label, 'muted'));
        box.append(card);
    });
}

function renderFeedbacks(feedbacks) {
    const body = document.getElementById('feedback-rows');
    body.replaceChildren();
    if (feedbacks.length === 0) {
        const td = el('td', 'Chưa có phản hồi.', 'muted');
        td.colSpan = 5;
        const tr = document.createElement('tr');
        tr.append(td);
        body.append(tr);
    }
    feedbacks.forEach(f => {
        const tr = document.createElement('tr');
        tr.append(
            // "2026-10-07T11:00:00" -> "2026-10-07 11:00"
            el('td', f.createdAt ? f.createdAt.replace('T', ' ').slice(0, 16) : '—'),
            el('td', f.artifactName),
            // Kem "x/5" de trinh doc man hinh doc duoc diem, khong chi doc ky tu sao
            el('td', '★'.repeat(f.rating) + '☆'.repeat(5 - f.rating) + ' ' + f.rating + '/5'),
            el('td', f.comment ?? '—', f.comment ? '' : 'muted')
        );
        const status = el('td');
        status.append(el('span', STATUS_LABEL[f.status] ?? f.status, 'status ' + f.status));
        tr.append(status);
        body.append(tr);
    });
    const count = document.getElementById('feedback-count');
    count.textContent = feedbacks.length + ' mới nhất';
    count.style.display = feedbacks.length === 0 ? 'none' : '';
}

function renderCharts(data) {
    // Lay mau tu bien CSS cua theme.css de bieu do cung tong mau voi trang
    const css = getComputedStyle(document.documentElement);
    const sage = css.getPropertyValue('--sage').trim();
    const gold = css.getPropertyValue('--gold').trim();
    const line = css.getPropertyValue('--line').trim();
    const muted = css.getPropertyValue('--muted').trim();

    Chart.defaults.font.family = getComputedStyle(document.body).fontFamily;
    Chart.defaults.color = muted;
    const grid = { color: line };

    new Chart(document.getElementById('chart-artifacts'), {
        type: 'bar',
        data: {
            labels: data.topArtifacts.map(a => a.artifactName),
            datasets: [{
                label: 'Lượt tương tác',
                data: data.topArtifacts.map(a => a.views),
                backgroundColor: data.topArtifacts.map(a => (a.siteId === 1 ? sage : gold)),
                borderRadius: 10,
                maxBarThickness: 64
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        afterLabel: ctx => data.topArtifacts[ctx.dataIndex].siteName
                    }
                }
            },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, grid, ticks: { precision: 0 } } }
        }
    });

    new Chart(document.getElementById('chart-daily'), {
        type: 'line',
        data: {
            labels: data.dailyViews.map(d => d.date.slice(8, 10) + '/' + d.date.slice(5, 7)),
            datasets: [{
                label: 'Lượt tương tác',
                data: data.dailyViews.map(d => d.views),
                borderColor: sage,
                backgroundColor: sage + '22',
                fill: true,
                tension: 0.35,
                cubicInterpolationMode: 'monotone',
                pointRadius: 4,
                pointBackgroundColor: sage
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, grid, ticks: { precision: 0 } } }
        }
    });
}

function showInChartBoxes(message) {
    clearCharts();
    CHART_BOXES.forEach(({ box }) => box.replaceChildren(el('p', message, 'muted')));
}

// Huy bieu do cu va dat lai canvas truoc moi lan ve (doi bo loc la ve lai)
function clearCharts() {
    CHART_BOXES.forEach(({ box, canvas }) => {
        if (window.Chart) Chart.getChart(canvas)?.destroy();
        box.replaceChildren(canvas);
    });
}

// Doi bo loc nhanh lien tuc: chi ve ket qua cua lan goi moi nhat
let latestRequest = 0;

async function refresh() {
    const note = document.getElementById('status-note');
    const params = readFilters();
    const requestId = ++latestRequest;
    note.textContent = 'Đang tải số liệu…';
    let data;
    try {
        data = await loadOverview(params);
    } catch (e) {
        if (requestId !== latestRequest) return;
        const message = e instanceof TypeError
            ? 'Không gọi được ' + API_BASE + '. Hãy chạy Backend rồi tải lại trang.'
            : 'Không tải được số liệu: ' + e.message;
        note.textContent = message;
        showInChartBoxes(message);
        return;
    }
    if (requestId !== latestRequest) return;
    const filtered = params.has('siteId') || params.has('from') || params.has('to');
    note.textContent = filtered
        ? 'Số liệu theo bộ lọc đang chọn. Xuất báo cáo sẽ hoạt động ở tuần sau.'
        : 'Số liệu toàn bộ (chưa lọc). Xuất báo cáo sẽ hoạt động ở tuần sau.';
    document.getElementById('daily-pill').textContent = (params.has('from') || params.has('to'))
        ? 'Tối đa 31 ngày có dữ liệu trong khoảng'
        : 'Tối đa 7 ngày có dữ liệu';
    renderKpis(data);
    renderFeedbacks(data.recentFeedbacks);
    if (data.totalViews === 0) {
        showInChartBoxes(filtered ? 'Không có lượt tương tác nào theo bộ lọc này.' : 'Chưa có lượt tương tác nào.');
    } else if (window.Chart) {
        clearCharts();
        renderCharts(data);
    } else {
        // CDN khong tai duoc (vd mo khi khong co mang): trang van hien KPI va bang.
        showInChartBoxes('Không tải được Chart.js (cần Internet).');
    }
}

// ---------- Tuan 6: so lieu hom nay (thoi gian thuc) ----------
const REALTIME_INTERVAL_MS = 15000;

function renderRealtime(data) {
    const box = document.getElementById('realtime-kpis');
    box.replaceChildren();
    const top = data.topArtifacts[0];
    // dim_artifact chua dong bo (NiFi chua chay) thi chua co ten, hien theo id
    const topName = top && (top.artifactName ?? ('Hiện vật #' + top.artifactId));
    const kpis = [
        { label: 'Lượt tương tác hôm nay', value: data.views.toLocaleString('vi-VN') },
        { label: 'Phản hồi mới hôm nay', value: data.feedbacks.toLocaleString('vi-VN') },
        { label: 'Điểm TB phản hồi hôm nay', value: data.averageRating ?? '—' },
        { label: 'Hiện vật nổi bật hôm nay', value: top ? topName + ' (' + top.views + ')' : '—' }
    ];
    kpis.forEach(k => {
        const card = el('div', undefined, 'card stat');
        card.append(el('strong', String(k.value)), el('span', k.label, 'muted'));
        box.append(card);
    });
}

async function refreshRealtime() {
    const pill = document.getElementById('realtime-pill');
    const params = new URLSearchParams();
    const siteId = document.getElementById('site-filter').value;
    if (siteId) params.set('siteId', siteId);
    try {
        const response = await fetch(API_BASE + '/api/statistics/realtime?' + params);
        const body = await response.json().catch(() => null);
        if (!response.ok || !body || !body.success) {
            throw new Error((body && body.message) || ('HTTP ' + response.status));
        }
        renderRealtime(body.data);
        pill.textContent = 'Cập nhật lúc ' + new Date().toLocaleTimeString('vi-VN');
    } catch (e) {
        // Giu so lieu lan truoc, chi bao loi o nhan
        pill.textContent = 'Không cập nhật được: ' + e.message;
    }
}

async function init() {
    await renderFilters();
    refresh();
    refreshRealtime();
    setInterval(refreshRealtime, REALTIME_INTERVAL_MS);
}

init();
