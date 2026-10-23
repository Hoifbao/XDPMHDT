const translations = document.getElementById('translations');
const mediaResources = document.getElementById('media-resources');
const form = document.getElementById('artifact-form');
const formMessage = document.getElementById('form-message');

function addRepeatedItem(container, templateId) {
    const template = document.getElementById(templateId);
    container.appendChild(template.content.cloneNode(true));
    updateRemoveButtons(container);
}

function updateRemoveButtons(container) {
    const removeButtons = container.querySelectorAll('.remove-item');
    removeButtons.forEach(function (button, index) {
        button.disabled = removeButtons.length === 1 && index === 0;
    });
}

document.getElementById('add-translation').addEventListener('click', function () {
    addRepeatedItem(translations, 'translation-template');
});

document.getElementById('add-media').addEventListener('click', function () {
    addRepeatedItem(mediaResources, 'media-template');
});

document.addEventListener('click', function (event) {
    const removeButton = event.target.closest('.remove-item');
    if (!removeButton || removeButton.disabled) {
        return;
    }

    const container = removeButton.closest('#translations, #media-resources');
    removeButton.closest('.repeat-item').remove();
    updateRemoveButtons(container);
});

form.addEventListener('input', function () {
    formMessage.textContent = 'Giao diện tĩnh: thông tin chưa được gửi hoặc lưu vào Backend.';
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
        formMessage.textContent = 'Giao diện tĩnh: thông tin chưa được gửi hoặc lưu vào Backend.';
    });
});

form.addEventListener('submit', function (event) {
    event.preventDefault();
    formMessage.textContent = 'Thông tin hợp lệ ở trình duyệt. Giao diện mẫu chưa gửi hoặc lưu dữ liệu vào Backend.';
});
