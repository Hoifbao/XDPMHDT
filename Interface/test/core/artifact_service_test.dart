import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:smartheritage/core/artifact_service.dart';

void main() {
  group('ArtifactDetail.fromJson', () {
    test('đọc content + fallback=false theo sample EN', () {
      final json = jsonDecode('''
{
  "id": 1,
  "name": "Dong Son bronze drum",
  "status": "ACTIVE",
  "locationName": "Museum",
  "requestedLanguage": "en",
  "languageCode": "en",
  "fallback": false,
  "content": {
    "languageCode": "en",
    "title": "Dong Son bronze drum",
    "description": "An artifact",
    "audioUrl": "https://example.com/a.mp3",
    "videoUrl": "https://example.com/v.mp4",
    "imageUrls": "[\\"https://example.com/i.jpg\\"]",
    "transcript": "English"
  },
  "contents": [],
  "media": [
    {"mediaType": "IMAGE", "fileUrl": "https://example.com/i.jpg"},
    {"mediaType": "AUDIO", "fileUrl": "https://example.com/a.mp3"},
    {"mediaType": "VIDEO", "fileUrl": "https://example.com/v.mp4"}
  ]
}
''') as Map<String, dynamic>;

      final a = ArtifactDetail.fromJson(json);
      final c = a.contentFor('en');

      expect(a.fallback, isFalse);
      expect(c, isNotNull);
      expect(c!.title, 'Dong Son bronze drum');
      expect(c.fallbackUsed, isFalse);
      expect(c.imageUrls, ['https://example.com/i.jpg']);
      expect(c.audioUrl, 'https://example.com/a.mp3');
      expect(c.videoUrl, 'https://example.com/v.mp4');
    });

    test('fallback=true → nhãn fallbackUsed trên content', () {
      final json = jsonDecode('''
{
  "id": 1,
  "name": "Trống đồng",
  "status": "ACTIVE",
  "requestedLanguage": "en",
  "languageCode": "vi",
  "fallback": true,
  "content": {
    "languageCode": "vi",
    "title": "Trống đồng Đông Sơn",
    "description": "Bản Việt",
    "imageUrls": "[]"
  },
  "contents": [],
  "media": []
}
''') as Map<String, dynamic>;

      final a = ArtifactDetail.fromJson(json);
      final c = a.contentFor('en');

      expect(a.fallback, isTrue);
      expect(c!.fallbackUsed, isTrue);
      expect(c.languageCode, 'vi');
      expect(c.title, 'Trống đồng Đông Sơn');
    });

    test('imageUrls dạng List được parse', () {
      final json = {
        'id': 2,
        'name': 'X',
        'status': 'ACTIVE',
        'fallback': false,
        'content': {
          'languageCode': 'vi',
          'title': 'X',
          'imageUrls': ['https://a.jpg', 'https://b.jpg'],
        },
        'contents': <dynamic>[],
        'media': <dynamic>[],
      };

      final c = ArtifactDetail.fromJson(json).contentFor('vi');
      expect(c!.imageUrls.length, 2);
    });
  });

}
