class AppNotification {
  final int id;
  final String title;
  final String message;
  final bool read;
  final String createdAt;

  const AppNotification({
    required this.id,
    required this.title,
    required this.message,
    required this.read,
    required this.createdAt,
  });

  factory AppNotification.fromJson(Map<String, dynamic> j) => AppNotification(
        id: j['id'] as int,
        title: j['title'] as String? ?? '',
        message: j['message'] as String? ?? '',
        read: j['read'] as bool? ?? false,
        createdAt: j['createdAt'] as String? ?? '',
      );
}
