import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../../../shared/widgets/nova_button.dart';
import '../../../shared/widgets/nova_input.dart';
import '../../../shared/widgets/shimmer_box.dart';
import '../../auth/providers/auth_provider.dart';
import '../providers/profile_provider.dart';

class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authProvider);

    if (!auth.isLoggedIn) {
      return Scaffold(
        backgroundColor: AppColors.background,
        appBar: const NovaAppBar(title: 'Account', showBack: false),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 48),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.person_outline, size: 56, color: AppColors.textDim),
                const SizedBox(height: 16),
                Text('Sign in to view your account', style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textMuted2), textAlign: TextAlign.center),
                const SizedBox(height: 24),
                NovaPrimaryButton(label: 'Sign In', onPressed: () => context.go('/login')),
                const SizedBox(height: 12),
                NovaOutlineButton(label: 'Create Account', onPressed: () => context.go('/register')),
              ],
            ),
          ),
        ),
      );
    }

    final profileAsync = ref.watch(profileProvider);

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: NovaAppBar(
        title: 'Account',
        showBack: false,
        actions: [
          IconButton(
            icon: const Icon(Icons.logout, size: 20, color: AppColors.textMuted),
            onPressed: () {
              ref.read(authProvider.notifier).logout();
              context.go('/');
            },
          ),
        ],
      ),
      body: profileAsync.when(
        data: (profile) => ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Center(
              child: Container(
                width: 72, height: 72,
                color: AppColors.surface,
                child: Center(
                  child: Text(
                    profile.fullName.isNotEmpty ? profile.fullName[0].toUpperCase() : '?',
                    style: TextStyle(fontFamily: 'BebasNeue', fontSize: 32, color: AppColors.textMuted),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            Center(child: Text(profile.fullName, style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, letterSpacing: 0.08, color: AppColors.textPrimary))),
            Center(child: Text(profile.email, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2))),
            if (profile.referralCode != null) ...[
              const SizedBox(height: 8),
              Center(
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 5),
                  decoration: BoxDecoration(border: Border.all(color: AppColors.accentBorder), color: AppColors.accentSurface),
                  child: Text('Referral Code: ${profile.referralCode}', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.accentText, letterSpacing: 0.08)),
                ),
              ),
            ],
            const SizedBox(height: 32),

            _MenuItem(icon: Icons.receipt_long_outlined, label: 'My Orders', onTap: () => context.push('/orders')),
            _MenuItem(icon: Icons.local_offer_outlined, label: 'My Coupons', onTap: () => context.push('/coupons')),
            _MenuItem(icon: Icons.favorite_border, label: 'Wishlist', onTap: () => context.push('/wishlist')),
            _MenuItem(icon: Icons.notifications_outlined, label: 'Notifications', onTap: () => context.push('/notifications')),
            _MenuItem(icon: Icons.edit_outlined, label: 'Edit Profile', onTap: () => _showEditProfile(context, profile.fullName, profile.phone ?? '', profile.address ?? '')),
            _MenuItem(icon: Icons.lock_outline, label: 'Change Password', onTap: () => _showChangePassword(context)),
            const Divider(color: AppColors.borderDark, height: 24),
            _MenuItem(icon: Icons.straighten_outlined, label: 'Sizing Guide', onTap: () => context.push('/sizing')),
            _MenuItem(icon: Icons.mail_outline, label: 'Contact', onTap: () => context.push('/contact')),
            _MenuItem(icon: Icons.autorenew_outlined, label: 'Returns', onTap: () => context.push('/returns')),
            const Divider(color: AppColors.borderDark, height: 32),
            _MenuItem(
              icon: Icons.logout,
              label: 'Sign Out',
              color: AppColors.error,
              onTap: () {
                ref.read(authProvider.notifier).logout();
                context.go('/');
              },
            ),
          ],
        ),
        loading: () => ListView(
          padding: const EdgeInsets.all(20),
          children: const [ShimmerBox(height: 72), SizedBox(height: 12), ShimmerBox(height: 20, width: 120)],
        ),
        error: (_, _) => Center(child: Text('Error loading profile', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
      ),
    );
  }

  void _showEditProfile(BuildContext context, String name, String phone, String address) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.backgroundDark,
      builder: (_) => _EditProfileSheet(name: name, phone: phone, address: address),
    );
  }

  void _showChangePassword(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppColors.backgroundDark,
      builder: (_) => const _ChangePasswordSheet(),
    );
  }
}

class _MenuItem extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final Color? color;
  const _MenuItem({required this.icon, required this.label, required this.onTap, this.color});

  @override
  Widget build(BuildContext context) => ListTile(
        contentPadding: const EdgeInsets.symmetric(vertical: 4),
        leading: Icon(icon, size: 20, color: color ?? AppColors.textMuted),
        title: Text(label, style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: color ?? AppColors.textPrimary)),
        trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: AppColors.textDim),
        onTap: onTap,
      );
}

class _EditProfileSheet extends ConsumerStatefulWidget {
  final String name;
  final String phone;
  final String address;
  const _EditProfileSheet({required this.name, required this.phone, required this.address});

  @override
  ConsumerState<_EditProfileSheet> createState() => _EditProfileSheetState();
}

class _EditProfileSheetState extends ConsumerState<_EditProfileSheet> {
  late final _nameCtrl = TextEditingController(text: widget.name);
  late final _phoneCtrl = TextEditingController(text: widget.phone);
  late final _addressCtrl = TextEditingController(text: widget.address);

  @override
  void dispose() {
    _nameCtrl.dispose();
    _phoneCtrl.dispose();
    _addressCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(profileUpdateProvider);
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, MediaQuery.of(context).viewInsets.bottom + 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('EDIT PROFILE', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, letterSpacing: 0.1, color: AppColors.white)),
          const SizedBox(height: 20),
          NovaInput(controller: _nameCtrl, hint: 'Full Name', textInputAction: TextInputAction.next),
          const SizedBox(height: 16),
          NovaInput(controller: _phoneCtrl, hint: 'Phone Number', keyboardType: TextInputType.phone, textInputAction: TextInputAction.next),
          const SizedBox(height: 16),
          NovaInput(controller: _addressCtrl, hint: 'Address', textInputAction: TextInputAction.done),
          const SizedBox(height: 24),
          NovaPrimaryButton(
            label: 'Save Changes',
            loading: state is AsyncLoading,
            onPressed: () async {
              final ok = await ref.read(profileUpdateProvider.notifier).updateProfile(
                fullName: _nameCtrl.text.trim(),
                phone: _phoneCtrl.text.trim(),
                address: _addressCtrl.text.trim(),
              );
              if (ok && context.mounted) Navigator.pop(context);
            },
          ),
        ],
      ),
    );
  }
}

class _ChangePasswordSheet extends ConsumerStatefulWidget {
  const _ChangePasswordSheet();

  @override
  ConsumerState<_ChangePasswordSheet> createState() => _ChangePasswordSheetState();
}

class _ChangePasswordSheetState extends ConsumerState<_ChangePasswordSheet> {
  final _oldCtrl = TextEditingController();
  final _newCtrl = TextEditingController();
  final _confirmCtrl = TextEditingController();

  @override
  void dispose() {
    _oldCtrl.dispose();
    _newCtrl.dispose();
    _confirmCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(profileUpdateProvider);
    return Padding(
      padding: EdgeInsets.fromLTRB(24, 24, 24, MediaQuery.of(context).viewInsets.bottom + 24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('CHANGE PASSWORD', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 22, letterSpacing: 0.1, color: AppColors.white)),
          const SizedBox(height: 20),
          NovaInput(controller: _oldCtrl, hint: 'Current Password', obscure: true, textInputAction: TextInputAction.next),
          const SizedBox(height: 16),
          NovaInput(controller: _newCtrl, hint: 'New Password', obscure: true, textInputAction: TextInputAction.next),
          const SizedBox(height: 16),
          NovaInput(controller: _confirmCtrl, hint: 'Confirm New Password', obscure: true, textInputAction: TextInputAction.done),
          const SizedBox(height: 24),
          NovaPrimaryButton(
            label: 'Change Password',
            loading: state is AsyncLoading,
            onPressed: () async {
              final ok = await ref.read(profileUpdateProvider.notifier).changePassword(
                oldPassword: _oldCtrl.text,
                newPassword: _newCtrl.text,
                confirmPassword: _confirmCtrl.text,
              );
              if (ok && context.mounted) {
                Navigator.pop(context);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(content: Text('Password changed successfully', style: TextStyle(fontFamily: 'DMSans', fontSize: 12))),
                );
              }
            },
          ),
        ],
      ),
    );
  }
}
