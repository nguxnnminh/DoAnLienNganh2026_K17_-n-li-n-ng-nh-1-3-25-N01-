import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../core/theme/app_colors.dart';
import '../features/auth/providers/auth_provider.dart';
import '../features/auth/screens/forgot_password_screen.dart';
import '../features/auth/screens/login_screen.dart';
import '../features/auth/screens/register_screen.dart';
import '../features/auth/screens/reset_password_screen.dart';
import '../features/cart/screens/cart_screen.dart';
import '../features/checkout/screens/checkout_screen.dart';
import '../features/checkout/screens/checkout_success_screen.dart';
import '../features/home/screens/home_screen.dart';
import '../features/notifications/screens/notifications_screen.dart';
import '../features/orders/screens/order_detail_screen.dart';
import '../features/orders/screens/orders_screen.dart';
import '../features/product/screens/product_detail_screen.dart';
import '../features/profile/screens/my_coupons_screen.dart';
import '../features/profile/screens/profile_screen.dart';
import '../features/shop/screens/info_screen.dart';
import '../features/shop/screens/shop_screen.dart';
import '../features/wishlist/screens/wishlist_screen.dart';

// Shared fade transition builder
CustomTransitionPage<void> _fadePage(Widget child, GoRouterState state) =>
    CustomTransitionPage<void>(
      key: state.pageKey,
      child: child,
      transitionDuration: const Duration(milliseconds: 220),
      reverseTransitionDuration: const Duration(milliseconds: 180),
      transitionsBuilder: (_, animation, _, child) =>
          FadeTransition(opacity: CurvedAnimation(parent: animation, curve: Curves.easeOut), child: child),
    );

// Slide-up transition for detail pages
CustomTransitionPage<void> _slidePage(Widget child, GoRouterState state) =>
    CustomTransitionPage<void>(
      key: state.pageKey,
      child: child,
      transitionDuration: const Duration(milliseconds: 320),
      reverseTransitionDuration: const Duration(milliseconds: 260),
      transitionsBuilder: (_, animation, _, child) {
        final offset = Tween<Offset>(begin: const Offset(1.0, 0), end: Offset.zero)
            .animate(CurvedAnimation(parent: animation, curve: Curves.easeOutCubic));
        return SlideTransition(position: offset, child: child);
      },
    );

final routerProvider = Provider<GoRouter>((ref) {
  final auth = ref.watch(authProvider);

  return GoRouter(
    initialLocation: '/',
    redirect: (context, state) {
      final authRequired = ['/checkout', '/orders', '/profile', '/wishlist', '/notifications', '/coupons'];
      final isAuthRoute = state.matchedLocation == '/login' || state.matchedLocation == '/register';
      final needsAuth = authRequired.any((p) => state.matchedLocation.startsWith(p));

      if (needsAuth && !auth.isLoggedIn) {
        return '/login?redirect=${state.uri}';
      }
      if (isAuthRoute && auth.isLoggedIn) return '/';
      return null;
    },
    routes: [
      // ── SHELL (bottom nav) — fade between tabs ───────────────────
      ShellRoute(
        builder: (context, state, child) => _ScaffoldWithNav(location: state.matchedLocation, child: child),
        routes: [
          GoRoute(path: '/', pageBuilder: (_, s) => _fadePage(const HomeScreen(), s)),
          GoRoute(
            path: '/shop',
            pageBuilder: (_, state) {
              final catId = int.tryParse(state.uri.queryParameters['categoryId'] ?? '');
              return _fadePage(ShopScreen(initialCategoryId: catId), state);
            },
          ),
          GoRoute(path: '/cart', pageBuilder: (_, s) => _fadePage(const CartScreen(), s)),
          GoRoute(path: '/profile', pageBuilder: (_, s) => _fadePage(const ProfileScreen(), s)),
          GoRoute(path: '/wishlist', pageBuilder: (_, s) => _fadePage(const WishlistScreen(), s)),
          GoRoute(path: '/orders', pageBuilder: (_, s) => _fadePage(const OrdersScreen(), s)),
          GoRoute(path: '/notifications', pageBuilder: (_, s) => _fadePage(const NotificationsScreen(), s)),
        ],
      ),

      // ── AUTH — fade ───────────────────────────────────────────────
      GoRoute(path: '/login', pageBuilder: (_, state) => _fadePage(LoginScreen(redirect: state.uri.queryParameters['redirect']), state)),
      GoRoute(path: '/register', pageBuilder: (_, s) => _fadePage(const RegisterScreen(), s)),
      GoRoute(path: '/forgot-password', pageBuilder: (_, s) => _slidePage(const ForgotPasswordScreen(), s)),
      GoRoute(path: '/reset-password', pageBuilder: (_, state) => _slidePage(ResetPasswordScreen(token: state.uri.queryParameters['token'] ?? ''), state)),

      // ── PRODUCT — slide from right ────────────────────────────────
      GoRoute(
        path: '/product/:id',
        pageBuilder: (_, state) => _slidePage(ProductDetailScreen(productId: int.parse(state.pathParameters['id']!)), state),
      ),

      // ── CHECKOUT — slide ──────────────────────────────────────────
      GoRoute(path: '/checkout', pageBuilder: (_, s) => _slidePage(const CheckoutScreen(), s)),
      GoRoute(
        path: '/checkout/success/:id',
        pageBuilder: (_, state) => _fadePage(CheckoutSuccessScreen(orderId: int.parse(state.pathParameters['id']!)), state),
      ),

      // ── ORDERS — slide ────────────────────────────────────────────
      GoRoute(
        path: '/orders/:id',
        pageBuilder: (_, state) => _slidePage(OrderDetailScreen(orderId: int.parse(state.pathParameters['id']!)), state),
      ),

      // ── ACCOUNT EXTRAS — slide ────────────────────────────────────
      GoRoute(path: '/coupons', pageBuilder: (_, s) => _slidePage(const MyCouponsScreen(), s)),

      // ── INFO PAGES — slide ────────────────────────────────────────
      GoRoute(path: '/sizing', pageBuilder: (_, s) => _slidePage(const SizingScreen(), s)),
      GoRoute(path: '/contact', pageBuilder: (_, s) => _slidePage(const ContactScreen(), s)),
      GoRoute(path: '/returns', pageBuilder: (_, s) => _slidePage(const ReturnsScreen(), s)),
    ],
    errorBuilder: (_, state) => Scaffold(
      backgroundColor: AppColors.background,
      body: Center(child: Text('404 — Page not found', style: TextStyle(fontFamily: 'DMSans', color: AppColors.textMuted))),
    ),
  );
});

class _ScaffoldWithNav extends ConsumerWidget {
  final Widget child;
  final String location;

  const _ScaffoldWithNav({required this.child, required this.location});

  int _currentIndex(String loc) {
    if (loc.startsWith('/shop')) return 1;
    if (loc.startsWith('/cart')) return 2;
    if (loc.startsWith('/profile') || loc.startsWith('/wishlist') ||
        loc.startsWith('/orders') || loc.startsWith('/notifications')) {
      return 3;
    }
    return 0;
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final idx = _currentIndex(location);

    return Scaffold(
      backgroundColor: AppColors.background,
      body: child,
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          border: Border(top: BorderSide(color: AppColors.borderDeep)),
          color: AppColors.backgroundDark,
        ),
        child: BottomNavigationBar(
          currentIndex: idx,
          backgroundColor: Colors.transparent,
          elevation: 0,
          selectedItemColor: AppColors.white,
          unselectedItemColor: AppColors.textDim,
          selectedLabelStyle: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.16, fontWeight: FontWeight.w500),
          unselectedLabelStyle: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.16),
          type: BottomNavigationBarType.fixed,
          onTap: (i) {
            switch (i) {
              case 0: context.go('/');
              case 1: context.go('/shop');
              case 2: context.go('/cart');
              case 3: context.go('/profile');
            }
          },
          items: const [
            BottomNavigationBarItem(icon: Icon(Icons.home_outlined, size: 22), activeIcon: Icon(Icons.home, size: 22), label: 'Home'),
            BottomNavigationBarItem(icon: Icon(Icons.grid_view_outlined, size: 22), activeIcon: Icon(Icons.grid_view, size: 22), label: 'Shop'),
            BottomNavigationBarItem(icon: Icon(Icons.shopping_bag_outlined, size: 22), activeIcon: Icon(Icons.shopping_bag, size: 22), label: 'Cart'),
            BottomNavigationBarItem(icon: Icon(Icons.person_outline, size: 22), activeIcon: Icon(Icons.person, size: 22), label: 'Account'),
          ],
        ),
      ),
    );
  }
}
