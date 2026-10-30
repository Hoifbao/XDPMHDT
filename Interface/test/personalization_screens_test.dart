import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:smartheritage/features/auth/auth_service.dart';
import 'package:smartheritage/features/favorites/favorites_screen.dart';
import 'package:smartheritage/features/visit_history/visit_history_screen.dart';

void main() {
  setUp(() {
    AuthService.logout();
  });

  testWidgets(
    'Favorites screen displays the empty state when no user is signed in',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: FavoritesScreen()),
      );
      await tester.pumpAndSettle();

      expect(find.text('Hiện vật yêu thích'), findsOneWidget);
      expect(find.text('Chưa có hiện vật yêu thích'), findsOneWidget);
      expect(
        find.text('Đăng nhập để xem danh sách yêu thích.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'Visit history screen displays the empty state when no user is signed in',
    (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(home: VisitHistoryScreen()),
      );
      await tester.pumpAndSettle();

      expect(find.text('Lịch sử tham quan'), findsOneWidget);
      expect(find.text('Chưa có lịch sử tham quan'), findsOneWidget);
      expect(
        find.text('Đăng nhập để xem lịch sử tham quan.'),
        findsOneWidget,
      );
    },
  );
}
