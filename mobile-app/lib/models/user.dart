class UserProfile {
  final String email;
  final String fullName;
  final String? phone;
  final String? address;
  final String role;
  final String? referralCode;

  const UserProfile({
    required this.email,
    required this.fullName,
    this.phone,
    this.address,
    required this.role,
    this.referralCode,
  });

  factory UserProfile.fromJson(Map<String, dynamic> j) => UserProfile(
        email: j['email'] as String,
        fullName: j['fullName'] as String? ?? '',
        phone: j['phone'] as String?,
        address: j['address'] as String?,
        role: j['role'] as String? ?? 'USER',
        referralCode: j['referralCode'] as String?,
      );
}

class AuthResponse {
  final String token;
  final String email;
  final String role;

  const AuthResponse({required this.token, required this.email, required this.role});

  factory AuthResponse.fromJson(Map<String, dynamic> j) => AuthResponse(
        token: j['token'] as String,
        email: j['email'] as String,
        role: j['role'] as String? ?? 'USER',
      );
}
