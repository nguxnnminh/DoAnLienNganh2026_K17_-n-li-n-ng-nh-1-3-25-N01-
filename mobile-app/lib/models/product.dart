class ProductVariant {
  final int id;
  final String size;
  final String color;
  final double price;
  final int stock;

  const ProductVariant({
    required this.id,
    required this.size,
    required this.color,
    required this.price,
    required this.stock,
  });

  factory ProductVariant.fromJson(Map<String, dynamic> j) => ProductVariant(
        id: j['id'] as int,
        size: j['size'] as String? ?? '',
        color: j['color'] as String? ?? '',
        price: (j['price'] as num).toDouble(),
        stock: j['stock'] as int? ?? 0,
      );
}

class ProductImage {
  final int? id;
  final String imageUrl;
  final bool primaryImage;

  const ProductImage({this.id, required this.imageUrl, required this.primaryImage});

  factory ProductImage.fromJson(Map<String, dynamic> j) => ProductImage(
        id: j['id'] as int?,
        imageUrl: j['imageUrl'] as String? ?? '',
        primaryImage: j['primaryImage'] as bool? ?? false,
      );
}

class Product {
  final int id;
  final String name;
  final String slug;
  final String? description;
  final double minPrice;
  final int totalStock;
  final int totalSold;
  final String? categoryName;
  final String? subCategoryName;
  final List<ProductVariant> variants;
  final List<ProductImage> images;

  const Product({
    required this.id,
    required this.name,
    required this.slug,
    this.description,
    required this.minPrice,
    required this.totalStock,
    required this.totalSold,
    this.categoryName,
    this.subCategoryName,
    required this.variants,
    required this.images,
  });

  String? get primaryImageUrl {
    if (images.isEmpty) return null;
    try {
      return images.firstWhere((i) => i.primaryImage).imageUrl;
    } catch (_) {
      return images.first.imageUrl;
    }
  }

  String? get secondImageUrl {
    if (images.length < 2) return null;
    return images[1].imageUrl;
  }

  factory Product.fromJson(Map<String, dynamic> j) => Product(
        id: j['id'] as int,
        name: j['name'] as String,
        slug: j['slug'] as String? ?? '',
        description: j['description'] as String?,
        minPrice: (j['minPrice'] as num?)?.toDouble() ?? 0,
        totalStock: j['totalStock'] as int? ?? 0,
        totalSold: j['totalSold'] as int? ?? 0,
        categoryName: j['categoryName'] as String?,
        subCategoryName: j['subCategoryName'] as String?,
        variants: (j['variants'] as List<dynamic>?)
                ?.map((v) => ProductVariant.fromJson(v as Map<String, dynamic>))
                .toList() ??
            [],
        images: (j['images'] as List<dynamic>?)
                ?.map((v) => ProductImage.fromJson(v as Map<String, dynamic>))
                .toList() ??
            [],
      );
}

class ProductPage {
  final List<Product> content;
  final int page;
  final int size;
  final int totalElements;
  final int totalPages;

  const ProductPage({
    required this.content,
    required this.page,
    required this.size,
    required this.totalElements,
    required this.totalPages,
  });

  factory ProductPage.fromJson(Map<String, dynamic> j) => ProductPage(
        content: (j['content'] as List<dynamic>)
            .map((v) => Product.fromJson(v as Map<String, dynamic>))
            .toList(),
        page: j['page'] as int? ?? 0,
        size: j['size'] as int? ?? 12,
        totalElements: j['totalElements'] as int? ?? 0,
        totalPages: j['totalPages'] as int? ?? 0,
      );
}
