package com.skrepta.skreptajava.auth.repository;

import com.skrepta.skreptajava.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address.
     * @param email the email address of the user
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks if a user with the given email already exists.
     * @param email the email address to check
     * @return true if a user with the email exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Finds a user by their password reset token (6-digit code).
     * @param token the reset token
     * @return an Optional containing the User if found, or empty otherwise
     */
    Optional<User> findByResetPasswordToken(String token);

    Optional<User> findByEmailIgnoreCase(String email);
}
