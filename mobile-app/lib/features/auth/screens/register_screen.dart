import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/nova_input.dart';
import '../providers/auth_provider.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _nameCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();
  final _refCtrl = TextEditingController();
  bool _obscure = true;
  String? _localError;

  @override
  void dispose() {
    _nameCtrl.dispose();
    _emailCtrl.dispose();
    _passCtrl.dispose();
    _confirmCtrl.dispose();
    _refCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    // Client-side validation
    if (_passCtrl.text.length < 8) {
      setState(() => _localError = 'Password must be at least 8 characters');
      return;
    }
    if (_passCtrl.text != _confirmCtrl.text) {
      setState(() => _localError = 'Passwords do not match');
      return;
    }
    setState(() => _localError = null);
    FocusScope.of(context).unfocus();

    final ok = await ref.read(authProvider.notifier).register(
          _emailCtrl.text.trim(),
          _passCtrl.text,
          _nameCtrl.text.trim(),
          ref: _refCtrl.text.trim().isEmpty ? null : _refCtrl.text.trim(),
        );
    if (ok && mounted) context.go('/');
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);
    final canPop = GoRouter.of(context).canPop();

    return Scaffold(
      backgroundColor: AppColors.backgroundDeep,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        automaticallyImplyLeading: false,
        leading: canPop
            ? IconButton(
                icon: const Icon(Icons.arrow_back_ios_new, size: 18, color: AppColors.textMuted),
                onPressed: () => context.pop(),
              )
            : IconButton(
                icon: const Icon(Icons.close, size: 20, color: AppColors.textMuted),
                onPressed: () => context.go('/'),
              ),
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 32),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(height: canPop ? 32 : 64),
              const Text('NOVA', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 48, letterSpacing: 0.12, color: AppColors.textPrimary)),
              const SizedBox(height: 8),
              const Text('CREATE AN ACCOUNT', style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.22, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
              const SizedBox(height: 48),
              NovaInput(controller: _nameCtrl, hint: 'Full Name', textInputAction: TextInputAction.next),
              const SizedBox(height: 24),
              NovaInput(controller: _emailCtrl, hint: 'Email', keyboardType: TextInputType.emailAddress, textInputAction: TextInputAction.next),
              const SizedBox(height: 24),
              NovaInput(
                controller: _passCtrl,
                hint: 'Password (min 8 characters)',
                obscure: _obscure,
                textInputAction: TextInputAction.next,
                suffix: IconButton(
                  icon: Icon(_obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined, size: 18, color: AppColors.textDim),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
              const SizedBox(height: 24),
              NovaInput(controller: _confirmCtrl, hint: 'Confirm Password', obscure: true, textInputAction: TextInputAction.next),
              const SizedBox(height: 24),
              NovaInput(controller: _refCtrl, hint: 'Referral Code (optional)', textInputAction: TextInputAction.done, onSubmitted: _submit),
              if (_localError != null || auth.error != null)
                Padding(
                  padding: const EdgeInsets.only(top: 16),
                  child: Text(_localError ?? auth.error!, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.error)),
                ),
              const SizedBox(height: 32),
              NovaPrimaryButton(label: 'Create Account', loading: auth.loading, onPressed: _submit),
              const SizedBox(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text('Already have an account? ', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
                  GestureDetector(
                    onTap: () => context.go('/login'),
                    child: const Text('Sign In', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary, fontWeight: FontWeight.w600, decoration: TextDecoration.underline, decorationColor: AppColors.textPrimary)),
                  ),
                ],
              ),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }
}
