// =====================================================================
// Dashboard thong ke – tuan 3: lay so lieu that tu GET /api/statistics/overview
// (Backend: StatisticsController). Bo loc va xuat bao cao van chua lam.
// =====================================================================
const API_BASE = (function () {
    try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
    catch (e) { return 'http://localhost:8080'; }
})();

async function loadOverview() {
    const response = await fetch(API_BASE + '/api/statistics/overview?top=5');
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

function renderFilters() {
    const select = document.getElementById('site-filter');
    select.append(new Option('Tất cả di tích', ''));
}

function renderKpis(data) {
    const box = document.getElementById('kpis');
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
    document.querySelectorAll('.chart-box').forEach(b => b.replaceChildren(el('p', message, 'muted')));
}

async function init() {
    const note = document.getElementById('status-note');
    renderFilters();
    let data;
    try {
        data = await loadOverview();
    } catch (e) {
        const message = e instanceof TypeError
            ? 'Không gọi được ' + API_BASE + '. Hãy chạy Backend rồi tải lại trang.'
            : 'Không tải được số liệu: ' + e.message;
        note.textContent = message;
        showInChartBoxes(message);
        return;
    }
    note.textContent = 'Số liệu lấy từ API thống kê. Bộ lọc và xuất báo cáo sẽ hoạt động ở tuần sau.';
    renderKpis(data);
    renderFeedbacks(data.recentFeedbacks);
    if (data.totalViews === 0) {
        showInChartBoxes('Chưa có lượt tương tác nào.');
    } else if (window.Chart) {
        renderCharts(data);
    } else {
        // CDN khong tai duoc (vd mo khi khong co mang): trang van hien KPI va bang.
        showInChartBoxes('Không tải được Chart.js (cần Internet).');
    }
}

init();
