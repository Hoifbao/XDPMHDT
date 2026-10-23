const STATUS_LABEL = {
    ACTIVE: 'Đang hoạt động',
    INACTIVE: 'Không hoạt động'
};

const STATUS_CLASS = {
    ACTIVE: 'pill',
    INACTIVE: 'fb-rejected'
};

const SAMPLE_BEACONS = [
    { id: 1, uuid: '550E8400-E29B-41D4-A716-446655440000', major: 1, minor: 1, artifactName: 'Trống đồng Đông Sơn', status: 'ACTIVE', updatedAt: '2026-10-05T08:00:00' },
    { id: 2, uuid: '550E8400-E29B-41D4-A716-446655440000', major: 1, minor: 2, artifactName: 'Khuê Văn Các', status: 'ACTIVE', updatedAt: '2026-10-05T08:10:00' },
    { id: 3, uuid: '550E8400-E29B-41D4-A716-446655440000', major: 1, minor: 3, artifactName: null, status: 'ACTIVE', updatedAt: '2026-10-06T09:00:00' },
    { id: 4, uuid: '550E8400-E29B-41D4-A716-446655440000', major: 2, minor: 1, artifactName: 'Bia Tiến sĩ', status: 'INACTIVE', updatedAt: '2026-10-07T15:30:00' }
];

// Bản sao để thay đổi lọc mà không làm hỏng dữ liệu mẫu gốc.
const beacons = SAMPLE_BEACONS.map(function (item) {
    return Object.assign({}, item);
});
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

function createActionsCell() {
    const cell = createCell();
    const group = document.createElement('div');
    group.className = 'fb-actions';

    const editButton = document.createElement('button');
    editButton.type = 'button';
    editButton.className = 'btn alt fb-sm';
    editButton.textContent = 'Sửa';
    group.appendChild(editButton);

    const deleteButton = document.createElement('button');
    deleteButton.type = 'button';
    deleteButton.className = 'btn fb-danger fb-sm';
    deleteButton.textContent = 'Xóa';
    group.appendChild(deleteButton);

    cell.appendChild(group);
    return cell;
}

function createRow(item) {
    const row = document.createElement('tr');

    row.appendChild(createCell('fb-nowrap')).textContent = item.uuid;
    row.appendChild(createCell()).textContent = item.major;
    row.appendChild(createCell()).textContent = item.minor;
    row.appendChild(createArtifactCell(item.artifactName));

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

render();
