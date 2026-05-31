import 'package:cookie_jar/cookie_jar.dart';
import 'package:dio/dio.dart';
import 'package:dio_cookie_manager/dio_cookie_manager.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

// Android emulator: 10.0.2.2 → host machine localhost
// Web/desktop: localhost
// Override with --dart-define=BASE_URL=http://...
const String kBaseUrl = String.fromEnvironment('BASE_URL', defaultValue: 'http://10.0.2.2:8080');
const String kTokenKey = 'nova_jwt';

final apiClientProvider = Provider<ApiClient>((ref) => ApiClient());

class ApiClient {
  late final Dio _dio;
  late final CookieJar _cookieJar;
  final _storage = const FlutterSecureStorage();

  ApiClient() {
    _cookieJar = CookieJar();
    _dio = Dio(BaseOptions(
      baseUrl: kBaseUrl,
      connectTimeout: const Duration(seconds: 15),
      receiveTimeout: const Duration(seconds: 30),
      headers: {'Accept': 'application/json', 'Content-Type': 'application/json'},
    ));
    _dio.interceptors.addAll([
      CookieManager(_cookieJar),
      _AuthInterceptor(_storage),
      LogInterceptor(requestBody: true, responseBody: true, logPrint: (o) {}),
    ]);
  }

  Dio get dio => _dio;

  Future<String?> getToken() => _storage.read(key: kTokenKey);
  Future<void> saveToken(String token) => _storage.write(key: kTokenKey, value: token);
  Future<void> clearToken() => _storage.delete(key: kTokenKey);
}

class _AuthInterceptor extends Interceptor {
  final FlutterSecureStorage _storage;
  _AuthInterceptor(this._storage);

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) async {
    final token = await _storage.read(key: kTokenKey);
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }
}
