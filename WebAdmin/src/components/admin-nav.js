// Menu chung cho mọi trang Web Admin (Bảo - Sprint 7).
// Cách dùng: nhúng SAU auth.js, TRƯỚC feedback-pending-badge.js:
//     <script src="../components/admin-nav.js" defer></script>      (trang trong src/pages/)
//     <script src="src/components/admin-nav.js" defer></script>     (admin.html ở thư mục WebAdmin)
// File tự dựng thanh menu trong .topbar của trang, đánh dấu trang đang mở, và thay menu cũ
// (khối .nav hoặc link "Bảng quản trị") để mọi trang có cùng một menu.
// Chỉ hiện cho ADMIN; người dùng thường (vd. VISITOR mở profile.html) giữ nguyên trang.
(function () {
    const MENU = [
        { file: 'dashboard.html', label: 'Tổng quan' },
        { file: 'heritage-sites.html', label: 'Di tích' },
        { file: 'artifacts.html', label: 'Hiện vật' },
        { file: 'ibeacons.html', label: 'Beacon' },
        { file: 'feedback.html', label: 'Phản hồi' },
        { file: 'users.html', label: 'Người dùng' },
        { file: 'profile.html', label: 'Tài khoản' },
    ];

    // Thư mục src/pages tính từ chính file này (src/components/admin-nav.js), nên đúng cả khi
    // trang mở bằng file:// hay Live Server, và cả khi trang nằm ở thư mục khác (admin.html).
    const scriptEl = document.currentScript;
    const PAGES_BASE = scriptEl ? new URL('../pages/', scriptEl.src).href : './';

    function isAdmin() {
        const user = window.SHAuth && window.SHAuth.currentUser ? window.SHAuth.currentUser() : null;
        return !!(user && user.role === 'ADMIN');
    }

    function currentFile() {
        const path = window.location.pathname;
        return path.substring(path.lastIndexOf('/') + 1).toLowerCase();
    }

    function buildNav() {
        const nav = document.createElement('nav');
        nav.className = 'nav';
        nav.id = 'admin-nav';
        nav.setAttribute('aria-label', 'Menu quản trị');
        const here = currentFile();

        MENU.forEach(function (item) {
            const link = document.createElement('a');
            link.href = PAGES_BASE + item.file;
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = item.label;
            if (here === item.file) {
                button.className = 'active';
                link.setAttribute('aria-current', 'page');
            }
            link.appendChild(button);
            nav.appendChild(link);
        });

        const logout = document.createElement('button');
        logout.type = 'button';
        logout.textContent = 'Đăng xuất';
        logout.addEventListener('click', async function () {
            if (window.SHAuth && window.SHAuth.logout) {
                await window.SHAuth.logout();
            }
            window.location.href = PAGES_BASE + 'login.html';
        });
        nav.appendChild(logout);
        return nav;
    }

    // Trang không nạp theme.css (vd. artifacts.html chỉ dùng artifacts.css) thì menu mất kiểu.
    // Tự thêm kiểu riêng cho #admin-nav, chép đúng .nav của theme.css; màu dự phòng khi trang không có biến CSS.
    function injectStyle() {
        if (document.getElementById('admin-nav-style')) {
            return;
        }
        const style = document.createElement('style');
        style.id = 'admin-nav-style';
        style.textContent =
            '#admin-nav{display:flex;gap:8px;flex-wrap:wrap}' +
            '#admin-nav a{text-decoration:none}' +
            '#admin-nav button{font:inherit;cursor:pointer;border:1px solid var(--line,#e4e6df);' +
            'background:#fff;border-radius:999px;padding:9px 15px;color:var(--muted,#68746c)}' +
            '#admin-nav button.active{background:var(--ink,#17211b);color:#fff;border-color:var(--ink,#17211b)}';
        document.head.appendChild(style);
    }

    function mount() {
        if (!isAdmin() || document.getElementById('admin-nav')) {
            return;
        }
        injectStyle();
        const topbar = document.querySelector('.topbar');
        if (!topbar) {
            return;
        }
        // Bỏ menu cũ của từng trang: khối .nav và link rời (vd. "Bảng quản trị" trên profile.html).
        // Giữ các phần khác như nhãn trạng thái (.pill) của trang.
        Array.prototype.slice.call(topbar.children).forEach(function (child) {
            const isOldNav = child.classList.contains('nav');
            const isLooseLink = child.tagName === 'A' && !child.classList.contains('brand');
            if (isOldNav || isLooseLink) {
                child.remove();
            }
        });
        const brand = topbar.querySelector('.brand');
        const nav = buildNav();
        if (brand && brand.nextSibling) {
            topbar.insertBefore(nav, brand.nextSibling);
        } else {
            topbar.appendChild(nav);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', mount);
    } else {
        mount();
    }
})();
