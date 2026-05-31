import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../shared/widgets/nova_app_bar.dart';

// ── Model ──────────────────────────────────────────────────────────────────────

class _Coupon {
  final String code;
  final String? description;
  final double discountValue;
  final double? minOrderAmount;
  final String? expiryDate;
  final bool used;
  final bool expired;
  final bool usable;

  const _Coupon({
    required this.code,
    this.description,
    required this.discountValue,
    this.minOrderAmount,
    this.expiryDate,
    required this.used,
    required this.expired,
    required this.usable,
  });

  factory _Coupon.fromJson(Map<String, dynamic> j) => _Coupon(
        code: j['code'] as String,
        description: j['description'] as String?,
        discountValue: (j['discountValue'] as num?)?.toDouble() ?? 0,
        minOrderAmount: (j['minOrderAmount'] as num?)?.toDouble(),
        expiryDate: j['expiryDate'] as String?,
        used: j['used'] as bool? ?? false,
        expired: j['expired'] as bool? ?? false,
        usable: j['usable'] as bool? ?? false,
      );

  String get statusLabel {
    if (used) return 'Used';
    if (expired) return 'Expired';
    if (usable) return 'Available';
    return 'Unavailable';
  }

  Color get statusColor {
    if (used) return AppColors.textDim;
    if (expired) return AppColors.error;
    if (usable) return AppColors.success;
    return AppColors.textMuted2;
  }
}

// ── Provider ───────────────────────────────────────────────────────────────────

final _myCouponsProvider = FutureProvider<List<_Coupon>>((ref) async {
  final res = await ref.read(apiClientProvider).dio.get('/api/coupons/my');
  return (res.data as List).map((e) => _Coupon.fromJson(e as Map<String, dynamic>)).toList();
});

// ── Screen ─────────────────────────────────────────────────────────────────────

class MyCouponsScreen extends ConsumerStatefulWidget {
  const MyCouponsScreen({super.key});

  @override
  ConsumerState<MyCouponsScreen> createState() => _MyCouponsScreenState();
}

class _MyCouponsScreenState extends ConsumerState<MyCouponsScreen> {
  String _filter = 'all'; // all | available | used | expired

  @override
  Widget build(BuildContext context) {
    final couponsAsync = ref.watch(_myCouponsProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'My Coupons'),
      body: Column(
        children: [
          // Filter tabs
          Container(
            color: AppColors.backgroundDark,
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              child: Row(
                children: [
                  _Tab(label: 'All', active: _filter == 'all', onTap: () => setState(() => _filter = 'all')),
                  const SizedBox(width: 8),
                  _Tab(label: 'Available', active: _filter == 'available', onTap: () => setState(() => _filter = 'available')),
                  const SizedBox(width: 8),
                  _Tab(label: 'Used', active: _filter == 'used', onTap: () => setState(() => _filter = 'used')),
                  const SizedBox(width: 8),
                  _Tab(label: 'Expired', active: _filter == 'expired', onTap: () => setState(() => _filter = 'expired')),
                ],
              ),
            ),
          ),

          Expanded(
            child: couponsAsync.when(
              data: (all) {
                final filtered = switch (_filter) {
                  'available' => all.where((c) => c.usable).toList(),
                  'used' => all.where((c) => c.used).toList(),
                  'expired' => all.where((c) => c.expired).toList(),
                  _ => all,
                };

                if (filtered.isEmpty) {
                  return Center(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.local_offer_outlined, size: 48, color: AppColors.textDim),
                        const SizedBox(height: 16),
                        const Text('No Coupons Yet', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, color: AppColors.textMuted, letterSpacing: 0.08)),
                        const SizedBox(height: 8),
                        const Text('Complete purchases to earn coupons', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
                        const SizedBox(height: 24),
                        TextButton(
                          onPressed: () => context.go('/shop'),
                          child: const Text('SHOP NOW', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.2, color: AppColors.white)),
                        ),
                      ],
                    ),
                  );
                }

                return RefreshIndicator(
                  color: AppColors.white,
                  backgroundColor: AppColors.surface,
                  onRefresh: () => ref.refresh(_myCouponsProvider.future),
                  child: ListView.separated(
                    padding: const EdgeInsets.all(16),
                    itemCount: filtered.length,
                    separatorBuilder: (_, _) => const SizedBox(height: 10),
                    itemBuilder: (_, i) => _CouponCard(coupon: filtered[i]),
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
              error: (_, _) => const Center(child: Text('Could not load coupons', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
            ),
          ),
        ],
      ),
    );
  }
}

class _Tab extends StatelessWidget {
  final String label;
  final bool active;
  final VoidCallback onTap;
  const _Tab({required this.label, required this.active, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 7),
          decoration: BoxDecoration(
            color: active ? AppColors.white : Colors.transparent,
            border: Border.all(color: active ? AppColors.white : AppColors.border),
          ),
          child: Text(label.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.14, color: active ? AppColors.background : AppColors.textMuted, fontWeight: active ? FontWeight.w600 : FontWeight.w400)),
        ),
      );
}

class _CouponCard extends StatelessWidget {
  final _Coupon coupon;
  const _CouponCard({required this.coupon});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: AppColors.backgroundDark,
        border: Border.all(color: coupon.usable ? AppColors.accent.withAlpha(60) : AppColors.borderDark),
      ),
      child: Row(
        children: [
          // Accent stripe
          Container(width: 4, height: 100, color: coupon.usable ? AppColors.accent : AppColors.border),
          // Content
          Expanded(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Text(formatPrice(coupon.discountValue), style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 28, letterSpacing: 0.06, color: AppColors.textPrimary)),
                      const Text(' OFF', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 16, color: AppColors.textMuted)),
                      const Spacer(),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          border: Border.all(color: coupon.statusColor.withAlpha(80)),
                          color: coupon.statusColor.withAlpha(20),
                        ),
                        child: Text(coupon.statusLabel.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 8, letterSpacing: 0.2, color: coupon.statusColor, fontWeight: FontWeight.w600)),
                      ),
                    ],
                  ),
                  // Code
                  Container(
                    margin: const EdgeInsets.symmetric(vertical: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(border: Border.all(color: AppColors.border), color: AppColors.surface),
                    child: Text(coupon.code, style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textPrimary, fontWeight: FontWeight.w600, letterSpacing: 0.12)),
                  ),
                  if (coupon.description != null && coupon.description!.isNotEmpty)
                    Text(coupon.description!, style: const TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted2, height: 1.4)),
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      if (coupon.minOrderAmount != null)
                        Text('Min order: ${formatPrice(coupon.minOrderAmount!)}', style: const TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.textDim, letterSpacing: 0.06)),
                      const Spacer(),
                      if (coupon.expiryDate != null)
                        Text('Expires: ${formatDate(coupon.expiryDate)}', style: const TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.textDim, letterSpacing: 0.06)),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
