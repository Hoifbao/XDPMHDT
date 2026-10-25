const translations = document.getElementById('translations');
const mediaResources = document.getElementById('media-resources');
const form = document.getElementById('artifact-form');
const formMessage = document.getElementById('form-message');
const siteSelect = document.getElementById('site-select');
const apiStatus = document.getElementById('api-status');
const submitButton = form.querySelector('button[type="submit"]');
// Sau khi lưu thành công, reset form nhưng giữ thông báo kết quả
let keepMessageOnReset = false;

// Địa chỉ Backend (dùng chung cách cấu hình với WebAdmin/src/api/auth.js)
const API_BASE = (function () {
    try { return (localStorage.getItem('sh_api_base') || 'http://localhost:8080').replace(/\/+$/, ''); }
    catch (e) { return 'http://localhost:8080'; }
})();

// ───────── Khối lặp: bản dịch và tài nguyên (giữ nguyên logic của Xuân Anh) ─────────

function addRepeatedItem(container, templateId) {
    const template = document.getElementById(templateId);
    container.appendChild(template.content.cloneNode(true));
    updateRemoveButtons(container);
}

function updateRemoveButtons(container) {
    const buttons = container.querySelectorAll('.remove-item');
    buttons.forEach(function (button, index) {
        button.disabled = buttons.length === 1 && index === 0;
    });
}

document.getElementById('add-translation').addEventListener('click', function () {
    addRepeatedItem(translations, 'translation-template');
});

document.getElementById('add-media').addEventListener('click', function () {
    addRepeatedItem(mediaResources, 'media-template');
});

document.addEventListener('click', function (event) {
    const button = event.target.closest('.remove-item');
    if (!button || button.disabled) {
        return;
    }

    const container = button.closest('#translations, #media-resources');
    button.closest('.repeat-item').remove();
    updateRemoveButtons(container);
});

form.addEventListener('reset', function () {
    window.setTimeout(function () {
        translations.querySelectorAll('.translation-item:not(:first-child)').forEach(function (item) {
            item.remove();
        });
        mediaResources.querySelectorAll('.media-item:not(:first-child)').forEach(function (item) {
            item.remove();
        });
        updateRemoveButtons(translations);
        updateRemoveButtons(mediaResources);
        if (!keepMessageOnReset) {
            showMessage('Điền thông tin rồi bấm "Lưu hiện vật" để gửi lên Backend.');
        }
        keepMessageOnReset = false;
    });
});

// ───────── Gọi API ─────────

function showMessage(text, isError) {
    formMessage.textContent = text;
    formMessage.style.color = isError ? '#b42318' : '';
}

// Chuỗi rỗng gửi lên dạng null: artifact_code và beacon_uuid có ràng buộc UNIQUE,
// gửi "" cho nhiều hiện vật sẽ bị trùng.
function textOrNull(value) {
    const text = (value || '').trim();
    return text === '' ? null : text;
}

async function readJson(response) {
    try { return await response.json(); } catch (e) { return null; }
}

// GET /api/heritage-sites trả về mảng trực tiếp (chưa bọc ApiResponse)
async function loadSites() {
    try {
        const response = await fetch(API_BASE + '/api/heritage-sites');
        if (!response.ok) throw new Error('HTTP ' + response.status);
        const body = await readJson(response);
        const sites = Array.isArray(body) ? body : (body && Array.isArray(body.data) ? body.data : []);

        siteSelect.innerHTML = '<option value="">Chọn địa điểm</option>';
        sites.forEach(function (site) {
            const option = document.createElement('option');
            option.value = site.id;
            option.textContent = site.name;
            siteSelect.appendChild(option);
        });
        apiStatus.textContent = 'Đã kết nối Backend';
    } catch (error) {
        siteSelect.innerHTML = '<option value="">Không tải được danh sách di tích</option>';
        apiStatus.textContent = 'Chưa kết nối Backend';
        showMessage('Không gọi được ' + API_BASE + '. Hãy chạy Backend rồi tải lại trang.', true);
    }
}

// Gom các khối bản dịch thành contents[] của ArtifactRequest
function collectContents() {
    return Array.from(translations.querySelectorAll('.translation-item')).map(function (item) {
        const imageLines = (item.querySelector('[name="image_urls[]"]').value || '')
            .split('\n').map(function (line) { return line.trim(); }).filter(Boolean);
        return {
            languageCode: item.querySelector('[name="language_code[]"]').value,
            title: textOrNull(item.querySelector('[name="content_title[]"]').value),
            description: textOrNull(item.querySelector('[name="content_description[]"]').value),
            audioUrl: textOrNull(item.querySelector('[name="audio_url[]"]').value),
            videoUrl: textOrNull(item.querySelector('[name="video_url[]"]').value),
            // Backend lưu imageUrls dạng chuỗi JSON: ["url1","url2"]
            imageUrls: imageLines.length ? JSON.stringify(imageLines) : null
        };
    });
}

function collectMedia() {
    return Array.from(mediaResources.querySelectorAll('.media-item')).map(function (item) {
        return {
            mediaType: item.querySelector('[name="media_type[]"]').value,
            fileUrl: textOrNull(item.querySelector('[name="file_url[]"]').value),
            caption: textOrNull(item.querySelector('[name="caption[]"]').value)
        };
    }).filter(function (media) { return media.fileUrl; });
}

form.addEventListener('submit', async function (event) {
    event.preventDefault();
    const data = new FormData(form);

    // ArtifactRequest: Backend vẫn đặt tên trường di tích là locationId (= heritage_sites.id)
    const payload = {
        locationId: Number(data.get('site_id')),
        name: textOrNull(data.get('name')),
        artifactCode: textOrNull(data.get('artifact_code')),
        description: textOrNull(data.get('description')),
        thumbnailUrl: textOrNull(data.get('thumbnail_url')),
        period: textOrNull(data.get('period')),
        material: textOrNull(data.get('material')),
        dimensions: textOrNull(data.get('dimensions')),
        beaconUuid: textOrNull(data.get('beacon_uuid')),
        status: data.get('status') || 'ACTIVE',
        contents: collectContents()
    };

    submitButton.disabled = true;
    showMessage('Đang lưu hiện vật…');
    try {
        // 1) Tạo hiện vật kèm nội dung đa ngôn ngữ
        const response = await fetch(API_BASE + '/api/artifacts', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const body = await readJson(response);
        if (!response.ok || !body || !body.success) {
            throw new Error((body && body.message) || ('Lưu thất bại (HTTP ' + response.status + ')'));
        }
        const artifactId = body.data.id;

        // 2) Thêm từng tài nguyên đa phương tiện
        const mediaList = collectMedia();
        let mediaFailed = 0;
        for (const media of mediaList) {
            const mediaResponse = await fetch(API_BASE + '/api/artifacts/' + artifactId + '/media', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(media)
            });
            if (!mediaResponse.ok) mediaFailed++;
        }

        if (mediaFailed > 0) {
            showMessage('Đã tạo hiện vật #' + artifactId + ', nhưng ' + mediaFailed + ' tài nguyên chưa lưu được.', true);
        } else {
            keepMessageOnReset = true;
            form.reset();
            showMessage('Đã lưu hiện vật #' + artifactId + ' cùng ' + mediaList.length + ' tài nguyên.');
        }
    } catch (error) {
        showMessage(error.message, true);
    } finally {
        submitButton.disabled = false;
    }
});

loadSites();
