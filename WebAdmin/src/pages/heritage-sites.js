// Dữ liệu mẫu khớp với Infra/init_db.sql (heritage_sites + latitude/longitude đã thêm).
// Khi backend sẵn sàng, thay bằng fetch('/api/heritage-sites').
let SITES = [
    {
        id: 1,
        name: 'Văn Miếu - Quốc Tử Giám',
        location: '58 Quốc Tử Giám, Đống Đa, Hà Nội',
        mapImageUrl: 'https://example.com/maps/van-mieu.png',
        latitude: 21.0277644,
        longitude: 105.8354028,
    },
    {
        id: 2,
        name: 'Hoàng thành Thăng Long',
        location: '19C Hoàng Diệu, Ba Đình, Hà Nội',
        mapImageUrl: 'https://example.com/maps/hoang-thanh.png',
        latitude: 21.0354430,
        longitude: 105.8349380,
    },
];

let currentFilter = 'ALL';
let nextId = 3;

// ─── Render ──────────────────────────────────────────────────────────────────

function getFiltered() {
    if (currentFilter === 'HAS_COORD') {
        return SITES.filter(s => s.latitude != null && s.longitude != null);
    }
    if (currentFilter === 'NO_COORD') {
        return SITES.filter(s => s.latitude == null || s.longitude == null);
    }
    return SITES;
}

function renderStats() {
    const total = SITES.length;
    const withCoord = SITES.filter(s => s.latitude != null && s.longitude != null).length;
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-coords').textContent = withCoord;
    document.getElementById('stat-no-coords').textContent = total - withCoord;
}

function renderMapPins() {
    const container = document.getElementById('map-pins');
    container.innerHTML = '';
    SITES.forEach((site, i) => {
        if (site.latitude == null || site.longitude == null) return;
        // Tính vị trí tương đối đơn giản cho placeholder map
        const left = 15 + (i * 35) % 70;
        const top  = 20 + (i * 28) % 60;
        const pin = document.createElement('div');
        pin.className = 'pin';
        pin.style.cssText = `left:${left}%;top:${top}%;position:absolute;`;
        pin.title = site.name;
        container.appendChild(pin);
    });
}

function renderTable() {
    const tbody = document.getElementById('sites-body');
    const empty = document.getElementById('sites-empty');
    const filtered = getFiltered();

    if (filtered.length === 0) {
        tbody.innerHTML = '';
        empty.hidden = false;
        return;
    }
    empty.hidden = true;

    tbody.innerHTML = filtered.map(site => {
        const coordHtml = (site.latitude != null && site.longitude != null)
            ? `<span class="hs-coord">${site.latitude.toFixed(5)}, ${site.longitude.toFixed(5)}</span>`
            : `<span class="pill" style="background:#f4e1e1;color:#9a4545;font-size:11px;">Chưa có</span>`;

        return `
        <tr>
            <td class="muted" style="font-size:13px;">#${site.id}</td>
            <td>
                <div class="hs-name">${escHtml(site.name)}</div>
                <div class="hs-location">${escHtml(site.location || '—')}</div>
            </td>
            <td style="font-size:13px;max-width:200px;">${escHtml(site.location || '—')}</td>
            <td>${coordHtml}</td>
            <td>
                <div class="hs-actions">
                    <button class="btn hs-sm hs-edit" onclick="openModal('edit', ${site.id})">Sửa</button>
                    <button class="btn hs-sm hs-danger" onclick="deleteSite(${site.id})">Xóa</button>
                </div>
            </td>
        </tr>`;
    }).join('');
}

function render() {
    renderStats();
    renderMapPins();
    renderTable();
}

// ─── Filter ───────────────────────────────────────────────────────────────────

document.getElementById('filter-bar').addEventListener('click', e => {
    const btn = e.target.closest('button[data-filter]');
    if (!btn) return;
    document.querySelectorAll('#filter-bar button').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    currentFilter = btn.dataset.filter;
    renderTable();
});

// ─── Modal ────────────────────────────────────────────────────────────────────

function openModal(mode, id) {
    const modal = document.getElementById('site-modal');
    const title = document.getElementById('modal-title');
    const submitBtn = document.getElementById('modal-submit-btn');

    clearForm();

    if (mode === 'create') {
        title.textContent = 'Thêm di tích';
        submitBtn.textContent = 'Thêm';
        document.getElementById('edit-id').value = '';
    } else {
        const site = SITES.find(s => s.id === id);
        if (!site) return;
        title.textContent = 'Chỉnh sửa di tích';
        submitBtn.textContent = 'Lưu thay đổi';
        document.getElementById('edit-id').value = site.id;
        document.getElementById('f-name').value = site.name;
        document.getElementById('f-location').value = site.location || '';
        document.getElementById('f-map-url').value = site.mapImageUrl || '';
        document.getElementById('f-lat').value = site.latitude != null ? site.latitude : '';
        document.getElementById('f-lng').value = site.longitude != null ? site.longitude : '';
    }

    modal.classList.add('open');
}

function closeModal() {
    document.getElementById('site-modal').classList.remove('open');
    clearForm();
}

function clearForm() {
    document.getElementById('site-form').reset();
    document.getElementById('edit-id').value = '';
}

// Đóng modal khi click nền
document.getElementById('site-modal').addEventListener('click', e => {
    if (e.target === e.currentTarget) closeModal();
});

function handleSubmit(e) {
    e.preventDefault();

    const editId = document.getElementById('edit-id').value;
    const name     = document.getElementById('f-name').value.trim();
    const location = document.getElementById('f-location').value.trim();
    const mapUrl   = document.getElementById('f-map-url').value.trim();
    const latVal   = document.getElementById('f-lat').value;
    const lngVal   = document.getElementById('f-lng').value;

    const latitude  = latVal  !== '' ? parseFloat(latVal)  : null;
    const longitude = lngVal  !== '' ? parseFloat(lngVal) : null;

    if (editId) {
        // Cập nhật
        const idx = SITES.findIndex(s => s.id === parseInt(editId));
        if (idx !== -1) {
            SITES[idx] = { ...SITES[idx], name, location, mapImageUrl: mapUrl, latitude, longitude };
        }
    } else {
        // Tạo mới
        SITES.push({ id: nextId++, name, location, mapImageUrl: mapUrl, latitude, longitude });
    }

    closeModal();
    render();
}

// ─── Xóa ─────────────────────────────────────────────────────────────────────

function deleteSite(id) {
    if (!confirm('Bạn có chắc muốn xóa di tích này không?')) return;
    SITES = SITES.filter(s => s.id !== id);
    render();
}

// ─── Tiện ích ─────────────────────────────────────────────────────────────────

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ─── Khởi động ────────────────────────────────────────────────────────────────

render();
