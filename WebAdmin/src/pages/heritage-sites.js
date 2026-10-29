let SITES = [];
let currentFilter = 'ALL';
let nextId = 1;
let siteMap;
let selectedMarker;
let overviewMap;
let overviewMarkers;

// ─── Render ──────────────────────────────────────────────────────────────────

const API_BASE = (window.SHAuth && window.SHAuth.apiBase)
    || (function () {
        try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
        catch (e) { return 'http://localhost:8080'; }
    })();

async function loadSites() {
    const note = document.getElementById('api-note');
    try {
        const response = await fetch(API_BASE + '/api/heritage-sites');
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error((body && body.message) || ('Không tải được di tích (HTTP ' + response.status + ')'));
        if (!Array.isArray(body) && !(body && Array.isArray(body.data))) {
            throw new Error('Backend trả về danh sách di tích không đúng định dạng.');
        }
        SITES = Array.isArray(body) ? body : body.data;
        nextId = SITES.reduce((maxId, site) => Math.max(maxId, site.id + 1), 1);
        if (note) note.textContent = 'Dữ liệu đang được đồng bộ trực tiếp với Backend.';
        render();
    } catch (error) {
        SITES = [];
        if (note) note.textContent = error.message + '. Kiểm tra Backend rồi tải lại trang.';
        render();
    }
}

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

function initializeSiteMap() {
    const mapElement = document.getElementById('site-map-picker');
    if (!mapElement) return;
    if (!window.L) {
        mapElement.textContent = 'Không tải được bản đồ. Bạn vẫn có thể nhập tọa độ thủ công.';
        return;
    }

    siteMap = L.map(mapElement).setView([21.0277644, 105.8354028], 13);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
    }).addTo(siteMap);
    siteMap.on('click', function (event) {
        setSelectedCoordinates(event.latlng.lat, event.latlng.lng);
    });

    document.getElementById('f-lat').addEventListener('change', syncMarkerFromInputs);
    document.getElementById('f-lng').addEventListener('change', syncMarkerFromInputs);
}

function setSelectedCoordinates(latitude, longitude) {
    document.getElementById('f-lat').value = latitude.toFixed(7);
    document.getElementById('f-lng').value = longitude.toFixed(7);
    if (!siteMap) return;
    if (selectedMarker) selectedMarker.setLatLng([latitude, longitude]);
    else selectedMarker = L.marker([latitude, longitude]).addTo(siteMap);
}

function syncMarkerFromInputs() {
    const latitudeText = document.getElementById('f-lat').value;
    const longitudeText = document.getElementById('f-lng').value;
    if (latitudeText === '' && longitudeText === '') {
        if (selectedMarker && siteMap) siteMap.removeLayer(selectedMarker);
        selectedMarker = null;
        return;
    }
    if (latitudeText === '' || longitudeText === '') return;
    const latitude = Number(latitudeText);
    const longitude = Number(longitudeText);
    if (Number.isFinite(latitude) && Number.isFinite(longitude)
        && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
        if (selectedMarker) selectedMarker.setLatLng([latitude, longitude]);
        else selectedMarker = L.marker([latitude, longitude]).addTo(siteMap);
        siteMap.setView([latitude, longitude], 15);
    }
}

function renderOverviewMap() {
    const mapElement = document.getElementById('overview-map');
    if (!mapElement || !window.L) return;
    if (!overviewMap) {
        overviewMap = L.map(mapElement).setView([21.0277644, 105.8354028], 6);
        L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        }).addTo(overviewMap);
        overviewMarkers = L.featureGroup().addTo(overviewMap);
    }
    overviewMarkers.clearLayers();        SITES.forEach(site => {
        if (site.latitude == null || site.longitude == null) return;
        const point = [Number(site.latitude), Number(site.longitude)];
        if (!point.every(Number.isFinite)) return;
        L.marker(point).bindPopup(escHtml(site.name)).addTo(overviewMarkers);
    });
    if (overviewMarkers.getLayers().length) {
        overviewMap.fitBounds(overviewMarkers.getBounds().pad(0.15), { maxZoom: 15 });
    }
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
    renderOverviewMap();
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
    if (siteMap) {
        window.setTimeout(function () {
            siteMap.invalidateSize();
            const latitudeText = document.getElementById('f-lat').value;
            const longitudeText = document.getElementById('f-lng').value;
            const latitude = Number(latitudeText);
            const longitude = Number(longitudeText);
            if (latitudeText !== '' && longitudeText !== ''
                && Number.isFinite(latitude) && Number.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
                siteMap.setView([latitude, longitude], 15);
                setSelectedCoordinates(latitude, longitude);
            } else {
                if (selectedMarker) siteMap.removeLayer(selectedMarker);
                selectedMarker = null;
                siteMap.setView([21.0277644, 105.8354028], 13);
            }
        }, 0);
    }
}

function closeModal() {
    document.getElementById('site-modal').classList.remove('open');
    clearForm();
}

function clearForm() {
    document.getElementById('site-form').reset();
    document.getElementById('edit-id').value = '';
    if (selectedMarker && siteMap) siteMap.removeLayer(selectedMarker);
    selectedMarker = null;
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

    if (latitude === null && longitude === null && editId) {
        const existingSite = SITES.find(site => site.id === parseInt(editId));
        if (existingSite && existingSite.latitude != null && existingSite.longitude != null
            && !confirm('Để trống tọa độ sẽ xóa vị trí hiện tại. Bạn có muốn tiếp tục?')) return;
    }

    if ((latitude === null) !== (longitude === null)) {
        alert('Nhập cả vĩ độ và kinh độ hoặc để trống cả hai.');
        return;
    }
    if (latitude !== null && (!Number.isFinite(latitude) || latitude < -90 || latitude > 90
        || !Number.isFinite(longitude) || longitude < -180 || longitude > 180)) {
        alert('Tọa độ không hợp lệ. Vĩ độ phải từ -90 đến 90, kinh độ từ -180 đến 180.');
        return;
    }

    const payload = {
        name,
        location: location || null,
        mapImageUrl: mapUrl || null,
        latitude,
        longitude,
        clearCoordinates: Boolean(editId) && latitude === null && longitude === null
    };
    const saveButton = document.getElementById('modal-submit-btn');
    saveButton.disabled = true;

    fetch(API_BASE + '/api/heritage-sites' + (editId ? '/' + editId : ''), {
        method: editId ? 'PUT' : 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
    }).then(async response => {
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error((body && body.message) || ('Lưu thất bại (HTTP ' + response.status + ')'));
        const site = body;
        if (editId) {
            const index = SITES.findIndex(item => item.id === parseInt(editId));
            if (index !== -1) SITES[index] = site;
        } else {
            SITES.push(site);
            nextId = Math.max(nextId, site.id + 1);
        }
        closeModal();
        render();
    }).catch(error => {
        alert(error.message);
    }).finally(() => {
        saveButton.disabled = false;
    });
}

// ─── Xóa ─────────────────────────────────────────────────────────────────────

async function deleteSite(id) {
    if (!confirm('Bạn có chắc muốn xóa di tích này không?')) return;
    try {
        const response = await fetch(API_BASE + '/api/heritage-sites/' + id, { method: 'DELETE' });
        const body = await response.json().catch(() => null);
        if (!response.ok) throw new Error((body && body.message) || ('Xóa thất bại (HTTP ' + response.status + ')'));
        SITES = SITES.filter(site => site.id !== id);
        render();
    } catch (error) {
        alert(error.message);
    }
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

initializeSiteMap();
render();
loadSites();
