class OrderItem {
  final String productName;
  final String size;
  final String color;
  final double price;
  final int quantity;

  const OrderItem({
    required this.productName,
    required this.size,
    required this.color,
    required this.price,
    required this.quantity,
  });

  factory OrderItem.fromJson(Map<String, dynamic> j) => OrderItem(
        productName: j['productName'] as String? ?? '',
        size: j['size'] as String? ?? '',
        color: j['color'] as String? ?? '',
        price: (j['price'] as num?)?.toDouble() ?? 0,
        quantity: j['quantity'] as int? ?? 1,
      );
}

class Order {
  final int id;
  final String customerName;
  final String phone;
  final String address;
  final String status;
  final double total;
  final List<OrderItem> items;
  final String createdAt;

  const Order({
    required this.id,
    required this.customerName,
    required this.phone,
    required this.address,
    required this.status,
    required this.total,
    required this.items,
    required this.createdAt,
  });

  factory Order.fromJson(Map<String, dynamic> j) => Order(
        id: j['id'] as int,
        customerName: j['customerName'] as String? ?? '',
        phone: j['phone'] as String? ?? '',
        address: j['address'] as String? ?? '',
        status: j['status'] as String? ?? '',
        total: (j['total'] as num?)?.toDouble() ?? 0,
        items: (j['items'] as List<dynamic>?)
                ?.map((v) => OrderItem.fromJson(v as Map<String, dynamic>))
                .toList() ??
            [],
        createdAt: j['createdAt'] as String? ?? '',
      );

  bool get canCancel => status == 'PENDING';
  bool get canRequestCancel => status == 'PROCESSING';
}
