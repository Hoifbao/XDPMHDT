package com.smartheritage.beacon.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Gửi FCM bằng Firebase Admin SDK - Khoa, Sprint 5 (module notification trong beacon-service).
 *
 * Cấu hình: biến môi trường FIREBASE_SERVICE_ACCOUNT chứa ĐƯỜNG DẪN tới service account JSON
 * tải từ Firebase Console (Cloud Messaging > Service accounts > Generate new private key).
 * File KHÔNG commit (nằm ngoài repo) - thiếu cấu hình thì service vẫn chạy bình thường,
 * chỉ bỏ qua gửi FCM và ghi log nhắc (không làm nghẽn luồng demo).
 *
 * Lưu ý: Firebase Admin chỉ khởi tạo 1 lần; dùng cho push qua Gateway (X-User-Id từ JWT).
 */
@Slf4j
@Service
public class FcmService {

    private final String serviceAccountFile;

    private volatile boolean initialized;

    public FcmService(@Value("${app.fcm.service-account-file:}") String serviceAccountFile) {
        this.serviceAccountFile = serviceAccountFile;
    }

    /** true khi FirebaseAdmin đã sẵn sàng (có service account và khởi tạo thành công). */
    public boolean isConfigured() {
        return tryInitialize();
    }

    /**
     * Gửi thông báo tới 1 device token.
     * @return true khi FCM nhận yêu cầu (không báo lỗi); false khi chưa cấu hình hoặc gửi lỗi.
     */
    public boolean sendToToken(String token, String title, String body) {
        if (!tryInitialize()) {
            return false;
        }
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("title", title)
                    .putData("body", body)
                    .build();
            String response = FirebaseMessaging.getInstance().send(message);
            log.info("[FCM] Gửi thành công tới token {}…: {}", tokenPreview(token), response);
            return true;
        } catch (Exception e) {
            // Token hết hạn/gán sai…: ghi log, không ném lỗi để luồng chính không bị chặn.
            log.warn("[FCM] Gửi tới token {}… thất bại: {}", tokenPreview(token), e.getMessage());
            return false;
        }
    }

    /** Khởi tạo FirebaseAdmin đúng 1 lần; chưa cấu hình/lỗi lần đầu -> false (không thử lại mỗi lần). */
    private synchronized boolean tryInitialize() {
        if (initialized) {
            return !FirebaseApp.getApps().isEmpty();
        }
        initialized = true;
        if (serviceAccountFile == null || serviceAccountFile.isBlank()) {
            log.info("[FCM] Chưa cấu hình FIREBASE_SERVICE_ACCOUNT -> bỏ qua gửi FCM "
                    + "(xem Docs/research/firebase-fcm-setup.md).");
            return false;
        }
        try (FileInputStream stream = new FileInputStream(serviceAccountFile)) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
            log.info("[FCM] Đã khởi tạo Firebase Admin từ {}", serviceAccountFile);
            return true;
        } catch (FileNotFoundException e) {
            log.warn("[FCM] Không tìm thấy file service account tại '{}': {}", serviceAccountFile, e.getMessage());
            return false;
        } catch (IOException | RuntimeException e) {
            log.warn("[FCM] Khởi tạo Firebase Admin thất bại ({}), bỏ qua gửi FCM", e.getMessage());
            return false;
        }
    }

    private static String tokenPreview(String token) {
        return token == null ? "null" : token.substring(0, Math.min(8, token.length()));
    }
}
