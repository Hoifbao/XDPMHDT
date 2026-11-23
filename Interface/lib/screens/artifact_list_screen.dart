import 'dart:convert';

import 'package:flutter/material.dart';

import '../features/auth/auth_service.dart';
import 'artifact_detail_screen.dart';

// ─────────────────────────────────────────────────────────────
//  DATA MODEL  (map từ GET /api/artifacts – ArtifactSummaryResponse)
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

  factory ArtifactSummary.fromJson(Map<String, dynamic> json) {
    return ArtifactSummary(
      id: (json['id'] as num).toInt(),
      name: _text(json['name']) ?? 'Hiện vật #${json['id']}',
      thumbnailUrl: _mediaUrl(_text(json['thumbnailUrl'])),
      period: _text(json['period']),
      material: _text(json['material']),
      locationName: _text(json['locationName']) ?? '',
      status: _text(json['status']) ?? 'ACTIVE',
    );
  }

  static String? _text(dynamic v) {
    if (v == null) return null;
    final s = v.toString().trim();
    return s.isEmpty ? null : s;
  }

  // Ảnh upload (/api/media/files/...) luôn đi theo địa chỉ Gateway của app (vd 10.0.2.2 trên máy ảo)
  static String? _mediaUrl(String? raw) {
    if (raw == null) return null;
    final uri = Uri.tryParse(raw);
    if (uri != null && uri.path.startsWith('/api/media/files/')) {
      return Uri.parse(AuthService.baseUrl).resolve(uri.path).toString();
    }
    return raw;
  }
}

/// Bảo W8: thay dữ liệu mẫu bằng GET /api/artifacts (công khai, qua Gateway).
/// Chỉ hiện hiện vật ACTIVE cho du khách.
class ArtifactListApi {
  ArtifactListApi._();

  static const Duration _timeout = Duration(seconds: 10);

  static Future<List<ArtifactSummary>> fetchAll() async {
    final response = await AuthService.rawClient
        .get(Uri.parse('${AuthService.baseUrl}/api/artifacts'))
        .timeout(_timeout);
    if (response.statusCode != 200) {
      throw Exception('Không tải được danh sách hiện vật (mã ${response.statusCode}).');
    }
    final body = jsonDecode(utf8.decode(response.bodyBytes));
    final data = body is Map<String, dynamic> ? body['data'] : body;
    if (data is! List) return const [];
    return data
        .whereType<Map<String, dynamic>>()
        .map(ArtifactSummary.fromJson)
        .where((a) => a.status == 'ACTIVE')
        .toList();
  }
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
  List<ArtifactSummary> _allArtifacts = const [];
  List<ArtifactSummary> _filtered = const [];
  String? _selectedLocation; // null = tất cả di tích
  bool _loading = true;
  String? _error;
  final TextEditingController _searchCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _searchCtrl.addListener(_onSearch);
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await ArtifactListApi.fetchAll();
      if (!mounted) return;
      setState(() {
        _allArtifacts = items;
        _loading = false;
      });
      _applyFilters();
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = 'Không tải được danh sách hiện vật. Kiểm tra kết nối rồi thử lại.';
      });
    }
  }

  void _onSearch() => _applyFilters();

  void _applyFilters() {
    final q = _searchCtrl.text.toLowerCase();
    setState(() {
      _filtered = _allArtifacts.where((a) {
        final matchLocation =
            _selectedLocation == null || a.locationName == _selectedLocation;
        final matchText = q.isEmpty ||
            a.name.toLowerCase().contains(q) ||
            a.locationName.toLowerCase().contains(q) ||
            (a.period?.toLowerCase().contains(q) ?? false);
        return matchLocation && matchText;
      }).toList();
    });
  }

  List<String> get _locations {
    final set = <String>{
      for (final a in _allArtifacts)
        if (a.locationName.isNotEmpty) a.locationName,
    };
    return set.toList()..sort();
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
          if (!_loading && _error == null) _buildResultCount(),
          Expanded(child: _buildBody()),
        ],
      ),
    );
  }

  Widget _buildBody() {
    if (_loading) {
      return const Center(child: CircularProgressIndicator());
    }
    if (_error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.cloud_off_rounded, size: 56, color: Colors.brown.shade300),
              const SizedBox(height: 12),
              Text(_error!, textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.brown.shade700)),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                onPressed: _load,
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF6B3A1F),
                  foregroundColor: Colors.white,
                ),
              ),
            ],
          ),
        ),
      );
    }
    return RefreshIndicator(onRefresh: _load, child: _buildArtifactGrid());
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
          fillColor: Colors.white.withValues(alpha: 0.18),
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

  Widget _buildArtifactGrid() {
    if (_filtered.isEmpty) {
      return _buildEmptyState();
    }

    return GridView.builder(
      physics: const AlwaysScrollableScrollPhysics(),
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

  // ── Filter Bottom Sheet ──────────────────────

  Future<void> _showFilterSheet() async {
    final result = await showModalBottomSheet<_FilterResult>(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (_) => _FilterSheet(
        locations: _locations,
        selected: _selectedLocation,
      ),
    );
    if (result == null) return;
    _selectedLocation = result.location;
    _applyFilters();
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
                        color: const Color(0xFF6B3A1F).withValues(alpha: 0.1),
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
        errorBuilder: (_, _, _) => _placeholderImage(),
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
//  FILTER BOTTOM SHEET  (lọc theo di tích lấy từ dữ liệu thật)
// ─────────────────────────────────────────────────────────────

class _FilterResult {
  final String? location;
  const _FilterResult(this.location);
}

class _FilterSheet extends StatefulWidget {
  final List<String> locations;
  final String? selected;

  const _FilterSheet({required this.locations, required this.selected});

  @override
  State<_FilterSheet> createState() => _FilterSheetState();
}

class _FilterSheetState extends State<_FilterSheet> {
  late String? _current = widget.selected;

  Widget _chip(String label, String? value) {
    return FilterChip(
      label: Text(label),
      selected: _current == value,
      onSelected: (_) => setState(() => _current = value),
      selectedColor: const Color(0x336B3A1F),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
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
          const Text('Di tích',
              style: TextStyle(fontWeight: FontWeight.w600, fontSize: 14)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 4,
            children: [
              _chip('Tất cả', null),
              for (final loc in widget.locations) _chip(loc, loc),
            ],
          ),
          const SizedBox(height: 20),
          Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  onPressed: () =>
                      Navigator.pop(context, const _FilterResult(null)),
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
                  onPressed: () =>
                      Navigator.pop(context, _FilterResult(_current)),
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
