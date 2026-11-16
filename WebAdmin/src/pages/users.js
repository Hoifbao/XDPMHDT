// Quản lý người dùng (Bảo - Sprint 7). API: GET /api/admin/users, PATCH /api/admin/users/{id}/role|status
// qua Gateway 8080 (auth-service). fetch() đã được auth.js gắn token và tự refresh.
(function () {
    const API_BASE = (window.SHAuth && window.SHAuth.apiBase) || 'http://localhost:8080';
    const PAGE_SIZE = 20;
    const ROLE_LABEL = { VISITOR: 'Du khách', ADMIN: 'Quản trị viên' };

    const state = { q: '', role: '', page: 0, totalPages: 1 };
    const me = window.SHAuth && window.SHAuth.currentUser ? window.SHAuth.currentUser() : null;
    const myId = me ? Number(me.userId) : null;

    const el = function (id) { return document.getElementById(id); };

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function formatDate(value) {
        if (!value) return '';
        const d = new Date(value);
        return isNaN(d.getTime()) ? value : d.toLocaleDateString('vi-VN');
    }

    function showNote(message, isError) {
        const note = el('users-note');
        note.textContent = message;
        note.classList.toggle('fb-error', !!isError);
        note.hidden = !message;
    }

    /** Gọi API, trả { ok, data, message }. Lỗi mạng cũng trả về object, không ném lỗi. */
    async function callApi(method, path, body) {
        try {
            const response = await fetch(API_BASE + path, {
                method: method,
                headers: body ? { 'Content-Type': 'application/json' } : {},
                body: body ? JSON.stringify(body) : undefined,
            });
            let data = {};
            try { data = await response.json(); } catch (e) { /* không có body JSON */ }
            return { ok: response.ok, data: data, message: data && data.message };
        } catch (e) {
            return { ok: false, data: {}, message: 'Không kết nối được máy chủ (' + API_BASE + '). Kiểm tra Gateway và auth-service đã chạy chưa.' };
        }
    }

    function render(page) {
        const rows = page.content || [];
        el('users-empty').hidden = rows.length > 0;
        el('stat-total').textContent = page.totalElements || 0;
        el('stat-admin').textContent = rows.filter(function (u) { return u.role === 'ADMIN'; }).length;
        el('stat-locked').textContent = rows.filter(function (u) { return u.locked; }).length;

        el('users-body').innerHTML = rows.map(function (u) {
            const self = Number(u.userId) === myId;
            const roleOptions = ['VISITOR', 'ADMIN'].map(function (r) {
                return '<option value="' + r + '"' + (u.role === r ? ' selected' : '') + '>' + ROLE_LABEL[r] + '</option>';
            }).join('');
            const status = u.locked
                ? '<span class="pill fb-rejected">Đã khoá</span>'
                : '<span class="pill">Hoạt động</span>';
            const action = u.locked
                ? '<button type="button" data-action="unlock" data-id="' + u.userId + '">Mở khoá</button>'
                : '<button type="button" data-action="lock" data-id="' + u.userId + '"' + (self ? ' disabled title="Không thể tự khoá chính mình"' : '') + '>Khoá</button>';
            return '<tr>'
                + '<td>' + escapeHtml(u.userId) + '</td>'
                + '<td>' + escapeHtml(u.username) + (self ? '<span class="pill us-you">Bạn</span>' : '') + '</td>'
                + '<td>' + escapeHtml(u.fullName) + '</td>'
                + '<td>' + escapeHtml(u.email) + '</td>'
                + '<td><select class="us-role" data-id="' + u.userId + '" data-current="' + escapeHtml(u.role) + '"'
                + (self ? ' disabled title="Không thể tự đổi vai trò của chính mình"' : '') + ' aria-label="Vai trò">'
                + roleOptions + '</select></td>'
                + '<td>' + status + '</td>'
                + '<td>' + escapeHtml(formatDate(u.createdAt)) + '</td>'
                + '<td class="nav">' + action + '</td>'
                + '</tr>';
        }).join('');

        state.totalPages = Math.max(page.totalPages || 1, 1);
        el('page-info').textContent = 'Trang ' + (state.page + 1) + '/' + state.totalPages;
        el('page-prev').disabled = state.page <= 0;
        el('page-next').disabled = state.page + 1 >= state.totalPages;
    }

    async function load() {
        const params = new URLSearchParams({ page: String(state.page), size: String(PAGE_SIZE) });
        if (state.q) params.set('q', state.q);
        if (state.role) params.set('role', state.role);
        const result = await callApi('GET', '/api/admin/users?' + params.toString());
        if (!result.ok) {
            render({ content: [], totalElements: 0, totalPages: 1 });
            showNote(result.message || 'Không tải được danh sách người dùng.', true);
            return;
        }
        render(result.data);
    }

    async function changeStatus(id, lock) {
        const question = lock
            ? 'Khoá tài khoản này? Người dùng sẽ bị đăng xuất khỏi mọi thiết bị và không đăng nhập được.'
            : 'Mở khoá tài khoản này?';
        if (!window.confirm(question)) return;
        const result = await callApi('PATCH', '/api/admin/users/' + id + '/status', { locked: lock });
        showNote(result.message || (result.ok ? 'Đã cập nhật.' : 'Không cập nhật được trạng thái.'), !result.ok);
        await load();
    }

    async function changeRole(select) {
        const id = select.dataset.id;
        const role = select.value;
        if (!window.confirm('Đổi vai trò tài khoản #' + id + ' thành "' + ROLE_LABEL[role] + '"?')) {
            select.value = select.dataset.current;
            return;
        }
        const result = await callApi('PATCH', '/api/admin/users/' + id + '/role', { role: role });
        showNote(result.message || (result.ok ? 'Đã đổi vai trò.' : 'Không đổi được vai trò.'), !result.ok);
        await load();
    }

    document.addEventListener('DOMContentLoaded', function () {
        el('users-filter').addEventListener('submit', function (event) {
            event.preventDefault();
            state.q = el('users-q').value.trim();
            state.role = el('users-role').value;
            state.page = 0;
            showNote('');
            load();
        });
        el('page-prev').addEventListener('click', function () {
            if (state.page > 0) { state.page -= 1; load(); }
        });
        el('page-next').addEventListener('click', function () {
            if (state.page + 1 < state.totalPages) { state.page += 1; load(); }
        });
        el('users-body').addEventListener('click', function (event) {
            const button = event.target.closest('button[data-action]');
            if (button && !button.disabled) {
                changeStatus(button.dataset.id, button.dataset.action === 'lock');
            }
        });
        el('users-body').addEventListener('change', function (event) {
            if (event.target.matches('select.us-role')) {
                changeRole(event.target);
            }
        });
        load();
    });
})();
