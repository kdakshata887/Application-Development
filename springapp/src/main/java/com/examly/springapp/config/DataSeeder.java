package com.examly.springapp.config;

import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the initial admin account on first startup (idempotent).
 * Runs on every startup but only creates the admin if no ADMIN role user exists yet.
 *
 * Development credentials:
 *   Username : dakshadmin
 *   Password : dakshu12345
 *
 * To use different credentials, set environment variables before starting:
 *   SEED_ADMIN_USERNAME and SEED_ADMIN_PASSWORD
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String username = System.getenv().getOrDefault("SEED_ADMIN_USERNAME", "dakshadmin");
        String password = System.getenv().getOrDefault("SEED_ADMIN_PASSWORD", "dakshu12345");

        // Only create if this exact username does not already exist.
        // Never delete or overwrite existing users — startup must be safe for existing databases.
        if (!userRepository.existsByUsername(username)) {
            User admin = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .role(Role.ADMIN)
                    .email(username + "@edutrack.edu")
                    .mobileNumber("9876543210")
                    .isActive(true)
                    .failedAttempts(0)
                    .build();
            userRepository.save(admin);
            log.info("DataSeeder: admin account '{}' created.", username);
        } else {
            log.debug("DataSeeder: admin account '{}' already exists — skipping.", username);
        }
    }
}
