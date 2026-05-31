import 'dart:async';
import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:smooth_page_indicator/smooth_page_indicator.dart';
import '../../../core/network/api_client.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/image_url_helper.dart';
import '../../../models/product.dart';
import '../../../shared/widgets/shimmer_box.dart';
import '../providers/home_provider.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  final _heroCtrl = PageController();
  int _heroIndex = 0;
  Timer? _autoPlayTimer;

  static const _slides = [
    _Slide(eyebrow: 'Spring / Summer 2026', title: 'NEW\nDROP', sub: 'ESSENTIALS', action: '/shop', btnLabel: 'Shop New Arrivals', bg: '/images/winter-collection.jpg'),
    _Slide(eyebrow: 'Statement Layers', title: 'THE\nTOPS', sub: 'TEES · HOODIES · SHIRTS', action: '/shop', btnLabel: 'Explore Tops', bg: '/images/top.jpg'),
    _Slide(eyebrow: 'Built To Move', title: 'THE\nBOTTOMS', sub: 'PANTS · JEANS · SHORTS', action: '/shop', btnLabel: 'Explore Bottoms', bg: '/images/bottom.jpg'),
  ];

  @override
  void initState() {
    super.initState();
    _startAutoPlay();
  }

  void _startAutoPlay() {
    _autoPlayTimer?.cancel();
    _autoPlayTimer = Timer.periodic(const Duration(seconds: 5), (_) {
      if (!mounted || !_heroCtrl.hasClients) return;
      final next = (_heroIndex + 1) % _slides.length;
      _heroCtrl.animateToPage(next, duration: const Duration(milliseconds: 700), curve: Curves.easeInOutCubic);
    });
  }

  void _goToSlide(int i) {
    _heroCtrl.animateToPage(i, duration: const Duration(milliseconds: 500), curve: Curves.easeInOutCubic);
    _startAutoPlay(); // reset timer
  }

  @override
  void dispose() {
    _autoPlayTimer?.cancel();
    _heroCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final bestSellers = ref.watch(bestSellersProvider);
    final categories = ref.watch(categoriesProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      body: CustomScrollView(
        slivers: [
          // ── HERO SLIDER ─────────────────────────────────────────────
          SliverToBoxAdapter(
            child: SizedBox(
              height: MediaQuery.of(context).size.height * 0.82,
              child: GestureDetector(
                onTapDown: (_) => _autoPlayTimer?.cancel(),
                onTapUp: (_) => _startAutoPlay(),
                child: Stack(
                  children: [
                    PageView.builder(
                      controller: _heroCtrl,
                      itemCount: _slides.length,
                      onPageChanged: (i) => setState(() => _heroIndex = i),
                      itemBuilder: (_, i) => _HeroSlide(slide: _slides[i], baseUrl: kBaseUrl),
                    ),
                    // Dots
                    Positioned(
                      bottom: 24, left: 0, right: 0,
                      child: Center(
                        child: SmoothPageIndicator(
                          controller: _heroCtrl,
                          count: _slides.length,
                          onDotClicked: _goToSlide,
                          effect: const WormEffect(
                            dotWidth: 28, dotHeight: 3, radius: 0,
                            activeDotColor: AppColors.white,
                            dotColor: Color(0x40F5F5F3),
                            spacing: 8,
                          ),
                        ),
                      ),
                    ),
                    // Arrows
                    if (_heroIndex > 0)
                      Positioned(left: 16, top: 0, bottom: 0, child: Center(child: _ArrowBtn(icon: Icons.arrow_back_ios_new, onTap: () => _goToSlide(_heroIndex - 1)))),
                    if (_heroIndex < _slides.length - 1)
                      Positioned(right: 16, top: 0, bottom: 0, child: Center(child: _ArrowBtn(icon: Icons.arrow_forward_ios, onTap: () => _goToSlide(_heroIndex + 1)))),
                  ],
                ),
              ),
            ),
          ),

          // ── TRUST BAR ─────────────────────────────────────────────────
          const SliverToBoxAdapter(child: _TrustBar()),

          // ── EDITORIAL STRIP ───────────────────────────────────────────
          SliverToBoxAdapter(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 32),
              decoration: const BoxDecoration(border: Border(top: BorderSide(color: AppColors.borderDark), bottom: BorderSide(color: AppColors.borderDark))),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.center,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('The Philosophy', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.3, color: AppColors.textDim)),
                        const SizedBox(height: 8),
                        const Text('Built for those who move through\nthe city without looking back.\nMinimal form. Maximum presence.', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2, height: 1.7)),
                      ],
                    ),
                  ),
                  const Text('SS\n26', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 56, color: AppColors.surface2, letterSpacing: 0.06, height: 0.9)),
                ],
              ),
            ),
          ),

          // ── BEST SELLERS ──────────────────────────────────────────────
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 36, 20, 20),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  const Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Curated', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.3, color: AppColors.textDim)),
                      SizedBox(height: 4),
                      Text('Best Sellers', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 32, letterSpacing: 0.06, color: AppColors.textPrimary)),
                    ],
                  ),
                  const Spacer(),
                  GestureDetector(
                    onTap: () => context.go('/shop'),
                    child: const Row(children: [
                      Text('View All', style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.22, color: AppColors.textDim)),
                      SizedBox(width: 4),
                      Icon(Icons.arrow_forward, size: 12, color: AppColors.textDim),
                    ]),
                  ),
                ],
              ),
            ),
          ),

          // Best sellers
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 0, 20, 0),
              child: AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: bestSellers.when(
                  data: (products) => Column(
                    key: const ValueKey('best-sellers-data'),
                    children: products.map((p) => _BestSellerCard(product: p, onTap: () => context.push('/product/${p.id}'))).toList(),
                  ),
                  loading: () => Column(
                    key: const ValueKey('best-sellers-loading'),
                    children: List.generate(3, (_) => Padding(
                      padding: const EdgeInsets.only(bottom: 2),
                      child: AspectRatio(aspectRatio: 3 / 4, child: ShimmerBox(height: double.infinity)),
                    )),
                  ),
                  error: (_, _) => const SizedBox.shrink(key: ValueKey('best-sellers-error')),
                ),
              ),
            ),
          ),

          // ── MARQUEE ───────────────────────────────────────────────────
          const SliverToBoxAdapter(child: _Marquee()),

          // ── CATEGORIES ────────────────────────────────────────────────
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 36, 20, 20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text('Browse', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.3, color: AppColors.textDim)),
                  SizedBox(height: 4),
                  Text('Categories', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 32, letterSpacing: 0.06, color: AppColors.textPrimary)),
                ],
              ),
            ),
          ),
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 40),
            sliver: categories.when(
              data: (cats) => SliverList(
                delegate: SliverChildBuilderDelegate(
                  (_, i) => Padding(
                    padding: const EdgeInsets.only(bottom: 2),
                    child: _CategoryTile(
                      name: cats[i].name,
                      imageUrl: '$kBaseUrl/images/${cats[i].slug}.jpg',
                      onTap: () => context.go('/shop?categoryId=${cats[i].id}'),
                    ),
                  ),
                  childCount: cats.length,
                ),
              ),
              loading: () => SliverList(
                delegate: SliverChildBuilderDelegate(
                  (_, _) => Padding(padding: const EdgeInsets.only(bottom: 2), child: AspectRatio(aspectRatio: 3 / 4, child: ShimmerBox(height: double.infinity))),
                  childCount: 3,
                ),
              ),
              error: (_, _) => const SliverToBoxAdapter(child: SizedBox.shrink()),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Slide data ────────────────────────────────────────────────────────────────

class _Slide {
  final String eyebrow, title, sub, action, btnLabel, bg;
  const _Slide({required this.eyebrow, required this.title, required this.sub, required this.action, required this.btnLabel, required this.bg});
}

// ── Hero slide ────────────────────────────────────────────────────────────────

class _HeroSlide extends StatelessWidget {
  final _Slide slide;
  final String baseUrl;
  const _HeroSlide({required this.slide, required this.baseUrl});

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        CachedNetworkImage(
          imageUrl: '$baseUrl${slide.bg}',
          fit: BoxFit.cover,
          placeholder: (_, _) => Container(color: AppColors.surface),
          errorWidget: (_, _, _) => Container(decoration: const BoxDecoration(gradient: LinearGradient(begin: Alignment.topCenter, end: Alignment.bottomCenter, colors: [Color(0xFF1A1A1A), Color(0xFF0A0A0A)]))),
        ),
        const DecoratedBox(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter, end: Alignment.bottomCenter,
              stops: [0.3, 0.75, 1.0],
              colors: [Colors.transparent, Color(0xB8111111), Color(0xFF111111)],
            ),
          ),
        ),
        Positioned(
          bottom: 72, left: 20, right: 20,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(slide.eyebrow.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.3, color: AppColors.textMuted2)),
              const SizedBox(height: 6),
              Text(slide.title, style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 64, letterSpacing: 0.04, color: AppColors.textPrimary, height: 0.9)),
              const SizedBox(height: 4),
              Text(slide.sub, style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 20, letterSpacing: 0.08, color: AppColors.textDim)),
              const SizedBox(height: 20),
              GestureDetector(
                onTap: () => GoRouter.of(context).go(slide.action),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 13),
                  color: AppColors.white,
                  child: Text(slide.btnLabel.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.22, color: AppColors.backgroundDeep, fontWeight: FontWeight.w500)),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _ArrowBtn extends StatelessWidget {
  final IconData icon;
  final VoidCallback onTap;
  const _ArrowBtn({required this.icon, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: Container(width: 40, height: 40, color: const Color(0x59111111), child: Icon(icon, size: 16, color: AppColors.white)),
      );
}

// ── Trust bar ─────────────────────────────────────────────────────────────────

class _TrustBar extends StatelessWidget {
  const _TrustBar();

  @override
  Widget build(BuildContext context) {
    const items = [
      (Icons.local_shipping_outlined, 'Free Shipping', 'Over 500k'),
      (Icons.autorenew_outlined, 'Free Returns', 'Within 14 Days'),
      (Icons.lock_outline, 'Secure Payment', 'SSL Encrypted'),
      (Icons.layers_outlined, 'New Drops', 'Every Friday'),
    ];
    return Container(
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: AppColors.borderDark))),
      child: Row(
        children: List.generate(items.length, (i) {
          final item = items[i];
          return Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 8),
              decoration: BoxDecoration(border: Border(right: i < items.length - 1 ? const BorderSide(color: AppColors.borderDark) : BorderSide.none)),
              child: Column(children: [
                Icon(item.$1, size: 20, color: AppColors.textDim),
                const SizedBox(height: 8),
                Text(item.$2, textAlign: TextAlign.center, style: const TextStyle(fontFamily: 'DMSans', fontSize: 8, letterSpacing: 0.18, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
                const SizedBox(height: 2),
                Text(item.$3, textAlign: TextAlign.center, style: const TextStyle(fontFamily: 'DMSans', fontSize: 7, letterSpacing: 0.12, color: AppColors.textDim)),
              ]),
            ),
          );
        }),
      ),
    );
  }
}

// ── Best seller card ──────────────────────────────────────────────────────────

class _BestSellerCard extends StatelessWidget {
  final Product product;
  final VoidCallback onTap;
  const _BestSellerCard({required this.product, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final img = resolveImageUrl(product.primaryImageUrl);
    final img2 = resolveImageUrl(product.secondImageUrl);
    return GestureDetector(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.only(bottom: 2),
        child: Stack(
          children: [
            AspectRatio(
              aspectRatio: 3 / 4,
              child: Stack(fit: StackFit.expand, children: [
                Hero(
                  tag: 'product-${product.id}',
                  child: img.isNotEmpty
                      ? CachedNetworkImage(imageUrl: img, fit: BoxFit.cover, placeholder: (_, _) => Container(color: AppColors.surface), errorWidget: (_, _, _) => Container(color: AppColors.surface))
                      : Container(color: AppColors.surface),
                ),
                if (img2.isNotEmpty)
                  Positioned.fill(child: CachedNetworkImage(imageUrl: img2, fit: BoxFit.cover, placeholder: (_, _) => const SizedBox.shrink(), errorWidget: (_, _, _) => const SizedBox.shrink())),
              ]),
            ),
            Positioned(
              top: 16, left: 16,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                color: AppColors.white,
                child: Text((product.categoryName ?? '').toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 8, letterSpacing: 0.22, color: AppColors.backgroundDeep, fontWeight: FontWeight.w500)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// ── Marquee ───────────────────────────────────────────────────────────────────

class _Marquee extends StatelessWidget {
  const _Marquee();

  @override
  Widget build(BuildContext context) {
    const t = 'NOVA ESSENTIALS  —  SPRING 2026  —  ';
    return Container(
      height: 52,
      decoration: const BoxDecoration(border: Border.symmetric(horizontal: BorderSide(color: AppColors.borderDark))),
      alignment: Alignment.center,
      child: Text(t + t + t, style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 26, letterSpacing: 0.22, color: AppColors.surface2), maxLines: 1, overflow: TextOverflow.clip),
    );
  }
}

// ── Category tile ─────────────────────────────────────────────────────────────

class _CategoryTile extends StatelessWidget {
  final String name, imageUrl;
  final VoidCallback onTap;
  const _CategoryTile({required this.name, required this.imageUrl, required this.onTap});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AspectRatio(
        aspectRatio: 3 / 4,
        child: Stack(fit: StackFit.expand, children: [
          CachedNetworkImage(imageUrl: imageUrl, fit: BoxFit.cover, placeholder: (_, _) => Container(color: AppColors.surface), errorWidget: (_, _, _) => Container(color: AppColors.surface)),
          const DecoratedBox(decoration: BoxDecoration(gradient: LinearGradient(begin: Alignment.bottomCenter, end: Alignment.topCenter, stops: [0, 0.5, 1], colors: [Color(0xD9111111), Color(0x33111111), Colors.transparent]))),
          Positioned(
            bottom: 20, left: 20, right: 20,
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              const Text('Category', style: TextStyle(fontFamily: 'DMSans', fontSize: 8, letterSpacing: 0.3, color: AppColors.textMuted2)),
              const SizedBox(height: 2),
              Text(name, style: const TextStyle(fontFamily: 'BebasNeue', fontSize: 36, letterSpacing: 0.06, color: AppColors.textPrimary)),
              const SizedBox(height: 6),
              const Row(children: [
                Text('Explore ', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.22, color: AppColors.textMuted2)),
                Icon(Icons.arrow_forward, size: 10, color: AppColors.textMuted2),
              ]),
            ]),
          ),
        ]),
      ),
    );
  }
}

