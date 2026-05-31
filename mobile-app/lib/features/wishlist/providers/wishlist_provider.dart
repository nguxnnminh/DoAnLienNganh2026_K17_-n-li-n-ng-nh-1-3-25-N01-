import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/wishlist.dart';

class WishlistNotifier extends StateNotifier<AsyncValue<List<WishlistItem>>> {
  final ApiClient _api;

  WishlistNotifier(this._api) : super(const AsyncValue.loading()) {
    fetch();
  }

  Future<void> fetch() async {
    try {
      final res = await _api.dio.get('/api/wishlist');
      final items = (res.data as List)
          .map((e) => WishlistItem.fromJson(e as Map<String, dynamic>))
          .toList();
      state = AsyncValue.data(items);
    } catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> toggle(int productId) async {
    final current = state.valueOrNull ?? [];
    final exists = current.any((w) => w.productId == productId);
    try {
      if (exists) {
        await _api.dio.delete('/api/wishlist/$productId');
      } else {
        await _api.dio.post('/api/wishlist/$productId');
      }
      await fetch();
    } catch (_) {}
  }

  bool contains(int productId) =>
      state.valueOrNull?.any((w) => w.productId == productId) ?? false;
}

final wishlistProvider =
    StateNotifierProvider<WishlistNotifier, AsyncValue<List<WishlistItem>>>((ref) {
  return WishlistNotifier(ref.read(apiClientProvider));
});
