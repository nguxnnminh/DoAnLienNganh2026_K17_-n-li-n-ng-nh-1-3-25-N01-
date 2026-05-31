import 'package:flutter/material.dart';
import '../../../core/theme/app_colors.dart';
import '../../../shared/widgets/nova_app_bar.dart';

// Generic info screen used for Sizing, Contact, Returns

class SizingScreen extends StatelessWidget {
  const SizingScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'Sizing Guide'),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Sizing', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 40, letterSpacing: 0.06, color: AppColors.textPrimary)),
            const SizedBox(height: 8),
            const Text('Our pieces are designed with a relaxed, minimal fit.', style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textMuted, height: 1.7)),
            const SizedBox(height: 32),

            _SectionLabel('Size Chart'),
            const SizedBox(height: 12),
            _SizeTable(),

            const SizedBox(height: 28),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(border: Border.all(color: AppColors.accentBorder), color: AppColors.accentSurface),
              child: const Row(
                children: [
                  Icon(Icons.info_outline, size: 18, color: AppColors.accent),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text("Between sizes? We recommend sizing up for a relaxed fit.", style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.accentText, height: 1.5)),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}

class _SizeTable extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    const headers = ['Size', 'Chest', 'Waist', 'Best For'];
    const rows = [
      ['S', '34–36"', '28–30"', 'Slim fit'],
      ['M', '36–38"', '30–32"', 'Regular fit'],
      ['L', '38–40"', '32–34"', 'Relaxed fit'],
      ['XL', '40–42"', '34–36"', 'Oversized fit'],
    ];

    return Container(
      decoration: BoxDecoration(border: Border.all(color: AppColors.borderDark)),
      child: Column(
        children: [
          // Header
          Container(
            color: AppColors.surface,
            child: Row(
              children: headers.map((h) => Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 12),
                  child: Text(h.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.3, color: AppColors.textMuted2, fontWeight: FontWeight.w600)),
                ),
              )).toList(),
            ),
          ),
          // Rows
          ...rows.map((row) => Container(
            decoration: const BoxDecoration(border: Border(top: BorderSide(color: AppColors.borderDark))),
            child: Row(
              children: row.asMap().entries.map((e) => Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
                  child: Text(e.value, style: TextStyle(fontFamily: 'DMSans', fontSize: 12, color: e.key == 0 ? AppColors.textPrimary : AppColors.textMuted, fontWeight: e.key == 0 ? FontWeight.w600 : FontWeight.w400)),
                ),
              )).toList(),
            ),
          )),
        ],
      ),
    );
  }
}

class ContactScreen extends StatelessWidget {
  const ContactScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'Contact'),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Contact Nova', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 36, letterSpacing: 0.06, color: AppColors.textPrimary)),
            const SizedBox(height: 24),

            _InfoCard(icon: Icons.receipt_long_outlined, title: 'Orders', body: 'Questions about your order? Have your order number ready when reaching out.'),
            const SizedBox(height: 12),
            _InfoCard(icon: Icons.straighten_outlined, title: 'Fit Help', body: "Not sure about sizing? Check our size guide or reach out — we'll help you find the right fit."),
            const SizedBox(height: 12),
            _InfoCard(icon: Icons.autorenew_outlined, title: 'Returns', body: 'Want to return an item? Contact us within 14 days of delivery with your order ID.'),

            const SizedBox(height: 32),
            const _SectionLabel('Customer Care'),
            const SizedBox(height: 16),

            _DetailRow(icon: Icons.email_outlined, label: 'Email', value: 'support@nova.store'),
            const Divider(color: AppColors.borderDark),
            _DetailRow(icon: Icons.access_time_outlined, label: 'Hours', value: 'Mon–Fri, 9am–6pm ICT'),
            const Divider(color: AppColors.borderDark),
            _DetailRow(icon: Icons.info_outline, label: 'Note', value: 'Include order ID & phone for faster support'),

            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}

class ReturnsScreen extends StatelessWidget {
  const ReturnsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: const NovaAppBar(title: 'Returns'),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Returns', style: TextStyle(fontFamily: 'BebasNeue', fontSize: 40, letterSpacing: 0.06, color: AppColors.textPrimary)),
            const SizedBox(height: 8),
            const Text('We stand behind our products. If something is wrong, we make it right.', style: TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textMuted, height: 1.7)),
            const SizedBox(height: 32),

            _InfoCard(icon: Icons.calendar_today_outlined, title: '14-Day Return Window', body: 'Items can be returned within 14 days of delivery. Items must be unworn and in original condition.'),
            const SizedBox(height: 12),
            _InfoCard(icon: Icons.verified_outlined, title: 'Fit Confidence', body: "Use our size guide to find your perfect fit. If it's still not right, we'll sort it out."),
            const SizedBox(height: 12),
            _InfoCard(icon: Icons.support_agent_outlined, title: 'How to Start', body: 'Contact support with your order ID and reason. We\'ll provide return instructions within 1 business day.'),

            const SizedBox(height: 40),
          ],
        ),
      ),
    );
  }
}

// ── Shared widgets ────────────────────────────────────────────────────────────

class _SectionLabel extends StatelessWidget {
  final String text;
  const _SectionLabel(this.text);
  @override
  Widget build(BuildContext context) => Text(text.toUpperCase(), style: const TextStyle(fontFamily: 'DMSans', fontSize: 9, letterSpacing: 0.4, color: AppColors.textMuted2, fontWeight: FontWeight.w600));
}

class _InfoCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String body;
  const _InfoCard({required this.icon, required this.title, required this.body});

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(color: AppColors.backgroundDark, border: Border.all(color: AppColors.borderDark)),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, size: 20, color: AppColors.textMuted2),
            const SizedBox(width: 14),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: const TextStyle(fontFamily: 'DMSans', fontSize: 13, color: AppColors.textPrimary, fontWeight: FontWeight.w600)),
                  const SizedBox(height: 4),
                  Text(body, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted, height: 1.5)),
                ],
              ),
            ),
          ],
        ),
      );
}

class _DetailRow extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;
  const _DetailRow({required this.icon, required this.label, required this.value});

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Row(
          children: [
            Icon(icon, size: 18, color: AppColors.textDim),
            const SizedBox(width: 12),
            Text(label, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textMuted2)),
            const Spacer(),
            Text(value, style: const TextStyle(fontFamily: 'DMSans', fontSize: 12, color: AppColors.textPrimary)),
          ],
        ),
      );
}
