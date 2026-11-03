import 'package:flutter/material.dart';

import '../auth/auth_service.dart';
import '../auth/login_screen.dart';
import 'feedback_service.dart';

// Màu lấy từ HomePage trong main.dart
const Color _sage = Color(0xFF718B78);
const Color _cream = Color(0xFFF7F5EF);
const Color _softCard = Color(0xFFECEDE5);
const Color _ink = Color(0xFF26352B);
const Color _muted = Color(0xFF69716B);
const Color _gold = Color(0xFFB99148);

const TextStyle _valueStyle = TextStyle(fontSize: 16, height: 1.4, color: _ink);

const int _maxRating = FeedbackService.maxRating;
const int _maxCommentLength = FeedbackService.maxCommentLength;

const List<String> _ratingLabels = [
  'Rất không hài lòng',
  'Không hài lòng',
  'Bình thường',
  'Hài lòng',
  'Rất hài lòng',
];

/// Màn hình đánh giá hiện vật (US08, Tường - Sprint 5), mở từ nút "Đánh giá" ở màn chi tiết.
///  - Điểm trung bình + số lượt theo mức sao (chỉ tính đánh giá đã duyệt)
///  - Gửi đánh giá 1–5 sao, bình luận ≤ 500 ký tự; chưa đăng nhập thì yêu cầu đăng nhập
///  - Danh sách bình luận đã duyệt
/// Mỗi người 1 đánh giá / hiện vật: gửi lại sẽ thay đánh giá cũ và chờ duyệt lại.
class FeedbackScreen extends StatefulWidget {
  final int artifactId;
  final String artifactName;

  const FeedbackScreen({
    super.key,
    required this.artifactId,
    required this.artifactName,
  });

  @override
  State<FeedbackScreen> createState() => _FeedbackScreenState();
}

class _FeedbackScreenState extends State<FeedbackScreen> {
  final TextEditingController _commentController = TextEditingController();
  late Future<FeedbackOverview> _overview;
  int _rating = 0;
  bool _showRatingError = false;
  bool _submitting = false;
  String? _submitError;
  FeedbackSubmitResult? _result;

  @override
  void initState() {
    super.initState();
    _overview = FeedbackService.loadOverview(widget.artifactId);
  }

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  Future<void> _reload() async {
    final future = FeedbackService.loadOverview(widget.artifactId);
    setState(() => _overview = future);
    try {
      await future;
    } catch (_) {
      // Lỗi đã hiển thị trong FutureBuilder
    }
  }

  void _selectRating(int value) {
    setState(() {
      _rating = value;
      _showRatingError = false;
    });
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    if (_rating == 0) {
      setState(() => _showRatingError = true);
      return;
    }
    setState(() {
      _submitting = true;
      _submitError = null;
    });
    final result = await FeedbackService.submit(
      widget.artifactId,
      _rating,
      _commentController.text,
    );
    if (!mounted) return;
    setState(() {
      _submitting = false;
      if (result.success) {
        _result = result;
      } else {
        _submitError = result.message;
      }
    });
  }

  /// Quay lại form với nội dung vừa gửi để sửa (gửi lại = ghi đè, chờ duyệt lại)
  void _editAgain() {
    setState(() {
      _result = null;
      _submitError = null;
    });
  }

  Future<void> _openLogin() async {
    await Navigator.push(
      context,
      MaterialPageRoute(builder: (_) => const LoginScreen()),
    );
    // AuthService.currentUser thay đổi thì ValueListenableBuilder tự vẽ lại form
    if (mounted) setState(() => _submitError = null);
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
          'Đánh giá hiện vật',
          style: TextStyle(
            fontWeight: FontWeight.bold,
            color: _ink,
          ),
        ),
      ),
      body: SafeArea(
        child: RefreshIndicator(
          color: _sage,
          onRefresh: _reload,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            padding: const EdgeInsets.all(20),
            children: [
              _buildArtifactCard(),
              const SizedBox(height: 16),
              _buildSummarySection(),
              const SizedBox(height: 16),
              ValueListenableBuilder<AuthUser?>(
                valueListenable: AuthService.currentUser,
                builder: (context, user, _) {
                  if (_result != null) return _buildSubmittedView();
                  if (user == null) return _buildLoginCard();
                  return _buildFormView();
                },
              ),
              const SizedBox(height: 16),
              _buildApprovedSection(),
            ],
          ),
        ),
      ),
    );
  }

  // ---------- Điểm trung bình ----------

  Widget _buildSummarySection() {
    return FutureBuilder<FeedbackOverview>(
      future: _overview,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return _card(
            child: const Center(
              child: Padding(
                padding: EdgeInsets.all(8),
                child: CircularProgressIndicator(color: _sage),
              ),
            ),
          );
        }
        if (snapshot.hasError) {
          return _buildLoadError(snapshot.error.toString());
        }
        final summary = snapshot.data!.summary;
        return _card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _sectionLabel('ĐIỂM TRUNG BÌNH'),
              const SizedBox(height: 12),
              Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Text(
                    summary.averageRating?.toStringAsFixed(1) ?? '–',
                    style: const TextStyle(
                      fontSize: 40,
                      fontWeight: FontWeight.bold,
                      color: _ink,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _starRow(summary.averageRating?.round() ?? 0, size: 20),
                        const SizedBox(height: 4),
                        Text(
                          summary.totalRatings == 0
                              ? 'Chưa có đánh giá nào được duyệt'
                              : '${summary.totalRatings} đánh giá đã duyệt',
                          style: const TextStyle(fontSize: 13, color: _muted),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              if (summary.totalRatings > 0) ...[
                const SizedBox(height: 12),
                for (var star = _maxRating; star >= 1; star--)
                  _distributionRow(star, summary.distribution[star] ?? 0, summary.totalRatings),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _distributionRow(int star, int count, int total) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          SizedBox(
            width: 28,
            child: Text('$star★', style: const TextStyle(fontSize: 12, color: _muted)),
          ),
          Expanded(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: LinearProgressIndicator(
                value: total == 0 ? 0 : count / total,
                minHeight: 8,
                backgroundColor: _softCard,
                color: _gold,
              ),
            ),
          ),
          SizedBox(
            width: 32,
            child: Text(
              '$count',
              textAlign: TextAlign.right,
              style: const TextStyle(fontSize: 12, color: _muted),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildLoadError(String message) {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.cloud_off, color: _muted),
          const SizedBox(height: 8),
          Text(message, style: const TextStyle(fontSize: 14, height: 1.4, color: _muted)),
          const SizedBox(height: 8),
          TextButton.icon(
            onPressed: _reload,
            icon: const Icon(Icons.refresh),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    );
  }

  // ---------- Chưa đăng nhập ----------

  Widget _buildLoginCard() {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _sectionLabel('ĐÁNH GIÁ CỦA BẠN'),
          const SizedBox(height: 10),
          const Text(
            'Bạn cần đăng nhập để gửi đánh giá cho hiện vật này.',
            style: TextStyle(fontSize: 14, height: 1.4, color: _ink),
          ),
          if (_submitError != null) ...[
            const SizedBox(height: 8),
            _errorText(_submitError!),
          ],
          const SizedBox(height: 14),
          SizedBox(
            width: double.infinity,
            height: 48,
            child: ElevatedButton.icon(
              onPressed: _openLogin,
              icon: const Icon(Icons.login),
              label: const Text('Đăng nhập để đánh giá'),
            ),
          ),
        ],
      ),
    );
  }

  // ---------- Form gửi đánh giá ----------

  Widget _buildFormView() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _buildRatingCard(),
        const SizedBox(height: 16),
        _buildCommentCard(),
        if (_submitError != null) ...[
          const SizedBox(height: 12),
          _errorText(_submitError!),
        ],
        const SizedBox(height: 16),
        SizedBox(
          height: 50,
          child: ElevatedButton(
            onPressed: _submitting ? null : _submit,
            child: _submitting
                ? const SizedBox(
                    width: 22,
                    height: 22,
                    child: CircularProgressIndicator(strokeWidth: 2.5),
                  )
                : const Text('Gửi đánh giá'),
          ),
        ),
        const SizedBox(height: 8),
        const Text(
          'Mỗi người có 1 đánh giá cho mỗi hiện vật. Nếu bạn đã đánh giá trước đó, '
          'gửi lại sẽ thay đánh giá cũ và chờ quản trị viên duyệt lại.',
          style: TextStyle(fontSize: 12, height: 1.4, color: _muted),
        ),
      ],
    );
  }

  Widget _buildArtifactCard() {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _sectionLabel('HIỆN VẬT'),
          const SizedBox(height: 6),
          Text(
            widget.artifactName,
            style: const TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: _ink,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRatingCard() {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _sectionLabel('ĐÁNH GIÁ CỦA BẠN'),
          const SizedBox(height: 12),
          Row(
            children: List.generate(_maxRating, (index) {
              final starValue = index + 1;
              final isSelected = starValue <= _rating;
              return Expanded(
                child: IconButton(
                  onPressed: _submitting ? null : () => _selectRating(starValue),
                  tooltip: '$starValue sao',
                  iconSize: 36,
                  icon: Icon(
                    isSelected ? Icons.star_rounded : Icons.star_border_rounded,
                    color: isSelected ? _sage : Colors.grey,
                  ),
                ),
              );
            }),
          ),
          const SizedBox(height: 8),
          _buildRatingMessage(),
        ],
      ),
    );
  }

  Widget _buildRatingMessage() {
    if (_showRatingError) {
      return _errorText('Vui lòng chọn số sao trước khi gửi.');
    }
    final text = _rating == 0
        ? 'Chạm vào ngôi sao để chọn từ 1 đến 5 sao.'
        : '$_rating/$_maxRating sao - ${_ratingLabels[_rating - 1]}';
    return Text(
      text,
      style: const TextStyle(fontSize: 13, color: _muted),
    );
  }

  Widget _buildCommentCard() {
    return _card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _sectionLabel('BÌNH LUẬN (KHÔNG BẮT BUỘC)'),
          const SizedBox(height: 12),
          TextField(
            controller: _commentController,
            enabled: !_submitting,
            minLines: 3,
            maxLines: 5,
            maxLength: _maxCommentLength,
            keyboardType: TextInputType.multiline,
            textCapitalization: TextCapitalization.sentences,
            decoration: InputDecoration(
              hintText: 'Chia sẻ cảm nhận của bạn về hiện vật...',
              filled: true,
              fillColor: _cream,
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: BorderSide.none,
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(14),
                borderSide: const BorderSide(color: _sage, width: 1.5),
              ),
            ),
          ),
        ],
      ),
    );
  }

  // ---------- Sau khi gửi thành công ----------

  Widget _buildSubmittedView() {
    final result = _result!;
    final comment = _commentController.text.trim();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.check_circle, color: _sage, size: 40),
              const SizedBox(height: 12),
              Text(
                result.updated ? 'Đã cập nhật đánh giá' : 'Đã gửi đánh giá',
                style: const TextStyle(
                  fontSize: 22,
                  fontWeight: FontWeight.bold,
                  color: _ink,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                result.message,
                style: const TextStyle(fontSize: 14, height: 1.5, color: _muted),
              ),
              const SizedBox(height: 16),
              _summaryItem(
                'Số sao',
                Text(
                  '$_rating/$_maxRating sao - ${_ratingLabels[_rating - 1]}',
                  style: _valueStyle,
                ),
              ),
              _summaryItem(
                'Bình luận',
                Text(comment.isEmpty ? 'Không có bình luận' : comment, style: _valueStyle),
              ),
              _summaryItem('Trạng thái', _pill('Chờ duyệt')),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          height: 50,
          child: ElevatedButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Quay lại hiện vật'),
          ),
        ),
        const SizedBox(height: 10),
        SizedBox(
          height: 50,
          child: OutlinedButton(
            onPressed: _editAgain,
            child: const Text('Sửa đánh giá'),
          ),
        ),
      ],
    );
  }

  // ---------- Bình luận đã duyệt ----------

  Widget _buildApprovedSection() {
    return FutureBuilder<FeedbackOverview>(
      future: _overview,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done || snapshot.hasError) {
          return const SizedBox.shrink();
        }
        final items = snapshot.data!.approved;
        return _card(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _sectionLabel('BÌNH LUẬN CỦA DU KHÁCH'),
              const SizedBox(height: 8),
              if (items.isEmpty)
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 8),
                  child: Text(
                    'Chưa có bình luận nào được duyệt.',
                    style: TextStyle(fontSize: 14, color: _muted),
                  ),
                )
              else
                for (final item in items) _approvedTile(item),
            ],
          ),
        );
      },
    );
  }

  Widget _approvedTile(FeedbackItem item) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  item.reviewerName,
                  style: const TextStyle(fontWeight: FontWeight.w600, color: _ink),
                ),
              ),
              if (item.createdAt != null)
                Text(
                  _formatDate(item.createdAt!),
                  style: const TextStyle(fontSize: 12, color: _muted),
                ),
            ],
          ),
          const SizedBox(height: 4),
          _starRow(item.rating, size: 16),
          if (item.comment != null) ...[
            const SizedBox(height: 6),
            Text(item.comment!, style: const TextStyle(fontSize: 14, height: 1.4, color: _ink)),
          ],
        ],
      ),
    );
  }

  // ---------- Thành phần dùng chung trong màn hình ----------

  static String _formatDate(DateTime date) {
    String two(int n) => n.toString().padLeft(2, '0');
    return '${two(date.day)}/${two(date.month)}/${date.year}';
  }

  Widget _starRow(int filled, {required double size}) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: List.generate(
        _maxRating,
        (i) => Icon(
          i < filled ? Icons.star_rounded : Icons.star_border_rounded,
          size: size,
          color: i < filled ? _gold : Colors.grey,
        ),
      ),
    );
  }

  Widget _errorText(String text) {
    return Text(
      text,
      style: TextStyle(
        fontSize: 13,
        fontWeight: FontWeight.w600,
        color: Theme.of(context).colorScheme.error,
      ),
    );
  }

  Widget _card({required Widget child}) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(22),
      ),
      child: child,
    );
  }

  Widget _sectionLabel(String text) {
    return Text(
      text,
      style: const TextStyle(
        fontSize: 11,
        letterSpacing: 1.5,
        color: _sage,
      ),
    );
  }

  Widget _summaryItem(String label, Widget value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 12, color: _muted),
          ),
          const SizedBox(height: 4),
          value,
        ],
      ),
    );
  }

  Widget _pill(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
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
