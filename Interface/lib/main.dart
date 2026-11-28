import 'dart:async';

import 'package:flutter/material.dart';
import 'features/personalization/personalization_service.dart';
import 'features/personalization/personalization_sync.dart';
import 'package:http/http.dart' as http;
import 'core/network/auth_http_client.dart';
import 'features/auth/auth_service.dart';
import 'features/auth/login_screen.dart';
import 'features/auth/profile_screen.dart';
import 'features/beacon/beacon_scan_screen.dart';
import 'features/beacon/beacon_service.dart';
import 'features/notify/device_registration_service.dart';
import 'features/heritage_sites/map_screen.dart'; // Ân W5
import 'screens/artifact_detail_screen.dart';
import 'screens/recent_artifacts_screen.dart';
import 'core/media_lifecycle.dart';
import 'core/app_strings.dart';
import 'shared/app_sidebar.dart';
import 'features/visit_history/visit_history_screen.dart';

/// Bảo W7: nút mô phỏng beacon chỉ hiện khi chạy chế độ demo (dự phòng khi beacon thật không phát):
///   flutter run --dart-define=DEMO_MODE=true
const bool kDemoMode = bool.fromEnvironment('DEMO_MODE');

void main() {
  // Client thật tạo NGOÀI runWithClient (tránh gọi đệ quy). Bên trong zone,
  // mọi http.get/post/put/delete ở mọi màn hình đều đi qua AuthHttpClient
  // để tự gắn "Authorization: Bearer <token>" và tự refresh khi hết hạn.
  final inner = http.Client();
  http.runWithClient(() async {
    WidgetsFlutterBinding.ensureInitialized();
    AuthService.rawClient = inner;
    // Khôi phục phiên đăng nhập đã lưu (JWT) trước khi vẽ giao diện
    await AuthService.init();
    // Prefetch registry iBeacon 1 lần khi mở app (cache 30 phút + đĩa)
    unawaited(BeaconService.getRegistry());
    // Sprint 5 – FCM: nếu đã đăng nhập thì lấy token (Firebase) + POST /api/me/devices;
    // đăng nhập xong (đổi user) cũng nhắc lại đăng ký.
    unawaited(DeviceRegistrationService.I.ensureRegistered());
    AuthService.currentUser.addListener(
      () => unawaited(DeviceRegistrationService.I.ensureRegistered()),
    );
    runApp(const PersonalizationSync(child: SmartHeritageApp()));
  }, () => AuthHttpClient(inner));
}

class SmartHeritageApp extends StatelessWidget {
  const SmartHeritageApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      navigatorObservers: [mediaRouteObserver],
      debugShowCheckedModeBanner: false,
      title: 'Smart Heritage',
      theme: ThemeData(
        useMaterial3: true,
        fontFamily: 'Inter',
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF718B78),
        ),
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  /// Ngôn ngữ giao diện dùng chung (core/app_strings.dart): nút VI/EN ở đây và nút cờ ở
  /// màn Chi tiết hiện vật cùng đổi một biến, menu bên cũng đổi theo.
  bool get isVietnamese => AppLang.isVietnamese;

  void _onLangChanged() {
    if (mounted) setState(() {});
  }

  @override
  void initState() {
    super.initState();
    AppLang.current.addListener(_onLangChanged);
  }

  @override
  void dispose() {
    AppLang.current.removeListener(_onLangChanged);
    super.dispose();
  }

  final List<Map<String, dynamic>> artifacts = [
    {
      'id': 1,
      'name': 'Khue Van Cac',
      // Lỗi Minor số 11 (TC-125): tên + di tích theo dữ liệu demo, dùng khi ghi lượt MANUAL
      // (lịch sử lưu trên máy hiện đúng cả lúc mất mạng)
      'historyName': 'Khuê Văn Các',
      'siteName': 'Văn Miếu - Quốc Tử Giám',
      'beacon': 'iBeacon 1',
      'description':
          'The constellation pavilion and signature symbol of the Temple of Literature.',
      'descriptionVi': 'Gác Khuê Văn – biểu tượng tiêu biểu của Văn Miếu - Quốc Tử Giám.',
      // Bỏ link Unsplash gõ cứng (bị 404) -> card hiện khung thay thế
      'image': '',
    },
    {
      'id': 2,
      'name': 'Doctors’ Stelae',
      'historyName': 'Bia Tiến sĩ',
      'siteName': 'Văn Miếu - Quốc Tử Giám',
      'beacon': 'iBeacon 2',
      'description':
          'Stone stelae recording royal examinations and doctoral laureates.',
      'descriptionVi': 'Những tấm bia đá ghi danh các vị đỗ tiến sĩ qua các khoa thi.',
      // Bỏ link Unsplash gõ cứng (bị 404) -> card hiện khung thay thế
      'image': '',
    },
  ];

  void openArtifact(Map<String, dynamic> artifact, {String source = 'MANUAL'}) {
    final id = (artifact['id'] as num).toInt();
    unawaited(PersonalizationService.recordVisit(
      artifactId: id,
      artifactName: (artifact['historyName'] ?? artifact['name'])?.toString() ?? 'Hiện vật',
      siteName: artifact['siteName']?.toString() ?? 'Di tích Smart Heritage',
      thumbnailUrl: artifact['image']?.toString() ?? '',
      source: source,
    ));
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => ArtifactDetailScreen(
          artifactId: id,
          initialLang: isVietnamese ? 'vi' : 'en',
        ),
      ),
    );
  }

  void beaconDemo() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: Text(tr('beaconFound')),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.bluetooth_searching,
              size: 60,
              color: Color(0xFF718B78),
            ),
            const SizedBox(height: 16),
            Text(
              tr('beaconFoundBody').replaceAll('{name}', _artifactName(artifacts.first)),
              textAlign: TextAlign.center,
            ),
          ],
        ),
        actions: [
          // Bảo W7: trước đây onPressed: null nên nút không bấm được
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              openArtifact(artifacts.first, source: 'BEACON');
            },
            child: Text(tr('viewArtifact')),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF7F5EF),
      drawer: const AppSidebar(),
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        title: Row(
          children: [
            Container(
              width: 38,
              height: 38,
              decoration: BoxDecoration(
                color: const Color(0xFF718B78),
                borderRadius: BorderRadius.circular(10),
              ),
              alignment: Alignment.center,
              child: const Text(
                'SH',
                style: TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
            const SizedBox(width: 10),
            // Cho tiêu đề co lại khi màn hình hẹp, tránh tràn ngang (OVERFLOWED BY 3)
            const Flexible(
              child: Text(
                'Smart Heritage',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF26352B),
                ),
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: tr('recentOffline'),
            icon: const Icon(Icons.history),
            onPressed: () => Navigator.push(context, MaterialPageRoute(builder: (_) => const RecentArtifactsScreen())),
          ),
          // Nút tài khoản: chưa đăng nhập -> Login, đã đăng nhập -> Profile
          ValueListenableBuilder<AuthUser?>(
            valueListenable: AuthService.currentUser,
            builder: (context, user, _) => IconButton(
              tooltip: user == null ? tr('login') : tr('profile'),
              icon: Icon(user == null ? Icons.login : Icons.account_circle),
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => user == null ? const LoginScreen() : const ProfileScreen(),
                ),
              ),
            ),
          ),
          TextButton(
            onPressed: () {
              // Listener _onLangChanged tự vẽ lại trang chủ
              AppLang.set(isVietnamese ? 'en' : 'vi');
            },
            child: Text(
              isVietnamese ? 'VI' : 'EN',
              style: const TextStyle(
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(20),
          children: [
            _buildHero(),
            const SizedBox(height: 24),
            _buildMap(),
            const SizedBox(height: 28),
            _buildArtifactSection(),
            const SizedBox(height: 28),
            _buildJourney(),
            const SizedBox(height: 16),
            _buildBeacon(),
          ],
        ),
      ),
    );
  }

  Widget _buildHero() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: const Color(0xFFECEDE5),
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'SMART HERITAGE',
            style: TextStyle(
              fontSize: 12,
              letterSpacing: 2,
              fontWeight: FontWeight.bold,
              color: Color(0xFF718B78),
            ),
          ),
          const SizedBox(height: 10),
          Text(
            tr('heroTitle'),
            style: TextStyle(
              fontSize: 32,
              fontWeight: FontWeight.bold,
              color: Color(0xFF26352B),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            tr('heroSubtitle'),
            style: TextStyle(
              fontSize: 16,
              height: 1.5,
              color: Color(0xFF69716B),
            ),
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                // Bảo W7: trước đây nút không làm gì; nay mở bản đồ di tích
                child: ElevatedButton(
                  onPressed: () => Navigator.push(
                      context, MaterialPageRoute(builder: (_) => const MapScreen())),
                  child: Text(tr('exploreHeritage')),
                ),
              ),
              if (kDemoMode) ...[
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton(
                    onPressed: beaconDemo,
                    child: Text(tr('simulateBeacon')),
                  ),
                ),
              ],
            ],
          ),
          // Bảo W7: bỏ 3 ô thống kê gõ cứng (1 khách / 2 tương tác / 4.0 điểm)
        ],
      ),
    );
  }

  Widget _buildMap() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              // Expanded: tiêu đề co lại theo bề ngang, không tràn khi màn hình hẹp
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      tr('mapEyebrow'),
                      style: const TextStyle(
                        fontSize: 11,
                        letterSpacing: 1.5,
                        color: Color(0xFF718B78),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      tr('mapTitle'),
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              _pill('OpenStreetMap'),
            ],
          ),
          const SizedBox(height: 16),
          Container(
            height: 180,
            decoration: BoxDecoration(
              color: const Color(0xFFE3E6DC),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Stack(
              children: [
                Positioned(
                  left: 80,
                  top: 50,
                  child: _mapPin(),
                ),
                Positioned(
                  right: 70,
                  top: 100,
                  child: _mapPin(),
                ),
                const Center(
                  child: Icon(
                    Icons.map_outlined,
                    size: 60,
                    color: Color(0xFF718B78),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          Text(
            tr('mapHint'),
            style: const TextStyle(fontSize: 13, color: Colors.grey),
          ),
          const SizedBox(height: 12),
          // Ân W5: mở bản đồ di tích tương tác
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => Navigator.push(context,
                  MaterialPageRoute(builder: (_) => const MapScreen())),
              icon: const Icon(Icons.map_outlined),
              label: Text(tr('openMap')),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF718B78),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12)),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _mapPin() {
    return const Icon(
      Icons.location_on,
      color: Color(0xFF718B78),
      size: 34,
    );
  }

  Widget _buildArtifactSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              tr('featuredArtifacts'),
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
              ),
            ),
            _pill(tr('artifactCount').replaceAll('{n}', '${artifacts.length}')),
          ],
        ),
        const SizedBox(height: 14),
        ...artifacts.map(_artifactCard),
      ],
    );
  }

  Widget _artifactCard(Map<String, dynamic> artifact) {
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _artifactImage(artifact['image']?.toString() ?? ''),
          Padding(
            padding: const EdgeInsets.all(18),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _pill(artifact['beacon']),
                const SizedBox(height: 10),
                Text(
                  _artifactName(artifact),
                  style: const TextStyle(
                    fontSize: 21,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  (isVietnamese ? artifact['descriptionVi'] : artifact['description'])?.toString() ?? '',
                  style: const TextStyle(
                    color: Colors.grey,
                    height: 1.4,
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: ElevatedButton(
                        onPressed: () => openArtifact(artifact),
                        child: Text(tr('openStory')),
                      ),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      onPressed: () {},
                      icon: const Icon(Icons.star_border),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // Bảo W7: trước đây gõ cứng "Khue Van Cac"; nay mở lịch sử tham quan thật (personalization-service)
  Widget _buildJourney() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            tr('journeyTitle'),
            style: const TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 6),
          Text(
            tr('journeyHint'),
            style: const TextStyle(color: Colors.grey),
          ),
          const SizedBox(height: 12),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const VisitHistoryScreen()),
              ),
              icon: const Icon(Icons.timeline),
              label: Text(tr('viewVisitHistory')),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildBeacon() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFFECEDE5),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            tr('beaconEyebrow'),
            style: const TextStyle(
              fontSize: 11,
              letterSpacing: 1.5,
              color: Color(0xFF718B78),
            ),
          ),
          const SizedBox(height: 6),
          const Text(
            'BLE / iBeacon',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 16),
          // Bảo W7: bỏ khung "Tín hiệu BLE đang hoạt động / UUID: SMART-HERITAGE-DEMO" gõ cứng
          Text(
            tr('beaconHint'),
            style: const TextStyle(color: Color(0xFF69716B)),
          ),
          const SizedBox(height: 14),
          if (kDemoMode) ...[
            SizedBox(
              width: double.infinity,
              child: OutlinedButton(
                onPressed: beaconDemo,
                child: Text(tr('beaconDemoButton')),
              ),
            ),
            const SizedBox(height: 10),
          ],
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => BeaconScanScreen(
                    lang: isVietnamese ? 'vi' : 'en',
                  ),
                ),
              ),
              icon: const Icon(Icons.bluetooth_searching),
              label: Text(tr('scanRealBeacon')),
            ),
          ),
        ],
      ),
    );
  }

  /// Tên hiện vật mẫu theo ngôn ngữ đang chọn (vi: historyName có dấu, en: name)
  String _artifactName(Map<String, dynamic> artifact) =>
      ((isVietnamese ? artifact['historyName'] : null) ?? artifact['name']).toString();

  /// Ảnh hiện vật: link rỗng hoặc tải lỗi (404, mất mạng) thì hiện khung thay thế, không báo lỗi
  Widget _artifactImage(String url) {
    final placeholder = Container(
      height: 190,
      width: double.infinity,
      color: const Color(0xFFE3E6DC),
      child: const Icon(Icons.account_balance, size: 48, color: Color(0xFF718B78)),
    );
    if (url.isEmpty) return placeholder;
    return Image.network(
      url,
      height: 190,
      width: double.infinity,
      fit: BoxFit.cover,
      errorBuilder: (_, _, _) => placeholder,
    );
  }

  Widget _pill(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(
        horizontal: 10,
        vertical: 6,
      ),
      decoration: BoxDecoration(
        color: const Color(0xFFECEDE5),
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
        ),
      ),
    );
  }
}