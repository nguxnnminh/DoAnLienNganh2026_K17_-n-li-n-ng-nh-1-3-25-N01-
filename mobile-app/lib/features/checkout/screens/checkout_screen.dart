import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/nova_input.dart';
import '../../cart/providers/cart_provider.dart';
import '../../profile/providers/profile_provider.dart';
import '../providers/checkout_provider.dart';

class CheckoutScreen extends ConsumerStatefulWidget {
  const CheckoutScreen({super.key});

  @override
  ConsumerState<CheckoutScreen> createState() => _CheckoutScreenState();
}

class _CheckoutScreenState extends ConsumerState<CheckoutScreen> {
  final _nameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _addressCtrl = TextEditingController();
  final _noteCtrl = TextEditingController();
  final _couponCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    ref.listenManual(profileProvider, (_, next) {
      next.whenData((p) {
        if (_nameCtrl.text.isEmpty) _nameCtrl.text = p.fullName;
        if (_phoneCtrl.text.isEmpty && p.phone != null) _phoneCtrl.text = p.phone!;
        if (_addressCtrl.text.isEmpty && p.address != null) _addressCtrl.text = p.address!;
      });
    }, fireImmediately: true);
  }

  @override
  void dispose() {
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _addressCtrl.dispose();
    _noteCtrl.dispose();
    _couponCtrl.dispose();
    super.dispose();
  }

  Future<void> _placeOrder() async {
    final order = await ref.read(checkoutProvider.notifier).placeOrder(
          name: _nameCtrl.text.trim(),
          phone: _phoneCtrl.text.trim(),
          address: _addressCtrl.text.trim(),
          note: _noteCtrl.text.trim(),
        );
    if (order != null && mounted) {
      ref.read(cartProvider.notifier).fetch();
      context.go('/checkout/success/${order.id}');
    }
  }

  Future<void> _validateCoupon() async {
    final cart = ref.read(cartProvider).valueOrNull;
    if (cart == null) return;
    await ref.read(checkoutProvider.notifier).validateCoupon(_couponCtrl.text.trim(), cart.total);
  }

  @override
  Widget build(BuildContext context) {
    final checkout = ref.watch(checkoutProvider);
    final cart = ref.watch(cartProvider).valueOrNull;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'Checkout'),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            _SectionLabel('Shipping Information'),
            const SizedBox(height: 16),
            NovaInput(controller: _nameCtrl, hint: 'Full Name', textInputAction: TextInputAction.next),
            const SizedBox(height: 20),
            NovaInput(controller: _phoneCtrl, hint: 'Phone Number', keyboardType: TextInputType.phone, textInputAction: TextInputAction.next),
            const SizedBox(height: 20),
            NovaInput(controller: _addressCtrl, hint: 'Shipping Address', textInputAction: TextInputAction.next),
            const SizedBox(height: 20),
            NovaInput(controller: _noteCtrl, hint: 'Order Notes (optional)', textInputAction: TextInputAction.done),

            const SizedBox(height: 32),
            _SectionLabel('Coupon Code'),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: NovaInput(controller: _couponCtrl, hint: 'Enter coupon code', textInputAction: TextInputAction.done, onSubmitted: _validateCoupon)),
                const SizedBox(width: 12),
                GestureDetector(
                  onTap: _validateCoupon,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    color: AppColors.surface2,
                    child: Text('Apply', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.14, color: AppColors.textMuted)),
                  ),
                ),
              ],
            ),
            AnimatedSize(
              duration: const Duration(milliseconds: 200),
              child: checkout.couponMessage != null
                  ? Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text(checkout.couponMessage!, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: checkout.discount != null ? AppColors.success : AppColors.error)),
                    )
                  : const SizedBox.shrink(),
            ),

            const SizedBox(height: 32),
            _SectionLabel('Order Summary'),
            const SizedBox(height: 12),
            if (cart != null) ...[
              ...cart.items.map((item) => Padding(
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    child: Row(
                      children: [
                        Expanded(child: Text('${item.productName} (${item.size}/${item.color}) x${item.quantity}', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted))),
                        Text(formatPrice(item.subtotal), style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary)),
                      ],
                    ),
                  )),
              const Divider(color: AppColors.borderDark, height: 24),
              if (checkout.discount != null)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('Discount', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.accent)),
                    Text('- ${formatPrice(checkout.discount)}', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.accent)),
                  ],
                ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('TOTAL', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, letterSpacing: 0.18, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
                  Text(formatPrice(cart.total - (checkout.discount ?? 0)), style: TextStyle(fontFamily: 'DMSans', fontSize: 18, color: AppColors.textPrimary, fontWeight: FontWeight.w500)),
                ],
              ),
            ],

            if (checkout.error != null)
              Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Text(checkout.error!, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.error)),
              ),

            const SizedBox(height: 32),
            NovaPrimaryButton(label: 'Place Order', loading: checkout.loading, onPressed: _placeOrder, height: 52),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  final String text;
  const _SectionLabel(this.text);

  @override
  Widget build(BuildContext context) => Text(text.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2, fontWeight: FontWeight.w600));
}
