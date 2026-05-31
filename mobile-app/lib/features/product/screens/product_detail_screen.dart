import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../core/utils/image_url_helper.dart';
import '../../../models/product.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/product_card.dart';
import '../../../shared/widgets/shimmer_box.dart';
import '../../cart/providers/cart_provider.dart';
import '../../wishlist/providers/wishlist_provider.dart';
import '../providers/product_provider.dart';

class ProductDetailScreen extends ConsumerStatefulWidget {
  final int productId;
  const ProductDetailScreen({super.key, required this.productId});

  @override
  ConsumerState<ProductDetailScreen> createState() => _ProductDetailScreenState();
}

class _ProductDetailScreenState extends ConsumerState<ProductDetailScreen> {
  int _imageIndex = 0;
  String? _selectedSize;
  String? _selectedColor;
  int _qty = 1;
  bool _addingToCart = false;
  bool _descExpanded = false;

  ProductVariant? _getVariant(Product p) {
    if (_selectedSize == null && _selectedColor == null) return null;
    return p.variants.where((v) =>
      (_selectedSize == null || v.size == _selectedSize) &&
      (_selectedColor == null || v.color == _selectedColor)
    ).firstOrNull;
  }

  Future<void> _addToCart(Product p) async {
    final variant = _getVariant(p);
    if (variant == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Please select size and color', style: TextStyle(fontFamily: 'DMSans', fontSize: 12)),
        backgroundColor: AppColors.surface,
      ));
      return;
    }
    FocusScope.of(context).unfocus();
    setState(() => _addingToCart = true);
    final err = await ref.read(cartProvider.notifier).addItem(variant.id, _qty);
    if (mounted) {
      setState(() => _addingToCart = false);
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Row(children: [
          Icon(err == null ? Icons.check_circle_outline : Icons.error_outline,
              color: err == null ? AppColors.success : AppColors.error, size: 16),
          const SizedBox(width: 8),
          Text(err ?? 'ADDED TO CART', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.14,
              color: err == null ? AppColors.textPrimary : AppColors.error)),
        ]),
        backgroundColor: AppColors.surface,
        behavior: SnackBarBehavior.floating,
        duration: const Duration(seconds: 2),
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    final productAsync = ref.watch(productDetailProvider(widget.productId));
    final similar = ref.watch(similarProductsProvider(widget.productId));
    final inWishlist = ref.watch(wishlistProvider).valueOrNull
            ?.any((w) => w.productId == widget.productId) ??
        false;

    return Scaffold(
      backgroundColor: AppColors.background,
      body: productAsync.when(
        data: (p) => _buildBody(context, p, similar, inWishlist),
        loading: () => const Center(child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
        error: (_, _) => const Center(child: Text('Could not load product', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
      ),
    );
  }

  Widget _buildBody(BuildContext ctx, Product product, AsyncValue<List<Product>> similar, bool inWishlist) {
    final images = product.images;
    final sizes = product.variants.map((v) => v.size).toSet().toList();
    final colors = product.variants.map((v) => v.color).toSet().toList();
    final variant = _getVariant(product);
    final price = variant?.price ?? product.minPrice;

    return CustomScrollView(
      slivers: [
        SliverAppBar(
          pinned: true,
          backgroundColor: AppColors.backgroundDark,
          elevation: 0,
          scrolledUnderElevation: 0,
          automaticallyImplyLeading: false,
          leading: IconButton(
            icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: AppColors.textPrimary),
            onPressed: () => ctx.pop(),
          ),
          actions: [
            IconButton(
              icon: AnimatedSwitcher(
                duration: const Duration(milliseconds: 200),
                child: Icon(inWishlist ? Icons.favorite : Icons.favorite_border,
                    key: ValueKey(inWishlist), size: 20,
                    color: inWishlist ? AppColors.error : AppColors.textMuted),
              ),
              onPressed: () => ref.read(wishlistProvider.notifier).toggle(product.id),
            ),
            IconButton(
              icon: const Icon(Icons.shopping_bag_outlined, size: 20, color: AppColors.textPrimary),
              onPressed: () => ctx.go('/cart'),
            ),
          ],
        ),

        // Main image with Hero
        SliverToBoxAdapter(
          child: AspectRatio(
            aspectRatio: 3 / 4,
            child: Stack(
              fit: StackFit.expand,
              children: [
                images.isNotEmpty
                    ? PageView.builder(
                        itemCount: images.length,
                        onPageChanged: (i) => setState(() => _imageIndex = i),
                        itemBuilder: (_, i) {
                          final img = CachedNetworkImage(
                            imageUrl: resolveImageUrl(images[i].imageUrl),
                            fit: BoxFit.cover,
                            placeholder: (_, _) => Container(color: AppColors.surface),
                            errorWidget: (_, _, _) => Container(color: AppColors.surface),
                          );
                          return i == 0 ? Hero(tag: 'product-${product.id}', child: img) : img;
                        },
                      )
                    : Hero(tag: 'product-${product.id}', child: Container(color: AppColors.surface)),
                if (images.length > 1)
                  Positioned(
                    bottom: 16, left: 0, right: 0,
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: List.generate(images.length, (i) => AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        margin: const EdgeInsets.symmetric(horizontal: 3),
                        width: i == _imageIndex ? 20 : 6,
                        height: 3,
                        color: i == _imageIndex ? AppColors.white : AppColors.surface2,
                      )),
                    ),
                  ),
              ],
            ),
          ),
        ),

        // Thumbnail strip
        if (images.length > 1)
          SliverToBoxAdapter(
            child: SizedBox(
              height: 72,
              child: ListView.separated(
                padding: const EdgeInsets.all(8),
                scrollDirection: Axis.horizontal,
                itemCount: images.length,
                separatorBuilder: (_, _) => const SizedBox(width: 6),
                itemBuilder: (_, i) => GestureDetector(
                  onTap: () => setState(() => _imageIndex = i),
                  child: AnimatedContainer(
                    duration: const Duration(milliseconds: 150),
                    width: 56,
                    decoration: BoxDecoration(
                      border: Border.all(color: i == _imageIndex ? AppColors.white : AppColors.border, width: i == _imageIndex ? 1.5 : 1),
                    ),
                    child: CachedNetworkImage(imageUrl: resolveImageUrl(images[i].imageUrl), fit: BoxFit.cover,
                        placeholder: (_, _) => Container(color: AppColors.surface),
                        errorWidget: (_, _, _) => Container(color: AppColors.surface)),
                  ),
                ),
              ),
            ),
          ),

        // Info
        SliverToBoxAdapter(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (product.categoryName != null)
                  Text(product.categoryName!.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.28, color: AppColors.textMuted2)),
                const SizedBox(height: 6),
                Text(product.name, style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 32, letterSpacing: 0.06, color: AppColors.textPrimary, height: 1.1)),
                const SizedBox(height: 8),

                // Price animates on variant select
                AnimatedSwitcher(
                  duration: const Duration(milliseconds: 200),
                  child: Align(alignment: Alignment.centerLeft, child: Text(formatPrice(price), key: ValueKey(price),
                      style: const TextStyle(fontFamily: 'DMSans', fontSize: 20, color: AppColors.textPrimary, fontWeight: FontWeight.w300))),
                ),

                // Stock status fades in
                AnimatedSize(
                  duration: const Duration(milliseconds: 200),
                  child: variant != null
                      ? Padding(
                          padding: const EdgeInsets.only(top: 6),
                          child: Text(
                            variant.stock > 10 ? 'In Stock' : variant.stock > 0 ? 'Only ${variant.stock} left' : 'Out of Stock',
                            style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: variant.stock > 0 ? AppColors.success : AppColors.error),
                          ),
                        )
                      : const SizedBox.shrink(),
                ),

                const SizedBox(height: 28),
                const Divider(color: AppColors.borderDark),
                const SizedBox(height: 20),

                // Size
                if (sizes.isNotEmpty) ...[
                  Row(children: [
                    const Text('SIZE', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
                    const Spacer(),
                    GestureDetector(
                      onTap: () => _showSizeGuide(ctx),
                      child: const Text('Size Guide', style: TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.textDim, decoration: TextDecoration.underline, decorationColor: AppColors.textDim)),
                    ),
                  ]),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8, runSpacing: 8,
                    children: sizes.map((s) {
                      final active = _selectedSize == s;
                      final hasStock = product.variants.any((v) => v.size == s && v.stock > 0);
                      return GestureDetector(
                        onTap: hasStock ? () => setState(() => _selectedSize = s) : null,
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 150),
                          padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 10),
                          decoration: BoxDecoration(
                            color: active ? AppColors.white : Colors.transparent,
                            border: Border.all(color: active ? AppColors.white : (hasStock ? AppColors.border : AppColors.borderDark)),
                          ),
                          child: Text(s.toUpperCase(), style: TextStyle(
                            fontFamily: 'DMSans', fontSize: 12, letterSpacing: 0.16,
                            color: !hasStock ? AppColors.borderDark : active ? AppColors.background : AppColors.textMuted,
                            fontWeight: active ? FontWeight.w600 : FontWeight.w400,
                            decoration: !hasStock ? TextDecoration.lineThrough : null,
                            decorationColor: AppColors.borderDark,
                          )),
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 20),
                ],

                // Color
                if (colors.isNotEmpty) ...[
                  const Text('COLOR', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 8, runSpacing: 8,
                    children: colors.map((c) {
                      final active = _selectedColor == c;
                      return GestureDetector(
                        onTap: () => setState(() => _selectedColor = c),
                        child: AnimatedContainer(
                          duration: const Duration(milliseconds: 150),
                          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
                          decoration: BoxDecoration(
                            color: active ? AppColors.white : Colors.transparent,
                            border: Border.all(color: active ? AppColors.white : AppColors.border),
                          ),
                          child: Text(c.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.14,
                              color: active ? AppColors.background : AppColors.textMuted)),
                        ),
                      );
                    }).toList(),
                  ),
                  const SizedBox(height: 20),
                ],

                // Qty
                Row(children: [
                  const Text('QTY', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2)),
                  const SizedBox(width: 16),
                  Container(
                    decoration: BoxDecoration(border: Border.all(color: AppColors.border)),
                    child: Row(children: [
                      _QBtn(icon: Icons.remove, onTap: _qty > 1 ? () => setState(() => _qty--) : null),
                      AnimatedSwitcher(
                        duration: const Duration(milliseconds: 150),
                        child: SizedBox(width: 44, key: ValueKey(_qty),
                            child: Text('$_qty', textAlign: TextAlign.center,
                                style: const TextStyle(fontFamily: 'DMSans', fontSize: 14, color: AppColors.textPrimary))),
                      ),
                      _QBtn(icon: Icons.add, onTap: () => setState(() => _qty++)),
                    ]),
                  ),
                ]),
                const SizedBox(height: 20),

                NovaPrimaryButton(label: 'Add to Cart', loading: _addingToCart, onPressed: () => _addToCart(product), height: 52),
                const SizedBox(height: 10),
                NovaOutlineButton(
                  label: 'Buy Now',
                  onPressed: () async {
                    final router = GoRouter.of(ctx);
                    await _addToCart(product);
                    if (!mounted) return;
                    router.go('/cart');
                  },
                  height: 48,
                ),

                const SizedBox(height: 20),
                const Divider(color: AppColors.borderDark),
                const SizedBox(height: 16),
                _Badges(),

                // Description accordion with AnimatedSize
                if (product.description != null && product.description!.isNotEmpty) ...[
                  const Divider(color: AppColors.borderDark),
                  GestureDetector(
                    onTap: () => setState(() => _descExpanded = !_descExpanded),
                    behavior: HitTestBehavior.opaque,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(vertical: 16),
                      child: Row(children: [
                        const Text('DESCRIPTION', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.22, color: AppColors.textMuted, fontWeight: FontWeight.w500)),
                        const Spacer(),
                        AnimatedRotation(turns: _descExpanded ? 0.125 : 0, duration: const Duration(milliseconds: 200),
                            child: const Icon(Icons.add, size: 16, color: AppColors.textDim)),
                      ]),
                    ),
                  ),
                  AnimatedSize(
                    duration: const Duration(milliseconds: 280),
                    curve: Curves.easeOutCubic,
                    child: _descExpanded
                        ? Padding(padding: const EdgeInsets.only(bottom: 16),
                            child: Text(product.description!, style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textMuted, height: 1.7)))
                        : const SizedBox.shrink(),
                  ),
                  const Divider(color: AppColors.borderDark),
                ],
                const SizedBox(height: 36),
              ],
            ),
          ),
        ),

        const SliverToBoxAdapter(
          child: Padding(
            padding: EdgeInsets.fromLTRB(20, 0, 20, 16),
            child: Text('You May Also Like', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 26, letterSpacing: 0.08, color: AppColors.textPrimary)),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 40),
          sliver: similar.when(
            data: (prods) => SliverGrid(
              delegate: SliverChildBuilderDelegate(
                (_, i) => ProductCard(product: prods[i], onTap: () => ctx.push('/product/${prods[i].id}')),
                childCount: prods.length,
              ),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, crossAxisSpacing: 2, mainAxisSpacing: 20, childAspectRatio: 0.62),
            ),
            loading: () => SliverGrid(
              delegate: SliverChildBuilderDelegate((_, _) => const ProductCardShimmer(), childCount: 4),
              gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(crossAxisCount: 2, crossAxisSpacing: 2, mainAxisSpacing: 20, childAspectRatio: 0.62),
            ),
            error: (_, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
          ),
        ),
      ],
    );
  }

  void _showSizeGuide(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.backgroundDark,
      builder: (_) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('SIZE GUIDE', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, letterSpacing: 0.1, color: AppColors.white)),
            const SizedBox(height: 16),
            ...[['XS','32–34"','24–26"'],['S','34–36"','26–28"'],['M','36–38"','28–30"'],['L','38–40"','30–32"'],['XL','40–42"','32–34"'],['XXL','42–44"','34–36"']].map((r) =>
              Padding(padding: const EdgeInsets.symmetric(vertical: 6), child: Row(children: [
                SizedBox(width: 48, child: Text(r[0], style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary, fontWeight: FontWeight.w600))),
                Expanded(child: Text('Chest: ${r[1]}', style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted))),
                Text('Waist: ${r[2]}', style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted)),
              ]))),
            const SizedBox(height: 8),
          ],
        ),
      ),
    );
  }
}

class _QBtn extends StatelessWidget {
  final IconData icon;
  final VoidCallback? onTap;
  const _QBtn({required this.icon, this.onTap});
  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: Container(width: 36, height: 36, alignment: Alignment.center,
            child: Icon(icon, size: 16, color: onTap != null ? AppColors.textMuted : AppColors.border)),
      );
}

class _Badges extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    const items = [(Icons.local_shipping_outlined,'Free Shipping','Over 500k'),(Icons.autorenew_outlined,'Free Returns','14 days'),(Icons.verified_outlined,'100% Authentic','Guaranteed')];
    return Row(
      children: items.map((item) => Expanded(
        child: Container(margin: const EdgeInsets.only(right: 6), padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 6),
          decoration: BoxDecoration(border: Border.all(color: AppColors.borderDark), color: AppColors.backgroundDark),
          child: Column(children: [
            Icon(item.$1, size: 16, color: AppColors.textMuted2),
            const SizedBox(height: 5),
            Text(item.$2, textAlign: TextAlign.center, style: const TextStyle(fontFamily: 'DMSans', fontSize: 8, color: AppColors.textMuted2, letterSpacing: 0.08, fontWeight: FontWeight.w600)),
            Text(item.$3, textAlign: TextAlign.center, style: const TextStyle(fontFamily: 'DMSans', fontSize: 8, color: AppColors.textDim)),
          ]),
        ),
      )).toList(),
    );
  }
}
