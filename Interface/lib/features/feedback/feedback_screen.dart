import 'package:flutter/material.dart';

// Màu lấy từ HomePage trong main.dart
const Color _sage = Color(0xFF718B78);
const Color _cream = Color(0xFFF7F5EF);
const Color _softCard = Color(0xFFECEDE5);
const Color _ink = Color(0xFF26352B);
const Color _muted = Color(0xFF69716B);

const TextStyle _valueStyle = TextStyle(fontSize: 16, height: 1.4, color: _ink);

const int _maxRating = 5;
const int _maxCommentLength = 500;

const List<String> _ratingLabels = [
  'Rất không hài lòng',
  'Không hài lòng',
  'Bình thường',
  'Hài lòng',
  'Rất hài lòng',
];

/// Màn hình đánh giá hiện vật (US08) - bản giao diện MÔ PHỎNG.
class FeedbackScreen extends StatefulWidget {
  final String artifactName;

  const FeedbackScreen({super.key, required this.artifactName});

  @override
  State<FeedbackScreen> createState() => _FeedbackScreenState();
}

class _FeedbackScreenState extends State<FeedbackScreen> {
  final TextEditingController _commentController = TextEditingController();
  int _rating = 0;
  bool _showRatingError = false;
  bool _submitted = false;
  String _submittedComment = '';

  @override
  void dispose() {
    _commentController.dispose();
    super.dispose();
  }

  void _selectRating(int value) {
    setState(() {
      _rating = value;
      _showRatingError = false;
    });
  }

  void _submit() {
    FocusScope.of(context).unfocus();
    if (_rating == 0) {
      setState(() {
        _showRatingError = true;
      });
      return;
    }
    setState(() {
      _submittedComment = _commentController.text.trim();
      _submitted = true;
    });
  }

  void _reset() {
    _commentController.clear();
    setState(() {
      _rating = 0;
      _showRatingError = false;
      _submitted = false;
      _submittedComment = '';
    });
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
        // ListView
        child: ListView(
          keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
          padding: const EdgeInsets.all(20),
          children: _submitted ? _buildSubmittedView() : _buildFormView(),
        ),
      ),
    );
  }

  // ---------- Màn hình nhập đánh giá ----------

  List<Widget> _buildFormView() {
    return [
      _buildSimulationNotice(),
      const SizedBox(height: 16),
      _buildArtifactCard(),
      const SizedBox(height: 16),
      _buildRatingCard(),
      const SizedBox(height: 16),
      _buildCommentCard(),
      const SizedBox(height: 20),
      SizedBox(
        width: double.infinity,
        height: 50,
        child: ElevatedButton(
          onPressed: _submit,
          child: const Text('Gửi đánh giá'),
        ),
      ),
    ];
  }

  Widget _buildSimulationNotice() {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: _softCard,
        borderRadius: BorderRadius.circular(14),
      ),
      child: const Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.info_outline, color: _sage, size: 20),
          SizedBox(width: 10),
          Expanded(
            child: Text(
              'Chế độ mô phỏng: đánh giá của bạn chỉ hiển thị trên giao diện, '
              'chưa được gửi lên máy chủ.',
              style: TextStyle(fontSize: 13, height: 1.4, color: _muted),
            ),
          ),
        ],
      ),
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
                  onPressed: () => _selectRating(starValue),
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
      return Text(
        'Vui lòng chọn số sao trước khi gửi.',
        style: TextStyle(
          fontSize: 13,
          fontWeight: FontWeight.w600,
          color: Theme.of(context).colorScheme.error,
        ),
      );
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

  // ---------- Màn hình xác nhận sau khi "gửi" ----------

  List<Widget> _buildSubmittedView() {
    final comment =
        _submittedComment.isEmpty ? 'Không có bình luận' : _submittedComment;

    return [
      _card(
        child: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(Icons.check_circle, color: _sage, size: 40),
            SizedBox(height: 12),
            Text(
              'Đã ghi nhận (mô phỏng)',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: _ink,
              ),
            ),
            SizedBox(height: 8),
            Text(
              'Đánh giá này chỉ được mô phỏng trên giao diện. '
              'Nó chưa được gửi lên máy chủ và chưa được lưu vào cơ sở dữ liệu.',
              style: TextStyle(fontSize: 14, height: 1.5, color: _muted),
            ),
          ],
        ),
      ),
      const SizedBox(height: 16),
      _card(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _sectionLabel('CHI TIẾT ĐÁNH GIÁ'),
            const SizedBox(height: 14),
            _summaryItem('Hiện vật', Text(widget.artifactName, style: _valueStyle)),
            _summaryItem(
              'Số sao',
              Text(
                '$_rating/$_maxRating sao - ${_ratingLabels[_rating - 1]}',
                style: _valueStyle,
              ),
            ),
            _summaryItem('Bình luận', Text(comment, style: _valueStyle)),
            _summaryItem('Trạng thái', _pill('Chờ duyệt')),
          ],
        ),
      ),
      const SizedBox(height: 20),
      SizedBox(
        width: double.infinity,
        height: 50,
        child: ElevatedButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Quay lại hiện vật'),
        ),
      ),
      const SizedBox(height: 10),
      SizedBox(
        width: double.infinity,
        height: 50,
        child: OutlinedButton(
          onPressed: _reset,
          child: const Text('Đánh giá lại'),
        ),
      ),
    ];
  }

  // ---------- Thành phần dùng chung trong màn hình ----------

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