class WishlistItem {
  final int id;
  final int productId;
  final String productName;
  final String productSlug;
  final double minPrice;
  final String? primaryImageUrl;

  const WishlistItem({
    required this.id,
    required this.productId,
    required this.productName,
    required this.productSlug,
    required this.minPrice,
    this.primaryImageUrl,
  });

  factory WishlistItem.fromJson(Map<String, dynamic> j) => WishlistItem(
        id: j['id'] as int,
        productId: j['productId'] as int,
        productName: j['productName'] as String? ?? '',
        productSlug: j['productSlug'] as String? ?? '',
        minPrice: (j['minPrice'] as num?)?.toDouble() ?? 0,
        primaryImageUrl: j['primaryImageUrl'] as String?,
      );
}
