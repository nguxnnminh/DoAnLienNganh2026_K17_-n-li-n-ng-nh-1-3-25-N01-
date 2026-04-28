package com.shop.clothingstore.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

import jakarta.transaction.Transactional;

@Service
public class UserService
        extends GenericServiceBase<User, Long> {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {

        super(userRepository);   // 🔥 QUAN TRỌNG

        this.userRepository = userRepository;
    }

    // =====================================================
    // UPDATE USER
    // =====================================================
    @Transactional
    public User updateUser(Long id, User updatedUser) {

        User user = findById(id) // 🔥 dùng generic
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());
        user.setRole(updatedUser.getRole());

        return save(user);   // 🔥 dùng generic
    }

    // =====================================================
    // DELETE USER (CUSTOM LOGIC)
    // =====================================================
    @Transactional
    public void deleteUser(Long id) {

        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Không thể xóa tài khoản admin");
        }

        delete(id);   // 🔥 dùng generic
    }

    // =====================================================
    // FIND BY EMAIL
    // =====================================================
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    // =====================================================
    // CHECK EMAIL EXISTS
    // =====================================================
    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return userRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    // =====================================================
    // REGISTER USER
    // =====================================================
    @Transactional
    public User registerUser(String email,
            String encodedPassword,
            Role role) {

        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(role);

        return save(user);   // 🔥 dùng generic
    }
}
