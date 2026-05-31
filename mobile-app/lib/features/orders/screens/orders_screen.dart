import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../models/order.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../providers/orders_provider.dart';

class OrdersScreen extends ConsumerWidget {
  const OrdersScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordersAsync = ref.watch(myOrdersProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'My Orders'),
      body: ordersAsync.when(
        data: (orders) => orders.isEmpty
            ? Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.receipt_long_outlined, size: 48, color: AppColors.textDim),
                    const SizedBox(height: 16),
                    Text('No orders yet', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, color: AppColors.textMuted, letterSpacing: 0.08)),
                    const SizedBox(height: 8),
                    TextButton(
                      onPressed: () => context.go('/shop'),
                      child: Text('SHOP NOW', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.2, color: AppColors.white)),
                    ),
                  ],
                ),
              )
            : RefreshIndicator(
                color: AppColors.white,
                backgroundColor: AppColors.surface,
                onRefresh: () => ref.refresh(myOrdersProvider.future),
                child: ListView.separated(
                  padding: const EdgeInsets.all(20),
                  itemCount: orders.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 12),
                  itemBuilder: (_, i) => _OrderCard(order: orders[i], onTap: () => context.go('/orders/${orders[i].id}')),
                ),
              ),
        loading: () => const Center(child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
        error: (_, _) => Center(child: Text('Could not load orders', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
      ),
    );
  }
}

class _OrderCard extends StatelessWidget {
  final Order order;
  final VoidCallback onTap;
  const _OrderCard({required this.order, required this.onTap});

  static const _statusColors = {
    'PENDING': AppColors.accent,
    'PROCESSING': Color(0xFF5B9BD5),
    'SHIPPED': Color(0xFF8B8BDF),
    'DELIVERED': AppColors.success,
    'CANCELLED': AppColors.error,
  };

  static const _statusLabels = {
    'PENDING': 'Pending',
    'PROCESSING': 'Processing',
    'SHIPPED': 'Shipped',
    'DELIVERED': 'Delivered',
    'CANCELLED': 'Cancelled',
  };

  @override
  Widget build(BuildContext context) {
    final status = order.status;
    final color = _statusColors[status] ?? AppColors.textMuted;
    final label = _statusLabels[status] ?? status;

    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: AppColors.backgroundDark, border: Border.all(color: AppColors.borderDark)),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Text('#${order.id}', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 18, letterSpacing: 0.08, color: AppColors.textPrimary)),
                const Spacer(),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(border: Border.all(color: color.withAlpha(100)), color: color.withAlpha(25)),
                  child: Text(label.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.18, color: color, fontWeight: FontWeight.w600)),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(formatDateTime(order.createdAt), style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted2)),
            const SizedBox(height: 6),
            Text('${order.items.length} item${order.items.length == 1 ? '' : 's'}', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted)),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text('Total', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted2, letterSpacing: 0.1)),
                Text(formatPrice(order.total), style: TextStyle(fontFamily: 'DMSans', fontSize: 15, color: AppColors.textPrimary, fontWeight: FontWeight.w500)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
