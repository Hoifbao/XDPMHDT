import 'package:flutter/material.dart';

import '../personalization/personalization_service.dart';

const _cream = Color(0xFFF7F5EF);
const _sage = Color(0xFF718B78);
const _ink = Color(0xFF26352B);

class VisitHistoryScreen extends StatefulWidget {
  const VisitHistoryScreen({super.key});

  @override
  State<VisitHistoryScreen> createState() => _VisitHistoryScreenState();
}

class _VisitHistoryScreenState extends State<VisitHistoryScreen> {
  bool _loading = true;
  bool _fromCache = false;
  String? _warning;
  List<Map<String, dynamic>> _items = [];

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() => _loading = true);
    final result = await PersonalizationService.loadVisitHistory();
    if (!mounted) return;
    setState(() {
      _items = result.items;
      _fromCache = result.fromCache;
      _warning = result.warning;
      _loading = false;
    });
  }

  String _string(Map<String, dynamic> item, String key, [String fallback = '']) =>
      item[key]?.toString() ?? fallback;

  String _date(String raw) {
    final parsed = DateTime.tryParse(raw)?.toLocal();
    if (parsed == null) return raw.isEmpty ? 'Thời điểm không rõ' : raw;
    String two(int v) => v.toString().padLeft(2, '0');
    return '${two(parsed.day)}/${two(parsed.month)}/${parsed.year} ${two(parsed.hour)}:${two(parsed.minute)}';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: _cream,
      appBar: AppBar(
        title: const Text('Lịch sử tham quan', style: TextStyle(color: _ink, fontWeight: FontWeight.bold)),
        backgroundColor: Colors.transparent,
        elevation: 0,
        iconTheme: const IconThemeData(color: _ink),
        actions: [IconButton(onPressed: _load, tooltip: 'Làm mới / đồng bộ', icon: const Icon(Icons.sync))],
      ),
      body: SafeArea(
        child: _loading
            ? const Center(child: CircularProgressIndicator(color: _sage))
            : RefreshIndicator(
                onRefresh: _load,
                color: _sage,
                child: ListView(
                  padding: const EdgeInsets.all(16),
                  children: [
                    if (_warning != null)
                      Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(color: const Color(0xFFECEDE5), borderRadius: BorderRadius.circular(12)),
                        child: Row(children: [
                          Icon(_fromCache ? Icons.cloud_off : Icons.sync_problem, color: _sage),
                          const SizedBox(width: 8),
                          Expanded(child: Text(_warning!, style: const TextStyle(color: _ink))),
                        ]),
                      ),
                    if (_items.isEmpty)
                      const Padding(
                        padding: EdgeInsets.only(top: 90),
                        child: Column(children: [
                          Icon(Icons.history, size: 64, color: _sage),
                          SizedBox(height: 12),
                          Text('Chưa có lịch sử tham quan', style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600, color: _ink)),
                          SizedBox(height: 6),
                          Text('Các hiện vật bạn mở khi đã đăng nhập sẽ xuất hiện tại đây.', textAlign: TextAlign.center),
                        ]),
                      )
                    else
                      ..._items.map((item) {
                        final rawId = item['artifactId'] ?? item['artifact_id'];
                        final name = _string(item, 'artifactName', _string(item, 'name', 'Hiện vật #$rawId'));
                        final date = _string(item, 'detectedAt', _string(item, 'visitedAt'));
                        final site = _string(item, 'siteName', 'Di tích');
                        final source = _string(item, 'source', 'MANUAL');
                        final duration = (item['durationSeconds'] as num?)?.toInt() ?? 0;
                        final pending = item['syncPending'] == true;
                        final imageUrl = _string(item, 'thumbnailUrl', _string(item, 'image'));
                        return Card(
                          elevation: 0,
                          color: Colors.white,
                          margin: const EdgeInsets.only(bottom: 12),
                          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
                          child: Padding(
                            padding: const EdgeInsets.all(12),
                            child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                              ClipRRect(
                                borderRadius: BorderRadius.circular(12),
                                child: imageUrl.isEmpty
                                    ? Container(width: 76, height: 76, color: const Color(0xFFECEDE5), child: const Icon(Icons.account_balance, color: _sage))
                                    : Image.network(imageUrl, width: 76, height: 76, fit: BoxFit.cover,
                                        errorBuilder: (_, __, ___) => Container(width: 76, height: 76, color: const Color(0xFFECEDE5), child: const Icon(Icons.account_balance, color: _sage))),
                              ),
                              const SizedBox(width: 12),
                              Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                                Text(name, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: _ink)),
                                const SizedBox(height: 5),
                                Text(site, style: const TextStyle(color: Colors.black54)),
                                const SizedBox(height: 5),
                                Text(_date(date), style: const TextStyle(fontSize: 12, color: Colors.black54)),
                                const SizedBox(height: 5),
                                Wrap(spacing: 6, runSpacing: 4, children: [
                                  _pill(source),
                                  _pill('$duration giây'),
                                  if (pending) _pill('Chờ đồng bộ'),
                                ]),
                              ])),
                            ]),
                          ),
                        );
                      }),
                  ],
                ),
              ),
      ),
    );
  }

  Widget _pill(String text) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(color: const Color(0xFFECEDE5), borderRadius: BorderRadius.circular(20)),
        child: Text(text, style: const TextStyle(fontSize: 10, color: _ink)),
      );
}
