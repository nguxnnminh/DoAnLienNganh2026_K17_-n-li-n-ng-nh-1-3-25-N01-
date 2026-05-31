import 'package:flutter/material.dart';
import '../../core/theme/app_colors.dart';

class SectionHeader extends StatelessWidget {
  final String title;
  final String? actionLabel;
  final VoidCallback? onAction;

  const SectionHeader({super.key, required this.title, this.actionLabel, this.onAction});

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.baseline,
      textBaseline: TextBaseline.alphabetic,
      children: [
        Text(
          title.toUpperCase(),
          style: TextStyle(fontFamily: 'BebasNeue',
            fontSize: 28,
            letterSpacing: 0.08,
            color: AppColors.textPrimary,
          ),
        ),
        const Spacer(),
        if (actionLabel != null)
          GestureDetector(
            onTap: onAction,
            child: Text(
              actionLabel!.toUpperCase(),
              style: TextStyle(fontFamily: 'DMSans',
                fontSize: 10,
                letterSpacing: 0.22,
                color: AppColors.textMuted2,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
      ],
    );
  }
}
