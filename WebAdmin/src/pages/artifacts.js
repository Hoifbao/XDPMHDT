const translations = document.getElementById('translations');
const mediaResources = document.getElementById('media-resources');
const form = document.getElementById('artifact-form');
const message = document.getElementById('form-message');
const submit = form.querySelector('[type="submit"]');
const base = location.protocol === 'file:' ? 'http://localhost:8080' : location.origin;
let id = new URLSearchParams(location.search).get('id');
let busy = false;
const fields = { name: 'name', artifact_code: 'artifactCode', description: 'description', thumbnail_url: 'thumbnailUrl', period: 'period', material: 'material', dimensions: 'dimensions', beacon_uuid: 'beaconUuid', status: 'status' };
const contentFields = { 'language_code[]': 'languageCode', 'content_title[]': 'title', 'content_description[]': 'description', 'audio_url[]': 'audioUrl', 'video_url[]': 'videoUrl' };
const mediaFields = { 'media_type[]': 'mediaType', 'file_url[]': 'fileUrl', 'caption[]': 'caption' };
const input = (row, name) => row.querySelector(`[name="${name}"]`);
const value = (row, name) => input(row, name).value.trim();
function read(row, mapping) { return Object.fromEntries(Object.entries(mapping).map(([name, key]) => [key, value(row, name)])); }
function fill(row, mapping, data) { for (const [name, key] of Object.entries(mapping)) input(row, name).value = data[key] ?? ''; }
async function api(path, method = 'GET', body) {
    const response = await fetch(base + path, { method, headers: body ? { 'Content-Type': 'application/json' } : {}, body: body ? JSON.stringify(body) : undefined });
    let data;
    try { data = await response.json(); } catch { throw new Error(`Gateway trả lỗi HTTP ${response.status}`); }
    if (!response.ok || data.success === false) throw new Error(data.message || `HTTP ${response.status}`);
    return data.data === undefined ? data : data.data;
}
function updateButtons(container) { container.querySelectorAll('.remove-item').forEach(button => { button.disabled = container.children.length === 1; }); }
function add(container, template) {
    container.appendChild(document.getElementById(template).content.cloneNode(true)); updateButtons(container);
    return container.lastElementChild;
}
document.getElementById('add-translation').onclick = () => {
    if (translations.children.length >= 2) return;
    const lang = value(translations.firstElementChild, 'language_code[]') === 'vi' ? 'en' : 'vi';
    input(add(translations, 'translation-template'), 'language_code[]').value = lang;
};
document.getElementById('add-media').onclick = () => add(mediaResources, 'media-template');
document.addEventListener('click', event => {
    const button = event.target.closest('.remove-item');
    if (!button || button.disabled) return;
    const container = button.closest('#translations, #media-resources');
    button.closest('.repeat-item').remove(); updateButtons(container);
});
function checkUrl(url) {
    if (!url) return;
    const parsed = new URL(url);
    if (!['http:', 'https:'].includes(parsed.protocol)) throw new Error('URL media phải dùng http hoặc https.');
}
function payload() {
    const body = { ...read(form, fields), siteId: Number(value(form, 'location_id')) };
    if (!id) { body.artifactCode ||= null; body.beaconUuid ||= null; }
    checkUrl(body.thumbnailUrl);
    body.contents = [...translations.children].map(row => {
        const c = read(row, contentFields);
        if (!c.title || !c.description) throw new Error('Bản dịch cần tiêu đề và nội dung thuyết minh.');
        checkUrl(c.audioUrl); checkUrl(c.videoUrl);
        const images = value(row, 'image_urls[]').split(/\r?\n/).map(s => s.trim()).filter(Boolean);
        images.forEach(checkUrl); c.imageUrls = JSON.stringify(images);
        return c;
    });
    if (!body.contents.some(c => c.languageCode === 'vi')) throw new Error('Cần bản tiếng Việt để fallback.');
    if (new Set(body.contents.map(c => c.languageCode)).size !== body.contents.length) throw new Error('Ngôn ngữ bị trùng.');
    body.media = [...mediaResources.children].map(row => read(row, mediaFields)).filter(m => m.fileUrl || m.caption);
    body.media.forEach(m => { if (!m.fileUrl) throw new Error('Tài nguyên cần URL.'); checkUrl(m.fileUrl); });
    return body;
}
function rememberId(nextId) {
    id = nextId;
    const url = new URL(location.href);
    if (id) url.searchParams.set('id', id); else url.searchParams.delete('id');
    history.replaceState(null, '', url);
    document.querySelector('h1').textContent = id ? 'Cập nhật hiện vật' : 'Thêm hiện vật';
}
form.addEventListener('submit', async event => {
    event.preventDefault(); if (busy) return;
    busy = true; submit.disabled = true;
    try {
        message.textContent = 'Đang lưu…';
        const data = await api('/api/artifacts' + (id ? '/' + id : ''), id ? 'PUT' : 'POST', payload());
        rememberId(data.id); message.textContent = `Đã lưu hiện vật #${id}.`; await loadList();
    } catch (error) { message.textContent = 'Không lưu được: ' + error.message; }
    finally { busy = false; submit.disabled = false; }
});
form.addEventListener('reset', event => {
    if (busy) { event.preventDefault(); return; }
    rememberId(null);
    setTimeout(() => {
        [translations, mediaResources].forEach(container => { [...container.children].slice(1).forEach(row => row.remove()); updateButtons(container); });
        message.textContent = 'Sẵn sàng tạo hiện vật mới.';
    });
});
async function loadList() {
    const list = document.getElementById('artifact-list');
    try {
        const data = await api('/api/artifacts'); list.replaceChildren();
        for (const item of data) {
            const row = document.createElement('p'), link = document.createElement('a'), remove = document.createElement('button');
            link.href = '?id=' + item.id; link.textContent = `${item.name} (${item.artifactCode || item.id})`;
            remove.type = 'button'; remove.className = 'text-button'; remove.textContent = 'Xóa';
            remove.onclick = async () => {
                if (!confirm(`Xóa hiện vật “${item.name}”?`)) return;
                remove.disabled = true;
                try { await api('/api/artifacts/' + item.id, 'DELETE'); if (String(id) === String(item.id)) form.reset(); await loadList(); }
                catch (error) { message.textContent = error.message; remove.disabled = false; }
            };
            row.append(link, remove); list.appendChild(row);
        }
        if (!data.length) list.textContent = 'Chưa có hiện vật.';
    } catch (error) { list.textContent = 'Không tải được danh sách: ' + error.message; }
}
async function init() {
    submit.disabled = true;
    try {
        const sites = await api('/api/heritage-sites'), select = form.elements.location_id;
        select.replaceChildren(new Option('Chọn địa điểm', ''));
        sites.forEach(site => select.add(new Option(site.name, site.id)));
        if (id) {
            const data = await api('/api/artifacts/' + id + '?lang=vi');
            input(form, 'location_id').value = data.siteId; fill(form, fields, data);
            translations.replaceChildren();
            for (const c of data.contents) {
                const row = add(translations, 'translation-template'); fill(row, contentFields, c);
                input(row, 'image_urls[]').value = c.imageUrls ? JSON.parse(c.imageUrls).join('\n') : '';
            }
            mediaResources.replaceChildren();
            for (const m of data.media) fill(add(mediaResources, 'media-template'), mediaFields, m);
            if (!mediaResources.children.length) add(mediaResources, 'media-template');
            rememberId(id);
        }
        message.textContent = 'Dữ liệu được lưu qua Gateway.'; submit.disabled = false;
    } catch (error) { message.textContent = 'Không tải được dữ liệu: ' + error.message; }
    await loadList();
}
init();
