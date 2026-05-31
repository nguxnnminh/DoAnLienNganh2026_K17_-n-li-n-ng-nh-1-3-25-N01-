import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/product.dart';

final productDetailProvider = FutureProvider.family<Product, int>((ref, id) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/products/$id');
  return Product.fromJson(res.data as Map<String, dynamic>);
});

final similarProductsProvider = FutureProvider.family<List<Product>, int>((ref, id) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/products/$id/similar', queryParameters: {'limit': 6});
  return (res.data as List).map((e) => Product.fromJson(e as Map<String, dynamic>)).toList();
});

final searchSuggestProvider = FutureProvider.family<List<Product>, String>((ref, q) async {
  if (q.trim().isEmpty) return [];
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/products/suggest', queryParameters: {'q': q, 'limit': 8});
  return (res.data as List).map((e) => Product.fromJson(e as Map<String, dynamic>)).toList();
});
