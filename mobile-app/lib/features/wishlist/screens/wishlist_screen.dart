import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../core/utils/image_url_helper.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../../../shared/widgets/nova_button.dart';
import '../providers/wishlist_provider.dart';

class WishlistScreen extends ConsumerWidget {
  const WishlistScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final wishlistAsync = ref.watch(wishlistProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'Wishlist'),
      body: wishlistAsync.when(
        data: (items) => items.isEmpty
            ? Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.favorite_border, size: 48, color: AppColors.textDim),
                    const SizedBox(height: 16),
                    Text('No items in wishlist', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, letterSpacing: 0.08, color: AppColors.textMuted)),
                    const SizedBox(height: 24),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 48),
                      child: NovaPrimaryButton(label: 'Explore Products', onPressed: () => context.go('/shop')),
                    ),
                  ],
                ),
              )
            : GridView.builder(
                padding: const EdgeInsets.all(16),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, crossAxisSpacing: 2, mainAxisSpacing: 16, childAspectRatio: 0.7),
                itemCount: items.length,
                itemBuilder: (_, i) {
                  final item = items[i];
                  final imgUrl = resolveImageUrl(item.primaryImageUrl);
                  return GestureDetector(
                    onTap: () => context.push('/product/${item.productId}'),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(
                          child: Stack(
                            fit: StackFit.expand,
                            children: [
                              imgUrl.isNotEmpty
                                  ? CachedNetworkImage(imageUrl: imgUrl, fit: BoxFit.cover,
                                      placeholder: (_, _) => Container(color: AppColors.surface),
                                      errorWidget: (_, _, _) => Container(color: AppColors.surface))
                                  : Container(color: AppColors.surface),
                              Positioned(
                                top: 8, right: 8,
                                child: GestureDetector(
                                  onTap: () => ref.read(wishlistProvider.notifier).toggle(item.productId),
                                  child: Container(
                                    padding: const EdgeInsets.all(6),
                                    color: AppColors.backgroundDark.withAlpha(200),
                                    child: const Icon(Icons.favorite, size: 16, color: AppColors.error),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 8),
                        Text(item.productName, maxLines: 1, overflow: TextOverflow.ellipsis, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary)),
                        Text(formatPrice(item.minPrice), style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted)),
                      ],
                    ),
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
        error: (_, _) => Center(child: Text('Could not load wishlist', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
      ),
    );
  }
}
