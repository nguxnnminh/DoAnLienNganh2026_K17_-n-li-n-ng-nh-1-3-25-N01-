import '../network/api_client.dart';

String resolveImageUrl(String? url) {
  if (url == null || url.isEmpty) return '';
  if (url.startsWith('http')) return url;
  return '$kBaseUrl$url';
}
