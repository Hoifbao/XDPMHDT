const STATUS_LABEL = {
    ACTIVE: 'Đang hoạt động',
    INACTIVE: 'Không hoạt động'
};

const STATUS_CLASS = {
    ACTIVE: 'pill',
    INACTIVE: 'fb-rejected'
};

// Khoa W6: ngưỡng pin thấp - cùng giá trị với BatteryAlertService của beacon-service
const LOW_BATTERY = 20;

// Địa chỉ Gateway (dùng chung cách cấu hình với WebAdmin/src/api/auth.js).
// auth.js đã bọc fetch(): TỰ gắn JWT và tự refresh khi 401 -> trang không phải tự làm.
const API_BASE = (window.SHAuth && window.SHAuth.apiBase)
    || (function () {
        try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
        catch (e) { return 'http://localhost:8080'; }
    })();

// UUID iBeacon chuẩn: 8-4-4-4-12 ký tự hex (cùng kiểu kiểm tra với Backend)
const UUID_PATTERN = /^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$/;

// Dữ liệu mẫu khớp với bảng ibeacons trong Infra/db/04_beacon.sql.
// Chỉ dùng khi không gọi được API (service chưa chạy).
const SAMPLE_BEACONS = [
    { id: 1, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 1, siteId: 1, siteName: 'Văn Miếu - Quốc Tử Giám', artifactId: 1, artifactName: 'Khuê Văn Các', batteryLevel: 87, status: 'ACTIVE', updatedAt: '2026-10-05T08:00:00' },
    { id: 2, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 2, siteId: 1, siteName: 'Văn Miếu - Quốc Tử Giám', artifactId: 2, artifactName: 'Bia Tiến sĩ', batteryLevel: 64, status: 'ACTIVE', updatedAt: '2026-10-05T08:10:00' },
    { id: 3, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 2, minor: 1, siteId: 2, siteName: 'Hoàng thành Thăng Long', artifactId: 3, artifactName: 'Đoan Môn', batteryLevel: 92, status: 'ACTIVE', updatedAt: '2026-10-06T09:00:00' },
    { id: 4, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 3, siteId: 1, siteName: 'Văn Miếu - Quốc Tử Giám', artifactId: null, artifactName: null, batteryLevel: 15, status: 'INACTIVE', updatedAt: '2026-10-07T15:30:00' }
];

// Đọc thông báo lỗi từ khung JSON {success:false, message}
async function readMessage(response, fallback) {
    try {
        const body = await response.json();
        return (body && body.message) || fallback(response);
    } catch (e) {
        return fallback(response);
    }
}

// Danh sách đang hiển thị (lấy từ API, lỗi thì dùng dữ liệu mẫu)
let beacons = [];
let sites = [];
let currentFilter = 'ALL';
let currentSite = 'ALL'; // 'ALL' hoặc id của di tích

const bodyElement = document.getElementById('beacon-body');
const emptyElement = document.getElementById('beacon-empty');
const tableElement = document.getElementById('beacon-table');
const filterBar = document.getElementById('filter-bar');
const siteFilter = document.getElementById('site-filter');

function pad(number) {
    return String(number).padStart(2, '0');
}

// Chuỗi ISO không có múi giờ được hiểu theo giờ địa phương
function formatDate(isoText) {
    if (!isoText) {
        return '';
    }
    const date = new Date(isoText);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return pad(date.getDate()) + '/' + pad(date.getMonth() + 1) + '/' + date.getFullYear()
        + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

function escHtml(text) {
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function createCell(className) {
    const cell = document.createElement('td');
    if (className) {
        cell.className = className;
    }
    return cell;
}

function createStatusPill(status) {
    const pill = document.createElement('span');
    pill.className = 'pill ' + STATUS_CLASS[status];
    pill.textContent = STATUS_LABEL[status];
    return pill;
}

function createArtifactCell(artifactName) {
    const cell = createCell();
    if (artifactName) {
        cell.textContent = artifactName;
    } else {
        cell.textContent = '(Chưa gán)';
        cell.classList.add('muted');
    }
    return cell;
}

// Pin dưới 20% tô đỏ để admin biết cần thay
function createBatteryCell(level) {
    const cell = createCell('fb-nowrap');
    if (level === null || level === undefined) {
        cell.textContent = '—';
        cell.classList.add('muted');
    } else {
        cell.textContent = level + '%';
        if (level < 20) {
            cell.style.color = 'var(--danger)';
            cell.style.fontWeight = '700';
        }
    }
    return cell;
}

function createActionsCell(item) {
    const cell = createCell('fb-nowrap');
    const group = document.createElement('div');
    group.className = 'fb-actions';

    const editButton = document.createElement('button');
    editButton.type = 'button';
    editButton.className = 'btn alt fb-sm';
    editButton.textContent = 'Sửa';
    editButton.addEventListener('click', function () { openBeaconModal('edit', item.id); });
    group.appendChild(editButton);

    const assignButton = document.createElement('button');
    assignButton.type = 'button';
    assignButton.className = 'btn fb-sm';
    assignButton.textContent = item.artifactId ? 'Bỏ gán' : 'Gán';
    assignButton.title = item.artifactId
        ? 'Đang gán: ' + (item.artifactName || ('hiện vật #' + item.artifactId))
        : 'Gán hiện vật cùng di tích';
    assignButton.addEventListener('click', function () { openAssignModal(item.id); });
    group.appendChild(assignButton);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn fb-danger fb-sm';
    deleteButton.textContent = 'Xóa';
    deleteButton.addEventListener('click', function () { deleteBeacon(item.id); });
    group.appendChild(deleteButton);

    cell.appendChild(group);
    return cell;
}

function createRow(item) {
    const row = document.createElement('tr');

    row.appendChild(createCell('fb-nowrap')).textContent = item.uuid;
    row.appendChild(createCell()).textContent = item.major;
    row.appendChild(createCell()).textContent = item.minor;
    row.appendChild(createCell()).textContent = item.siteName || '—';
    row.appendChild(createArtifactCell(item.artifactName));
    row.appendChild(createBatteryCell(item.batteryLevel));

    const statusCell = createCell('fb-nowrap');
    statusCell.appendChild(createStatusPill(item.status));
    row.appendChild(statusCell);

    row.appendChild(createCell('fb-nowrap')).textContent = formatDate(item.updatedAt) || '—';
    row.appendChild(createActionsCell(item));

    return row;
}

function countByStatus(status) {
    return beacons.filter(function (item) {
        return item.status === status;
    }).length;
}

function renderStats() {
    document.getElementById('stat-total').textContent = beacons.length;
    document.getElementById('stat-active').textContent = countByStatus('ACTIVE');
    document.getElementById('stat-low-battery').textContent = lowBatteryBeacons().length;
    document.getElementById('stat-inactive').textContent = countByStatus('INACTIVE');
    document.getElementById('stat-unassigned').textContent = beacons.filter(function (item) {
        return !item.artifactId && !item.artifactName;
    }).length;
}

// Khoa W6: beacon ACTIVE pin < 20% - đối tượng cần cảnh báo (INACTIVE không báo tránh báo ảo)
function lowBatteryBeacons() {
    return beacons.filter(function (item) {
        return item.status === 'ACTIVE' && item.batteryLevel != null && item.batteryLevel < LOW_BATTERY;
    });
}

// Khoa W6: banner "biểu tượng cảnh báo" phía trên - hiện khi có beacon ACTIVE pin thấp
function renderBatteryAlert() {
    const alertBox = document.getElementById('battery-alert');
    const alertText = document.getElementById('battery-alert-text');
    const low = lowBatteryBeacons();
    if (low.length === 0) {
        alertBox.hidden = true;
        return;
    }
    alertText.textContent = low.map(function (item) {
        return 'Beacon #' + item.id
            + (item.siteName ? ' tại ' + item.siteName : '')
            + (item.artifactName ? ' (hiện vật ' + item.artifactName + ')' : '')
            + ' còn ' + item.batteryLevel + '%';
    }).join('; ') + ' — cần sạc hoặc thay pin.';
    alertBox.hidden = false;
}

// Khoa W6: chạy 1 lượt quét pin ngay (POST /api/ibeacons/battery-check - ADMIN qua Gateway)
// Demo thay vì chờ tác vụ định kỳ 15 phút; xong tự tải lại danh sách để banner hiện.
async function checkBatteryNow() {
    const button = document.getElementById('battery-check-btn');
    button.disabled = true;
    try {
        const response = await fetch(API_BASE + '/api/ibeacons/battery-check', { method: 'POST' });
        const body = await response.json().catch(function () { return {}; });
        if (!response.ok) {
            throw new Error((body && body.message) || ('HTTP ' + response.status));
        }
        const alerted = body.data != null ? body.data : 0;
        alert(alerted > 0
            ? 'Đã quét: cảnh báo ' + alerted + ' beacon pin thấp (sự kiện beacon.battery.low đã phát).'
            : 'Đã quét: không có beacon nào pin thấp.');
        await loadBeacons();
    } catch (error) {
        alert('Kiểm tra pin thất bại: ' + error.message);
    } finally {
        button.disabled = false;
    }
}

function renderSiteFilter() {
    // Giữ lựa chọn đang có (hoặc về ALL nếu di tích bị xóa)
    const keep = currentSite;
    const options = [['ALL', 'Tất cả di tích']].concat(
        sites.map(function (site) { return [String(site.id), site.name]; })
    );
    if (options.every(function (option) { return option[0] !== String(keep); })) {
        currentSite = 'ALL';
    }
    siteFilter.replaceChildren();
    options.forEach(function (option) {
        const optionElement = document.createElement('option');
        optionElement.value = option[0];
        optionElement.textContent = option[1];
        siteFilter.appendChild(optionElement);
    });
}

function renderFilterBar() {
    filterBar.querySelectorAll('button').forEach(function (button) {
        const filter = button.dataset.filter;
        const isActive = filter === currentFilter;
        const count = filter === 'ALL' ? beacons.length : countByStatus(filter);
        const baseLabel = filter === 'ALL' ? 'Tất cả' : STATUS_LABEL[filter];
        button.textContent = baseLabel + ' (' + count + ')';
        button.classList.toggle('active', isActive);
        button.setAttribute('aria-pressed', String(isActive));
    });
    siteFilter.value = String(currentSite);
}

function renderTable() {
    // Nhớ id đang lọc trước khi vẽ lại (tránh giữ reference ra ngoài render)
    const visible = beacons.filter(function (item) {
        const byStatus = currentFilter === 'ALL' || item.status === currentFilter;
        const bySite = currentSite === 'ALL' || Number(currentSite) === Number(item.siteId);
        return byStatus && bySite;
    });

    bodyElement.replaceChildren();
    visible.forEach(function (item) {
        bodyElement.appendChild(createRow(item));
    });

    const isEmpty = visible.length === 0;
    emptyElement.hidden = !isEmpty;
    tableElement.hidden = isEmpty;
}

function render() {
    renderStats();
    renderSiteFilter();
    renderFilterBar();
    renderTable();
    // Khoa W6: banner cảnh báo pin
    renderBatteryAlert();
    // Ân W5: cập nhật select di tích cho bản đồ
    if (typeof renderMapSiteSelect === 'function') renderMapSiteSelect();
}

filterBar.addEventListener('click', function (event) {
    const button = event.target.closest('button[data-filter]');
    if (!button) {
        return;
    }
    currentFilter = button.dataset.filter;
    renderFilterBar();
    renderTable();
});

siteFilter.addEventListener('change', function () {
    currentSite = siteFilter.value !== 'ALL'
        ? Number(siteFilter.value)
        : siteFilter.value;
    renderTable();
});

// ─── Tải dữ liệu ─────────────────────────────────────────────────────────────

// Gọi GET /api/ibeacons; service chưa chạy thì hiện dữ liệu mẫu
async function loadBeacons() {
    const sourceLabel = document.getElementById('data-source');
    try {
        const response = await fetch(API_BASE + '/api/ibeacons');
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        const body = await response.json();
        beacons = Array.isArray(body.data) ? body.data : [];
        sourceLabel.textContent = 'Dữ liệu từ API';
    } catch (error) {
        beacons = SAMPLE_BEACONS.map(function (item) {
            return Object.assign({}, item);
        });
        sourceLabel.textContent = 'Dữ liệu mẫu (chưa kết nối service)';
    }
    // Không có danh sách di tích thật thì dựng tạm từ dữ liệu beacon
    if (sites.length === 0) {
        sites = beaconsToSites();
    }
    render();
}

// Lấy danh sách di tích cho select lọc + modal thêm/sửa (GET /api/heritage-sites)
async function loadSites() {
    try {
        const response = await fetch(API_BASE + '/api/heritage-sites');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const body = await response.json();
        const list = Array.isArray(body) ? body : (body && body.data);
        if (Array.isArray(list) && list.length) {
            sites = list.map(function (site) {
                return { id: site.id, name: site.name };
            });
        }
    } catch (error) {
        // Không tải được (chưa tách site-service + legacy tắt): dựng từ beacon, vẫn lọc được
        sites = beaconsToSites();
    }
    render();
}

// Dự danh sách di tích từ các beacon đang có (id + name duy nhất)
function beaconsToSites() {
    const seen = new Map();
    beacons.forEach(function (item) {
        if (item.siteId != null && !seen.has(String(item.siteId))) {
            seen.set(String(item.siteId), {
                id: item.siteId,
                name: item.siteName || ('Di tích #' + item.siteId)
            });
        }
    });
    return Array.from(seen.values()).sort(function (a, b) { return a.id - b.id; });
}

// ─── Modal Thêm / Sửa ────────────────────────────────────────────────────────

function fillSiteOptions(selected) {
    const select = document.getElementById('f-site');
    select.replaceChildren();
    // sites trống (cả API lẫn beacon mẫu đều rỗng): nhắc service chưa chạy
    if (sites.length === 0) {
        const empty = document.createElement('option');
        empty.value = '';
        empty.textContent = '— Không có di tích (kiểm tra Backend) —';
        select.appendChild(empty);
        return;
    }
    sites.forEach(function (site) {
        const option = document.createElement('option');
        option.value = site.id;
        option.textContent = site.name;
        select.appendChild(option);
    });
    if (selected != null) {
        select.value = String(selected);
    }
}

function openBeaconModal(mode, id) {
    const modal = document.getElementById('beacon-modal');
    const title = document.getElementById('beacon-modal-title');
    const submitBtn = document.getElementById('beacon-submit-btn');
    document.getElementById('beacon-form').reset();
    document.getElementById('beacon-edit-id').value = '';
    fillSiteOptions();

    if (mode === 'create') {
        title.textContent = 'Thêm iBeacon';
        submitBtn.textContent = 'Thêm';
    } else {
        const item = beacons.find(function (beacon) { return beacon.id === id; });
        if (!item) return;
        title.textContent = 'Sửa iBeacon #' + id;
        submitBtn.textContent = 'Lưu thay đổi';
        document.getElementById('beacon-edit-id').value = id;
        document.getElementById('f-uuid').value = item.uuid || '';
        document.getElementById('f-major').value = item.major != null ? item.major : '';
        document.getElementById('f-minor').value = item.minor != null ? item.minor : '';
        document.getElementById('f-posx').value = item.posX != null ? item.posX : '';
        document.getElementById('f-posy').value = item.posY != null ? item.posY : '';
        document.getElementById('f-battery').value = item.batteryLevel != null ? item.batteryLevel : '';
        document.getElementById('f-status').value = item.status === 'INACTIVE' ? 'INACTIVE' : 'ACTIVE';
        fillSiteOptions(item.siteId);
    }

    modal.classList.add('open');
}

function closeBeaconModal() {
    document.getElementById('beacon-modal').classList.remove('open');
}

// Nút Hủy + click nền đều đóng modal
document.getElementById('beacon-modal').addEventListener('click', function (event) {
    if (event.target === event.currentTarget) closeBeaconModal();
});

// Đóng modal bằng phím Escape (như các trang khác của CMS)
document.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') return;
    closeBeaconModal();
    closeAssignModal();
});

// Kiểm tra dữ liệu client trước khi gửi (cùng quy tắc với Backend)
function validateBeacon(uuidText, major, minor, battery) {
    if (!UUID_PATTERN.test(uuidText)) {
        return 'UUID không đúng định dạng (8-4-4-4-12 ký tự hex).';
    }
    if (!Number.isInteger(major) || major < 0 || major > 65535
        || !Number.isInteger(minor) || minor < 0 || minor > 65535) {
        return 'Major/Minor phải là số nguyên trong khoảng 0–65535.';
    }
    if (battery != null && (battery < 0 || battery > 100)) {
        return 'Pin (batteryLevel) phải nằm trong khoảng 0–100.';
    }
    return null;
}

async function handleBeaconSubmit(event) {
    event.preventDefault();

    const editId = document.getElementById('beacon-edit-id').value;
    const siteId = Number(document.getElementById('f-site').value) || null;
    const uuidText = document.getElementById('f-uuid').value.trim();
    const majorText = document.getElementById('f-major').value;
    const minorText = document.getElementById('f-minor').value;
    const posxText = document.getElementById('f-posx').value;
    const posyText = document.getElementById('f-posy').value;
    const batteryText = document.getElementById('f-battery').value;
    const statusValue = document.getElementById('f-status').value;

    const major = majorText === '' ? null : parseInt(majorText, 10);
    const minor = minorText === '' ? null : parseInt(minorText, 10);
    const battery = batteryText === '' ? null : parseInt(batteryText, 10);

    const error = validateBeacon(uuidText, major, minor, battery);
    if (error) {
        alert(error);
        return;
    }

    const payload = {
        uuid: uuidText,
        major: major,
        minor: minor,
        siteId: siteId,
        // Sửa beacon không đổi gán qua form này: giữ nguyên artifactId hiện tại
        artifactId: editId ? (findBeacon(editId).artifactId) : null,
        posX: posxText === '' ? null : Number(posxText),
        posY: posyText === '' ? null : Number(posyText),
        batteryLevel: battery,
        status: statusValue
    };

    const saveButton = document.getElementById('beacon-submit-btn');
    saveButton.disabled = true;
    try {
        const response = await fetch(API_BASE + '/api/ibeacons' + (editId ? '/' + editId : ''), {
            method: editId ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(await readMessage(response, function () {
                return 'Lưu thất bại (HTTP ' + response.status + ').';
            }));
        }
        closeBeaconModal();
        await loadBeacons();
    } catch (error) {
        alert(error.message);
    } finally {
        saveButton.disabled = false;
    }
}

function findBeacon(id) {
    return beacons.find(function (beacon) {
        return Number(beacon.id) === Number(id);
    }) || { artifactId: null };
}

// ─── Modal Gán / Bỏ gán hiện vật ────────────────────────────────────────────

let assignBeaconId = null;

// Lable chuẩn hoá của 1 mục artifact trả về từ các API
function artifactInfo(item) {
    // Backend cũ trả {id, name, siteId}; service mới (content_db) cũng trả cùng tên trường
    return {
        id: item.id,
        name: item.name || ('Hiện vật #' + item.id),
        siteId: item.siteId != null ? item.siteId : null
    };
}

function openAssignModal(id) {
    const item = findBeacon(id);
    assignBeaconId = id;
    const target = document.getElementById('assign-target');
    target.innerHTML = 'Beacon <strong>#' + escHtml(id) + '</strong> — Major <strong>' + escHtml(item.major) + '</strong> / Minor <strong>' + escHtml(item.minor) + '</strong> — '
        + escHtml(item.siteName || ('di tích #' + (item.siteId != null ? item.siteId : '?')));

    loadAssignArtifacts(item);
    document.getElementById('assign-modal').classList.add('open');
}

async function loadAssignArtifacts(item) {
    const select = document.getElementById('assign-artifact');
    const note = document.getElementById('assign-note');

    // Danh sách hiện vật: hiện giữ lại hiện vật cùng di tích
    const options = [['', '— Bỏ gán (chưa gán hiện vật) —']];
    note.textContent = 'Đang tải danh sách hiện vật cùng di tích…';

    try {
        const response = await fetch(API_BASE + '/api/artifacts?siteId=' + item.siteId);
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const body = await response.json();
        const list = Array.isArray(body) ? body : body.data;
        (list || []).forEach(function (raw) {
            const artifact = artifactInfo(raw);
            // API đã lọc theo siteId; lọc thêm 1 lần phòng trường hợp backend cũ trả đủ danh sách
            if (artifact.siteId != null && item.siteId != null && Number(artifact.siteId) !== Number(item.siteId)) return;
            options.push([String(artifact.id), artifact.name]);
        });
        note.innerHTML = 'Chỉ gán được hiện vật thuộc <strong>cùng di tích</strong> với beacon. Chọn “Bỏ gán” để tháo hiện vật đang gán.';
    } catch (error) {
        // Không tải được (content-service tắt): hiện tượng đang gán nếu có vẫn cho bỏ gán
        note.textContent = 'Không tải được danh sách hiện vật (status: ' + (error.message || 'lỗi') + '). Bạn vẫn có thể bỏ gán.';
        if (item.artifactId) {
            options.push([String(item.artifactId), 'Hiện vật #' + item.artifactId + (item.artifactName ? ' - ' + item.artifactName : ' (đang gán)')]);
        }
    }

    select.replaceChildren();
    options.forEach(function (option) {
        const optionElement = document.createElement('option');
        optionElement.value = option[0];
        optionElement.textContent = option[1];
        select.appendChild(optionElement);
    });
    select.value = item.artifactId != null ? String(item.artifactId) : '';
}

function closeAssignModal() {
    document.getElementById('assign-modal').classList.remove('open');
    assignBeaconId = null;
}

document.getElementById('assign-modal').addEventListener('click', function (event) {
    if (event.target === event.currentTarget) closeAssignModal();
});

async function submitAssign() {
    const select = document.getElementById('assign-artifact');
    const saveButton = document.getElementById('assign-submit-btn');
    const artifactValue = select.value;

    const payload = {
        artifactId: artifactValue === '' ? null : Number(artifactValue)
    };
    const unassigning = payload.artifactId === null;

    if (unassigning && !confirm('Bỏ gán hiện vật khỏi beacon này?')) return;

    saveButton.disabled = true;
    try {
        const response = await fetch(API_BASE + '/api/ibeacons/' + assignBeaconId + '/assign', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(await readMessage(response, function () {
                return 'Gán hiện vật thất bại (HTTP ' + response.status + ').';
            }));
        }
        closeAssignModal();
        await loadBeacons();
    } catch (error) {
        alert(error.message);
    } finally {
        saveButton.disabled = false;
    }
}

// ─── Xóa ────────────────────────────────────────────────────────────────────

async function deleteBeacon(id) {
    const item = findBeacon(id);
    const label = 'beacon Major ' + (item.major != null ? item.major : '?') + ' / Minor ' + (item.minor != null ? item.minor : '?');
    if (!confirm('Bạn có chắc muốn xóa ' + label + ' không?')) return;
    try {
        const response = await fetch(API_BASE + '/api/ibeacons/' + id, { method: 'DELETE' });
        if (!response.ok) {
            throw new Error(await readMessage(response, function () {
                return 'Xóa thất bại (HTTP ' + response.status + ').';
            }));
        }
        await loadBeacons();
    } catch (error) {
        alert(error.message);
    }
}

// ─── Khởi động ─────────────────────────────────────────────────────────────

loadBeacons();
loadSites();

// Khoa W6: tự tải lại mỗi 60 giây - pin giảm giữa chừng web vẫn hiện cảnh báo
// mà không cần F5 (tiêu chí "≤ 15 phút web hiện cảnh báo": tác vụ 15 phút + web 60 giây)
setInterval(loadBeacons, 60000);


// ─── Ân W5: Bản đồ vị trí beacon trên sơ đồ di tích ─────────────────────────
//
// Dùng Leaflet với L.CRS.Simple (toạ độ pixel, không phải địa lý).
// Trong CRS.Simple: lat = posY, lng = posX.
// Kéo marker beacon → thả → tự lưu PUT /api/ibeacons/{id} (posX, posY).
// Beacon chưa có toạ độ → nút "Đặt trên bản đồ" → bấm vào sơ đồ để đặt.
// ─────────────────────────────────────────────────────────────────────────────

var floorplanMap = null;
var floorplanMarkers = [];
var floorplanImageOverlay = null;
var currentMapSiteId = null;
var placingBeaconId = null;

var DEFAULT_MAP_W = 1000;
var DEFAULT_MAP_H = 700;

// ── Khởi tạo Leaflet map ─────────────────────────────────────────────────────

function initFloorplanMap() {
    if (floorplanMap || !window.L) return;

    floorplanMap = L.map('beacon-floorplan-map', {
        crs: L.CRS.Simple,
        minZoom: -3,
        maxZoom: 4,
        zoomSnap: 0.5,
    });

    // Bấm vào bản đồ khi đang đặt beacon
    floorplanMap.on('click', function (e) {
        if (placingBeaconId == null) return;
        confirmPlaceBeacon(placingBeaconId, e.latlng.lng, e.latlng.lat);
    });
}

// ── Điền select di tích cho bản đồ ──────────────────────────────────────────

function renderMapSiteSelect() {
    var sel = document.getElementById('map-site-select');
    if (!sel) return;
    var current = sel.value;
    sel.replaceChildren();
    var emptyOpt = document.createElement('option');
    emptyOpt.value = '';
    emptyOpt.textContent = '— Chọn di tích —';
    sel.appendChild(emptyOpt);
    sites.forEach(function (site) {
        var opt = document.createElement('option');
        opt.value = site.id;
        opt.textContent = site.name;
        sel.appendChild(opt);
    });
    if (current && sites.some(function (s) { return String(s.id) === current; })) {
        sel.value = current;
    }
}

document.getElementById('map-site-select').addEventListener('change', function () {
    var siteId = this.value ? Number(this.value) : null;
    renderFloorplanMap(siteId);
});

// ── Render bản đồ cho di tích đang chọn ─────────────────────────────────────

async function renderFloorplanMap(siteId) {
    if (!window.L) {
        document.getElementById('map-hint').textContent =
            'Leaflet chưa tải được. Kiểm tra thư mục WebAdmin/vendor/leaflet/.';
        return;
    }
    initFloorplanMap();
    currentMapSiteId = siteId;
    placingBeaconId = null;
    document.getElementById('beacon-floorplan-map').style.cursor = '';

    // Xóa markers + overlay cũ
    floorplanMarkers.forEach(function (m) { floorplanMap.removeLayer(m); });
    floorplanMarkers = [];
    if (floorplanImageOverlay) {
        floorplanMap.removeLayer(floorplanImageOverlay);
        floorplanImageOverlay = null;
    }

    if (!siteId) {
        document.getElementById('map-beacon-count').textContent = '0 beacon';
        document.getElementById('map-hint').textContent = 'Chọn di tích ở trên để xem sơ đồ.';
        return;
    }

    var siteBeacons = beacons.filter(function (b) {
        return Number(b.siteId) === Number(siteId);
    });
    document.getElementById('map-beacon-count').textContent = siteBeacons.length + ' beacon';
    // Lấy mapImageUrl từ site-service
    var mapImageUrl = null;
    var mapW = DEFAULT_MAP_W;
    var mapH = DEFAULT_MAP_H;
    try {
        var resp = await fetch(API_BASE + '/api/heritage-sites/' + siteId);
        if (resp.ok) {
            var body = await resp.json();
            var siteData = body.data || body;
            mapImageUrl = siteData.mapImageUrl || null;
        }
    } catch (e) { /* giữ mapImageUrl null */ }

    // Tải ảnh để lấy kích thước thật
    if (mapImageUrl) {
        await new Promise(function (resolve) {
            var img = new Image();
            img.onload = function () {
                mapW = img.naturalWidth || DEFAULT_MAP_W;
                mapH = img.naturalHeight || DEFAULT_MAP_H;
                resolve();
            };
            img.onerror = resolve;
            img.src = mapImageUrl;
        });
        var bounds = [[0, 0], [mapH, mapW]];
        floorplanImageOverlay = L.imageOverlay(mapImageUrl, bounds, {
            opacity: 0.9, interactive: false,
        }).addTo(floorplanMap);
        floorplanMap.fitBounds(bounds, { padding: [10, 10] });
    }

    // Vẽ marker beacon có tọa độ
    var withPos = siteBeacons.filter(function (b) { return b.posX != null && b.posY != null; });
    var withoutPos = siteBeacons.filter(function (b) { return b.posX == null || b.posY == null; });
    withPos.forEach(function (b) { addBeaconMarker(b); });

    // Thêm nút đặt vị trí cho beacon chưa có tọa độ
    addPlaceButtons(withoutPos);

    // Cập nhật hint
    if (!mapImageUrl) {
        document.getElementById('map-hint').textContent =
            'Di tích chưa có ảnh sơ đồ (mapImageUrl trống). '
            + 'Cập nhật ảnh sơ đồ trong trang Quản lý Di tích, rồi quay lại.';
    } else if (withoutPos.length > 0) {
        document.getElementById('map-hint').textContent =
            withoutPos.length + ' beacon chưa có vị trí – bấm "📍 Đặt trên bản đồ" trong bảng bên dưới, rồi bấm vào sơ đồ.';
    } else if (withPos.length > 0) {
        document.getElementById('map-hint').textContent =
            'Kéo ghim để di chuyển. Thả chuột → tự lưu.';
    } else {
        document.getElementById('map-hint').textContent =
            'Không có beacon nào thuộc di tích này.';
    }
}

// ── Marker beacon ─────────────────────────────────────────────────────────────

function beaconColor(beacon) {
    if (beacon.status === 'INACTIVE') return '#888888';
    if (beacon.batteryLevel != null && beacon.batteryLevel < 20) return '#c0392b';
    return '#27ae60';
}

function addBeaconMarker(beacon) {
    if (!floorplanMap) return;
    var latlng = L.latLng(beacon.posY, beacon.posX); // CRS.Simple: lat=Y, lng=X
    var color = beaconColor(beacon);

    var icon = L.divIcon({
        html: '<div style="width:20px;height:20px;border-radius:50%;'
            + 'background:' + color + ';border:3px solid #fff;'
            + 'box-shadow:0 2px 6px rgba(0,0,0,.35);cursor:grab;"></div>',
        iconSize: [20, 20], iconAnchor: [10, 10], className: '',
    });

    var marker = L.marker(latlng, { draggable: true, icon: icon }).addTo(floorplanMap);

    var artifactLabel = beacon.artifactName
        ? escHtml(beacon.artifactName)
        : '<span style="color:#999">Chưa gán hiện vật</span>';
    var battLabel = beacon.batteryLevel != null
        ? beacon.batteryLevel + '%' + (beacon.batteryLevel < 20 ? ' ⚠️' : '')
        : '—';

    function popupHtml(px, py) {
        return '<strong>Beacon #' + escHtml(String(beacon.id)) + '</strong><br>'
            + 'Major ' + escHtml(String(beacon.major))
            + ' / Minor ' + escHtml(String(beacon.minor)) + '<br>'
            + 'Hiện vật: ' + artifactLabel + '<br>'
            + 'Pin: ' + battLabel + '<br>'
            + 'Trạng thái: ' + escHtml(STATUS_LABEL[beacon.status] || beacon.status) + '<br>'
            + '<small style="color:#999">X=' + Number(px).toFixed(1)
            + ' Y=' + Number(py).toFixed(1) + '</small>';
    }

    marker.bindPopup(popupHtml(beacon.posX, beacon.posY));

    marker.on('dragend', function () {
        var pos = marker.getLatLng();
        var newX = pos.lng, newY = pos.lat;
        marker.setPopupContent(popupHtml(newX, newY));
        saveBeaconPosition(beacon.id, newX, newY);
    });

    floorplanMarkers.push(marker);
}

// ── Lưu vị trí beacon ────────────────────────────────────────────────────────

async function saveBeaconPosition(id, posX, posY) {
    // findBeacon so sánh string, ép kiểu để chắc chắn match dù API trả number hay string
    var beacon = beacons.find(function (b) { return Number(b.id) === Number(id); });
    if (!beacon || !beacon.uuid || beacon.siteId == null) {
        alert('Không tìm thấy dữ liệu beacon #' + id + ' để lưu vị trí.');
        return;
    }
    var payload = {
        uuid: beacon.uuid,
        major: beacon.major,
        minor: beacon.minor,
        siteId: Number(beacon.siteId),
        artifactId: beacon.artifactId != null ? Number(beacon.artifactId) : null,
        posX: Math.round(posX * 10) / 10,
        posY: Math.round(posY * 10) / 10,
        batteryLevel: beacon.batteryLevel != null ? Number(beacon.batteryLevel) : null,
        status: beacon.status || 'ACTIVE',
    };
    try {
        var resp = await fetch(API_BASE + '/api/ibeacons/' + id, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });
        if (!resp.ok) {
            var msg = await readMessage(resp, function () {
                return 'Lưu vị trí thất bại (HTTP ' + resp.status + ')';
            });
            alert('Lưu vị trí beacon #' + id + ' thất bại: ' + msg);
            return;
        }
        // Cập nhật cache local ngay, không cần reload toàn bộ
        var idx = beacons.findIndex(function (b) { return Number(b.id) === Number(id); });
        if (idx >= 0) {
            beacons[idx] = Object.assign({}, beacons[idx], {
                posX: payload.posX, posY: payload.posY,
            });
        }
    } catch (e) {
        alert('Lỗi kết nối khi lưu vị trí: ' + e.message);
    }
}

// ── Beacon chưa có tọa độ: thêm nút vào bảng ────────────────────────────────

function addPlaceButtons(unplaced) {
    if (unplaced.length === 0) return;
    var rows = document.querySelectorAll('#beacon-body tr');
    unplaced.forEach(function (beacon) {
        rows.forEach(function (row) {
            var cells = row.querySelectorAll('td');
            if (cells.length < 3) return;
            if (cells[0].textContent.trim() === beacon.uuid
                && parseInt(cells[1].textContent.trim()) === beacon.major
                && parseInt(cells[2].textContent.trim()) === beacon.minor
                && !row.querySelector('.ib-place-btn')) {
                var btn = document.createElement('button');
                btn.type = 'button';
                btn.className = 'btn fb-sm ib-place-btn';
                btn.style.cssText = 'background:var(--sage);color:#fff;margin-left:4px;';
                btn.textContent = '📍 Đặt trên bản đồ';
                btn.addEventListener('click', function () { startPlacingBeacon(beacon.id); });
                var actionGroup = cells[cells.length - 1].querySelector('.fb-actions');
                if (actionGroup) actionGroup.appendChild(btn);
            }
        });
    });
}

// ── Chế độ đặt vị trí ────────────────────────────────────────────────────────

function startPlacingBeacon(id) {
    placingBeaconId = id;
    var beacon = findBeacon(id);
    var label = beacon ? 'Major ' + beacon.major + '/Minor ' + beacon.minor : '#' + id;
    document.getElementById('map-hint').textContent =
        '🎯 Đang đặt beacon ' + label + ' – bấm vào sơ đồ để xác nhận vị trí.';
    document.getElementById('beacon-floorplan-map').style.cursor = 'crosshair';
    document.getElementById('beacon-floorplan-map').scrollIntoView({
        behavior: 'smooth', block: 'center',
    });
}

async function confirmPlaceBeacon(id, posX, posY) {
    placingBeaconId = null;
    document.getElementById('beacon-floorplan-map').style.cursor = '';
    document.getElementById('map-hint').textContent = 'Đang lưu…';
    await saveBeaconPosition(id, posX, posY);
    await loadBeacons();
    if (currentMapSiteId) renderFloorplanMap(currentMapSiteId);
}

// Khởi tạo map ngay khi script chạy (script ở cuối body nên DOM đã sẵn sàng)
if (window.L) {
    initFloorplanMap();
} else {
    // Leaflet chưa parse xong (hiếm gặp vì script đặt sau) – chờ thêm
    document.addEventListener('DOMContentLoaded', function () {
        if (window.L) initFloorplanMap();
    });
}
