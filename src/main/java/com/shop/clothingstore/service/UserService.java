package com.shop.clothingstore.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class UserService extends GenericServiceBase<User, Long> {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        super(userRepository);
        this.userRepository = userRepository;
    }

    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFullName(updatedUser.getFullName());
        user.setPhone(updatedUser.getPhone());
        user.setAddress(updatedUser.getAddress());
        user.setRole(updatedUser.getRole());
        return save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new IllegalStateException("Cannot delete admin account");
        }
        delete(id);
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email.toLowerCase().trim());
    }

    public boolean existsByEmail(String email) {
        if (email == null) {
            return false;
        }
        return userRepository.findByEmail(email.toLowerCase().trim()).isPresent();
    }

    @Transactional
    public User registerUser(String email, String encodedPassword, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(encodedPassword);
        user.setRole(role);
        return save(user);
    }
}
