class CartItem {
  final int variantId;
  final String productName;
  final String size;
  final String color;
  final double price;
  final int quantity;
  final String? imageUrl;
  final int productId;
  final String productSlug;

  const CartItem({
    required this.variantId,
    required this.productName,
    required this.size,
    required this.color,
    required this.price,
    required this.quantity,
    this.imageUrl,
    required this.productId,
    required this.productSlug,
  });

  factory CartItem.fromJson(Map<String, dynamic> j) => CartItem(
        variantId: j['variantId'] as int,
        productName: j['productName'] as String? ?? '',
        size: j['size'] as String? ?? '',
        color: j['color'] as String? ?? '',
        price: (j['price'] as num?)?.toDouble() ?? 0,
        quantity: j['quantity'] as int? ?? 1,
        imageUrl: j['imageUrl'] as String?,
        productId: j['productId'] as int? ?? 0,
        productSlug: j['productSlug'] as String? ?? '',
      );

  double get subtotal => price * quantity;
}

class Cart {
  final List<CartItem> items;
  final double total;
  final int itemCount;

  const Cart({required this.items, required this.total, required this.itemCount});

  factory Cart.fromJson(Map<String, dynamic> j) => Cart(
        items: (j['items'] as List<dynamic>?)
                ?.map((v) => CartItem.fromJson(v as Map<String, dynamic>))
                .toList() ??
            [],
        total: (j['total'] as num?)?.toDouble() ?? 0,
        itemCount: j['itemCount'] as int? ?? 0,
      );

  factory Cart.empty() => const Cart(items: [], total: 0, itemCount: 0);
}
