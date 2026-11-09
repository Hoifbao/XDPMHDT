// Số đánh giá chờ duyệt cạnh nút "Phản hồi" trên menu (Tường - Tuần 6).
// Trang chỉ cần nhúng file này sau auth.js. Không có đánh giá chờ duyệt hoặc gọi API lỗi
// thì menu giữ nguyên, không ảnh hưởng phần còn lại của trang.
(function () {
    const API_BASE = (window.SHAuth && window.SHAuth.apiBase) || 'http://localhost:8080';

    function addBadge(button, pending) {
        const badge = document.createElement('span');
        badge.className = 'pill';
        badge.style.marginLeft = '6px';
        badge.title = pending + ' đánh giá đang chờ duyệt';
        badge.textContent = String(pending);
        button.appendChild(badge);
    }

    async function showPendingCount() {
        const button = document.querySelector('a[href$="feedback.html"] button');
        if (!button) {
            return;
        }
        try {
            const response = await fetch(API_BASE + '/api/admin/feedbacks/pending-count');
            if (!response.ok) {
                return;
            }
            const body = await response.json();
            const pending = body && body.data ? Number(body.data.pending) : 0;
            if (pending > 0) {
                addBadge(button, pending);
            }
        } catch (error) {
            // Menu vẫn dùng được khi không lấy được số
        }
    }

    showPendingCount();
})();
