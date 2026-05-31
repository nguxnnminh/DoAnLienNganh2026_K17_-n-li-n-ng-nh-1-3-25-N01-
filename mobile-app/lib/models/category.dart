class Category {
  final int id;
  final String name;
  final String slug;

  const Category({required this.id, required this.name, required this.slug});

  factory Category.fromJson(Map<String, dynamic> j) => Category(
        id: j['id'] as int,
        name: j['name'] as String,
        slug: j['slug'] as String? ?? '',
      );
}

class SubCategory {
  final int id;
  final String name;
  final String slug;
  final int categoryId;
  final String categoryName;

  const SubCategory({
    required this.id,
    required this.name,
    required this.slug,
    required this.categoryId,
    required this.categoryName,
  });

  factory SubCategory.fromJson(Map<String, dynamic> j) => SubCategory(
        id: j['id'] as int,
        name: j['name'] as String,
        slug: j['slug'] as String? ?? '',
        categoryId: j['categoryId'] as int? ?? 0,
        categoryName: j['categoryName'] as String? ?? '',
      );
}
