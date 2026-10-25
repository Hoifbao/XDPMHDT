import 'package:flutter/material.dart';

import '../../screens/artifact_detail_screen.dart';
import '../favorites/personalization_service.dart';

/// Màn hình Lịch sử tham quan (Phú).
class VisitHistoryScreen extends StatefulWidget {
  const VisitHistoryScreen({super.key});

  @override
  State<VisitHistoryScreen> createState() => _VisitHistoryScreenState();
}

class _VisitHistoryScreenState extends State<VisitHistoryScreen> {
  late Future<List<VisitItem>> _future;

  @override
  void initState() {
    super.initState();
    _future = PersonalizationService.getVisits();
  }

  Future<void> _reload() async {
    final next = PersonalizationService.getVisits();
    setState(() {
      _future = next;
    });
    await next.catchError((_) => <VisitItem>[]);
  }

  static String _two(int n) => n.toString().padLeft(2, '0');

  static String _formatTime(DateTime? time) {
    if (time == null) return '';
    return '${_two(time.day)}/${_two(time.month)}/${time.year} ${_two(time.hour)}:${_two(time.minute)}';
  }

  static String _formatDuration(int seconds) {
    if (seconds < 60) return '$seconds giây';
    return '${seconds ~/ 60} phút ${_two(seconds % 60)} giây';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lịch sử tham quan')),
      body: FutureBuilder<List<VisitItem>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _CenterText(
              text: snapshot.error.toString().replaceFirst('Exception: ', ''),
              onRetry: _reload,
            );
          }
          final visits = snapshot.data ?? [];
          if (visits.isEmpty) {
            return const _CenterText(
              text: 'Chưa có lượt tham quan nào.\nLịch sử được ghi khi app phát hiện iBeacon của hiện vật.',
            );
          }
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView.separated(
              itemCount: visits.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final visit = visits[index];
                return ListTile(
                  leading: const CircleAvatar(child: Icon(Icons.history)),
                  title: Text(visit.artifactName),
                  subtitle: Text(
                    [visit.siteName, _formatTime(visit.detectedAt)]
                        .where((s) => s != null && s.isNotEmpty)
                        .join(' · '),
                  ),
                  trailing: Text(_formatDuration(visit.durationSeconds)),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ArtifactDetailScreen(artifactId: visit.artifactId),
                    ),
                  ),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class _CenterText extends StatelessWidget {
  final String text;
  final VoidCallback? onRetry;
  const _CenterText({required this.text, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.history_toggle_off, size: 56, color: Colors.grey),
            const SizedBox(height: 12),
            Text(text, textAlign: TextAlign.center),
            if (onRetry != null) ...[
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onRetry, child: const Text('Thử lại')),
            ],
          ],
        ),
      ),
    );
  }
}
