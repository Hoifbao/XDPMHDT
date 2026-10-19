// =====================================================================
// DU LIEU GIA – tuan 2, se thay bang API thong ke.
// Ten di tich / hien vat / ma hien vat / trang thai lay theo du lieu mau
// trong Infra/init_db.sql (nhanh Feedback_Rating). So luot da duoc phong to
// cho bieu do de nhin, khong phai so lieu that.
// =====================================================================
const DATA = {
    // heritage_sites (id, name)
    sites: [
        { id: 1, name: 'Văn Miếu - Quốc Tử Giám' },
        { id: 2, name: 'Hoàng thành Thăng Long' }
    ],

    kpis: [
        // users co role = 'VISITOR' (dem DISTINCT visits.user_id neu chi tinh nguoi da ghe)
        { label: 'Tổng khách', value: 1248 },
        // COUNT(*) bang visits
        { label: 'Lượt tương tác', value: 3560 },
        // AVG(feedbacks.rating), chi tinh status = 'APPROVED'
        { label: 'Điểm đánh giá TB', value: 4.2 },
        // COUNT(*) bang feedbacks
        { label: 'Số phản hồi', value: 312 },
        // AVG(visits.duration_seconds), hien thi theo phut:giay
        { label: 'Thời gian dừng TB', value: '3:05' }
    ],

    // visits GROUP BY artifact_id; ten lay tu artifact_translations.name (language_code = 'vi')
    topArtifacts: [
        { code: 'KVC-01', name: 'Khuê Văn Các', siteId: 1, visits: 1640 },
        { code: 'BTS-01', name: 'Bia Tiến sĩ', siteId: 1, visits: 1150 },
        { code: 'DM-01', name: 'Đoan Môn', siteId: 2, visits: 770 }
    ],

    // visits GROUP BY DATE(detected_at)
    daily: [
        { date: '2026-10-05', visits: 420 },
        { date: '2026-10-06', visits: 465 },
        { date: '2026-10-07', visits: 510 },
        { date: '2026-10-08', visits: 380 },
        { date: '2026-10-09', visits: 455 },
        { date: '2026-10-10', visits: 640 },
        { date: '2026-10-11', visits: 690 }
    ],

    // feedbacks (created_at, artifact_id, rating 1-5, comment co the NULL, status)
    // Giu nguyen 6 dong mau trong init_db.sql.
    recentFeedbacks: [
        { createdAt: '2026-10-07 11:00', artifact: 'Khuê Văn Các', rating: 1, comment: 'Nội dung quảng cáo không liên quan đến hiện vật.', status: 'REJECTED' },
        { createdAt: '2026-10-07 10:50', artifact: 'Đoan Môn', rating: 5, comment: 'The English audio guide is clear and helpful.', status: 'APPROVED' },
        { createdAt: '2026-10-06 14:40', artifact: 'Bia Tiến sĩ', rating: 2, comment: null, status: 'PENDING' },
        { createdAt: '2026-10-06 14:20', artifact: 'Khuê Văn Các', rating: 3, comment: 'Tín hiệu beacon hơi chậm khi đông người.', status: 'PENDING' },
        { createdAt: '2026-10-05 09:45', artifact: 'Bia Tiến sĩ', rating: 4, comment: 'Thông tin về bia Tiến sĩ hay, mong có thêm video.', status: 'APPROVED' },
        { createdAt: '2026-10-05 09:30', artifact: 'Khuê Văn Các', rating: 5, comment: 'Khuê Văn Các rất đẹp, phần thuyết minh dễ hiểu.', status: 'APPROVED' }
    ]
};

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
    DATA.sites.forEach(s => select.append(new Option(s.name, s.id)));
}

function renderKpis() {
    const box = document.getElementById('kpis');
    DATA.kpis.forEach(k => {
        const card = el('div', undefined, 'card stat');
        card.append(el('strong', k.value.toLocaleString('vi-VN')), el('span', k.label, 'muted'));
        box.append(card);
    });
}

function renderFeedbacks() {
    const body = document.getElementById('feedback-rows');
    DATA.recentFeedbacks.forEach(f => {
        const tr = document.createElement('tr');
        tr.append(
            el('td', f.createdAt),
            el('td', f.artifact),
            el('td', '★'.repeat(f.rating) + '☆'.repeat(5 - f.rating)),
            el('td', f.comment ?? '—', f.comment ? '' : 'muted')
        );
        const status = el('td');
        status.append(el('span', STATUS_LABEL[f.status], 'status ' + f.status));
        tr.append(status);
        body.append(tr);
    });
    document.getElementById('feedback-count').textContent = DATA.recentFeedbacks.length + ' mới nhất';
}

function renderCharts() {
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
            labels: DATA.topArtifacts.map(a => a.name),
            datasets: [{
                label: 'Lượt tương tác',
                data: DATA.topArtifacts.map(a => a.visits),
                backgroundColor: DATA.topArtifacts.map(a => (a.siteId === 1 ? sage : gold)),
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
                        afterLabel: ctx => DATA.sites.find(s => s.id === DATA.topArtifacts[ctx.dataIndex].siteId).name
                    }
                }
            },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, grid } }
        }
    });

    new Chart(document.getElementById('chart-daily'), {
        type: 'line',
        data: {
            labels: DATA.daily.map(d => d.date.slice(8, 10) + '/' + d.date.slice(5, 7)),
            datasets: [{
                label: 'Lượt tương tác',
                data: DATA.daily.map(d => d.visits),
                borderColor: sage,
                backgroundColor: sage + '22',
                fill: true,
                tension: 0.35,
                pointRadius: 4,
                pointBackgroundColor: sage
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { grid: { display: false } }, y: { beginAtZero: true, grid } }
        }
    });
}

renderFilters();
renderKpis();
renderFeedbacks();
if (window.Chart) {
    renderCharts();
} else {
    // CDN khong tai duoc (vd mo khi khong co mang): trang van hien KPI va bang.
    document.querySelectorAll('.chart-box').forEach(b => b.replaceChildren(el('p', 'Không tải được Chart.js (cần Internet).', 'muted')));
}
