import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_button.dart';

class CheckoutSuccessScreen extends StatefulWidget {
  final int orderId;
  const CheckoutSuccessScreen({super.key, required this.orderId});

  @override
  State<CheckoutSuccessScreen> createState() => _CheckoutSuccessScreenState();
}

class _CheckoutSuccessScreenState extends State<CheckoutSuccessScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;
  late final Animation<double> _scaleAnim;
  late final Animation<double> _fadeAnim;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(vsync: this, duration: const Duration(milliseconds: 600));
    _scaleAnim = CurvedAnimation(parent: _ctrl, curve: Curves.elasticOut);
    _fadeAnim = CurvedAnimation(parent: _ctrl, curve: const Interval(0.3, 1, curve: Curves.easeOut));
    // Start animation after first frame
    WidgetsBinding.instance.addPostFrameCallback((_) => _ctrl.forward());
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDeep,
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              // Checkmark bounces in
              ScaleTransition(
                scale: _scaleAnim,
                child: const Icon(Icons.check_circle_outline, size: 80, color: AppColors.success),
              ),
              const SizedBox(height: 28),
              // Text fades up
              FadeTransition(
                opacity: _fadeAnim,
                child: SlideTransition(
                  position: Tween<Offset>(begin: const Offset(0, 0.3), end: Offset.zero)
                      .animate(_fadeAnim),
                  child: Column(children: [
                    Text('ORDER PLACED', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 32, letterSpacing: 0.1, color: AppColors.textPrimary)),
                    const SizedBox(height: 10),
                    Text(
                      'Order #${widget.orderId} has been confirmed.\nWe\'ll contact you shortly.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textMuted, height: 1.6),
                    ),
                  ]),
                ),
              ),
              const SizedBox(height: 48),
              FadeTransition(
                opacity: _fadeAnim,
                child: Column(children: [
                  NovaPrimaryButton(label: 'View My Orders', onPressed: () => context.go('/orders'), height: 52),
                  const SizedBox(height: 12),
                  NovaOutlineButton(label: 'Continue Shopping', onPressed: () => context.go('/'), height: 48),
                ]),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
