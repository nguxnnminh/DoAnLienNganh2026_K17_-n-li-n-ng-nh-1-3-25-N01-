import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/theme/app_colors.dart';

class NovaAppBar extends StatelessWidget implements PreferredSizeWidget {
  final String title;
  final bool showBack;
  final List<Widget>? actions;
  final bool isDisplay;

  const NovaAppBar({
    super.key,
    required this.title,
    this.showBack = true,
    this.actions,
    this.isDisplay = true,
  });

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) {
    // Auto-detect: show back if canPop OR showBack was explicitly requested
    final canPop = GoRouter.of(context).canPop();
    final shouldShowBack = showBack && canPop;

    return AppBar(
      backgroundColor: AppColors.backgroundDark,
      elevation: 0,
      scrolledUnderElevation: 0,
      automaticallyImplyLeading: false,
      leading: shouldShowBack
          ? IconButton(
              icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: AppColors.textPrimary),
              onPressed: () => context.pop(),
            )
          : null,
      centerTitle: true,
      title: isDisplay
          ? Text(
              title.toUpperCase(),
              style: const TextStyle(
                fontFamily: 'BebasNeue',
                fontSize: 20,
                letterSpacing: 0.14,
                color: AppColors.textPrimary,
              ),
            )
          : Text(
              title,
              style: const TextStyle(
                fontFamily: 'DMSans',
                fontSize: 14,
                fontWeight: FontWeight.w500,
                color: AppColors.textPrimary,
              ),
            ),
      actions: actions,
      bottom: const PreferredSize(
        preferredSize: Size.fromHeight(1),
        child: Divider(height: 1, color: AppColors.borderDeep),
      ),
    );
  }
}
