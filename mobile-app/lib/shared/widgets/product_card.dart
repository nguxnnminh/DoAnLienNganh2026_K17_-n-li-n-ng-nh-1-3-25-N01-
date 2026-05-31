import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';
import '../../core/utils/format.dart';
import '../../core/utils/image_url_helper.dart';
import '../../models/product.dart';

class ProductCard extends StatelessWidget {
  final Product product;
  final VoidCallback onTap;

  const ProductCard({super.key, required this.product, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final img = resolveImageUrl(product.primaryImageUrl);

    return GestureDetector(
      onTap: onTap,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          AspectRatio(
            aspectRatio: 3 / 4,
            child: Stack(
              fit: StackFit.expand,
              children: [
                // Hero wraps primary image for smooth product→detail transition
                Hero(
                  tag: 'product-${product.id}',
                  child: img.isNotEmpty
                      ? CachedNetworkImage(
                          imageUrl: img,
                          fit: BoxFit.cover,
                          placeholder: (_, _) => Container(color: AppColors.surface),
                          errorWidget: (_, _, _) => Container(color: AppColors.surface),
                        )
                      : Container(color: AppColors.surface),
                ),
                // Category badge
                Positioned(
                  top: 12, left: 12,
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: const BoxDecoration(color: Color(0xCC0D0D0D)),
                    child: Text(
                      (product.categoryName ?? '').toUpperCase(),
                      style: const TextStyle(fontFamily: 'DMSans', fontSize: 8, letterSpacing: 0.18, color: AppColors.textMuted, fontWeight: FontWeight.w500),
                    ),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          Text(
            product.name,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textPrimary, fontWeight: FontWeight.w400, letterSpacing: 0.02),
          ),
          const SizedBox(height: 4),
          Text(
            formatPrice(product.minPrice),
            style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted, letterSpacing: 0.04),
          ),
        ],
      ),
    );
  }
}

