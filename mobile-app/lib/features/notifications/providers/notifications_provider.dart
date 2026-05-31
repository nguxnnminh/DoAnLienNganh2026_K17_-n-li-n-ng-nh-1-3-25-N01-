import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/notification.dart';

final notificationsProvider = FutureProvider<List<AppNotification>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/notifications');
  return (res.data as List)
      .map((e) => AppNotification.fromJson(e as Map<String, dynamic>))
      .toList();
});

final notificationCountProvider = FutureProvider<int>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/notifications/count');
  return (res.data as Map<String, dynamic>)['count'] as int? ?? 0;
});
