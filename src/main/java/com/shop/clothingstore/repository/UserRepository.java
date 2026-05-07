package com.shop.clothingstore.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shop.clothingstore.entity.Role;
import com.shop.clothingstore.entity.User;
import com.shop.clothingstore.repository.base.BaseRepository;

public interface UserRepository extends BaseRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Admin paginated list (all users)
    Page<User> findAllByOrderByIdDesc(Pageable pageable);

    // Admin search: email, fullName, phone — combined with optional role filter
    @Query("""
        SELECT u FROM User u
        WHERE (:keyword IS NULL OR :keyword = ''
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR u.phone           LIKE CONCAT('%', :keyword, '%'))
          AND (:role IS NULL OR u.role = :role)
        ORDER BY u.id DESC
    """)
    Page<User> searchAdmin(
            @Param("keyword") String keyword,
            @Param("role")    Role role,
            Pageable pageable
    );
}