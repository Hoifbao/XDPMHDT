import 'package:flutter/material.dart';

import '../auth/auth_service.dart';
import '../auth/login_screen.dart';
import 'feedback_screen.dart';
import 'feedback_service.dart';

const Color _sage = Color(0xFF718B78);
const Color _cream = Color(0xFFF7F5EF);
const Color _softCard = Color(0xFFECEDE5);
const Color _ink = Color(0xFF26352B);
const Color _muted = Color(0xFF69716B);
const Color _gold = Color(0xFFB99148);

const Map<String, String> _statusLabels = {
  'PENDING': 'Chờ duyệt',
  'APPROVED': 'Đã duyệt',
  'REJECTED': 'Không được duyệt',
};

/// Màn hình "Đánh giá của tôi" (Tuần 6): mọi đánh giá của du khách kèm trạng thái kiểm duyệt.
/// Kéo xuống để tải lại; bấm một dòng để xem hoặc sửa đánh giá của hiện vật đó.
class MyFeedbacksScreen extends StatefulWidget {
  const MyFeedbacksScreen({super.key});

  @override
  State<MyFeedbacksScreen> createState() => _MyFeedbacksScreenState();
}

class _MyFeedbacksScreenState extends State<MyFeedbacksScreen> {
  late Future<List<MyFeedback>> _feedbacks;

  @override
  void initState() {
    super.initState();
    _feedbacks = FeedbackService.loadMine();
  }

  Future<void> _reload() async {
    final future = FeedbackService.loadMine();
    // Dùng khối { } chứ không dùng =>: setState không được trả về Future
    setState(() {
      _feedbacks = future;
    });
    try {
      await future;
    } catch (_) {
      // Lỗi đã hiển thị trong FutureBuilder
    }
  }

  Future<void> _openLogin() async {
    await Navigator.push(context, MaterialPageRoute(builder: (_) => const LoginScreen()));
    if (mounted) await _reload();
  }

  /// Quay lại từ màn đánh giá thì tải lại vì du khách có thể vừa sửa đánh giá
  Future<void> _openFeedback(MyFeedback feedback) async {
    await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => FeedbackScreen(
          artifactId: feedback.item.artifactId,
          artifactName: feedback.artifactName,
        ),
      ),
    );
    if (mounted) await _reload();
  }

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
          'Đánh giá của tôi',
          style: TextStyle(fontWeight: FontWeight.bold, color: _ink),
        ),
      ),
      body: SafeArea(
        child: ValueListenableBuilder<AuthUser?>(
          valueListenable: AuthService.currentUser,
          builder: (context, user, _) => user == null ? _buildLoginPrompt() : _buildList(),
        ),
      ),
    );
  }

  Widget _buildLoginPrompt() {
    return _centerMessage(
      icon: Icons.lock_outline,
      message: 'Bạn cần đăng nhập để xem đánh giá của mình.',
      action: ElevatedButton(onPressed: _openLogin, child: const Text('Đăng nhập')),
    );
  }

  Widget _buildList() {
    return RefreshIndicator(
      color: _sage,
      onRefresh: _reload,
      child: FutureBuilder<List<MyFeedback>>(
        future: _feedbacks,
        builder: (context, snapshot) {
          if (snapshot.connectionState != ConnectionState.done) {
            return const Center(child: CircularProgressIndicator(color: _sage));
          }
          if (snapshot.hasError) {
            return _centerMessage(
              icon: Icons.cloud_off,
              message: snapshot.error.toString(),
              action: TextButton.icon(
                onPressed: _reload,
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
              ),
            );
          }
          final feedbacks = snapshot.data!;
          if (feedbacks.isEmpty) {
            return _centerMessage(
              icon: Icons.rate_review_outlined,
              message: 'Bạn chưa có đánh giá nào. Hãy mở một hiện vật và bấm "Đánh giá".',
            );
          }
          return ListView.separated(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(20),
            itemCount: feedbacks.length,
            separatorBuilder: (context, index) => const SizedBox(height: 12),
            itemBuilder: (_, index) => _buildTile(feedbacks[index]),
          );
        },
      ),
    );
  }

  Widget _buildTile(MyFeedback feedback) {
    final item = feedback.item;
    final date = item.updatedAt ?? item.createdAt;
    return Material(
      color: Colors.white,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: () => _openFeedback(feedback),
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Text(
                      feedback.artifactName,
                      style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: _ink),
                    ),
                  ),
                  const SizedBox(width: 8),
                  _statusPill(item.status),
                ],
              ),
              const SizedBox(height: 8),
              _stars(item.rating),
              if (item.comment != null) ...[
                const SizedBox(height: 8),
                Text(item.comment!, style: const TextStyle(fontSize: 14, height: 1.4, color: _ink)),
              ],
              if (date != null) ...[
                const SizedBox(height: 8),
                Text(_formatDate(date), style: const TextStyle(fontSize: 12, color: _muted)),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _statusPill(String status) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(color: _softCard, borderRadius: BorderRadius.circular(20)),
      child: Text(
        _statusLabels[status] ?? status,
        style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: _ink),
      ),
    );
  }

  Widget _stars(int rating) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(
        FeedbackService.maxRating,
        (i) => Icon(
          i < rating ? Icons.star_rounded : Icons.star_border_rounded,
          size: 18,
          color: i < rating ? _gold : Colors.grey,
        ),
      ),
    );
  }

  /// Dùng ListView để vẫn kéo xuống tải lại được khi danh sách trống hoặc lỗi
  Widget _centerMessage({required IconData icon, required String message, Widget? action}) {
    return ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.fromLTRB(32, 96, 32, 32),
      children: [
        Icon(icon, size: 48, color: _muted),
        const SizedBox(height: 12),
        Text(
          message,
          textAlign: TextAlign.center,
          style: const TextStyle(fontSize: 15, height: 1.4, color: _muted),
        ),
        if (action != null) ...[
          const SizedBox(height: 16),
          Center(child: action),
        ],
      ],
    );
  }

  static String _formatDate(DateTime date) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(date.day)}/${two(date.month)}/${date.year}';
  }
}
