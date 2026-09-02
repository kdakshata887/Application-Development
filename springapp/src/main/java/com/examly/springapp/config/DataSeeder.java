package com.examly.springapp.config;

import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default admin account on first startup so the API can be
 * exercised immediately after cloning the project.
 * Username: admin | Password: Admin@123
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("Admin@123"))
                    .role(Role.ADMIN)
                    .email("admin@edutrack.edu")
                    .mobileNumber("9876543210")
                    .isActive(true)
                    .failedAttempts(0)
                    .build();
            userRepository.save(admin);
        }
    }
}
