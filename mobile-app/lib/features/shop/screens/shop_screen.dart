import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../../../shared/widgets/product_card.dart';
import '../../../shared/widgets/shimmer_box.dart';
import '../../home/providers/home_provider.dart';
import '../providers/shop_provider.dart';
class ShopScreen extends ConsumerStatefulWidget {
  final int? initialCategoryId;
  const ShopScreen({super.key, this.initialCategoryId});

  @override
  ConsumerState<ShopScreen> createState() => _ShopScreenState();
}

class _ShopScreenState extends ConsumerState<ShopScreen> {
  final _searchCtrl = TextEditingController();
  final _minPriceCtrl = TextEditingController();
  final _maxPriceCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    if (widget.initialCategoryId != null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        ref.read(shopFilterProvider.notifier).state =
            ShopFilter(categoryId: widget.initialCategoryId);
      });
    }
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    _minPriceCtrl.dispose();
    _maxPriceCtrl.dispose();
    super.dispose();
  }

  void _applySearch() {
    final f = ref.read(shopFilterProvider);
    ref.read(shopFilterProvider.notifier).state =
        f.copyWith(keyword: _searchCtrl.text.trim(), page: 0);
  }

  @override
  Widget build(BuildContext context) {
    final filter = ref.watch(shopFilterProvider);
    final products = ref.watch(shopProductsProvider);
    final categories = ref.watch(categoriesProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: NovaAppBar(
        title: 'Shop',
        showBack: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.tune, size: 20, color: AppColors.textMuted),
            onPressed: () => _showFilterSheet(context),
          ),
        ],
      ),
      body: Column(
        children: [
          // ── SEARCH BAR ────────────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _searchCtrl,
                    onSubmitted: (_) => _applySearch(),
                    style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.whiteSoft),
                    cursorColor: AppColors.textMuted,
                    decoration: InputDecoration(
                      hintText: 'SEARCH PRODUCTS',
                      hintStyle: const TextStyle(fontFamily: 'DMSans', fontSize: 11, letterSpacing: 0.14, color: AppColors.textDisabled),
                      prefixIcon: const Icon(Icons.search, size: 18, color: AppColors.textDim),
                      suffixIcon: filter.keyword.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.close, size: 16, color: AppColors.textDim),
                              onPressed: () {
                                _searchCtrl.clear();
                                ref.read(shopFilterProvider.notifier).state =
                                    filter.copyWith(keyword: '', page: 0);
                              },
                            )
                          : null,
                      border: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.border), borderRadius: BorderRadius.zero),
                      enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.border), borderRadius: BorderRadius.zero),
                      focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.textDim), borderRadius: BorderRadius.zero),
                      contentPadding: const EdgeInsets.symmetric(vertical: 10),
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                GestureDetector(
                  onTap: _applySearch,
                  child: Container(
                    height: 44,
                    width: 44,
                    color: AppColors.white,
                    child: const Icon(Icons.search, color: AppColors.background, size: 18),
                  ),
                ),
              ],
            ),
          ),

          // ── ACTIVE FILTER CHIPS ───────────────────────────────────────
          if (filter.categoryId != null || filter.keyword.isNotEmpty || filter.minPrice != null || filter.maxPrice != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
              child: SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    if (filter.keyword.isNotEmpty)
                      _Chip(label: '"${filter.keyword}"', onRemove: () {
                        _searchCtrl.clear();
                        ref.read(shopFilterProvider.notifier).state = filter.copyWith(keyword: '', page: 0);
                      }),
                    ...categories.whenOrNull(data: (cats) {
                      final cat = cats.where((c) => c.id == filter.categoryId).firstOrNull;
                      return cat != null
                          ? [_Chip(label: cat.name, onRemove: () {
                              ref.read(shopFilterProvider.notifier).state =
                                  filter.copyWith(clearCategory: true, clearSub: true, page: 0);
                            })]
                          : <Widget>[];
                    }) ?? [],
                    if (filter.minPrice != null || filter.maxPrice != null)
                      _Chip(
                        label: '${filter.minPrice != null ? formatPrice(filter.minPrice!) : '0'} – ${filter.maxPrice != null ? formatPrice(filter.maxPrice!) : '∞'}',
                        onRemove: () {
                          _minPriceCtrl.clear();
                          _maxPriceCtrl.clear();
                          ref.read(shopFilterProvider.notifier).state =
                              filter.copyWith(minPrice: null, maxPrice: null, page: 0);
                        },
                      ),
                  ],
                ),
              ),
            ),

          // ── SORT + COUNT ROW ──────────────────────────────────────────
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 10, 16, 0),
            child: Row(
              children: [
                Text(
                  products.when(data: (p) => '${p.totalElements} products', loading: () => '', error: (_, _) => ''),
                  style: const TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted2, letterSpacing: 0.1),
                ),
                const Spacer(),
                _SortPicker(
                  current: filter.sort,
                  onChanged: (s) => ref.read(shopFilterProvider.notifier).state = filter.copyWith(sort: s, page: 0),
                ),
              ],
            ),
          ),

          const SizedBox(height: 12),

          // ── PRODUCT GRID ──────────────────────────────────────────────
          Expanded(
            child: products.when(
              data: (page) => page.content.isEmpty
                  ? _EmptyState(onClear: () => ref.read(shopFilterProvider.notifier).state = const ShopFilter())
                  : Column(
                      children: [
                        Expanded(
                          child: GridView.builder(
                            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                              crossAxisCount: 2,
                              crossAxisSpacing: 2,
                              mainAxisSpacing: 20,
                              childAspectRatio: 0.62,
                            ),
                            itemCount: page.content.length,
                            itemBuilder: (_, i) => ProductCard(
                              product: page.content[i],
                              onTap: () => context.push('/product/${page.content[i].id}'),
                            ),
                          ),
                        ),
                        // ── PAGINATION ────────────────────────────────────
                        if (page.totalPages > 1)
                          _Pagination(
                            current: page.page,
                            total: page.totalPages,
                            onPage: (p) => ref.read(shopFilterProvider.notifier).state = filter.copyWith(page: p),
                          ),
                      ],
                    ),
              loading: () => GridView.builder(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
                gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
                  crossAxisCount: 2, crossAxisSpacing: 2, mainAxisSpacing: 20, childAspectRatio: 0.62,
                ),
                itemCount: 6,
                itemBuilder: (_, _) => const ProductCardShimmer(),
              ),
              error: (_, _) => Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, color: AppColors.textDim, size: 32),
                    const SizedBox(height: 12),
                    const Text('Could not load products', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error)),
                    const SizedBox(height: 12),
                    TextButton(
                      onPressed: () => ref.invalidate(shopProductsProvider),
                      child: const Text('Retry', style: TextStyle(fontFamily: 'DMSans', color: AppColors.white)),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  void _showFilterSheet(BuildContext context) {
    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.backgroundDark,
      isScrollControlled: true,
      shape: const Border(top: BorderSide(color: AppColors.border)),
      builder: (_) => _FilterSheet(
        currentFilter: ref.read(shopFilterProvider),
        minPriceCtrl: _minPriceCtrl,
        maxPriceCtrl: _maxPriceCtrl,
        onApply: (f) => ref.read(shopFilterProvider.notifier).state = f,
        onClear: () {
          _minPriceCtrl.clear();
          _maxPriceCtrl.clear();
          ref.read(shopFilterProvider.notifier).state = const ShopFilter();
        },
      ),
    );
  }
}

// ── ACTIVE FILTER CHIP ────────────────────────────────────────────────────────

class _Chip extends StatelessWidget {
  final String label;
  final VoidCallback onRemove;
  const _Chip({required this.label, required this.onRemove});

  @override
  Widget build(BuildContext context) => Container(
        margin: const EdgeInsets.only(right: 8),
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
        decoration: BoxDecoration(border: Border.all(color: AppColors.accentBorder), color: AppColors.accentSurface),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(label.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.accentText, letterSpacing: 0.1)),
            const SizedBox(width: 6),
            GestureDetector(onTap: onRemove, child: const Icon(Icons.close, size: 12, color: AppColors.accentText)),
          ],
        ),
      );
}

// ── SORT PICKER ───────────────────────────────────────────────────────────────

class _SortPicker extends StatelessWidget {
  final String current;
  final ValueChanged<String> onChanged;
  const _SortPicker({required this.current, required this.onChanged});

  static const _opts = {
    'newest': 'Newest',
    'price_asc': 'Price: Low → High',
    'price_desc': 'Price: High → Low',
    'popular': 'Best Sellers',
  };

  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: () => showModalBottomSheet(
          context: context,
          backgroundColor: AppColors.surface,
          builder: (_) => Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 16, 20, 8),
                child: Text('SORT BY', style: TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2)),
              ),
              ..._opts.entries.map((e) => ListTile(
                title: Text(e.value, style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: current == e.key ? AppColors.white : AppColors.textMuted)),
                trailing: current == e.key ? const Icon(Icons.check, size: 16, color: AppColors.white) : null,
                onTap: () { Navigator.pop(context); onChanged(e.key); },
              )),
              const SizedBox(height: 8),
            ],
          ),
        ),
        child: Row(
          children: [
            Text(_opts[current] ?? 'Newest', style: const TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted, letterSpacing: 0.1)),
            const SizedBox(width: 4),
            const Icon(Icons.keyboard_arrow_down, size: 16, color: AppColors.textDim),
          ],
        ),
      );
}

// ── PAGINATION ────────────────────────────────────────────────────────────────

class _Pagination extends StatelessWidget {
  final int current;
  final int total;
  final ValueChanged<int> onPage;
  const _Pagination({required this.current, required this.total, required this.onPage});

  @override
  Widget build(BuildContext context) {
    // Build window: show up to 5 pages around current
    final pages = <int>[];
    for (var i = 0; i < total; i++) {
      if (i == 0 || i == total - 1 || (i - current).abs() <= 2) pages.add(i);
    }

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          // Prev
          _PageBtn(label: '‹', enabled: current > 0, active: false, onTap: () => onPage(current - 1)),
          const SizedBox(width: 4),
          // Pages with ellipsis
          ...() {
            final widgets = <Widget>[];
            int? prev;
            for (final p in pages) {
              if (prev != null && p - prev > 1) {
                widgets.add(const Padding(padding: EdgeInsets.symmetric(horizontal: 4), child: Text('…', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textDim))));
              }
              widgets.add(_PageBtn(label: '${p + 1}', enabled: true, active: p == current, onTap: () => onPage(p)));
              widgets.add(const SizedBox(width: 4));
              prev = p;
            }
            return widgets;
          }(),
          // Next
          _PageBtn(label: '›', enabled: current < total - 1, active: false, onTap: () => onPage(current + 1)),
        ],
      ),
    );
  }
}

class _PageBtn extends StatelessWidget {
  final String label;
  final bool enabled;
  final bool active;
  final VoidCallback onTap;
  const _PageBtn({required this.label, required this.enabled, required this.active, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: enabled ? onTap : null,
        child: Container(
          width: 32, height: 32,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: active ? AppColors.white : Colors.transparent,
            border: Border.all(color: active ? AppColors.white : (enabled ? AppColors.border : AppColors.borderDark)),
          ),
          child: Text(
            label,
            style: TextStyle(
              fontFamily: 'DMSans', fontSize: 11,
              color: active ? AppColors.background : (enabled ? AppColors.textMuted : AppColors.borderDark),
            ),
          ),
        ),
      );
}

// ── EMPTY STATE ───────────────────────────────────────────────────────────────

class _EmptyState extends StatelessWidget {
  final VoidCallback onClear;
  const _EmptyState({required this.onClear});

  @override
  Widget build(BuildContext context) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(Icons.search_off, size: 48, color: AppColors.textDim),
            const SizedBox(height: 16),
            const Text('No products found', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, color: AppColors.textMuted, letterSpacing: 0.08)),
            const SizedBox(height: 8),
            const Text('Try adjusting your filters', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
            const SizedBox(height: 20),
            TextButton(
              onPressed: onClear,
              child: const Text('Clear Filters', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.white, letterSpacing: 0.12)),
            ),
          ],
        ),
      );
}

// ── FILTER BOTTOM SHEET ───────────────────────────────────────────────────────

class _FilterSheet extends ConsumerStatefulWidget {
  final ShopFilter currentFilter;
  final TextEditingController minPriceCtrl;
  final TextEditingController maxPriceCtrl;
  final ValueChanged<ShopFilter> onApply;
  final VoidCallback onClear;

  const _FilterSheet({
    required this.currentFilter,
    required this.minPriceCtrl,
    required this.maxPriceCtrl,
    required this.onApply,
    required this.onClear,
  });

  @override
  ConsumerState<_FilterSheet> createState() => _FilterSheetState();
}

class _FilterSheetState extends ConsumerState<_FilterSheet> {
  late ShopFilter _f;

  @override
  void initState() {
    super.initState();
    _f = widget.currentFilter;
  }

  @override
  Widget build(BuildContext context) {
    final categories = ref.watch(categoriesProvider);

    return DraggableScrollableSheet(
      initialChildSize: 0.75,
      maxChildSize: 0.95,
      minChildSize: 0.4,
      expand: false,
      builder: (_, scrollCtrl) => SingleChildScrollView(
        controller: scrollCtrl,
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Handle
            Center(child: Container(width: 36, height: 4, color: AppColors.border, margin: const EdgeInsets.only(bottom: 20))),

            Text('FILTERS', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 24, letterSpacing: 0.1, color: AppColors.white)),
            const SizedBox(height: 24),

            // Category
            _FilterLabel('Category'),
            const SizedBox(height: 10),
            categories.when(
              data: (cats) => Wrap(
                spacing: 8, runSpacing: 8,
                children: [
                  _FilterChip(label: 'All', selected: _f.categoryId == null, onTap: () => setState(() => _f = _f.copyWith(clearCategory: true))),
                  ...cats.map((c) => _FilterChip(
                    label: c.name,
                    selected: _f.categoryId == c.id,
                    onTap: () => setState(() => _f = _f.copyWith(categoryId: c.id, clearSub: true)),
                  )),
                ],
              ),
              loading: () => const SizedBox(height: 40, child: Center(child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.textMuted))),
              error: (_, _) => const SizedBox.shrink(),
            ),
            const SizedBox(height: 24),

            // Price range
            _FilterLabel('Price Range'),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(child: _PriceInput(controller: widget.minPriceCtrl, hint: 'Min')),
                const Padding(padding: EdgeInsets.symmetric(horizontal: 10), child: Text('–', style: TextStyle(color: AppColors.textDim))),
                Expanded(child: _PriceInput(controller: widget.maxPriceCtrl, hint: 'Max')),
              ],
            ),
            const SizedBox(height: 32),

            // Buttons
            Row(
              children: [
                Expanded(child: OutlinedButton(
                  onPressed: () { Navigator.pop(context); widget.onClear(); },
                  style: OutlinedButton.styleFrom(side: const BorderSide(color: AppColors.border), shape: const RoundedRectangleBorder(), minimumSize: const Size(0, 48)),
                  child: const Text('Clear All', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted, letterSpacing: 0.14)),
                )),
                const SizedBox(width: 12),
                Expanded(child: ElevatedButton(
                  onPressed: () {
                    // Apply price from text fields
                    final min = double.tryParse(widget.minPriceCtrl.text.replaceAll(',', ''));
                    final max = double.tryParse(widget.maxPriceCtrl.text.replaceAll(',', ''));
                    Navigator.pop(context);
                    widget.onApply(_f.copyWith(minPrice: min, maxPrice: max, page: 0));
                  },
                  style: ElevatedButton.styleFrom(backgroundColor: AppColors.white, foregroundColor: AppColors.background, shape: const RoundedRectangleBorder(), minimumSize: const Size(0, 48)),
                  child: const Text('Apply', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, fontWeight: FontWeight.w600, letterSpacing: 0.14)),
                )),
              ],
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }
}

class _FilterLabel extends StatelessWidget {
  final String text;
  const _FilterLabel(this.text);
  @override
  Widget build(BuildContext context) =>
      Text(text.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2));
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;
  const _FilterChip({required this.label, required this.selected, required this.onTap});

  @override
  Widget build(BuildContext context) => GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          decoration: BoxDecoration(
            color: selected ? AppColors.white : Colors.transparent,
            border: Border.all(color: selected ? AppColors.white : AppColors.border),
          ),
          child: Text(label.toUpperCase(), style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.14, color: selected ? AppColors.background : AppColors.textMuted, fontWeight: selected ? FontWeight.w600 : FontWeight.w400)),
        ),
      );
}

class _PriceInput extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  const _PriceInput({required this.controller, required this.hint});

  @override
  Widget build(BuildContext context) => TextField(
        controller: controller,
        keyboardType: TextInputType.number,
        style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.whiteSoft),
        cursorColor: AppColors.textMuted,
        decoration: InputDecoration(
          hintText: hint,
          hintStyle: const TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textDisabled),
          border: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.border), borderRadius: BorderRadius.zero),
          enabledBorder: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.border), borderRadius: BorderRadius.zero),
          focusedBorder: const OutlineInputBorder(borderSide: BorderSide(color: AppColors.textDim), borderRadius: BorderRadius.zero),
          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        ),
      );
}
