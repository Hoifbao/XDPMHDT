/**
 * Smart Heritage - Web Admin: xác thực JWT dùng chung cho mọi trang (Bảo - Sprint 3).
 *
 * Cách dùng trên 1 trang quản trị: đặt trong <head>, TRƯỚC các file JS của trang:
 *     <script src="../api/auth.js" data-guard="admin"></script>
 *   - data-guard="admin": chưa đăng nhập hoặc không phải ADMIN -> tự chuyển về login.html
 *   - data-guard="login": chỉ cần đăng nhập (vd: profile.html)
 *   - không có data-guard: trang công khai (login.html, register.html)
 *
 * Sau khi nạp file này, mọi lệnh fetch() gọi tới Backend (API_BASE) sẽ TỰ ĐỘNG:
 *   - gắn header "Authorization: Bearer <accessToken>"
 *   - gặp 401 (token hết hạn) thì gọi /api/auth/refresh 1 lần rồi gửi lại request
 *   - refresh thất bại thì xóa phiên và chuyển về trang đăng nhập
 * => Các trang CMS (artifacts.js, ibeacons.js, ...) KHÔNG phải sửa code fetch.
 *
 * Lưu ý: chặn ở trình duyệt chỉ để trải nghiệm người dùng; bảo mật thật nằm ở Backend
 * (SecurityConfig trả 401/403 nếu không có quyền).
 */
(function () {
  // Đổi địa chỉ Backend: thêm ?api=http://<ip>:8080 vào URL một lần (được nhớ trong localStorage)
  const DEFAULT_API = 'http://localhost:8080';
  const USER_KEY = 'sh_auth_user';
  const ACCESS_KEY = 'sh_access_token';
  const REFRESH_KEY = 'sh_refresh_token';
  const ROLE_ADMIN = 'ADMIN';
  // Các API xác thực không gắn token và không tự refresh khi lỗi
  const AUTH_PATHS = ['/api/auth/login', '/api/auth/register', '/api/auth/refresh', '/api/auth/logout'];

  const store = window.localStorage; // dùng chung giữa các tab; đăng xuất là xóa hết

  function readStorage(key) {
    try { return store.getItem(key); } catch (e) { return null; }
  }
  function writeStorage(key, value) {
    try {
      if (value === null || value === undefined) store.removeItem(key);
      else store.setItem(key, value);
    } catch (e) { /* trình duyệt chặn storage: bỏ qua */ }
  }

  const apiFromUrl = new URLSearchParams(window.location.search).get('api');
  if (apiFromUrl) writeStorage('sh_api_base', apiFromUrl);
  const API_BASE = (readStorage('sh_api_base') || DEFAULT_API).replace(/\/+$/, '');

  // Đường dẫn tới login.html, tính theo vị trí của chính file auth.js (src/api/auth.js)
  const scriptEl = document.currentScript;
  const LOGIN_URL = scriptEl ? new URL('../pages/login.html', scriptEl.src).href : 'login.html';
  const ADMIN_HOME_URL = scriptEl ? new URL('../../admin.html', scriptEl.src).href : '../../admin.html';
  const guardMode = scriptEl ? scriptEl.getAttribute('data-guard') : null;

  // Giữ bản fetch gốc trước khi bọc lại bên dưới
  const nativeFetch = window.fetch.bind(window);

  // ---------- Phiên đăng nhập ----------

  function saveSession(result) {
    const user = Object.assign({}, result);
    ['message', 'accessToken', 'refreshToken', 'tokenType', 'expiresIn', 'refreshExpiresIn']
      .forEach(function (k) { delete user[k]; });
    writeStorage(USER_KEY, JSON.stringify(user));
    if (result.accessToken) writeStorage(ACCESS_KEY, result.accessToken);
    if (result.refreshToken) writeStorage(REFRESH_KEY, result.refreshToken);
  }

  function saveUser(user) {
    writeStorage(USER_KEY, user ? JSON.stringify(user) : null);
  }

  function clearSession() {
    writeStorage(USER_KEY, null);
    writeStorage(ACCESS_KEY, null);
    writeStorage(REFRESH_KEY, null);
  }

  function getUser() {
    if (!readStorage(ACCESS_KEY) && !readStorage(REFRESH_KEY)) return null; // phiên cũ chưa có JWT
    const raw = readStorage(USER_KEY);
    if (!raw) return null;
    try { return JSON.parse(raw); } catch (e) { return null; }
  }

  function isAdmin(user) {
    return !!user && user.role === ROLE_ADMIN;
  }

  // ---------- Gọi API ----------

  function urlOf(input) {
    if (typeof input === 'string') return input;
    if (input instanceof URL) return input.href;
    if (input && input.url) return input.url;
    return '';
  }

  function isBackendCall(url) {
    return url.indexOf(API_BASE + '/') === 0;
  }

  function isAuthPath(url) {
    const path = url.slice(API_BASE.length).split('?')[0];
    return AUTH_PATHS.indexOf(path) >= 0;
  }

  function withToken(input, init) {
    const token = readStorage(ACCESS_KEY);
    const headers = new Headers((init && init.headers) || (input instanceof Request ? input.headers : undefined));
    if (token && !headers.has('Authorization')) headers.set('Authorization', 'Bearer ' + token);
    return Object.assign({}, init, { headers: headers });
  }

  // Nhiều request cùng gặp 401 thì chỉ gọi refresh 1 lần
  let refreshing = null;
  function refreshTokens() {
    if (refreshing) return refreshing;
    const refreshToken = readStorage(REFRESH_KEY);
    if (!refreshToken) return Promise.resolve(false);

    refreshing = nativeFetch(API_BASE + '/api/auth/refresh', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: refreshToken }),
    })
      .then(function (res) {
        if (!res.ok) return false;
        return res.json().then(function (data) { saveSession(data); return true; });
      })
      .catch(function () { return false; })
      .finally(function () { refreshing = null; });
    return refreshing;
  }

  function goToLogin(reason) {
    const params = new URLSearchParams();
    params.set('next', window.location.href);
    if (reason) params.set('reason', reason);
    window.location.replace(LOGIN_URL + '?' + params.toString());
  }

  /** fetch() có gắn token + tự refresh. Được gán đè lên window.fetch. */
  async function authFetch(input, init) {
    const url = urlOf(input);
    if (!isBackendCall(url) || isAuthPath(url)) return nativeFetch(input, init);

    // Request object chỉ đọc body được 1 lần: giữ bản sao để gửi lại sau khi refresh
    const retryInput = input instanceof Request ? input.clone() : input;
    const response = await nativeFetch(input, withToken(input, init));
    if (response.status !== 401 || !readStorage(REFRESH_KEY)) return response;

    const ok = await refreshTokens();
    if (!ok) {
      clearSession();
      if (guardMode) goToLogin('expired');
      return response;
    }
    return nativeFetch(retryInput, withToken(retryInput, init));
  }
  window.fetch = authFetch;

  async function request(method, path, body) {
    let response;
    try {
      response = await authFetch(API_BASE + path, {
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

  /** Trang tiếp theo sau khi đăng nhập: chỉ chấp nhận URL cùng nguồn (chống open redirect). */
  function safeNext(user) {
    const next = new URLSearchParams(window.location.search).get('next');
    if (next) {
      try {
        const target = new URL(next, window.location.href);
        if (target.origin === window.location.origin) return target.href;
      } catch (e) { /* next không hợp lệ */ }
    }
    return isAdmin(user) ? ADMIN_HOME_URL : new URL('profile.html', LOGIN_URL).href;
  }

  // ---------- Chặn trang (route guard) ----------

  if (guardMode) {
    const user = getUser();
    if (!user) {
      goToLogin('login');
    } else if (guardMode === 'admin' && !isAdmin(user)) {
      goToLogin('forbidden');
    }
  }

  // ---------- API công khai cho các trang ----------

  window.SHAuth = {
    apiBase: API_BASE,
    fetch: authFetch,

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
        saveSession(result);
        return getUser();
      }
      return result;
    },

    refreshProfile: async function () {
      const user = getUser();
      if (!user) return { error: 'Bạn chưa đăng nhập.' };
      const result = await request('GET', '/api/auth/me');
      if (!result.error) saveUser(result);
      return result;
    },

    currentUser: getUser,
    isAdmin: function () { return isAdmin(getUser()); },
    accessToken: function () { return readStorage(ACCESS_KEY); },

    /** Thu hồi refresh token trên Backend rồi xóa phiên ở trình duyệt. */
    logout: async function () {
      const refreshToken = readStorage(REFRESH_KEY);
      clearSession();
      if (refreshToken) {
        try {
          await nativeFetch(API_BASE + '/api/auth/logout', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: refreshToken }),
          });
        } catch (e) { /* mất mạng: phiên ở trình duyệt vẫn đã bị xóa */ }
      }
    },

    /** Trang đích sau khi đăng nhập (theo ?next= hoặc theo vai trò). */
    nextUrl: function () { return safeNext(getUser()); },

    /** Giữ lại cho code cũ: chưa đăng nhập thì chuyển về login.html */
    requireLogin: function (loginPage) {
      const user = getUser();
      if (!user) window.location.replace(loginPage || LOGIN_URL);
      return user;
    },
  };
})();
