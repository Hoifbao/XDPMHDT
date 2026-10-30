import 'package:flutter/material.dart';

import '../features/auth/auth_service.dart';
import '../features/personalization/personalization_service.dart';

// ─────────────────────────────────────────────────────────────
//  DATA MODELS  (sẽ thay bằng API response)
// ─────────────────────────────────────────────────────────────

class ArtifactDetail {
  final int id;
  final String name;
  final String? artifactCode;
  final String? thumbnailUrl;
  final String? period;
  final String? material;
  final String? dimensions;
  final String? beaconUuid;
  final String locationName;
  final String status;
  final List<ArtifactContentItem> contents;

  const ArtifactDetail({
    required this.id,
    required this.name,
    this.artifactCode,
    this.thumbnailUrl,
    this.period,
    this.material,
    this.dimensions,
    this.beaconUuid,
    required this.locationName,
    required this.status,
    required this.contents,
  });
}

class ArtifactContentItem {
  final String languageCode;
  final String languageLabel;
  final String? title;
  final String? description;
  final String? audioUrl;
  final String? videoUrl;
  final List<String> imageUrls;
  final String? transcript;

  const ArtifactContentItem({
    required this.languageCode,
    required this.languageLabel,
    this.title,
    this.description,
    this.audioUrl,
    this.videoUrl,
    this.imageUrls = const [],
    this.transcript,
  });
}

// ─────────────────────────────────────────────────────────────
//  ARTIFACT DETAIL SCREEN
// ─────────────────────────────────────────────────────────────

class ArtifactDetailScreen extends StatefulWidget {
  final int artifactId;

  const ArtifactDetailScreen({super.key, required this.artifactId});

  @override
  State<ArtifactDetailScreen> createState() => _ArtifactDetailScreenState();
}

class _ArtifactDetailScreenState extends State<ArtifactDetailScreen>
    with SingleTickerProviderStateMixin {
  // Dữ liệu mẫu – sẽ thay bằng GET /api/artifacts/{id}
  late ArtifactDetail _artifact;

  late TabController _tabController;
  int _selectedLangIndex = 0;
  bool _isFavorite = false;

  static const _primaryBrown = Color(0xFF6B3A1F);
  static const _bgColor = Color(0xFFF5F0EB);

  @override
  void initState() {
    super.initState();
    _artifact = _sampleArtifact();
    _tabController = TabController(
      length: 4, // Mô tả | Hình ảnh | Âm thanh | Thông tin
      vsync: this,
    );
    _loadFavorite();
  }

  // ── Yêu thích (API của Phú) ───────────────────

  /// Đọc trạng thái yêu thích từ Backend khi đã đăng nhập.
  Future<void> _loadFavorite() async {
    if (!AuthService.isLoggedIn) return;
    try {
      final liked = await PersonalizationService.isFavorite(widget.artifactId);
      if (mounted) setState(() => _isFavorite = liked);
    } catch (_) {
      // Backend chưa chạy: giữ trạng thái mặc định
    }
  }

  Future<void> _toggleFavorite() async {
    final messenger = ScaffoldMessenger.of(context);
    if (!AuthService.isLoggedIn) {
      messenger.showSnackBar(
        const SnackBar(content: Text('Đăng nhập để lưu hiện vật yêu thích')),
      );
      return;
    }
    final next = !_isFavorite;
    setState(() => _isFavorite = next);
    final result = next
        ? await PersonalizationService.addFavorite(
            artifactId: widget.artifactId,
            artifactName: _artifact.name,
            siteName: _artifact.locationName,
            thumbnailUrl: _artifact.thumbnailUrl ?? '',
          )
        : await PersonalizationService.removeFavorite(widget.artifactId);
    if (!mounted) return;
    if (!result.success) {
      setState(() => _isFavorite = !next); // hoàn tác khi máy chủ từ chối
    }
    messenger.showSnackBar(SnackBar(content: Text(result.message)));
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  ArtifactDetail _sampleArtifact() {
    return const ArtifactDetail(
      id: 1,
      name: 'Trống đồng Đông Sơn',
      artifactCode: 'ART-001',
      period: 'Thế kỷ VII – I TCN',
      material: 'Đồng',
      dimensions: 'Đường kính: 60 cm, Cao: 40 cm',
      beaconUuid: '550e8400-e29b-41d4-a716-446655440000',
      locationName: 'Bảo tàng Lịch sử Quốc gia',
      status: 'ACTIVE',
      contents: [
        ArtifactContentItem(
          languageCode: 'vi',
          languageLabel: '🇻🇳 Tiếng Việt',
          title: 'Trống đồng Đông Sơn',
          description:
              'Trống đồng Đông Sơn là hiện vật tiêu biểu nhất của nền văn hóa Đông Sơn, '
              'xuất hiện vào khoảng thế kỷ VII đến thế kỷ I trước Công nguyên.\n\n'
              'Trống được đúc bằng đồng thau theo kỹ thuật khuôn đất sét, được sử dụng '
              'trong các nghi lễ tín ngưỡng, lễ cầu mưa, lễ hội mùa màng và trong '
              'chiến tranh để phát tín hiệu.\n\n'
              'Mặt trống được trang trí bằng hoa văn hình học đồng tâm và hình ảnh '
              'người, thuyền, chim, thú mang đậm bản sắc văn hóa Đông Nam Á cổ đại.',
          imageUrls: [],
          transcript:
              'Đây là trống đồng Đông Sơn, một trong những hiện vật quý giá nhất...',
        ),
        ArtifactContentItem(
          languageCode: 'en',
          languageLabel: '🇬🇧 English',
          title: 'Dong Son Bronze Drum',
          description:
              'The Dong Son Bronze Drum is the most iconic artifact of the Dong Son culture, '
              'dating from approximately the 7th to the 1st century BC.\n\n'
              'Cast using clay mold techniques, these drums were used in ritual ceremonies, '
              'rain-calling rites, harvest festivals, and as signaling instruments in warfare.\n\n'
              'The drum surface is decorated with concentric geometric patterns and depictions '
              'of people, boats, birds, and animals reflecting ancient Southeast Asian culture.',
          imageUrls: [],
          transcript:
              'This is the Dong Son Bronze Drum, one of the most precious artifacts...',
        ),
      ],
    );
  }

  // ─────────────────── BUILD ───────────────────

  @override
  Widget build(BuildContext context) {
    final content = _artifact.contents.isNotEmpty
        ? _artifact.contents[_selectedLangIndex]
        : null;

    return Scaffold(
      backgroundColor: _bgColor,
      body: NestedScrollView(
        headerSliverBuilder: (context, _) => [
          _buildSliverAppBar(content),
          _buildLanguageTabBar(),
          _buildMediaTabBar(),
        ],
        body: _buildTabBarView(content),
      ),
      bottomNavigationBar: _buildBottomActions(),
    );
  }

  // ── Sliver App Bar ────────────────────────────

  SliverAppBar _buildSliverAppBar(ArtifactContentItem? content) {
    return SliverAppBar(
      expandedHeight: 280,
      pinned: true,
      backgroundColor: _primaryBrown,
      foregroundColor: Colors.white,
      actions: [
        IconButton(
          icon: Icon(
            _isFavorite ? Icons.favorite : Icons.favorite_border,
            color: _isFavorite ? Colors.red.shade300 : Colors.white,
          ),
          tooltip: 'Yêu thích',
          onPressed: _toggleFavorite,
        ),
        IconButton(
          icon: const Icon(Icons.share_outlined),
          tooltip: 'Chia sẻ',
          onPressed: () {},
        ),
      ],
      flexibleSpace: FlexibleSpaceBar(
        title: Text(
          content?.title ?? _artifact.name,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: const TextStyle(
            fontSize: 14,
            fontWeight: FontWeight.bold,
            shadows: [Shadow(blurRadius: 4, color: Colors.black54)],
          ),
        ),
        background: Stack(
          fit: StackFit.expand,
          children: [
            // Hero image
            _buildHeroImage(),
            // Gradient overlay
            Container(
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.transparent,
                    Colors.black54,
                  ],
                  stops: [0.5, 1.0],
                ),
              ),
            ),
            // Beacon badge
            if (_artifact.beaconUuid != null)
              Positioned(
                top: 16,
                right: 16,
                child: _BeaconBadge(active: _artifact.status == 'ACTIVE'),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildHeroImage() {
    if (_artifact.thumbnailUrl != null) {
      return Image.network(_artifact.thumbnailUrl!,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => _placeholderHero());
    }
    return _placeholderHero();
  }

  Widget _placeholderHero() {
    return Container(
      color: const Color(0xFFEDD9C0),
      child: const Center(
        child: Icon(Icons.museum_outlined, size: 80, color: Color(0xFF6B3A1F)),
      ),
    );
  }

  // ── Language Selector ─────────────────────────

  SliverPersistentHeader _buildLanguageTabBar() {
    return SliverPersistentHeader(
      pinned: true,
      delegate: _StickyHeaderDelegate(
        child: Container(
          color: const Color(0xFF8B4A26),
          height: 44,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            itemCount: _artifact.contents.length,
            itemBuilder: (context, index) {
              final lang = _artifact.contents[index];
              final selected = _selectedLangIndex == index;
              return Padding(
                padding: const EdgeInsets.only(right: 8),
                child: GestureDetector(
                  onTap: () => setState(() => _selectedLangIndex = index),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 200),
                    padding: const EdgeInsets.symmetric(
                        horizontal: 12, vertical: 4),
                    decoration: BoxDecoration(
                      color: selected
                          ? Colors.white
                          : Colors.white.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      lang.languageLabel,
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: selected
                            ? FontWeight.bold
                            : FontWeight.normal,
                        color: selected ? _primaryBrown : Colors.white,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
        height: 44,
      ),
    );
  }

  // ── Media Tab Bar ─────────────────────────────

  SliverPersistentHeader _buildMediaTabBar() {
    return SliverPersistentHeader(
      pinned: true,
      delegate: _StickyHeaderDelegate(
        height: 46,
        child: Container(
          color: Colors.white,
          child: TabBar(
            controller: _tabController,
            labelColor: _primaryBrown,
            unselectedLabelColor: Colors.grey.shade500,
            indicatorColor: _primaryBrown,
            indicatorWeight: 2.5,
            labelStyle: const TextStyle(
                fontSize: 12, fontWeight: FontWeight.w600),
            tabs: const [
              Tab(icon: Icon(Icons.article_outlined, size: 18), text: 'Mô tả'),
              Tab(icon: Icon(Icons.photo_library_outlined, size: 18), text: 'Hình ảnh'),
              Tab(icon: Icon(Icons.volume_up_outlined, size: 18), text: 'Âm thanh'),
              Tab(icon: Icon(Icons.info_outline, size: 18), text: 'Thông tin'),
            ],
          ),
        ),
      ),
    );
  }

  // ── Tab Bar View ──────────────────────────────

  Widget _buildTabBarView(ArtifactContentItem? content) {
    return TabBarView(
      controller: _tabController,
      children: [
        _DescriptionTab(content: content),
        _GalleryTab(content: content),
        _AudioTab(content: content),
        _InfoTab(artifact: _artifact),
      ],
    );
  }

  // ── Bottom Actions ────────────────────────────

  Widget _buildBottomActions() {
    return SafeArea(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.08),
              blurRadius: 8,
              offset: const Offset(0, -2),
            ),
          ],
        ),
        child: Row(
          children: [
            // Nút đánh giá
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () => _showRatingDialog(),
                icon: const Icon(Icons.star_outline_rounded, size: 18),
                label: const Text('Đánh giá'),
                style: OutlinedButton.styleFrom(
                  foregroundColor: _primaryBrown,
                  side: const BorderSide(color: _primaryBrown),
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8)),
                  padding: const EdgeInsets.symmetric(vertical: 10),
                ),
              ),
            ),
            const SizedBox(width: 12),
            // Nút xem video
            Expanded(
              child: ElevatedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.play_circle_outline, size: 18),
                label: const Text('Xem video'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _primaryBrown,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8)),
                  padding: const EdgeInsets.symmetric(vertical: 10),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  void _showRatingDialog() {
    showDialog(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Đánh giá hiện vật'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text('Bạn cảm thấy thế nào về hiện vật này?'),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: List.generate(5, (i) => Padding(
                padding: const EdgeInsets.symmetric(horizontal: 4),
                child: GestureDetector(
                  onTap: () {},
                  child: Icon(Icons.star_rounded,
                      size: 36, color: Colors.amber.shade400),
                ),
              )),
            ),
          ],
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Huỷ')),
          ElevatedButton(
            onPressed: () => Navigator.pop(context),
            style: ElevatedButton.styleFrom(
                backgroundColor: _primaryBrown,
                foregroundColor: Colors.white),
            child: const Text('Gửi'),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  TAB: MÔ TẢ
// ─────────────────────────────────────────────────────────────

class _DescriptionTab extends StatelessWidget {
  final ArtifactContentItem? content;
  const _DescriptionTab({this.content});

  @override
  Widget build(BuildContext context) {
    if (content == null) {
      return const _EmptyContent(message: 'Chưa có nội dung mô tả.');
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Tiêu đề
          if (content!.title != null) ...[
            Text(
              content!.title!,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Color(0xFF3E2000),
              ),
            ),
            const SizedBox(height: 12),
            const Divider(height: 1),
            const SizedBox(height: 16),
          ],

          // Nội dung mô tả
          if (content!.description != null)
            Text(
              content!.description!,
              style: const TextStyle(
                fontSize: 15,
                height: 1.7,
                color: Color(0xFF4A3728),
              ),
            ),

          // Transcript
          if (content!.transcript != null) ...[
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: const Color(0xFF6B3A1F).withOpacity(0.06),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                    color: const Color(0xFF6B3A1F).withOpacity(0.2)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(children: [
                    Icon(Icons.record_voice_over_outlined,
                        size: 16, color: Color(0xFF6B3A1F)),
                    SizedBox(width: 6),
                    Text('Phụ đề thuyết minh',
                        style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                            color: Color(0xFF6B3A1F))),
                  ]),
                  const SizedBox(height: 8),
                  Text(
                    content!.transcript!,
                    style: TextStyle(
                        fontSize: 13,
                        color: Colors.brown.shade600,
                        height: 1.6),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  TAB: HÌNH ẢNH
// ─────────────────────────────────────────────────────────────

class _GalleryTab extends StatelessWidget {
  final ArtifactContentItem? content;
  const _GalleryTab({this.content});

  @override
  Widget build(BuildContext context) {
    final images = content?.imageUrls ?? [];

    if (images.isEmpty) {
      return const _EmptyContent(
          message: 'Chưa có hình ảnh cho hiện vật này.',
          icon: Icons.photo_library_outlined);
    }

    return GridView.builder(
      padding: const EdgeInsets.all(8),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 8,
        mainAxisSpacing: 8,
      ),
      itemCount: images.length,
      itemBuilder: (context, index) => ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.network(
          images[index],
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => Container(
            color: const Color(0xFFEDD9C0),
            child: const Center(
                child: Icon(Icons.broken_image_outlined,
                    color: Color(0xFF6B3A1F))),
          ),
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  TAB: ÂM THANH
// ─────────────────────────────────────────────────────────────

class _AudioTab extends StatelessWidget {
  final ArtifactContentItem? content;
  const _AudioTab({this.content});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          const SizedBox(height: 16),
          // Audio player UI
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              gradient: const LinearGradient(
                colors: [Color(0xFF6B3A1F), Color(0xFF9C5932)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: const Color(0xFF6B3A1F).withOpacity(0.3),
                  blurRadius: 16,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Column(
              children: [
                // Album art
                Container(
                  width: 120,
                  height: 120,
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.2),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.museum_outlined,
                      size: 56, color: Colors.white),
                ),
                const SizedBox(height: 16),

                // Title
                Text(
                  content?.title ?? 'Thuyết minh âm thanh',
                  textAlign: TextAlign.center,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  content?.languageLabel ?? '',
                  style: const TextStyle(
                      color: Colors.white70, fontSize: 13),
                ),
                const SizedBox(height: 20),

                // Progress bar
                SliderTheme(
                  data: SliderTheme.of(context).copyWith(
                    trackHeight: 3,
                    thumbShape:
                        const RoundSliderThumbShape(enabledThumbRadius: 6),
                    overlayShape:
                        const RoundSliderOverlayShape(overlayRadius: 12),
                    activeTrackColor: Colors.white,
                    inactiveTrackColor: Colors.white24,
                    thumbColor: Colors.white,
                    overlayColor: Colors.white24,
                  ),
                  child: Slider(
                    value: 0.35,
                    onChanged: (_) {},
                  ),
                ),

                // Time labels
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 4),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: const [
                      Text('0:42', style: TextStyle(color: Colors.white70, fontSize: 11)),
                      Text('2:00', style: TextStyle(color: Colors.white70, fontSize: 11)),
                    ],
                  ),
                ),
                const SizedBox(height: 12),

                // Controls
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    IconButton(
                      icon: const Icon(Icons.replay_10_rounded,
                          color: Colors.white, size: 28),
                      onPressed: () {},
                    ),
                    const SizedBox(width: 8),
                    Container(
                      width: 56,
                      height: 56,
                      decoration: BoxDecoration(
                        color: Colors.white,
                        shape: BoxShape.circle,
                        boxShadow: [
                          BoxShadow(
                              color: Colors.black.withOpacity(0.2),
                              blurRadius: 8)
                        ],
                      ),
                      child: const Icon(Icons.play_arrow_rounded,
                          color: Color(0xFF6B3A1F), size: 32),
                    ),
                    const SizedBox(width: 8),
                    IconButton(
                      icon: const Icon(Icons.forward_10_rounded,
                          color: Colors.white, size: 28),
                      onPressed: () {},
                    ),
                  ],
                ),
              ],
            ),
          ),

          if (content?.audioUrl == null) ...[
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                  color: Colors.orange.shade50,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: Colors.orange.shade200)),
              child: Row(children: [
                Icon(Icons.info_outline,
                    color: Colors.orange.shade600, size: 18),
                const SizedBox(width: 8),
                const Expanded(
                  child: Text(
                    'Chưa có file âm thanh cho ngôn ngữ này.',
                    style: TextStyle(fontSize: 13),
                  ),
                ),
              ]),
            ),
          ],
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  TAB: THÔNG TIN VẬT LÝ
// ─────────────────────────────────────────────────────────────

class _InfoTab extends StatelessWidget {
  final ArtifactDetail artifact;
  const _InfoTab({required this.artifact});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _InfoCard(
            title: 'Thông tin hiện vật',
            icon: Icons.info_outline,
            children: [
              _InfoRow(label: 'Mã hiện vật', value: artifact.artifactCode),
              _InfoRow(label: 'Niên đại', value: artifact.period),
              _InfoRow(label: 'Chất liệu', value: artifact.material),
              _InfoRow(label: 'Kích thước', value: artifact.dimensions),
              _InfoRow(label: 'Trạng thái', value: artifact.status == 'ACTIVE' ? 'Đang trưng bày' : 'Tạm ẩn'),
            ],
          ),
          const SizedBox(height: 12),
          _InfoCard(
            title: 'Địa điểm',
            icon: Icons.location_on_outlined,
            children: [
              _InfoRow(label: 'Di tích', value: artifact.locationName),
            ],
          ),
          if (artifact.beaconUuid != null) ...[
            const SizedBox(height: 12),
            _InfoCard(
              title: 'iBeacon',
              icon: Icons.bluetooth_searching_rounded,
              children: [
                _InfoRow(label: 'Beacon UUID', value: artifact.beaconUuid),
                _InfoRow(
                    label: 'Trạng thái',
                    value: artifact.status == 'ACTIVE' ? '🟢 Hoạt động' : '🔴 Không hoạt động'),
              ],
            ),
          ],
          const SizedBox(height: 12),
          _InfoCard(
            title: 'Nội dung ngôn ngữ',
            icon: Icons.language_outlined,
            children: artifact.contents
                .map((c) => _InfoRow(
                    label: c.languageLabel,
                    value: c.title ?? 'Không có tiêu đề'))
                .toList(),
          ),
        ],
      ),
    );
  }
}

class _InfoCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final List<Widget> children;

  const _InfoCard(
      {required this.title, required this.icon, required this.children});

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 1.5,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Icon(icon, size: 16, color: const Color(0xFF6B3A1F)),
              const SizedBox(width: 6),
              Text(title,
                  style: const TextStyle(
                      fontWeight: FontWeight.bold,
                      fontSize: 14,
                      color: Color(0xFF6B3A1F))),
            ]),
            const Divider(height: 16),
            ...children,
          ],
        ),
      ),
    );
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String? value;

  const _InfoRow({required this.label, this.value});

  @override
  Widget build(BuildContext context) {
    if (value == null) return const SizedBox.shrink();
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label,
                style: TextStyle(
                    fontSize: 13, color: Colors.brown.shade500)),
          ),
          const Text(' : ',
              style: TextStyle(fontSize: 13, color: Colors.grey)),
          Expanded(
            child: Text(value!,
                style: const TextStyle(
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                    color: Color(0xFF3E2000))),
          ),
        ],
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  HELPERS
// ─────────────────────────────────────────────────────────────

class _BeaconBadge extends StatelessWidget {
  final bool active;
  const _BeaconBadge({required this.active});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: active
            ? Colors.green.shade600.withOpacity(0.9)
            : Colors.grey.withOpacity(0.8),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: Colors.white54, width: 1),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.bluetooth_searching_rounded,
              color: Colors.white, size: 14),
          const SizedBox(width: 4),
          Text(
            active ? 'Beacon Active' : 'Beacon Off',
            style: const TextStyle(
                color: Colors.white,
                fontSize: 11,
                fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );
  }
}

class _EmptyContent extends StatelessWidget {
  final String message;
  final IconData icon;

  const _EmptyContent({
    required this.message,
    this.icon = Icons.article_outlined,
  });

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: Colors.brown.shade200),
            const SizedBox(height: 16),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(
                  color: Colors.brown.shade400, fontSize: 15, height: 1.5),
            ),
          ],
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  SLIVER STICKY HEADER DELEGATE
// ─────────────────────────────────────────────────────────────

class _StickyHeaderDelegate extends SliverPersistentHeaderDelegate {
  final Widget child;
  final double height;

  const _StickyHeaderDelegate({required this.child, required this.height});

  @override
  Widget build(_, double shrink, bool overlaps) => child;

  @override
  double get maxExtent => height;

  @override
  double get minExtent => height;

  @override
  bool shouldRebuild(_StickyHeaderDelegate old) =>
      child != old.child || height != old.height;
}
