package com.shop.clothingstore.dto.api;

import com.shop.clothingstore.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileResponse {

    private String email;
    private String fullName;
    private String phone;
    private String address;
    private String role;
    private String referralCode;

    public static ProfileResponse from(User user) {
        return new ProfileResponse(
                user.getEmail(),
                user.getFullName(),
                user.getPhone(),
                user.getAddress(),
                user.getRole().name(),
                user.getReferralCode()
        );
    }
}
