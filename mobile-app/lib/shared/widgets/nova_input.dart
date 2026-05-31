import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class NovaInput extends StatelessWidget {
  final TextEditingController controller;
  final String hint;
  final bool obscure;
  final Widget? suffix;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final VoidCallback? onSubmitted;

  const NovaInput({
    super.key,
    required this.controller,
    required this.hint,
    this.obscure = false,
    this.suffix,
    this.keyboardType,
    this.textInputAction,
    this.onSubmitted,
  });

  @override
  Widget build(BuildContext context) {
    return TextField(
      controller: controller,
      obscureText: obscure,
      keyboardType: keyboardType,
      textInputAction: textInputAction,
      onSubmitted: onSubmitted != null ? (_) => onSubmitted!() : null,
      style: TextStyle(fontFamily: 'DMSans',fontSize: 14, color: AppColors.whiteSoft),
      cursorColor: AppColors.textMuted,
      decoration: InputDecoration(
        hintText: hint.toUpperCase(),
        hintStyle: TextStyle(fontFamily: 'DMSans',
          fontSize: 11,
          letterSpacing: 0.14,
          color: AppColors.textDisabled,
        ),
        suffixIcon: suffix,
        border: const UnderlineInputBorder(
          borderSide: BorderSide(color: AppColors.surface2),
        ),
        enabledBorder: const UnderlineInputBorder(
          borderSide: BorderSide(color: AppColors.surface2),
        ),
        focusedBorder: const UnderlineInputBorder(
          borderSide: BorderSide(color: AppColors.textDim),
        ),
        contentPadding: const EdgeInsets.symmetric(vertical: 12),
      ),
    );
  }
}
