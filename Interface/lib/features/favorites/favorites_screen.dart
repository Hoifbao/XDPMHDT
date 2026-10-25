import 'package:flutter/material.dart';

import '../../screens/artifact_detail_screen.dart';
import 'personalization_service.dart';

/// Màn hình Hiện vật yêu thích (Phú).
class FavoritesScreen extends StatefulWidget {
  const FavoritesScreen({super.key});

  @override
  State<FavoritesScreen> createState() => _FavoritesScreenState();
}

class _FavoritesScreenState extends State<FavoritesScreen> {
  late Future<List<FavoriteItem>> _future;

  @override
  void initState() {
    super.initState();
    _future = PersonalizationService.getFavorites();
  }

  Future<void> _reload() async {
    final next = PersonalizationService.getFavorites();
    setState(() {
      _future = next;
    });
    await next.catchError((_) => <FavoriteItem>[]);
  }

  Future<void> _remove(FavoriteItem item) async {
    try {
      await PersonalizationService.removeFavorite(item.artifactId);
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Đã bỏ "${item.artifactName}" khỏi yêu thích')),
      );
      _reload();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(e.toString().replaceFirst('Exception: ', ''))),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Hiện vật yêu thích')),
      body: FutureBuilder<List<FavoriteItem>>(
        future: _future,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator());
          }
          if (snapshot.hasError) {
            return _Message(
              icon: Icons.cloud_off,
              text: snapshot.error.toString().replaceFirst('Exception: ', ''),
              onRetry: _reload,
            );
          }
          final items = snapshot.data ?? [];
          if (items.isEmpty) {
            return const _Message(
              icon: Icons.favorite_border,
              text: 'Chưa có hiện vật yêu thích.\nBấm biểu tượng trái tim ở trang chi tiết hiện vật để lưu.',
            );
          }
          return RefreshIndicator(
            onRefresh: _reload,
            child: ListView.separated(
              itemCount: items.length,
              separatorBuilder: (_, __) => const Divider(height: 1),
              itemBuilder: (context, index) {
                final item = items[index];
                return ListTile(
                  leading: _Thumb(url: item.thumbnailUrl),
                  title: Text(item.artifactName),
                  subtitle: Text(item.siteName ?? ''),
                  trailing: IconButton(
                    icon: const Icon(Icons.favorite, color: Colors.red),
                    tooltip: 'Bỏ yêu thích',
                    onPressed: () => _remove(item),
                  ),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => ArtifactDetailScreen(artifactId: item.artifactId),
                    ),
                  ).then((_) => _reload()),
                );
              },
            ),
          );
        },
      ),
    );
  }
}

class _Thumb extends StatelessWidget {
  final String? url;
  const _Thumb({this.url});

  @override
  Widget build(BuildContext context) {
    const placeholder = CircleAvatar(child: Icon(Icons.museum_outlined));
    if (url == null || url!.isEmpty) return placeholder;
    return ClipRRect(
      borderRadius: BorderRadius.circular(8),
      child: Image.network(
        url!,
        width: 48,
        height: 48,
        fit: BoxFit.cover,
        errorBuilder: (_, __, ___) => placeholder,
      ),
    );
  }
}

/// Thông báo giữa màn hình (trống / lỗi), dùng chung cho Yêu thích và Lịch sử.
class _Message extends StatelessWidget {
  final IconData icon;
  final String text;
  final VoidCallback? onRetry;
  const _Message({required this.icon, required this.text, this.onRetry});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 56, color: Colors.grey),
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
