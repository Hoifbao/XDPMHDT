import 'package:flutter/material.dart';
import 'artifact_detail_screen.dart';

// ─────────────────────────────────────────────────────────────
//  DATA MODEL (temporary local model – sẽ thay bằng API call)
// ─────────────────────────────────────────────────────────────

class ArtifactSummary {
  final int id;
  final String name;
  final String? thumbnailUrl;
  final String? period;
  final String? material;
  final String locationName;
  final String status;

  const ArtifactSummary({
    required this.id,
    required this.name,
    this.thumbnailUrl,
    this.period,
    this.material,
    required this.locationName,
    required this.status,
  });
}

// ─────────────────────────────────────────────────────────────
//  ARTIFACT LIST SCREEN
// ─────────────────────────────────────────────────────────────

class ArtifactListScreen extends StatefulWidget {
  const ArtifactListScreen({super.key});

  @override
  State<ArtifactListScreen> createState() => _ArtifactListScreenState();
}

class _ArtifactListScreenState extends State<ArtifactListScreen> {
  // Sample data – sẽ thay bằng HTTP call tới GET /api/artifacts
  final List<ArtifactSummary> _allArtifacts = const [
    ArtifactSummary(
      id: 1,
      name: 'Trống đồng Đông Sơn',
      period: 'Thế kỷ VII – I TCN',
      material: 'Đồng',
      locationName: 'Bảo tàng Lịch sử Quốc gia',
      status: 'ACTIVE',
    ),
    ArtifactSummary(
      id: 2,
      name: 'Tượng Phật A Di Đà',
      period: 'Thế kỷ XI',
      material: 'Đá',
      locationName: 'Chùa Một Cột',
      status: 'ACTIVE',
    ),
    ArtifactSummary(
      id: 3,
      name: 'Ấn vàng triều Nguyễn',
      period: 'Thế kỷ XIX',
      material: 'Vàng',
      locationName: 'Đại Nội Huế',
      status: 'ACTIVE',
    ),
    ArtifactSummary(
      id: 4,
      name: 'Gốm hoa lam thế kỷ XV',
      period: 'Thế kỷ XV',
      material: 'Gốm sứ',
      locationName: 'Bảo tàng Mỹ thuật',
      status: 'ACTIVE',
    ),
  ];

  List<ArtifactSummary> _filtered = [];
  final TextEditingController _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _filtered = List.from(_allArtifacts);
    _searchCtrl.addListener(_onSearch);
  }

  void _onSearch() {
    final q = _searchCtrl.text.toLowerCase();
    setState(() {
      _filtered = _allArtifacts.where((a) {
        return a.name.toLowerCase().contains(q) ||
            a.locationName.toLowerCase().contains(q) ||
            (a.period?.toLowerCase().contains(q) ?? false);
      }).toList();
    });
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  // ─────────────────── BUILD ───────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F0EB),
      appBar: _buildAppBar(),
      body: Column(
        children: [
          _buildSearchBar(),
          _buildResultCount(),
          Expanded(child: _buildArtifactGrid()),
        ],
      ),
      floatingActionButton: _buildFAB(),
    );
  }

  // ── App Bar ──────────────────────────────────

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      backgroundColor: const Color(0xFF6B3A1F),
      foregroundColor: Colors.white,
      title: const Row(
        children: [
          Icon(Icons.museum_outlined, size: 22),
          SizedBox(width: 8),
          Text(
            'Hiện Vật Di Sản',
            style: TextStyle(
              fontWeight: FontWeight.bold,
              fontSize: 18,
              letterSpacing: 0.5,
            ),
          ),
        ],
      ),
      actions: [
        IconButton(
          icon: const Icon(Icons.filter_list_rounded),
          tooltip: 'Bộ lọc',
          onPressed: _showFilterSheet,
        ),
        IconButton(
          icon: const Icon(Icons.sort_rounded),
          tooltip: 'Sắp xếp',
          onPressed: () {},
        ),
      ],
      elevation: 2,
    );
  }

  // ── Search Bar ───────────────────────────────

  Widget _buildSearchBar() {
    return Container(
      color: const Color(0xFF6B3A1F),
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: TextField(
        controller: _searchCtrl,
        style: const TextStyle(color: Colors.white),
        decoration: InputDecoration(
          hintText: 'Tìm kiếm hiện vật, địa điểm...',
          hintStyle: const TextStyle(color: Colors.white60),
          prefixIcon: const Icon(Icons.search, color: Colors.white70),
          suffixIcon: _searchCtrl.text.isNotEmpty
              ? IconButton(
                  icon: const Icon(Icons.close, color: Colors.white70),
                  onPressed: () {
                    _searchCtrl.clear();
                  },
                )
              : null,
          filled: true,
          fillColor: Colors.white.withOpacity(0.18),
          contentPadding:
              const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          border: OutlineInputBorder(
            borderRadius: BorderRadius.circular(30),
            borderSide: BorderSide.none,
          ),
        ),
      ),
    );
  }

  // ── Result count ─────────────────────────────

  Widget _buildResultCount() {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          Text(
            '${_filtered.length} hiện vật',
            style: TextStyle(
              color: Colors.brown.shade700,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
          ),
          const Spacer(),
          // Toggle list/grid
          Icon(Icons.grid_view_rounded,
              size: 20, color: Colors.brown.shade400),
        ],
      ),
    );
  }

  // ── Artifact Grid ────────────────────────────

  Widget _buildArtifactGrid() {
    if (_filtered.isEmpty) {
      return _buildEmptyState();
    }

    return GridView.builder(
      padding: const EdgeInsets.fromLTRB(12, 0, 12, 80),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 2,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
        childAspectRatio: 0.72,
      ),
      itemCount: _filtered.length,
      itemBuilder: (context, index) =>
          _ArtifactCard(artifact: _filtered[index]),
    );
  }

  // ── Empty State ──────────────────────────────

  Widget _buildEmptyState() {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.search_off_rounded,
              size: 64, color: Colors.brown.shade200),
          const SizedBox(height: 16),
          Text(
            'Không tìm thấy hiện vật',
            style: TextStyle(
                color: Colors.brown.shade600,
                fontSize: 16,
                fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 8),
          Text(
            'Thử từ khoá khác hoặc xoá bộ lọc',
            style: TextStyle(color: Colors.brown.shade400, fontSize: 13),
          ),
        ],
      ),
    );
  }

  // ── FAB ──────────────────────────────────────

  Widget _buildFAB() {
    return FloatingActionButton.extended(
      onPressed: () {
        // TODO: điều hướng tới màn hình tạo hiện vật mới (Admin)
      },
      backgroundColor: const Color(0xFF6B3A1F),
      foregroundColor: Colors.white,
      icon: const Icon(Icons.add),
      label: const Text('Thêm hiện vật'),
    );
  }

  // ── Filter Bottom Sheet ──────────────────────

  void _showFilterSheet() {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _FilterSheet(),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  ARTIFACT CARD  (Grid item)
// ─────────────────────────────────────────────────────────────

class _ArtifactCard extends StatelessWidget {
  final ArtifactSummary artifact;

  const _ArtifactCard({required this.artifact});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (_) => ArtifactDetailScreen(artifactId: artifact.id),
          ),
        );
      },
      child: Card(
        clipBehavior: Clip.hardEdge,
        elevation: 3,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Thumbnail
            Expanded(
              flex: 6,
              child: _buildThumbnail(),
            ),

            // Info
            Expanded(
              flex: 5,
              child: Padding(
                padding: const EdgeInsets.all(10),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Tên hiện vật
                    Text(
                      artifact.name,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                        color: Color(0xFF3E2000),
                      ),
                    ),
                    const SizedBox(height: 4),

                    // Niên đại
                    if (artifact.period != null)
                      Row(
                        children: [
                          Icon(Icons.history_rounded,
                              size: 12, color: Colors.brown.shade400),
                          const SizedBox(width: 4),
                          Expanded(
                            child: Text(
                              artifact.period!,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                  fontSize: 11, color: Colors.brown.shade500),
                            ),
                          ),
                        ],
                      ),
                    const SizedBox(height: 2),

                    // Chất liệu
                    if (artifact.material != null)
                      Row(
                        children: [
                          Icon(Icons.layers_outlined,
                              size: 12, color: Colors.brown.shade400),
                          const SizedBox(width: 4),
                          Text(
                            artifact.material!,
                            style: TextStyle(
                                fontSize: 11, color: Colors.brown.shade500),
                          ),
                        ],
                      ),

                    const Spacer(),

                    // Location badge
                    Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: const Color(0xFF6B3A1F).withOpacity(0.1),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.location_on_outlined,
                              size: 10, color: Colors.brown.shade600),
                          const SizedBox(width: 2),
                          Flexible(
                            child: Text(
                              artifact.locationName,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                  fontSize: 10, color: Colors.brown.shade600),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildThumbnail() {
    if (artifact.thumbnailUrl != null) {
      return Image.network(
        artifact.thumbnailUrl!,
        fit: BoxFit.cover,
        width: double.infinity,
        errorBuilder: (_, __, ___) => _placeholderImage(),
      );
    }
    return _placeholderImage();
  }

  Widget _placeholderImage() {
    return Container(
      color: const Color(0xFFEDD9C0),
      child: const Center(
        child: Icon(Icons.museum_outlined,
            size: 48, color: Color(0xFF6B3A1F)),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────
//  FILTER BOTTOM SHEET
// ─────────────────────────────────────────────────────────────

class _FilterSheet extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Handle
          Center(
            child: Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                  color: Colors.grey.shade300,
                  borderRadius: BorderRadius.circular(2)),
            ),
          ),
          const SizedBox(height: 16),
          const Text('Bộ lọc',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
          const SizedBox(height: 16),

          // Lọc theo địa điểm
          const Text('Địa điểm di tích',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              'Tất cả',
              'Hà Nội',
              'Huế',
              'Hội An',
              'TP.HCM',
            ]
                .map((loc) => FilterChip(
                      label: Text(loc),
                      selected: loc == 'Tất cả',
                      onSelected: (_) {},
                      selectedColor:
                          const Color(0xFF6B3A1F).withOpacity(0.2),
                    ))
                .toList(),
          ),

          const SizedBox(height: 16),

          // Lọc theo thời kỳ
          const Text('Thời kỳ lịch sử',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: [
              'Tất cả',
              'Tiền sử',
              'Đông Sơn',
              'Phong kiến',
              'Cận đại',
            ]
                .map((p) => FilterChip(
                      label: Text(p),
                      selected: p == 'Tất cả',
                      onSelected: (_) {},
                      selectedColor:
                          const Color(0xFF6B3A1F).withOpacity(0.2),
                    ))
                .toList(),
          ),

          const SizedBox(height: 20),

          // Buttons
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () => Navigator.pop(context),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: const Color(0xFF6B3A1F),
                    side: const BorderSide(color: Color(0xFF6B3A1F)),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8)),
                  ),
                  child: const Text('Đặt lại'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: ElevatedButton(
                  onPressed: () => Navigator.pop(context),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: const Color(0xFF6B3A1F),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8)),
                  ),
                  child: const Text('Áp dụng'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}


