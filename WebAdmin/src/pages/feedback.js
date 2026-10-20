const STATUS_LABEL = {
    PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt',
    REJECTED: 'Từ chối'
};

const STATUS_CLASS = {
    PENDING: 'fb-pending',
    APPROVED: 'fb-approved',
    REJECTED: 'fb-rejected'
};

const MAX_RATING = 5;

const SAMPLE_FEEDBACKS = [
    { id: 1, userName: 'Nguyễn Văn An', artifactName: 'Khuê Văn Các', rating: 5, comment: 'Khuê Văn Các rất đẹp, phần thuyết minh dễ hiểu.', status: 'APPROVED', createdAt: '2026-10-05T09:30:00' },
    { id: 2, userName: 'Nguyễn Văn An', artifactName: 'Bia Tiến sĩ', rating: 4, comment: 'Thông tin về bia Tiến sĩ hay, mong có thêm video.', status: 'APPROVED', createdAt: '2026-10-05T09:45:00' },
    { id: 3, userName: 'Trần Thị Mai', artifactName: 'Khuê Văn Các', rating: 3, comment: 'Tín hiệu beacon hơi chậm khi đông người.', status: 'PENDING', createdAt: '2026-10-06T14:20:00' },
    { id: 4, userName: 'John Smith', artifactName: 'Đoan Môn', rating: 5, comment: 'The English audio guide is clear and helpful.', status: 'APPROVED', createdAt: '2026-10-07T10:50:00' },
    { id: 5, userName: 'Trần Thị Mai', artifactName: 'Bia Tiến sĩ', rating: 2, comment: null, status: 'PENDING', createdAt: '2026-10-06T14:40:00' },
    { id: 6, userName: 'John Smith', artifactName: 'Khuê Văn Các', rating: 1, comment: 'Nội dung quảng cáo không liên quan đến hiện vật.', status: 'REJECTED', createdAt: '2026-10-07T11:00:00' }
];

// Bản sao để thay đổi trạng thái mà không làm hỏng dữ liệu mẫu gốc.
let feedbacks = SAMPLE_FEEDBACKS.map(function (item) {
    return Object.assign({}, item);
});
let currentFilter = 'ALL';

const bodyElement = document.getElementById('feedback-body');
const emptyElement = document.getElementById('feedback-empty');
const tableElement = document.getElementById('feedback-table');
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
    pill.className = 'pill ' + STATUS_CLASS[status];
    pill.textContent = STATUS_LABEL[status];
    return pill;
}

function createActionButton(label, className, feedbackId, newStatus) {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'btn fb-sm ' + className;
    button.textContent = label;
    button.addEventListener('click', function () {
        changeStatus(feedbackId, newStatus);
    });
    return button;
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
    if (item.status !== 'APPROVED') {
        group.appendChild(createActionButton('Duyệt', 'alt', item.id, 'APPROVED'));
    }
    if (item.status !== 'REJECTED') {
        group.appendChild(createActionButton('Từ chối', 'fb-danger', item.id, 'REJECTED'));
    }
    cell.appendChild(group);
    return cell;
}

function createRow(item) {
    const row = document.createElement('tr');

    row.appendChild(createCell('fb-nowrap')).textContent = item.userName;
    row.appendChild(createCell()).textContent = item.artifactName;

    const starsCell = createCell('fb-nowrap');
    starsCell.appendChild(createStars(item.rating));
    row.appendChild(starsCell);

    row.appendChild(createCommentCell(item.comment));

    const statusCell = createCell('fb-nowrap');
    statusCell.appendChild(createStatusPill(item.status));
    row.appendChild(statusCell);

    row.appendChild(createCell('fb-nowrap')).textContent = formatDate(item.createdAt);
    row.appendChild(createActionsCell(item));

    return row;
}

function countByStatus(status) {
    return feedbacks.filter(function (item) {
        return item.status === status;
    }).length;
}

function renderStats() {
    const total = feedbacks.length;
    let average = 0;
    if (total > 0) {
        const sum = feedbacks.reduce(function (acc, item) {
            return acc + item.rating;
        }, 0);
        average = sum / total;
    }
    document.getElementById('stat-total').textContent = total;
    document.getElementById('stat-pending').textContent = countByStatus('PENDING');
    document.getElementById('stat-approved').textContent = countByStatus('APPROVED');
    document.getElementById('stat-average').textContent = average.toFixed(1);
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
}

function changeStatus(feedbackId, newStatus) {
    const target = feedbacks.find(function (item) {
        return item.id === feedbackId;
    });
    if (!target) {
        return;
    }
    target.status = newStatus;
    render();
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