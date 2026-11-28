import 'package:flutter/foundation.dart';

/// Ngôn ngữ giao diện dùng chung cho Trang chủ, Menu bên và Chi tiết hiện vật (chỉ vi/en).
///
/// Chỉ đổi CHỮ GIAO DIỆN (tab, nút, nhãn, thông báo). Nội dung hiện vật
/// (tiêu đề, mô tả, âm thanh) vẫn lấy từ API theo `?lang=`.
/// Ngôn ngữ khác vi (vd `fr` của nội dung) → giao diện dùng tiếng Anh.
class AppLang {
  AppLang._();

  /// Ngôn ngữ đang chọn ở nút VI/EN (trang chủ) hoặc nút cờ (chi tiết hiện vật).
  static final ValueNotifier<String> current = ValueNotifier<String>('vi');

  static bool get isVietnamese => current.value == 'vi';

  /// Quy về ngôn ngữ giao diện có bảng chữ: `vi` giữ nguyên, còn lại → `en`.
  static String ui(String? lang) => lang == 'vi' ? 'vi' : 'en';

  static void set(String lang) => current.value = ui(lang);
}

/// Chữ giao diện theo [lang] (bỏ trống = ngôn ngữ đang chọn).
/// Thiếu key → trả lại chính key để dễ phát hiện khi chạy thử.
String tr(String key, [String? lang]) {
  final entry = _strings[key];
  if (entry == null) return key;
  return entry[AppLang.ui(lang ?? AppLang.current.value)] ?? entry['vi'] ?? key;
}

const Map<String, Map<String, String>> _strings = {
  // ── Dùng chung ────────────────────────────────────────────────────────────
  'retry': {'vi': 'Thử lại', 'en': 'Retry'},
  'artifact': {'vi': 'Hiện vật', 'en': 'Artifact'},
  'login': {'vi': 'Đăng nhập', 'en': 'Log in'},
  'register': {'vi': 'Đăng ký', 'en': 'Sign up'},
  'profile': {'vi': 'Hồ sơ cá nhân', 'en': 'My profile'},
  'heritageMap': {'vi': 'Bản đồ di tích', 'en': 'Heritage map'},
  'visitHistory': {'vi': 'Lịch sử tham quan', 'en': 'Visit history'},

  // ── Menu bên ──────────────────────────────────────────────────────────────
  'guest': {'vi': 'Khách tham quan', 'en': 'Guest visitor'},
  'hello': {'vi': 'Xin chào, {name}', 'en': 'Hello, {name}'},
  'home': {'vi': 'Trang chủ', 'en': 'Home'},
  'favorites': {'vi': 'Hiện vật yêu thích', 'en': 'Favorite artifacts'},
  'myFeedbacks': {'vi': 'Đánh giá của tôi', 'en': 'My reviews'},
  'logout': {'vi': 'Đăng xuất', 'en': 'Log out'},

  // ── Trang chủ ─────────────────────────────────────────────────────────────
  'recentOffline': {'vi': 'Hiện vật đã xem — đọc offline', 'en': 'Viewed artifacts — read offline'},
  'heroTitle': {'vi': 'Lịch sử trong tầm tay.', 'en': 'History at your fingertips.'},
  'heroSubtitle': {
    'vi': 'Trải nghiệm du lịch văn hóa ứng dụng iBeacon, biến mỗi hiện vật thành một câu chuyện tương tác.',
    'en': 'A cultural tourism experience powered by iBeacon, turning every artifact into an interactive story.',
  },
  'exploreHeritage': {'vi': 'Khám phá di sản', 'en': 'Explore heritage'},
  'simulateBeacon': {'vi': 'Mô phỏng beacon', 'en': 'Simulate beacon'},
  'mapEyebrow': {'vi': 'BẢN ĐỒ DI SẢN', 'en': 'HERITAGE MAP'},
  'mapTitle': {'vi': 'Các di tích trong hệ thống', 'en': 'Heritage sites in the system'},
  'mapHint': {
    'vi': 'Ứng dụng sẽ tự động phát hiện iBeacon khi bạn đến gần hiện vật.',
    'en': 'The app automatically detects iBeacons when you get close to an artifact.',
  },
  'openMap': {'vi': 'Mở bản đồ di tích', 'en': 'Open heritage map'},
  'featuredArtifacts': {'vi': 'Hiện vật nổi bật', 'en': 'Featured artifacts'},
  'artifactCount': {'vi': '{n} hiện vật', 'en': '{n} artifacts'},
  'openStory': {'vi': 'Mở câu chuyện', 'en': 'Open story'},
  'journeyTitle': {'vi': 'Hành trình của bạn', 'en': 'Your journey'},
  'journeyHint': {
    'vi': 'Các hiện vật bạn đã ghé thăm được lưu lại, kể cả khi mất mạng.',
    'en': 'Artifacts you have visited are saved, even when offline.',
  },
  'viewVisitHistory': {'vi': 'Xem lịch sử tham quan', 'en': 'View visit history'},
  'beaconEyebrow': {'vi': 'QUÉT IBEACON', 'en': 'IBEACON SCAN'},
  'beaconHint': {
    'vi': 'Bật Bluetooth và đi lại gần hiện vật, ứng dụng sẽ tự hiện nội dung.',
    'en': 'Turn on Bluetooth and walk close to an artifact; the app will show its content automatically.',
  },
  'beaconDemoButton': {'vi': 'Phát hiện hiện vật gần bạn (demo) →', 'en': 'Detect a nearby artifact (demo) →'},
  'scanRealBeacon': {'vi': 'Quét iBeacon thật', 'en': 'Scan real iBeacons'},
  'beaconFound': {'vi': 'Beacon được phát hiện', 'en': 'Beacon detected'},
  'beaconFoundBody': {
    'vi': 'Đã phát hiện iBeacon gần bạn.\n\nHiện vật: {name}\nKhoảng cách: khoảng 2m',
    'en': 'An iBeacon was detected near you.\n\nArtifact: {name}\nDistance: about 2 m',
  },
  'viewArtifact': {'vi': 'Xem hiện vật', 'en': 'View artifact'},

  // ── Chi tiết hiện vật ─────────────────────────────────────────────────────
  'artifactDetail': {'vi': 'Chi tiết hiện vật', 'en': 'Artifact details'},
  'tabDescription': {'vi': 'Mô tả', 'en': 'Description'},
  'tabImages': {'vi': 'Hình ảnh', 'en': 'Images'},
  'tabAudio': {'vi': 'Âm thanh', 'en': 'Audio'},
  'tabInfo': {'vi': 'Thông tin', 'en': 'Info'},
  'reloadArtifact': {'vi': 'Tải lại hiện vật', 'en': 'Reload artifact'},
  'favorite': {'vi': 'Yêu thích', 'en': 'Favorite'},
  'loginToFavorite': {'vi': 'Đăng nhập để lưu hiện vật yêu thích', 'en': 'Log in to save favorite artifacts'},
  'offlineNoCache': {
    'vi': 'Không có kết nối mạng và chưa có bản đã tải.\nVui lòng kết nối internet rồi thử lại.',
    'en': 'No network connection and no downloaded copy.\nPlease connect to the internet and try again.',
  },
  'loadArtifactFailed': {'vi': 'Không tải được hiện vật.\n{error}', 'en': 'Could not load the artifact.\n{error}'},
  'offlineBanner': {
    'vi': 'Đang xem nội dung đã tải – có thể thiếu cập nhật mới nhất.',
    'en': 'Viewing downloaded content – latest updates may be missing.',
  },
  'fallbackVi': {
    'vi': 'Chưa có bản dịch, đang hiển thị tiếng Việt',
    'en': 'No translation yet, showing Vietnamese',
  },
  'fallbackOther': {
    'vi': 'Chưa lưu bản dịch này, đang hiển thị nội dung {lang}',
    'en': 'This translation is not saved, showing {lang} content',
  },
  'rate': {'vi': 'Đánh giá', 'en': 'Rate'},
  'watchVideo': {'vi': 'Xem video', 'en': 'Watch video'},
  'noDescription': {'vi': 'Chưa có nội dung mô tả.', 'en': 'No description yet.'},
  'transcript': {'vi': 'Phụ đề thuyết minh', 'en': 'Narration transcript'},
  'noImages': {'vi': 'Chưa có hình ảnh cho hiện vật này.', 'en': 'No images for this artifact yet.'},
  'tapToZoom': {'vi': 'Bấm vào ảnh để phóng to  •  {i}/{n}', 'en': 'Tap an image to zoom  •  {i}/{n}'},
  'noAudio': {'vi': 'Chưa có file âm thanh cho ngôn ngữ này.', 'en': 'No audio file for this language yet.'},
  'audioFailed': {
    'vi': 'Không tải được file âm thanh. Kiểm tra đường link hoặc kết nối mạng.',
    'en': 'Could not load the audio file. Check the link or your network connection.',
  },
  'audioGuide': {'vi': 'Thuyết minh âm thanh', 'en': 'Audio guide'},
  'videoGuide': {'vi': 'Video thuyết minh', 'en': 'Video guide'},
  'videoFailed': {
    'vi': 'Không tải được video.\nKiểm tra đường link hoặc kết nối mạng.',
    'en': 'Could not load the video.\nCheck the link or your network connection.',
  },
  'exitFullscreen': {'vi': 'Thoát toàn màn hình', 'en': 'Exit full screen'},
  'fullscreen': {'vi': 'Toàn màn hình', 'en': 'Full screen'},
  'artifactInfo': {'vi': 'Thông tin hiện vật', 'en': 'Artifact information'},
  'artifactCode': {'vi': 'Mã hiện vật', 'en': 'Artifact code'},
  'period': {'vi': 'Niên đại', 'en': 'Period'},
  'material': {'vi': 'Chất liệu', 'en': 'Material'},
  'dimensions': {'vi': 'Kích thước', 'en': 'Dimensions'},
  'status': {'vi': 'Trạng thái', 'en': 'Status'},
  'onDisplay': {'vi': 'Đang trưng bày', 'en': 'On display'},
  'hidden': {'vi': 'Tạm ẩn', 'en': 'Temporarily hidden'},
  'location': {'vi': 'Vị trí', 'en': 'Location'},
  'site': {'vi': 'Di tích', 'en': 'Heritage site'},
};
