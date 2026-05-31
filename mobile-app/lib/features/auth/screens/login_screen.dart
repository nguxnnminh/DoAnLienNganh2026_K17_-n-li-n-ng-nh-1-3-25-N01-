import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/nova_input.dart';
import '../providers/auth_provider.dart';

class LoginScreen extends ConsumerStatefulWidget {
  final String? redirect;
  const LoginScreen({super.key, this.redirect});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _emailCtrl = TextEditingController();
  final _passCtrl = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    FocusScope.of(context).unfocus();
    final ok = await ref.read(authProvider.notifier).login(
          _emailCtrl.text.trim(),
          _passCtrl.text,
        );
    if (ok && mounted) context.go(widget.redirect ?? '/');
  }

  @override
  Widget build(BuildContext context) {
    final auth = ref.watch(authProvider);
    final canPop = GoRouter.of(context).canPop();
    // Always show a close/back button on auth screens so user can exit
    final hasRedirect = widget.redirect != null && widget.redirect!.isNotEmpty;

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
            : hasRedirect
                ? IconButton(
                    icon: const Icon(Icons.close, size: 20, color: AppColors.textMuted),
                    onPressed: () => context.go('/'),
                  )
                : null,
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
              const Text('SIGN IN TO CONTINUE', style: TextStyle(fontFamily: 'DMSans', fontSize: 10, letterSpacing: 0.22, color: AppColors.textMuted2, fontWeight: FontWeight.w500)),
              const SizedBox(height: 48),
              NovaInput(controller: _emailCtrl, hint: 'Email', keyboardType: TextInputType.emailAddress, textInputAction: TextInputAction.next),
              const SizedBox(height: 28),
              NovaInput(
                controller: _passCtrl,
                hint: 'Password',
                obscure: _obscure,
                textInputAction: TextInputAction.done,
                onSubmitted: _submit,
                suffix: IconButton(
                  icon: Icon(_obscure ? Icons.visibility_off_outlined : Icons.visibility_outlined, size: 18, color: AppColors.textDim),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
              // Forgot password link
              Align(
                alignment: Alignment.centerRight,
                child: TextButton(
                  onPressed: () => context.push('/forgot-password'),
                  style: TextButton.styleFrom(padding: const EdgeInsets.symmetric(vertical: 8)),
                  child: const Text('Forgot Password?', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textDim, letterSpacing: 0.08)),
                ),
              ),
              AnimatedSize(
                duration: const Duration(milliseconds: 200),
                child: auth.error != null
                    ? Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Text(auth.error!, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.error)),
                      )
                    : const SizedBox.shrink(),
              ),
              const SizedBox(height: 8),
              NovaPrimaryButton(label: 'Sign In', loading: auth.loading, onPressed: _submit),
              const SizedBox(height: 24),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Text("Don't have an account? ", style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
                  GestureDetector(
                    onTap: () => context.go('/register'),
                    child: const Text('Register', style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary, fontWeight: FontWeight.w600, decoration: TextDecoration.underline, decorationColor: AppColors.textPrimary)),
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
