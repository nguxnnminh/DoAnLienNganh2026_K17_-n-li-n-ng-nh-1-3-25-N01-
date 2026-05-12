package com.shop.clothingstore.service;

import java.util.Optional;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.event.UserRegisteredEvent;
import com.shop.clothingstore.repository.UserRepository;
import com.shop.clothingstore.service.base.GenericServiceBase;

@Service
public class UserService extends GenericServiceBase<User, Long> {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserService(UserRepository userRepository,
                       ApplicationEventPublisher eventPublisher) {
        super(userRepository);
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
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
    public User createAdminManagedUser(
            String email,
            String encodedPassword,
            Role role,
            String fullName,
            String phone,
            String address) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        String normalizedEmail = email.toLowerCase().trim();
        if (existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(encodedPassword);
        user.setRole(role != null ? role : Role.USER);
        user.setFullName(fullName != null ? fullName.trim() : null);
        user.setPhone(phone != null ? phone.trim() : null);
        user.setAddress(address != null ? address.trim() : null);

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

    /**
     * Admin paginated user list with optional keyword + role filter.
     * Runs DB-level search — never loads all users into memory.
     */
    public Page<User> searchUsers(String keyword, String roleStr, Pageable pageable) {
        Role role = null;
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                role = Role.valueOf(roleStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        String kw = (keyword != null && keyword.isBlank()) ? null : keyword;
        return userRepository.searchAdmin(kw, role, pageable);
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
        user = save(user);

        // Publish event — listeners (e.g., welcome coupon) react asynchronously
        eventPublisher.publishEvent(new UserRegisteredEvent(this, user));

        return user;
    }
}
