import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../core/utils/image_url_helper.dart';
import '../../../models/cart.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../../../shared/widgets/nova_button.dart';
import '../providers/cart_provider.dart';

class CartScreen extends ConsumerWidget {
  const CartScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final cartAsync = ref.watch(cartProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: NovaAppBar(
        title: 'Shopping Cart',
        showBack: false,
        actions: [
          cartAsync.whenOrNull(
            data: (cart) => cart.items.isNotEmpty
                ? TextButton(
                    onPressed: () => ref.read(cartProvider.notifier).clearCart(),
                    child: const Text('Clear', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted, letterSpacing: 0.1)),
                  )
                : null,
          ) ?? const SizedBox.shrink(),
        ],
      ),
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 250),
        child: cartAsync.when(
          data: (cart) => cart.items.isEmpty
              ? const _Empty(key: ValueKey('empty'))
              : _CartBody(key: const ValueKey('cart'), cart: cart, ref: ref),
          loading: () => const Center(key: ValueKey('loading'), child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
          error: (_, _) => Center(
            key: const ValueKey('error'),
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              const Text('Could not load cart', style: TextStyle(fontFamily: 'DMSans', color: AppColors.textMuted2)),
              const SizedBox(height: 12),
              TextButton(onPressed: () => ref.read(cartProvider.notifier).fetch(),
                  child: const Text('Retry', style: TextStyle(fontFamily: 'DMSans', color: AppColors.white))),
            ]),
          ),
        ),
      ),
    );
  }
}

class _CartBody extends StatelessWidget {
  final Cart cart;
  final WidgetRef ref;
  const _CartBody({super.key, required this.cart, required this.ref});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Expanded(
          child: ListView.separated(
            padding: const EdgeInsets.fromLTRB(20, 16, 20, 0),
            itemCount: cart.items.length,
            separatorBuilder: (_, _) => const Divider(color: AppColors.borderDark, height: 28),
            itemBuilder: (_, i) {
              final item = cart.items[i];
              return Dismissible(
                key: ValueKey(item.variantId),
                direction: DismissDirection.endToStart,
                background: Container(
                  alignment: Alignment.centerRight,
                  padding: const EdgeInsets.only(right: 20),
                  color: AppColors.error.withAlpha(180),
                  child: const Icon(Icons.delete_outline, color: AppColors.white, size: 22),
                ),
                onDismissed: (_) => ref.read(cartProvider.notifier).removeItem(item.variantId),
                child: _CartItem(
                  item: item,
                  onRemove: () => ref.read(cartProvider.notifier).removeItem(item.variantId),
                  onQtyChange: (q) {
                    if (q < 1) {
                      ref.read(cartProvider.notifier).removeItem(item.variantId);
                    } else {
                      ref.read(cartProvider.notifier).updateItem(item.variantId, q);
                    }
                  },
                ),
              );
            },
          ),
        ),

        // Order summary panel
        Container(
          decoration: const BoxDecoration(
            border: Border(top: BorderSide(color: AppColors.borderDark)),
            color: AppColors.backgroundDark,
          ),
          padding: const EdgeInsets.fromLTRB(20, 20, 20, 24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('ORDER SUMMARY', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
              const SizedBox(height: 14),
              _SummaryRow('Subtotal (${cart.itemCount} item${cart.itemCount > 1 ? 's' : ''})', formatPrice(cart.total)),
              const SizedBox(height: 8),
              _SummaryRow(
                'Shipping',
                cart.total >= 500000 ? 'Free' : formatPrice(30000),
                valueColor: cart.total >= 500000 ? AppColors.success : null,
              ),
              const Divider(color: AppColors.borderDark, height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text('TOTAL', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, letterSpacing: 0.2, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
                  // Total animates when quantity/items change
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 300),
                    transitionBuilder: (child, anim) => FadeTransition(opacity: anim, child: SlideTransition(
                      position: Tween<Offset>(begin: const Offset(0, 0.3), end: Offset.zero).animate(anim),
                      child: child,
                    )),
                    child: Text(
                      formatPrice(cart.total >= 500000 ? cart.total : cart.total + 30000),
                      key: ValueKey(cart.total),
                      style: const TextStyle(fontFamily: 'DMSans', fontSize: 20, color: AppColors.textPrimary, fontWeight: FontWeight.w400),
                    ),
                  ),
                ],
              ),
              if (cart.total < 500000) ...[
                const SizedBox(height: 6),
                Text(
                  'Add ${formatPrice(500000 - cart.total)} more for free shipping',
                  style: const TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.accent, letterSpacing: 0.06),
                ),
              ],
              const SizedBox(height: 16),
              NovaPrimaryButton(
                label: 'Checkout — ${cart.itemCount} item${cart.itemCount > 1 ? 's' : ''}',
                onPressed: () => context.go('/checkout'),
                height: 52,
              ),
              const SizedBox(height: 10),
              NovaOutlineButton(label: 'Continue Shopping', onPressed: () => context.go('/shop'), height: 44),
            ],
          ),
        ),
      ],
    );
  }
}

class _SummaryRow extends StatelessWidget {
  final String label, value;
  final Color? valueColor;
  const _SummaryRow(this.label, this.value, {this.valueColor});

  @override
  Widget build(BuildContext context) => Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(label, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted)),
          Text(value, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: valueColor ?? AppColors.textPrimary)),
        ],
      );
}

class _CartItem extends StatelessWidget {
  final CartItem item;
  final VoidCallback onRemove;
  final ValueChanged<int> onQtyChange;
  const _CartItem({required this.item, required this.onRemove, required this.onQtyChange});

  @override
  Widget build(BuildContext context) {
    final imgUrl = resolveImageUrl(item.imageUrl);
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: 88, height: 112,
          color: AppColors.surface,
          child: imgUrl.isNotEmpty
              ? CachedNetworkImage(imageUrl: imgUrl, fit: BoxFit.cover,
                  placeholder: (_, _) => Container(color: AppColors.surface),
                  errorWidget: (_, _, _) => Container(color: AppColors.surface))
              : null,
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(item.productName.toUpperCase(), maxLines: 2, overflow: TextOverflow.ellipsis,
                  style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary, fontWeight: FontWeight.w500, letterSpacing: 0.06)),
              const SizedBox(height: 4),
              Text('${item.size} / ${item.color}', style: const TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted2)),
              const SizedBox(height: 8),
              Text(formatPrice(item.price), style: const TextStyle(fontFamily: 'DMSans', fontSize: 14, color: AppColors.textPrimary)),
              const SizedBox(height: 12),
              Row(children: [
                Container(
                  decoration: BoxDecoration(border: Border.all(color: AppColors.border)),
                  child: Row(children: [
                    _QBtn(icon: Icons.remove, onTap: () => onQtyChange(item.quantity - 1)),
                    AnimatedSwitcher(
                      duration: const Duration(milliseconds: 150),
                      child: SizedBox(width: 36, key: ValueKey(item.quantity),
                          child: Text('${item.quantity}', textAlign: TextAlign.center,
                              style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textPrimary))),
                    ),
                    _QBtn(icon: Icons.add, onTap: () => onQtyChange(item.quantity + 1)),
                  ]),
                ),
                const Spacer(),
                GestureDetector(onTap: onRemove, child: const Icon(Icons.delete_outline, size: 18, color: AppColors.textMuted2)),
              ]),
            ],
          ),
        ),
      ],
    );
  }
}

class _QBtn extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _QBtn({required this.icon, required this.onTap});
  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: SizedBox(width: 32, height: 32, child: Icon(icon, size: 14, color: AppColors.textMuted)),
      );
}

class _Empty extends StatelessWidget {
  const _Empty({super.key});
  @override
  Widget build(BuildContext context) => Center(
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.shopping_bag_outlined, size: 52, color: AppColors.textDim),
          const SizedBox(height: 16),
          const Text('Your cart is empty', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 24, letterSpacing: 0.08, color: AppColors.textMuted)),
          const SizedBox(height: 8),
          const Text('Add items to your cart to continue', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
          const SizedBox(height: 28),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 48),
            child: NovaPrimaryButton(label: 'Shop Now', onPressed: () => context.go('/shop')),
          ),
        ]),
      );
}
