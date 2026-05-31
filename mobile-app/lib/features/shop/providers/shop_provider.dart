import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/product.dart';
import '../../../models/category.dart';

class ShopFilter {
  final int page;
  final String sort;
  final String keyword;
  final int? categoryId;
  final int? subCategoryId;
  final double? minPrice;
  final double? maxPrice;

  const ShopFilter({
    this.page = 0,
    this.sort = 'newest',
    this.keyword = '',
    this.categoryId,
    this.subCategoryId,
    this.minPrice,
    this.maxPrice,
  });

  ShopFilter copyWith({
    int? page,
    String? sort,
    String? keyword,
    int? categoryId,
    int? subCategoryId,
    double? minPrice,
    double? maxPrice,
    bool clearCategory = false,
    bool clearSub = false,
  }) =>
      ShopFilter(
        page: page ?? this.page,
        sort: sort ?? this.sort,
        keyword: keyword ?? this.keyword,
        categoryId: clearCategory ? null : (categoryId ?? this.categoryId),
        subCategoryId: clearSub ? null : (subCategoryId ?? this.subCategoryId),
        minPrice: minPrice ?? this.minPrice,
        maxPrice: maxPrice ?? this.maxPrice,
      );

  Map<String, dynamic> toQuery() => {
        'page': page,
        'size': 12,
        'sort': sort,
        if (keyword.isNotEmpty) 'keyword': keyword,
        if (categoryId != null) 'categoryId': categoryId,
        if (subCategoryId != null) 'subCategoryId': subCategoryId,
        if (minPrice != null) 'minPrice': minPrice,
        if (maxPrice != null) 'maxPrice': maxPrice,
      };
}

final shopFilterProvider = StateProvider<ShopFilter>((ref) => const ShopFilter());

final shopProductsProvider = FutureProvider<ProductPage>((ref) async {
  final filter = ref.watch(shopFilterProvider);
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/products', queryParameters: filter.toQuery());
  return ProductPage.fromJson(res.data as Map<String, dynamic>);
});

final subCategoriesProvider = FutureProvider.family<List<SubCategory>, int>((ref, categoryId) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/subcategories/by-category/$categoryId');
  return (res.data as List).map((e) => SubCategory.fromJson(e as Map<String, dynamic>)).toList();
});
