const STATUS_LABEL = {
    ACTIVE: 'Đang hoạt động',
    INACTIVE: 'Không hoạt động'
};

const STATUS_CLASS = {
    ACTIVE: 'pill',
    INACTIVE: 'fb-rejected'
};

// Địa chỉ Backend (dùng chung cách cấu hình với WebAdmin/src/api/auth.js)
const API_BASE = (function () {
    try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
    catch (e) { return 'http://localhost:8080'; }
})();

// Dữ liệu mẫu khớp với bảng ibeacons trong Infra/init_db.sql.
// Chỉ dùng khi không gọi được API (Backend chưa chạy).
const SAMPLE_BEACONS = [
    { id: 1, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 1, siteName: 'Văn Miếu - Quốc Tử Giám', artifactName: 'Khuê Văn Các', batteryLevel: 87, status: 'ACTIVE', updatedAt: '2026-10-05T08:00:00' },
    { id: 2, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 2, siteName: 'Văn Miếu - Quốc Tử Giám', artifactName: 'Bia Tiến sĩ', batteryLevel: 64, status: 'ACTIVE', updatedAt: '2026-10-05T08:10:00' },
    { id: 3, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 2, minor: 1, siteName: 'Hoàng thành Thăng Long', artifactName: 'Đoan Môn', batteryLevel: 92, status: 'ACTIVE', updatedAt: '2026-10-06T09:00:00' },
    { id: 4, uuid: 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', major: 1, minor: 3, siteName: 'Văn Miếu - Quốc Tử Giám', artifactName: null, batteryLevel: 15, status: 'INACTIVE', updatedAt: '2026-10-07T15:30:00' }
];

// Danh sách đang hiển thị (lấy từ API, lỗi thì dùng dữ liệu mẫu)
let beacons = [];
let currentFilter = 'ALL';

const bodyElement = document.getElementById('beacon-body');
const emptyElement = document.getElementById('beacon-empty');
const tableElement = document.getElementById('beacon-table');
const filterBar = document.getElementById('filter-bar');

function pad(number) {
    return String(number).padStart(2, '0');
}

// Chuỗi ISO không có múi giờ được hiểu theo giờ địa phương
function formatDate(isoText) {
    const date = new Date(isoText);
    if (Number.isNaN(date.getTime())) {
        return '';
    }
    return pad(date.getDate()) + '/' + pad(date.getMonth() + 1) + '/' + date.getFullYear()
        + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
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

function createActionsCell() {
    const cell = createCell();
    const group = document.createElement('div');
    group.className = 'fb-actions';

    const editButton = document.createElement('button');
    editButton.type = 'button';
    editButton.className = 'btn alt fb-sm';
    editButton.textContent = 'Sửa';
    editButton.disabled = true;
    editButton.title = 'Làm ở tuần 4';
    group.appendChild(editButton);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn fb-danger fb-sm';
    deleteButton.textContent = 'Xóa';
    deleteButton.disabled = true;
    deleteButton.title = 'Làm ở tuần 4';
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

    row.appendChild(createCell('fb-nowrap')).textContent = formatDate(item.updatedAt);
    row.appendChild(createActionsCell());

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
    document.getElementById('stat-inactive').textContent = countByStatus('INACTIVE');
    document.getElementById('stat-unassigned').textContent = beacons.filter(function (item) {
        return !item.artifactName;
    }).length;
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
}

function renderTable() {
    const visible = beacons.filter(function (item) {
        return currentFilter === 'ALL' || item.status === currentFilter;
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
    renderFilterBar();
    renderTable();
}

filterBar.addEventListener('click', function (event) {
    const button = event.target.closest('button[data-filter]');
    if (!button) {
        return;
    }
    currentFilter = button.dataset.filter;
    render();
});

// Gọi GET /api/ibeacons; Backend chưa chạy thì hiện dữ liệu mẫu
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
        sourceLabel.textContent = 'Dữ liệu mẫu (chưa kết nối Backend)';
    }
    render();
}

loadBeacons();
