// Trang quản lý phản hồi (Tường - Sprint 5): gọi feedback-service qua Gateway.
//   GET   /api/admin/feedbacks?artifactId=   – danh sách (mọi trạng thái), lọc theo hiện vật
//   PATCH /api/admin/feedbacks/{id}/status   – Duyệt (APPROVED) hoặc Ẩn (REJECTED)
//   GET   /api/artifacts                     – tên hiện vật cho cột "Hiện vật" và ô lọc
// Dùng PATCH vì Backend cũ chỉ nhận PATCH (feedback-service nhận cả PUT và PATCH),
// nên trang chạy được cả trước và sau khi Gateway chuyển /api/admin/feedbacks/** sang 8086.

// Địa chỉ Gateway (dùng chung cách cấu hình với WebAdmin/src/api/auth.js).
// auth.js đã bọc fetch(): TỰ gắn JWT và tự refresh khi 401 -> trang không phải tự làm.
const API_BASE = (window.SHAuth && window.SHAuth.apiBase)
    || (function () {
        try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
        catch (e) { return 'http://localhost:8080'; }
    })();

const STATUS_LABEL = {
    PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt',
    REJECTED: 'Đã ẩn'
};

const STATUS_CLASS = {
    PENDING: 'fb-pending',
    APPROVED: 'fb-approved',
    REJECTED: 'fb-rejected'
};

const MAX_RATING = 5;

let feedbacks = [];
let artifactNames = {};
let currentFilter = 'ALL';
let currentArtifactId = '';
let detailId = null;
let busy = false;

const bodyElement = document.getElementById('feedback-body');
const emptyElement = document.getElementById('feedback-empty');
const tableElement = document.getElementById('feedback-table');
const filterBar = document.getElementById('filter-bar');
const artifactFilter = document.getElementById('artifact-filter');
const errorElement = document.getElementById('feedback-error');
const sourceLabel = document.getElementById('data-source');
const modal = document.getElementById('detail-modal');

function pad(number) {
    return String(number).padStart(2, '0');
}

// Chuỗi ISO không có múi giờ được hiểu theo giờ địa phương
function formatDate(isoText) {
    const date = new Date(isoText);
    if (!isoText || Number.isNaN(date.getTime())) {
        return '';
    }
    return pad(date.getDate()) + '/' + pad(date.getMonth() + 1) + '/' + date.getFullYear()
        + ' ' + pad(date.getHours()) + ':' + pad(date.getMinutes());
}

function artifactLabel(artifactId) {
    return artifactNames[artifactId] || ('Hiện vật #' + artifactId);
}

// Du khách sửa đánh giá hoặc ADMIN đổi trạng thái thì updatedAt mới hơn createdAt
function isEdited(item) {
    return item.updatedAt && item.createdAt && new Date(item.updatedAt) > new Date(item.createdAt);
}

function showError(message) {
    errorElement.textContent = message;
    errorElement.hidden = !message;
}

// Đọc thông báo lỗi trong JSON {success:false, message} của service
async function errorMessage(response, fallback) {
    try {
        const body = await response.json();
        if (body && body.message) {
            return body.message;
        }
    } catch (e) { /* body không phải JSON */ }
    if (response.status === 401 || response.status === 403) {
        return 'Phiên đăng nhập hết hạn hoặc không đủ quyền ADMIN (mã ' + response.status + ').';
    }
    return fallback + ' (mã ' + response.status + ').';
}

function createCell(className) {
    const cell = document.createElement('td');
    if (className) {
        cell.className = className;
    }
    return cell;
}

function createStars(rating) {
    const wrapper = document.createElement('span');
    wrapper.className = 'fb-stars';
    wrapper.setAttribute('role', 'img');
    wrapper.setAttribute('aria-label', rating + ' trên ' + MAX_RATING + ' sao');
    for (let i = 1; i <= MAX_RATING; i++) {
        const star = document.createElement('span');
        star.className = i <= rating ? 'fb-star-on' : 'fb-star-off';
        star.textContent = '★';
        wrapper.appendChild(star);
    }
    return wrapper;
}

function createStatusPill(status) {
    const pill = document.createElement('span');
    pill.className = 'pill ' + (STATUS_CLASS[status] || '');
    pill.textContent = STATUS_LABEL[status] || status;
    return pill;
}

function createButton(label, className, onClick) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn fb-sm ' + className;
    button.textContent = label;
    button.disabled = busy;
    button.addEventListener('click', onClick);
    return button;
}

// Nút Duyệt / Ẩn theo trạng thái hiện tại (dùng cho cả bảng và modal chi tiết)
function appendStatusButtons(group, item) {
    if (item.status !== 'APPROVED') {
        group.appendChild(createButton('Duyệt', 'alt', function () {
            changeStatus(item.id, 'APPROVED');
        }));
    }
    if (item.status !== 'REJECTED') {
        group.appendChild(createButton('Ẩn', 'fb-danger', function () {
            changeStatus(item.id, 'REJECTED');
        }));
    }
}

// Nội dung do du khách nhập. Luôn gán bằng textContent (không dùng innerHTML) tránh chèn mã HTML
function createCommentCell(comment) {
    const cell = createCell('fb-comment');
    if (comment) {
        cell.textContent = comment;
    } else {
        cell.textContent = '(Không có nội dung)';
        cell.classList.add('muted');
    }
    return cell;
}

function createActionsCell(item) {
    const cell = createCell();
    const group = document.createElement('div');
    group.className = 'fb-actions';
    group.appendChild(createButton('Xem', '', function () {
        openDetail(item.id);
    }));
    appendStatusButtons(group, item);
    cell.appendChild(group);
    return cell;
}

function createRow(item) {
    const row = document.createElement('tr');

    row.appendChild(createCell('fb-nowrap')).textContent = item.reviewerName || item.userName || 'Khách';
    row.appendChild(createCell()).textContent = artifactLabel(item.artifactId);

    const starsCell = createCell('fb-nowrap');
    starsCell.appendChild(createStars(item.rating));
    row.appendChild(starsCell);

    row.appendChild(createCommentCell(item.comment));

    const statusCell = createCell('fb-nowrap');
    statusCell.appendChild(createStatusPill(item.status));
    row.appendChild(statusCell);

    const dateCell = row.appendChild(createCell('fb-nowrap'));
    dateCell.textContent = formatDate(item.createdAt);
    if (isEdited(item)) {
        const edited = document.createElement('div');
        edited.className = 'muted fb-small';
        edited.textContent = 'Cập nhật ' + formatDate(item.updatedAt);
        dateCell.appendChild(edited);
    }

    row.appendChild(createActionsCell(item));
    return row;
}

function countByStatus(status) {
    return feedbacks.filter(function (item) {
        return item.status === status;
    }).length;
}

// Điểm trung bình chỉ tính đánh giá đã duyệt (giống API rating-summary)
function renderStats() {
    const approved = feedbacks.filter(function (item) {
        return item.status === 'APPROVED';
    });
    let average = 0;
    if (approved.length > 0) {
        const sum = approved.reduce(function (acc, item) {
            return acc + item.rating;
        }, 0);
        average = sum / approved.length;
    }
    document.getElementById('stat-total').textContent = feedbacks.length;
    document.getElementById('stat-pending').textContent = countByStatus('PENDING');
    document.getElementById('stat-approved').textContent = approved.length;
    document.getElementById('stat-average').textContent = approved.length ? average.toFixed(1) : '–';
}

function renderFilterBar() {
    filterBar.querySelectorAll('button').forEach(function (button) {
        const filter = button.dataset.filter;
        const isActive = filter === currentFilter;
        const count = filter === 'ALL' ? feedbacks.length : countByStatus(filter);
        const baseLabel = filter === 'ALL' ? 'Tất cả' : STATUS_LABEL[filter];
        button.textContent = baseLabel + ' (' + count + ')';
        button.classList.toggle('active', isActive);
        button.setAttribute('aria-pressed', String(isActive));
    });
}

function renderTable() {
    const visible = feedbacks.filter(function (item) {
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
    if (detailId !== null) {
        renderDetail();
    }
}

// ───────────── Modal chi tiết ─────────────

function addDetailLine(list, label, value) {
    const term = document.createElement('dt');
    term.textContent = label;
    const desc = document.createElement('dd');
    if (value instanceof Node) {
        desc.appendChild(value);
    } else {
        desc.textContent = value;
    }
    list.appendChild(term);
    list.appendChild(desc);
}

function renderDetail() {
    const item = feedbacks.find(function (f) {
        return f.id === detailId;
    });
    if (!item) {
        closeDetail();
        return;
    }
    const list = document.getElementById('detail-list');
    list.replaceChildren();
    addDetailLine(list, 'Mã đánh giá', '#' + item.id);
    addDetailLine(list, 'Người gửi', item.reviewerName || item.userName || 'Khách');
    addDetailLine(list, 'Hiện vật', artifactLabel(item.artifactId));
    addDetailLine(list, 'Số sao', createStars(item.rating));
    addDetailLine(list, 'Bình luận', item.comment || '(Không có nội dung)');
    addDetailLine(list, 'Trạng thái', createStatusPill(item.status));
    addDetailLine(list, 'Ngày gửi', formatDate(item.createdAt));
    if (isEdited(item)) {
        addDetailLine(list, 'Cập nhật lần cuối', formatDate(item.updatedAt));
    }

    const actions = document.getElementById('detail-actions');
    actions.replaceChildren();
    appendStatusButtons(actions, item);
    actions.appendChild(createButton('Đóng', '', closeDetail));
}

function openDetail(feedbackId) {
    detailId = feedbackId;
    renderDetail();
    modal.hidden = false;
}

function closeDetail() {
    detailId = null;
    modal.hidden = true;
}

// ───────────── Gọi API ─────────────

// Tên hiện vật: lỗi thì vẫn chạy tiếp, cột hiện vật hiện "Hiện vật #id"
async function loadArtifacts() {
    try {
        const response = await fetch(API_BASE + '/api/artifacts');
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        const body = await response.json();
        const list = Array.isArray(body) ? body : (body && body.data);
        if (!Array.isArray(list)) {
            return;
        }
        artifactNames = {};
        list.forEach(function (artifact) {
            artifactNames[artifact.id] = artifact.name;
            const option = document.createElement('option');
            option.value = String(artifact.id);
            option.textContent = artifact.name;
            artifactFilter.appendChild(option);
        });
    } catch (error) {
        // Không có tên hiện vật vẫn duyệt được
    }
}

// Lọc trạng thái làm trên trình duyệt để các nút lọc luôn hiện đủ số đếm
async function loadFeedbacks() {
    sourceLabel.textContent = 'Đang tải...';
    let url = API_BASE + '/api/admin/feedbacks';
    if (currentArtifactId) {
        url += '?artifactId=' + encodeURIComponent(currentArtifactId);
    }
    try {
        const response = await fetch(url);
        if (!response.ok) {
            throw new Error(await errorMessage(response, 'Không tải được danh sách phản hồi'));
        }
        const body = await response.json();
        feedbacks = Array.isArray(body.data) ? body.data : [];
        sourceLabel.textContent = 'Dữ liệu từ API';
        showError('');
    } catch (error) {
        // Không quay về dữ liệu mẫu: tránh để ADMIN tưởng thao tác duyệt đã được lưu
        feedbacks = [];
        sourceLabel.textContent = 'Lỗi kết nối';
        showError(error instanceof TypeError
            ? 'Không kết nối được máy chủ (' + API_BASE + '). Hãy kiểm tra Gateway và feedback-service đã chạy chưa.'
            : error.message);
    }
    render();
}

async function changeStatus(feedbackId, newStatus) {
    if (busy) {
        return;
    }
    busy = true;
    render();
    try {
        const response = await fetch(API_BASE + '/api/admin/feedbacks/' + feedbackId + '/status', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        if (!response.ok) {
            throw new Error(await errorMessage(response, 'Không cập nhật được trạng thái'));
        }
        const body = await response.json();
        const updated = body && body.data;
        feedbacks = feedbacks.map(function (item) {
            if (item.id !== feedbackId) {
                return item;
            }
            return updated ? Object.assign({}, item, updated) : Object.assign({}, item, { status: newStatus });
        });
        showError('');
        if (window.SHFeedbackBadge) {
            window.SHFeedbackBadge.refresh();
        }
    } catch (error) {
        showError(error instanceof TypeError
            ? 'Không kết nối được máy chủ (' + API_BASE + '), trạng thái chưa được lưu.'
            : error.message);
    } finally {
        busy = false;
        render();
    }
}

// ───────────── Sự kiện ─────────────

filterBar.addEventListener('click', function (event) {
    const button = event.target.closest('button[data-filter]');
    if (!button) {
        return;
    }
    currentFilter = button.dataset.filter;
    render();
});

artifactFilter.addEventListener('change', function () {
    currentArtifactId = artifactFilter.value;
    loadFeedbacks();
});

// Bấm ra ngoài khung hoặc nhấn Esc để đóng modal
modal.addEventListener('click', function (event) {
    if (event.target === modal) {
        closeDetail();
    }
});

document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape' && !modal.hidden) {
        closeDetail();
    }
});

loadArtifacts().then(loadFeedbacks);
