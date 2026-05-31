import 'package:intl/intl.dart';

final _vnd = NumberFormat.currency(locale: 'vi_VN', symbol: '₫', decimalDigits: 0);
final _date = DateFormat('dd/MM/yyyy');
final _dateTime = DateFormat('dd/MM/yyyy HH:mm');

String formatPrice(num? price) {
  if (price == null) return '₫0';
  return _vnd.format(price);
}

String formatDate(String? iso) {
  if (iso == null || iso.isEmpty) return '';
  try {
    return _date.format(DateTime.parse(iso).toLocal());
  } catch (_) {
    return iso;
  }
}

String formatDateTime(String? iso) {
  if (iso == null || iso.isEmpty) return '';
  try {
    return _dateTime.format(DateTime.parse(iso).toLocal());
  } catch (_) {
    return iso;
  }
}
