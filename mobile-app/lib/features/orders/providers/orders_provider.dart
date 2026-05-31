import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/order.dart';

final myOrdersProvider = FutureProvider<List<Order>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/orders/my');
  return (res.data as List).map((e) => Order.fromJson(e as Map<String, dynamic>)).toList();
});

final cancelOrderProvider = FutureProvider.family<bool, int>((ref, orderId) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.post('/api/orders/$orderId/cancel');
  return (res.data as Map<String, dynamic>)['success'] == true;
});
