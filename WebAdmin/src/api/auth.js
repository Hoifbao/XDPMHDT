/**
 * Smart Heritage - Web Admin: gọi API xác thực của Backend (AuthController).
 * Dùng chung cho login.html, register.html, profile.html.
 *
 * Phiên đăng nhập lưu trong sessionStorage (đóng tab là hết).
 * JWT + phân quyền RBAC làm ở Sprint 3.
 */
(function () {
  // Đổi địa chỉ Backend: thêm ?api=http://<ip>:8080 vào URL một lần (được nhớ trong localStorage)
  const DEFAULT_API = 'http://localhost:8080';
  const SESSION_KEY = 'sh_auth_user';

  function readStorage(store, key) {
    try { return store.getItem(key); } catch (e) { return null; }
  }
  function writeStorage(store, key, value) {
    try {
      if (value === null) store.removeItem(key);
      else store.setItem(key, value);
    } catch (e) { /* trình duyệt chặn storage: bỏ qua */ }
  }

  const apiFromUrl = new URLSearchParams(window.location.search).get('api');
  if (apiFromUrl) writeStorage(window.localStorage, 'sh_api_base', apiFromUrl);
  const API_BASE = (readStorage(window.localStorage, 'sh_api_base') || DEFAULT_API).replace(/\/+$/, '');

  async function request(method, path, body) {
    let response;
    try {
      response = await fetch(API_BASE + path, {
        method: method,
        headers: body ? { 'Content-Type': 'application/json' } : {},
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch (e) {
      return { error: 'Không kết nối được máy chủ (' + API_BASE + '). Hãy kiểm tra Backend đã chạy chưa.' };
    }

    let data = {};
    try { data = await response.json(); } catch (e) { /* body không phải JSON */ }

    if (response.ok) return data;
    return { error: (data && data.message) || ('Có lỗi xảy ra (mã ' + response.status + ').') };
  }

  function saveUser(user) {
    writeStorage(window.sessionStorage, SESSION_KEY, user ? JSON.stringify(user) : null);
  }

  function getUser() {
    const raw = readStorage(window.sessionStorage, SESSION_KEY);
    if (!raw) return null;
    try { return JSON.parse(raw); } catch (e) { return null; }
  }

  window.SHAuth = {
    apiBase: API_BASE,

    register: function (username, email, password, fullName) {
      return request('POST', '/api/auth/register', {
        username: username.trim(),
        email: email.trim(),
        password: password,
        fullName: fullName.trim(),
      });
    },

    login: async function (username, password) {
      const result = await request('POST', '/api/auth/login', {
        username: username.trim(),
        password: password,
      });
      if (!result.error) {
        delete result.message;
        saveUser(result);
      }
      return result;
    },

    refreshProfile: async function () {
      const user = getUser();
      if (!user) return { error: 'Bạn chưa đăng nhập.' };
      const result = await request('GET', '/api/auth/users/' + encodeURIComponent(user.userId));
      if (!result.error) saveUser(result);
      return result;
    },

    currentUser: getUser,

    logout: function () {
      saveUser(null);
    },

    /** Gọi ở đầu trang cần đăng nhập: chưa đăng nhập thì chuyển về login.html */
    requireLogin: function (loginPage) {
      const user = getUser();
      if (!user) window.location.replace(loginPage || 'login.html');
      return user;
    },
  };
})();
