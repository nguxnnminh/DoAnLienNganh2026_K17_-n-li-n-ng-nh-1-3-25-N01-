import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/cart.dart';

class CartNotifier extends StateNotifier<AsyncValue<Cart>> {
  final ApiClient _api;

  CartNotifier(this._api) : super(const AsyncValue.loading()) {
    fetch();
  }

  Future<void> fetch() async {
    try {
      final res = await _api.dio.get('/api/cart');
      state = AsyncValue.data(Cart.fromJson(res.data as Map<String, dynamic>));
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<String?> addItem(int variantId, int quantity) async {
    try {
      final res = await _api.dio.post('/api/cart/add', data: {'variantId': variantId, 'quantity': quantity});
      state = AsyncValue.data(Cart.fromJson(res.data as Map<String, dynamic>));
      return null;
    } on DioException catch (e) {
      return _extractError(e);
    }
  }

  Future<void> updateItem(int variantId, int quantity) async {
    try {
      final res = await _api.dio.put('/api/cart/update', data: {'variantId': variantId, 'quantity': quantity});
      state = AsyncValue.data(Cart.fromJson(res.data as Map<String, dynamic>));
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> removeItem(int variantId) async {
    try {
      final res = await _api.dio.delete('/api/cart/$variantId');
      state = AsyncValue.data(Cart.fromJson(res.data as Map<String, dynamic>));
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  Future<void> clearCart() async {
    try {
      final res = await _api.dio.delete('/api/cart');
      state = AsyncValue.data(Cart.fromJson(res.data as Map<String, dynamic>));
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
    }
  }

  String _extractError(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) return data['message'] as String;
    return 'Unable to complete the operation';
  }
}

final cartProvider = StateNotifierProvider<CartNotifier, AsyncValue<Cart>>((ref) {
  return CartNotifier(ref.read(apiClientProvider));
});
