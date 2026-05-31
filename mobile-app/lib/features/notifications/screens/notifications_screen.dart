import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/network/api_client.dart';
import '../../../core/theme/app_colors.dart';
import '../../../core/utils/format.dart';
import '../../../shared/widgets/nova_app_bar.dart';
import '../providers/notifications_provider.dart';

class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notiAsync = ref.watch(notificationsProvider);
    final dio = ref.read(apiClientProvider).dio;

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: NovaAppBar(
        title: 'Notifications',
        actions: [
          TextButton(
            onPressed: () async {
              await dio.post('/api/notifications/read-all');
              ref.invalidate(notificationsProvider);
              ref.invalidate(notificationCountProvider);
            },
            child: Text('Mark all read', style: TextStyle(fontFamily: 'DMSans', fontSize: 11, color: AppColors.textMuted, letterSpacing: 0.1)),
          ),
        ],
      ),
      body: notiAsync.when(
        data: (items) => items.isEmpty
            ? Center(child: Text('No notifications', style: TextStyle(fontFamily: 'DMSans', color: AppColors.textMuted2)))
            : ListView.separated(
                itemCount: items.length,
                separatorBuilder: (_, _) => const Divider(color: AppColors.borderDeep, height: 1),
                itemBuilder: (_, i) {
                  final n = items[i];
                  return ListTile(
                    tileColor: n.read ? null : AppColors.surface.withAlpha(40),
                    leading: Icon(Icons.notifications_outlined, size: 20, color: n.read ? AppColors.textDim : AppColors.accent),
                    title: Text(n.title, style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textPrimary, fontWeight: n.read ? FontWeight.w400 : FontWeight.w600)),
                    subtitle: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(n.message, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted)),
                        Text(formatDateTime(n.createdAt), style: TextStyle(fontFamily: 'DMSans', fontSize: 10, color: AppColors.textDim)),
                      ],
                    ),
                    isThreeLine: true,
                    onTap: () async {
                      if (!n.read) {
                        await dio.post('/api/notifications/${n.id}/read');
                        ref.invalidate(notificationsProvider);
                        ref.invalidate(notificationCountProvider);
                      }
                    },
                  );
                },
              ),
        loading: () => const Center(child: CircularProgressIndicator(color: AppColors.textMuted, strokeWidth: 2)),
        error: (_, _) => Center(child: Text('Could not load notifications', style: TextStyle(fontFamily: 'DMSans', color: AppColors.error))),
      ),
    );
  }
}
