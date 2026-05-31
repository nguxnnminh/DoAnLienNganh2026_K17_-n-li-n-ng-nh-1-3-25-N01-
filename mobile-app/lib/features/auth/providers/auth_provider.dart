import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../models/user.dart';

class AuthState {
  final String? token;
  final String? email;
  final String? role;
  final bool loading;
  final String? error;

  const AuthState({this.token, this.email, this.role, this.loading = false, this.error});

  bool get isLoggedIn => token != null;
  AuthState copyWith({String? token, String? email, String? role, bool? loading, String? error}) =>
      AuthState(
        token: token ?? this.token,
        email: email ?? this.email,
        role: role ?? this.role,
        loading: loading ?? this.loading,
        error: error,
      );
}

class AuthNotifier extends StateNotifier<AuthState> {
  final ApiClient _api;

  AuthNotifier(this._api) : super(const AuthState()) {
    _loadSaved();
  }

  Future<void> _loadSaved() async {
    final token = await _api.getToken();
    if (token != null) state = state.copyWith(token: token);
  }

  Future<bool> login(String email, String password) async {
    state = state.copyWith(loading: true, error: null);
    try {
      final res = await _api.dio.post('/api/auth/login', data: {'email': email, 'password': password});
      final auth = AuthResponse.fromJson(res.data as Map<String, dynamic>);
      await _api.saveToken(auth.token);
      state = AuthState(token: auth.token, email: auth.email, role: auth.role);
      return true;
    } on DioException catch (e) {
      state = state.copyWith(loading: false, error: _extractError(e));
      return false;
    }
  }

  Future<bool> register(String email, String password, String fullName, {String? ref}) async {
    state = state.copyWith(loading: true, error: null);
    try {
      final res = await _api.dio.post('/api/auth/register', data: {
        'email': email,
        'password': password,
        'fullName': fullName,
        'ref': ?ref,
      });
      final auth = AuthResponse.fromJson(res.data as Map<String, dynamic>);
      await _api.saveToken(auth.token);
      state = AuthState(token: auth.token, email: auth.email, role: auth.role);
      return true;
    } on DioException catch (e) {
      state = state.copyWith(loading: false, error: _extractError(e));
      return false;
    }
  }

  Future<void> logout() async {
    await _api.clearToken();
    state = const AuthState();
  }

  String _extractError(DioException e) {
    final data = e.response?.data;
    if (data is Map && data['message'] != null) return data['message'] as String;
    return 'An error occurred. Please try again.';
  }
}

final authProvider = StateNotifierProvider<AuthNotifier, AuthState>((ref) {
  return AuthNotifier(ref.read(apiClientProvider));
});
