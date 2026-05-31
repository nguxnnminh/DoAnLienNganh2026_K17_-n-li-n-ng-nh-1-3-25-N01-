import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/user.dart';

final profileProvider = FutureProvider<UserProfile>((ref) async {
  final dio = ref.read(apiClientProvider).dio;
  final res = await dio.get('/api/profile');
  return UserProfile.fromJson(res.data as Map<String, dynamic>);
});

class ProfileUpdateNotifier extends StateNotifier<AsyncValue<void>> {
  final ApiClient _api;
  final Ref _ref;

  ProfileUpdateNotifier(this._api, this._ref) : super(const AsyncValue.data(null));

  Future<bool> updateProfile({required String fullName, String? phone, String? address}) async {
    state = const AsyncValue.loading();
    try {
      await _api.dio.put('/api/profile', data: {
        'fullName': fullName,
        if (phone != null && phone.isNotEmpty) 'phone': phone,
        if (address != null && address.isNotEmpty) 'address': address,
      });
      _ref.invalidate(profileProvider);
      state = const AsyncValue.data(null);
      return true;
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
      return false;
    }
  }

  Future<bool> changePassword({
    required String oldPassword,
    required String newPassword,
    required String confirmPassword,
  }) async {
    state = const AsyncValue.loading();
    try {
      await _api.dio.post('/api/profile/change-password', data: {
        'oldPassword': oldPassword,
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      });
      state = const AsyncValue.data(null);
      return true;
    } on DioException catch (e, st) {
      state = AsyncValue.error(e, st);
      return false;
    }
  }
}

final profileUpdateProvider = StateNotifierProvider<ProfileUpdateNotifier, AsyncValue<void>>((ref) {
  return ProfileUpdateNotifier(ref.read(apiClientProvider), ref);
});
