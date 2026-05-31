import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/network/api_client.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/nova_input.dart';

class ResetPasswordScreen extends ConsumerStatefulWidget {
  final String token;
  const ResetPasswordScreen({super.key, required this.token});

  @override
  ConsumerState<ResetPasswordScreen> createState() => _ResetPasswordScreenState();
}

class _ResetPasswordScreenState extends ConsumerState<ResetPasswordScreen> {
  final _passCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();
  bool _loading = false;
  bool _done = false;
  String? _error;

  @override
  void dispose() {
    _passCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_passCtrl.text.length < 8) {
      setState(() => _error = 'Password must be at least 8 characters');
      return;
    }
    if (_passCtrl.text != _confirmCtrl.text) {
      setState(() => _error = 'Passwords do not match');
      return;
    }
    setState(() { _loading = true; _error = null; });
    try {
      await ref.read(apiClientProvider).dio.post('/api/auth/reset-password', data: {
        'token': widget.token,
        'password': _passCtrl.text,
        'confirmPassword': _confirmCtrl.text,
      });
      if (mounted) setState(() { _loading = false; _done = true; });
    } on DioException catch (e) {
      final msg = (e.response?.data as Map?)?['message'] as String? ?? 'Failed to reset password';
      if (mounted) setState(() { _loading = false; _error = msg; });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.backgroundDeep,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: AppColors.textMuted),
          onPressed: () => context.go('/login'),
        ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const SizedBox(height: 32),
              const Text('NOVA', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 40, letterSpacing: 0.12, color: AppColors.textPrimary)),
              const SizedBox(height: 6),
              const Text('ACCOUNT RECOVERY', style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.22, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
              const SizedBox(height: 8),
              const Text('New Password', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 28, letterSpacing: 0.08, color: AppColors.textPrimary)),
              const SizedBox(height: 8),
              const Text('Enter a new password for your account.', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2, height: 1.6)),
              const SizedBox(height: 40),

              if (_done) ...[
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(border: Border.all(color: AppColors.success.withAlpha(80)), color: AppColors.success.withAlpha(20)),
                  child: const Row(
                    children: [
                      Icon(Icons.check_circle_outline, color: AppColors.success, size: 20),
                      SizedBox(width: 12),
                      Expanded(child: Text('Password reset successfully. Please sign in.', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.success, height: 1.5))),
                    ],
                  ),
                ),
                const SizedBox(height: 32),
                NovaPrimaryButton(label: 'Sign In', onPressed: () => context.go('/login')),
              ] else ...[
                NovaInput(controller: _passCtrl, hint: 'New Password (min 8 characters)', obscure: true, textInputAction: TextInputAction.next),
                const SizedBox(height: 24),
                NovaInput(controller: _confirmCtrl, hint: 'Confirm New Password', obscure: true, textInputAction: TextInputAction.done, onSubmitted: _submit),
                if (_error != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: Text(_error!, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.error)),
                  ),
                const SizedBox(height: 32),
                NovaPrimaryButton(label: 'Set New Password', loading: _loading, onPressed: _submit),
                const SizedBox(height: 20),
                Center(
                  child: TextButton(
                    onPressed: () => context.go('/login'),
                    child: const Text('← Back to Sign In', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textDim, letterSpacing: 0.08)),
                  ),
                ),
              ],
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}
