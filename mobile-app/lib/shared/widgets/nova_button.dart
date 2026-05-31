import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class NovaPrimaryButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final double height;

  const NovaPrimaryButton({
    super.key,
    required this.label,
    this.onPressed,
    this.loading = false,
    this.height = 48,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: height,
      child: ElevatedButton(
        onPressed: loading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.white,
          foregroundColor: AppColors.backgroundDeep,
          disabledBackgroundColor: AppColors.surface2,
          shape: const RoundedRectangleBorder(),
          elevation: 0,
        ),
        child: loading
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.background),
              )
            : Text(
                label.toUpperCase(),
                style: TextStyle(fontFamily: 'DMSans',
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.22,
                  color: AppColors.backgroundDeep,
                ),
              ),
      ),
    );
  }
}

class NovaOutlineButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool loading;
  final double height;
  final Color? borderColor;
  final Color? textColor;

  const NovaOutlineButton({
    super.key,
    required this.label,
    this.onPressed,
    this.loading = false,
    this.height = 48,
    this.borderColor,
    this.textColor,
  });

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: double.infinity,
      height: height,
      child: OutlinedButton(
        onPressed: loading ? null : onPressed,
        style: OutlinedButton.styleFrom(
          side: BorderSide(color: borderColor ?? AppColors.border),
          shape: const RoundedRectangleBorder(),
        ),
        child: loading
            ? const SizedBox(
                width: 18,
                height: 18,
                child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.textMuted),
              )
            : Text(
                label.toUpperCase(),
                style: TextStyle(fontFamily: 'DMSans',
                  fontSize: 11,
                  fontWeight: FontWeight.w500,
                  letterSpacing: 0.16,
                  color: textColor ?? AppColors.textMuted,
                ),
              ),
      ),
    );
  }
}
