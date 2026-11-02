import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:geolocator/geolocator.dart';
import 'package:latlong2/latlong.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../screens/artifact_list_screen.dart';
import 'heritage_site_service.dart';

// ─── Màu dùng chung với main.dart ────────────────────────────────────────────
const Color _sage = Color(0xFF718B78);
const Color _cream = Color(0xFFF7F5EF);
const Color _softCard = Color(0xFFECEDE5);
const Color _ink = Color(0xFF26352B);
const Color _muted = Color(0xFF69716B);

// ─────────────────────────────────────────────────────────────────────────────
//  MapScreen – Bản đồ Di tích tương tác (US03, Ân W5)
// ─────────────────────────────────────────────────────────────────────────────
///
/// Luồng:
/// 1. Xin quyền GPS → lấy vị trí hiện tại.
/// 2. Gọi GET /api/heritage-sites/nearby theo vị trí (bán kính 50 km).
/// 3. Vẽ marker từng di tích; bấm marker → bảng thông tin + nút hành động.
/// 4. "Chỉ đường" → mở Google Maps / app bản đồ điện thoại.
/// 5. "Xem hiện vật" → ArtifactListScreen.
/// 6. Từ chối quyền → hiện tất cả di tích + banner cảnh báo.
/// 7. Mất mạng → thông báo rõ ràng, không crash.
class MapScreen extends StatefulWidget {
  const MapScreen({super.key});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  bool _loadingLocation = true;
  bool _loadingSites = false;
  String? _locationError;
  String? _sitesError;

  Position? _currentPosition;
  List<HeritageSiteItem> _sites = [];
  HeritageSiteItem? _selectedSite;

  final MapController _mapController = MapController();

  // Hà Nội – trung tâm mặc định khi chưa có GPS
  static const LatLng _defaultCenter = LatLng(21.0277644, 105.8354028);

  @override
  void initState() {
    super.initState();
    _initLocation();
  }

  // ── GPS ──────────────────────────────────────────────────────────────────

  Future<void> _initLocation() async {
    setState(() {
      _loadingLocation = true;
      _locationError = null;
    });

    try {
      final position = await _getLocation();
      if (!mounted) return;
      setState(() {
        _currentPosition = position;
        _loadingLocation = false;
      });
      await _loadNearbySites(position);
    } on _PermissionDenied {
      if (!mounted) return;
      setState(() {
        _loadingLocation = false;
        _locationError =
            'Chưa cấp quyền vị trí. Vào Cài đặt → Ứng dụng → Smart Heritage '
            '→ Quyền → Vị trí để bật.';
      });
      await _loadAllSites();
    } on _ServiceDisabled {
      if (!mounted) return;
      setState(() {
        _loadingLocation = false;
        _locationError =
            'Dịch vụ vị trí đang tắt. Hãy bật GPS trong Cài đặt điện thoại.';
      });
      await _loadAllSites();
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loadingLocation = false;
        _locationError = 'Không lấy được vị trí: $e';
      });
      await _loadAllSites();
    }
  }

  Future<Position> _getLocation() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw const _ServiceDisabled();
    }
    LocationPermission perm = await Geolocator.checkPermission();
    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission();
    }
    if (perm == LocationPermission.denied ||
        perm == LocationPermission.deniedForever) {
      throw const _PermissionDenied();
    }
    return Geolocator.getCurrentPosition(
      locationSettings: const LocationSettings(
        accuracy: LocationAccuracy.high,
        timeLimit: Duration(seconds: 10),
      ),
    );
  }

  // ── Load sites ────────────────────────────────────────────────────────────

  Future<void> _loadNearbySites(Position pos) async {
    setState(() {
      _loadingSites = true;
      _sitesError = null;
    });
    try {
      final list = await HeritageSiteService.getNearby(
        lat: pos.latitude,
        lng: pos.longitude,
        radius: 50,
      );
      if (!mounted) return;
      setState(() {
        _sites = list;
        _loadingSites = false;
      });
      _mapController.move(LatLng(pos.latitude, pos.longitude), 13);
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loadingSites = false;
        _sitesError = 'Không tải được danh sách di tích. Kiểm tra kết nối mạng.';
      });
    }
  }

  Future<void> _loadAllSites() async {
    setState(() {
      _loadingSites = true;
      _sitesError = null;
    });
    try {
      final list = await HeritageSiteService.getAll();
      if (!mounted) return;
      setState(() {
        _sites = list;
        _loadingSites = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _loadingSites = false;
        _sitesError = 'Không tải được danh sách di tích. Kiểm tra kết nối mạng.';
      });
    }
  }

  // ── Chỉ đường ─────────────────────────────────────────────────────────────

  Future<void> _openDirections(HeritageSiteItem site) async {
    if (!site.hasCoords) return;
    final lat = site.latitude!;
    final lng = site.longitude!;
    final googleUri = Uri.parse(
        'https://www.google.com/maps/dir/?api=1'
        '&destination=$lat,$lng&travelmode=walking');
    final geoUri = Uri.parse(
        'geo:$lat,$lng?q=$lat,$lng(${Uri.encodeComponent(site.name)})');

    if (await canLaunchUrl(googleUri)) {
      await launchUrl(googleUri, mode: LaunchMode.externalApplication);
    } else if (await canLaunchUrl(geoUri)) {
      await launchUrl(geoUri, mode: LaunchMode.externalApplication);
    } else if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
            content: Text('Không tìm thấy ứng dụng bản đồ trên thiết bị.')),
      );
    }
  }

  // ── Bấm marker ────────────────────────────────────────────────────────────

  void _onMarkerTap(HeritageSiteItem site) {
    setState(() => _selectedSite = site);
    if (site.hasCoords) {
      _mapController.move(LatLng(site.latitude!, site.longitude!), 15);
    }
  }

  // ── BUILD ─────────────────────────────────────────────────────────────────

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _cream,
      appBar: _buildAppBar(),
      body: SafeArea(
        child: Column(
          children: [
            if (_locationError != null) _buildLocationBanner(),
            Expanded(flex: 5, child: _buildMap()),
            Expanded(
              flex: 4,
              child: _selectedSite != null
                  ? _buildSiteDetail(_selectedSite!)
                  : _buildSiteList(),
            ),
          ],
        ),
      ),
      floatingActionButton: _currentPosition != null
          ? FloatingActionButton.small(
              backgroundColor: _sage,
              foregroundColor: Colors.white,
              tooltip: 'Về vị trí của tôi',
              onPressed: () => _mapController.move(
                LatLng(_currentPosition!.latitude, _currentPosition!.longitude),
                14,
              ),
              child: const Icon(Icons.my_location),
            )
          : null,
    );
  }

  // ── App Bar ───────────────────────────────────────────────────────────────

  PreferredSizeWidget _buildAppBar() {
    return AppBar(
      backgroundColor: Colors.transparent,
      elevation: 0,
      scrolledUnderElevation: 0,
      iconTheme: const IconThemeData(color: _ink),
      title: const Text(
        'Bản đồ Di tích',
        style: TextStyle(fontWeight: FontWeight.bold, color: _ink),
      ),
      actions: [
        if (_loadingSites)
          const Padding(
            padding: EdgeInsets.only(right: 16),
            child: SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(strokeWidth: 2, color: _sage),
            ),
          )
        else
          Padding(
            padding: const EdgeInsets.only(right: 12),
            child: _pill('${_sites.length} di tích'),
          ),
        IconButton(
          icon: const Icon(Icons.refresh, color: _ink),
          tooltip: 'Làm mới',
          onPressed: _initLocation,
        ),
      ],
    );
  }

  // ── Banner cảnh báo GPS ───────────────────────────────────────────────────

  Widget _buildLocationBanner() {
    return Container(
      width: double.infinity,
      margin: const EdgeInsets.fromLTRB(12, 6, 12, 0),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: const Color(0xFFFFF3CD),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFFFFD566)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.location_off_outlined,
              size: 18, color: Color(0xFF856404)),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              _locationError!,
              style: const TextStyle(fontSize: 12, color: Color(0xFF856404)),
            ),
          ),
          GestureDetector(
            onTap: _initLocation,
            child: const Text(
              'Thử lại',
              style: TextStyle(
                  fontSize: 12, fontWeight: FontWeight.bold, color: _sage),
            ),
          ),
        ],
      ),
    );
  }

  // ── Bản đồ flutter_map + OpenStreetMap ───────────────────────────────────

  Widget _buildMap() {
    final center = _currentPosition != null
        ? LatLng(_currentPosition!.latitude, _currentPosition!.longitude)
        : _defaultCenter;

    return Container(
      margin: const EdgeInsets.fromLTRB(12, 8, 12, 0),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.07),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: FlutterMap(
        mapController: _mapController,
        options: MapOptions(
          initialCenter: center,
          initialZoom: 13,
          onTap: (_, __) => setState(() => _selectedSite = null),
        ),
        children: [
          // Tile OpenStreetMap – không cần API key
          TileLayer(
            urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
            userAgentPackageName: 'com.smartheritage.app',
            maxZoom: 19,
          ),

          // Vòng tròn vị trí người dùng
          if (_currentPosition != null)
            CircleLayer(
              circles: [
                CircleMarker(
                  point: LatLng(
                    _currentPosition!.latitude,
                    _currentPosition!.longitude,
                  ),
                  radius: 10,
                  color: _sage.withOpacity(0.25),
                  borderColor: _sage,
                  borderStrokeWidth: 2,
                  useRadiusInMeter: false,
                ),
              ],
            ),

          // Marker di tích
          MarkerLayer(
            markers: _sites
                .where((s) => s.hasCoords)
                .map(
                  (site) => Marker(
                    point: LatLng(site.latitude!, site.longitude!),
                    width: 44,
                    height: 52,
                    child: GestureDetector(
                      onTap: () => _onMarkerTap(site),
                      child: _MarkerPin(
                        isSelected: _selectedSite?.id == site.id,
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
        ],
      ),
    );
  }

  // ── Bảng chi tiết di tích đã chọn ────────────────────────────────────────

  Widget _buildSiteDetail(HeritageSiteItem site) {
    return Container(
      margin: const EdgeInsets.fromLTRB(12, 10, 12, 8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
              color: Colors.black.withOpacity(0.05),
              blurRadius: 8,
              offset: const Offset(0, 2))
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                    color: _sage, borderRadius: BorderRadius.circular(10)),
                child: const Icon(Icons.account_balance_outlined,
                    color: Colors.white, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      site.name,
                      style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: _ink),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    if (site.location != null)
                      Text(site.location!,
                          style: const TextStyle(fontSize: 12, color: _muted),
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis),
                  ],
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: _muted, size: 20),
                onPressed: () => setState(() => _selectedSite = null),
              ),
            ],
          ),
          if (site.distanceKm != null)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Row(
                children: [
                  const Icon(Icons.near_me, size: 13, color: _sage),
                  const SizedBox(width: 4),
                  Text(
                    'Cách bạn ${site.distanceKm!.toStringAsFixed(1)} km',
                    style: const TextStyle(fontSize: 12, color: _sage),
                  ),
                ],
              ),
            ),
          const Spacer(),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed:
                      site.hasCoords ? () => _openDirections(site) : null,
                  icon: const Icon(Icons.directions, size: 16),
                  label: const Text('Chỉ đường'),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: _sage,
                    side: const BorderSide(color: _sage),
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                    padding: const EdgeInsets.symmetric(vertical: 10),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                        builder: (_) => const ArtifactListScreen()),
                  ),
                  icon: const Icon(Icons.museum_outlined, size: 16),
                  label: const Text('Xem hiện vật'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: _sage,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12)),
                    padding: const EdgeInsets.symmetric(vertical: 10),
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  // ── Danh sách di tích ─────────────────────────────────────────────────────

  Widget _buildSiteList() {
    if (_loadingSites) {
      return const Center(child: CircularProgressIndicator(color: _sage));
    }
    if (_sitesError != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.cloud_off_outlined, size: 40, color: _muted),
              const SizedBox(height: 12),
              Text(_sitesError!,
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: _muted, fontSize: 13)),
              const SizedBox(height: 12),
              ElevatedButton(
                onPressed: _initLocation,
                style: ElevatedButton.styleFrom(
                    backgroundColor: _sage, foregroundColor: Colors.white),
                child: const Text('Thử lại'),
              ),
            ],
          ),
        ),
      );
    }
    if (_sites.isEmpty) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.map_outlined, size: 40, color: _muted),
            const SizedBox(height: 8),
            const Text('Chưa có di tích trong khu vực này.',
                style: TextStyle(color: _muted)),
            TextButton(
              onPressed: _loadAllSites,
              child: const Text('Xem tất cả di tích',
                  style: TextStyle(color: _sage)),
            ),
          ],
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                _currentPosition != null ? 'Di tích gần bạn' : 'Danh sách di tích',
                style: const TextStyle(
                    fontSize: 16, fontWeight: FontWeight.bold, color: _ink),
              ),
              _pill('${_sites.length} địa điểm'),
            ],
          ),
        ),
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            itemCount: _sites.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
            itemBuilder: (_, i) => _SiteListTile(
              site: _sites[i],
              isSelected: _selectedSite?.id == _sites[i].id,
              onTap: () => _onMarkerTap(_sites[i]),
            ),
          ),
        ),
      ],
    );
  }

  Widget _pill(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
      decoration: BoxDecoration(
          color: _softCard, borderRadius: BorderRadius.circular(20)),
      child: Text(text,
          style: const TextStyle(
              fontSize: 12, fontWeight: FontWeight.w600, color: _ink)),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Widgets con
// ─────────────────────────────────────────────────────────────────────────────

class _MarkerPin extends StatelessWidget {
  final bool isSelected;
  const _MarkerPin({required this.isSelected});

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        AnimatedContainer(
          duration: const Duration(milliseconds: 200),
          width: isSelected ? 26 : 20,
          height: isSelected ? 26 : 20,
          decoration: BoxDecoration(
            color: isSelected ? _sage : const Color(0xFFA45151),
            shape: BoxShape.circle,
            border: Border.all(color: Colors.white, width: 3),
            boxShadow: [
              BoxShadow(
                  color: Colors.black.withOpacity(0.2),
                  blurRadius: 6,
                  offset: const Offset(0, 2))
            ],
          ),
          child: Icon(Icons.account_balance_outlined,
              size: isSelected ? 13 : 10, color: Colors.white),
        ),
        Container(
          width: 2,
          height: 8,
          color: isSelected ? _sage : const Color(0xFFA45151),
        ),
      ],
    );
  }
}

class _SiteListTile extends StatelessWidget {
  final HeritageSiteItem site;
  final bool isSelected;
  final VoidCallback onTap;

  const _SiteListTile({
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
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: isSelected ? const Color(0xFFE8F0E9) : Colors.white,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
              color: isSelected ? _sage : Colors.transparent, width: 1.5),
          boxShadow: [
            BoxShadow(
                color: Colors.black.withOpacity(0.04),
                blurRadius: 6,
                offset: const Offset(0, 2))
          ],
        ),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                  color: isSelected ? _sage : _softCard,
                  borderRadius: BorderRadius.circular(10)),
              child: Icon(Icons.account_balance_outlined,
                  color: isSelected ? Colors.white : _sage, size: 20),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(site.name,
                      style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: _ink),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis),
                  if (site.location != null)
                    Text(site.location!,
                        style: const TextStyle(fontSize: 12, color: _muted),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis),
                  if (site.distanceKm != null)
                    Text('${site.distanceKm!.toStringAsFixed(1)} km',
                        style: const TextStyle(fontSize: 11, color: _sage)),
                ],
              ),
            ),
            Icon(Icons.chevron_right,
                color: isSelected ? _sage : _muted, size: 20),
          ],
        ),
      ),
    );
  }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Exception nội bộ
// ─────────────────────────────────────────────────────────────────────────────

class _PermissionDenied implements Exception {
  const _PermissionDenied();
}

class _ServiceDisabled implements Exception {
  const _ServiceDisabled();
}
