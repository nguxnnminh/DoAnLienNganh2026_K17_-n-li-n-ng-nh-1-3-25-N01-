package com.shop.clothingstore.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Chỉ cập nhật các trường cho phép
        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());
        user.setRole(updatedUser.getRole());

        // Nếu cần thêm active/inactive, thêm trường active vào User entity
        // user.setActive(updatedUser.getActive());

        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // Không cho xóa admin hoặc chính mình (tùy chọn)
        if (user.getRole().equals("ADMIN")) {
            throw new IllegalStateException("Không thể xóa tài khoản admin");
        }

        userRepository.delete(user);
    }
}