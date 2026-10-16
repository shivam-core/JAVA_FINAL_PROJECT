package com.shivamcore.loanapp.dao;

import com.shivamcore.loanapp.model.User;

import java.util.Optional;

/**
 * Data Access Object interface for User entities.
 */
public interface UserDao {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByIdentifier(String identifier);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByRole(com.shivamcore.loanapp.model.Role role);

    int incrementFailedAttempts(Long userId);

    void lockAccount(Long userId);

    void resetFailedAttempts(Long userId);

    void updatePassword(Long userId, String newPasswordHash);

    void update(User user);

    void deleteById(Long id);

    java.util.List<User> search(String field, String query);

    java.util.List<User> findAll();
}
