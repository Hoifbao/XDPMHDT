import 'package:flutter/material.dart';

import '../core/artifact_service.dart';
import 'artifact_detail_screen.dart';

class RecentArtifactsScreen extends StatefulWidget {
  const RecentArtifactsScreen({super.key});
  @override
  State<RecentArtifactsScreen> createState() => _RecentArtifactsScreenState();
}

class _RecentArtifactsScreenState extends State<RecentArtifactsScreen> {
  late Future<List<ArtifactDetail>> _items;
  @override
  void initState() {
    super.initState();
    _items = ArtifactApiService.recentArtifacts();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Hiện vật đã xem')),
    body: FutureBuilder<List<ArtifactDetail>>(
      future: _items,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        final items = snapshot.data ?? [];
        if (items.isEmpty) {
          return const Center(
            child: Text(
              'Chưa có nội dung đã lưu. Hãy mở một hiện vật khi có mạng.',
            ),
          );
        }
        return ListView(
          children: [
            const Padding(
              padding: EdgeInsets.all(16),
              child: Text(
                'Lưu 20 hiện vật gần nhất để đọc khi mất mạng. Ảnh đã tải có thể có trong cache; audio/video cần kết nối mạng.',
              ),
            ),
            ...items.map(
              (item) => ListTile(
                leading: const Icon(Icons.history),
                title: Text(item.name),
                subtitle: Text(item.locationName ?? ''),
                onTap: () async {
                  await Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ArtifactDetailScreen(artifactId: item.id),
                    ),
                  );
                  if (mounted) {
                    setState(
                      () => _items = ArtifactApiService.recentArtifacts(),
                    );
                  }
                },
              ),
            ),
          ],
        );
      },
    ),
  );
}
