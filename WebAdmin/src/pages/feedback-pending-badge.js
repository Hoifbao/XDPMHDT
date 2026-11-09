// Số đánh giá chờ duyệt cạnh nút "Phản hồi" trên menu (Tường - Tuần 6).
// Trang chỉ cần nhúng file này sau auth.js. Số được tải lại khi mở trang, khi quay lại trang/tab,
// và khi trang gọi window.SHFeedbackBadge.refresh() (feedback.js gọi sau khi duyệt hoặc ẩn).
// Không có đánh giá chờ duyệt hoặc gọi API lỗi thì không hiện số, phần còn lại của trang vẫn chạy.
(function () {
    const API_BASE = (window.SHAuth && window.SHAuth.apiBase) || 'http://localhost:8080';
    const BADGE_ID = 'feedback-pending-badge';

    function findMenuItem() {
        const link = document.querySelector('a[href$="feedback.html"]');
        return link && (link.querySelector('button') || link);
    }

    async function fetchPendingCount() {
        try {
            const response = await fetch(API_BASE + '/api/admin/feedbacks/pending-count');
            if (!response.ok) {
                return 0;
            }
            const body = await response.json();
            return body && body.data ? Number(body.data.pending) : 0;
        } catch (error) {
            return 0;
        }
    }

    function renderBadge(menuItem, pending) {
        let badge = document.getElementById(BADGE_ID);
        if (pending <= 0) {
            if (badge) {
                badge.remove();
            }
            return;
        }
        if (!badge) {
            badge = document.createElement('span');
            badge.id = BADGE_ID;
            badge.className = 'pill';
            badge.style.marginLeft = '6px';
            menuItem.appendChild(badge);
        }
        badge.title = pending + ' đánh giá đang chờ duyệt';
        badge.textContent = String(pending);
    }

    async function refresh() {
        const menuItem = findMenuItem();
        if (menuItem) {
            renderBadge(menuItem, await fetchPendingCount());
        }
    }

    window.SHFeedbackBadge = { refresh: refresh };
    window.addEventListener('pageshow', refresh);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            refresh();
        }
    });
})();
