import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/product.dart';
import '../../../models/category.dart';

final categoriesProvider = FutureProvider<List<Category>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/categories');
  return (res.data as List).map((e) => Category.fromJson(e as Map<String, dynamic>)).toList();
});

// Top 3 best-selling products (mirrors desktop: 1 per category, popular sort)
final bestSellersProvider = FutureProvider<List<Product>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final cats = await ref.watch(categoriesProvider.future);
  final results = <Product>[];
  for (final cat in cats.take(3)) {
    try {
      final res = await dio.get('/api/products', queryParameters: {
        'page': 0, 'size': 1, 'sort': 'popular', 'categoryId': cat.id,
      });
      final page = ProductPage.fromJson(res.data as Map<String, dynamic>);
      if (page.content.isNotEmpty) results.add(page.content.first);
    } catch (_) {}
  }
  return results;
});

final featuredProductsProvider = FutureProvider<List<Product>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/products', queryParameters: {'page': 0, 'size': 8, 'sort': 'newest'});
  return ProductPage.fromJson(res.data as Map<String, dynamic>).content;
});

final recommendationsProvider = FutureProvider<List<Product>>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/recommendations', queryParameters: {'limit': 8});
  return (res.data as List).map((e) => Product.fromJson(e as Map<String, dynamic>)).toList();
});
