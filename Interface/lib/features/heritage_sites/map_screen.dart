import 'package:flutter/material.dart';

// Màu đồng bộ với main.dart
const Color _sage = Color(0xFF718B78);
const Color _cream = Color(0xFFF7F5EF);
const Color _softCard = Color(0xFFECEDE5);
const Color _ink = Color(0xFF26352B);
const Color _muted = Color(0xFF69716B);

/// Dữ liệu di tích mẫu – khớp với Infra/init_db.sql.
/// Khi API sẵn sàng, thay bằng lời gọi GET /api/heritage-sites.
const List<Map<String, dynamic>> _sampleSites = [
  {
    'id': 1,
    'name': 'Văn Miếu - Quốc Tử Giám',
    'location': '58 Quốc Tử Giám, Đống Đa, Hà Nội',
    'latitude': 21.0277644,
    'longitude': 105.8354028,
    'artifactCount': 2,
  },
  {
    'id': 2,
    'name': 'Hoàng thành Thăng Long',
    'location': '19C Hoàng Diệu, Ba Đình, Hà Nội',
    'latitude': 21.0354430,
    'longitude': 105.8349380,
    'artifactCount': 1,
  },
];

/// Màn hình Bản đồ Di tích (US03) – khung UI tĩnh.
/// Chưa tích hợp thư viện bản đồ thật; sử dụng placeholder đồng bộ
/// với phong cách thiết kế hiện có trong main.dart.
class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  int? _selectedSiteId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _cream,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        scrolledUnderElevation: 0,
        iconTheme: const IconThemeData(color: _ink),
        title: const Text(
          'Bản đồ Di tích',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: _ink,
          ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: _pill('${_sampleSites.length} di tích'),
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            // ── Khu vực bản đồ ──────────────────────────────────────────
            Expanded(
              flex: 5,
              child: _buildMapArea(),
            ),
            // ── Danh sách di tích ────────────────────────────────────────
            Expanded(
              flex: 4,
              child: _buildSiteList(),
            ),
          ],
        ),
      ),
    );
  }

  // ─── Bản đồ placeholder ────────────────────────────────────────────────────

  Widget _buildMapArea() {
    return Container(
      margin: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        color: const Color(0xFFDFE8DC),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.06),
            blurRadius: 12,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Stack(
        children: [
          // Nền bản đồ giả
          _buildMapBackground(),
          // Các pin di tích
          ..._buildMapPins(),
          // Label placeholder
          _buildMapPlaceholderLabel(),
          // Nút zoom giả
          _buildZoomButtons(),
        ],
      ),
    );
  }

  Widget _buildMapBackground() {
    return Positioned.fill(
      child: CustomPaint(
        painter: _MapGridPainter(),
      ),
    );
  }

  List<Widget> _buildMapPins() {
    // Vị trí tương đối các pin trên placeholder map
    final pinPositions = [
      const Alignment(-0.3, -0.1), // Văn Miếu
      const Alignment(0.4, -0.5),  // Hoàng thành
    ];

    return List.generate(_sampleSites.length, (i) {
      final site = _sampleSites[i];
      final isSelected = _selectedSiteId == site['id'];
      return Align(
        alignment: pinPositions[i % pinPositions.length],
        child: GestureDetector(
          onTap: () => setState(() {
            _selectedSiteId = isSelected ? null : site['id'] as int;
          }),
          child: AnimatedScale(
            scale: isSelected ? 1.25 : 1.0,
            duration: const Duration(milliseconds: 200),
            child: _MapPin(
              label: site['name'] as String,
              isSelected: isSelected,
            ),
          ),
        ),
      );
    });
  }

  Widget _buildMapPlaceholderLabel() {
    return Positioned(
      bottom: 12,
      left: 0,
      right: 0,
      child: Center(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.85),
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Text(
            'Bản đồ tương tác – tích hợp sau',
            style: TextStyle(fontSize: 12, color: _muted),
          ),
        ),
      ),
    );
  }

  Widget _buildZoomButtons() {
    return Positioned(
      top: 12,
      right: 12,
      child: Column(
        children: [
          _mapButton(Icons.add),
          const SizedBox(height: 4),
          _mapButton(Icons.remove),
          const SizedBox(height: 8),
          _mapButton(Icons.my_location),
        ],
      ),
    );
  }

  Widget _mapButton(IconData icon) {
    return Container(
      width: 36,
      height: 36,
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.08),
            blurRadius: 6,
          ),
        ],
      ),
      child: Icon(icon, size: 18, color: _ink),
    );
  }

  // ─── Danh sách di tích ──────────────────────────────────────────────────────

  Widget _buildSiteList() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 16, 20, 10),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Di tích gần đây',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: _ink,
                ),
              ),
              _pill('${_sampleSites.length} địa điểm'),
            ],
          ),
        ),
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            itemCount: _sampleSites.length,
            separatorBuilder: (_, __) => const SizedBox(height: 10),
            itemBuilder: (context, index) {
              return _SiteCard(
                site: _sampleSites[index],
                isSelected: _selectedSiteId == _sampleSites[index]['id'],
                onTap: () => setState(() {
                  final id = _sampleSites[index]['id'] as int;
                  _selectedSiteId = _selectedSiteId == id ? null : id;
                }),
              );
            },
          ),
        ),
      ],
    );
  }

  // ─── Tiện ích ──────────────────────────────────────────────────────────────

  Widget _pill(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
        color: _softCard,
        borderRadius: BorderRadius.circular(20),
      ),
      child: Text(
        text,
        style: const TextStyle(
          fontSize: 12,
          fontWeight: FontWeight.w600,
          color: _ink,
        ),
      ),
    );
  }
}

// ─── Widget con ───────────────────────────────────────────────────────────────

class _MapPin extends StatelessWidget {
  final String label;
  final bool isSelected;

  const _MapPin({required this.label, required this.isSelected});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        if (isSelected)
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            margin: const EdgeInsets.only(bottom: 4),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(8),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 6,
                ),
              ],
            ),
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 11,
                fontWeight: FontWeight.bold,
                color: _ink,
              ),
            ),
          ),
        Container(
          width: isSelected ? 22 : 18,
          height: isSelected ? 22 : 18,
          decoration: BoxDecoration(
            color: isSelected ? _sage : const Color(0xFFA45151),
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 3),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.15),
                blurRadius: 6,
                offset: const Offset(0, 2),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _SiteCard extends StatelessWidget {
  final Map<String, dynamic> site;
  final bool isSelected;
  final VoidCallback onTap;

  const _SiteCard({
    required this.site,
    required this.isSelected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFE8F0E9) : Colors.white,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(
            color: isSelected ? _sage : Colors.transparent,
            width: 1.5,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withOpacity(0.04),
              blurRadius: 8,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Row(
          children: [
            // Icon di tích
            Container(
              width: 44,
              height: 44,
              decoration: BoxDecoration(
                color: isSelected ? _sage : const Color(0xFFECEDE5),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                Icons.account_balance_outlined,
                color: isSelected ? Colors.white : _sage,
                size: 22,
              ),
            ),
            const SizedBox(width: 14),
            // Thông tin
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    site['name'] as String,
                    style: const TextStyle(
                      fontSize: 15,
                      fontWeight: FontWeight.bold,
                      color: _ink,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 3),
                  Text(
                    site['location'] as String,
                    style: const TextStyle(fontSize: 12, color: _muted),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  const SizedBox(height: 5),
                  Row(
                    children: [
                      const Icon(Icons.explore_outlined, size: 12, color: _sage),
                      const SizedBox(width: 4),
                      Text(
                        '${(site['latitude'] as double).toStringAsFixed(4)}, '
                        '${(site['longitude'] as double).toStringAsFixed(4)}',
                        style: const TextStyle(fontSize: 11, color: _sage),
                      ),
                      const SizedBox(width: 10),
                      const Icon(Icons.museum_outlined, size: 12, color: _muted),
                      const SizedBox(width: 4),
                      Text(
                        '${site['artifactCount']} hiện vật',
                        style: const TextStyle(fontSize: 11, color: _muted),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            // Arrow
            Icon(
              Icons.chevron_right,
              color: isSelected ? _sage : _muted,
              size: 20,
            ),
          ],
        ),
      ),
    );
  }
}

// ─── Custom Painter – nền bản đồ giả ─────────────────────────────────────────

class _MapGridPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    // Gradient nền
    final bgPaint = Paint()
      ..shader = const LinearGradient(
        begin: Alignment.topLeft,
        end: Alignment.bottomRight,
        colors: [Color(0xFFE7EEE4), Color(0xFFDFE8DC)],
      ).createShader(Rect.fromLTWH(0, 0, size.width, size.height));
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, size.height), bgPaint);

    // Lưới đường chéo nhẹ
    final linePaint = Paint()
      ..color = Colors.white.withOpacity(0.45)
      ..strokeWidth = 1.0;

    const spacing = 32.0;
    for (double x = 0; x < size.width + size.height; x += spacing) {
      canvas.drawLine(Offset(x, 0), Offset(x - size.height, size.height), linePaint);
    }

    // Blob màu xanh lá nhẹ (mô phỏng vùng cây xanh)
    final greenPaint = Paint()..color = const Color(0xFFC9DDCA).withOpacity(0.7);
    canvas.drawCircle(
      Offset(size.width * 0.25, size.height * 0.35),
      size.width * 0.08,
      greenPaint,
    );

    // Blob vàng nhạt (mô phỏng vùng đất)
    final goldPaint = Paint()..color = const Color(0xFFD6C79F).withOpacity(0.6);
    canvas.drawCircle(
      Offset(size.width * 0.72, size.height * 0.58),
      size.width * 0.09,
      goldPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
