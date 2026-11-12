package com.smartheritage.beacon.events;

/**
 * Phần data của sự kiện ArtifactUpdated (routing key artifact.updated) - Khoa, W6.
 * content-service (Xuân Anh) phát khi tạo/sửa/xóa hiện vật; theo Docs/testing/test-plan.md TC-94
 * data đầy đủ là {artifactId, siteId, changedFields, updatedAt} - beacon-service chỉ dùng
 * artifactId (hủy cache tên hiện vật để registry hiển thị tên mới), các trường thừa bị Jackson bỏ qua.
 */
public record ArtifactUpdatedData(Long artifactId, Long siteId) {
}
