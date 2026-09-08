package com.examly.springapp.service.impl;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.LoginResponse;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.exception.AccountLockedException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidCredentialsException;
import com.examly.springapp.model.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.security.JwtUtil;
import com.examly.springapp.service.AuthService;
import com.examly.springapp.service.ValidationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final ClassSectionRepository classSectionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.security.lockout.stage1-attempts}")
    private int stage1Attempts;
    @Value("${app.security.lockout.stage1-minutes}")
    private int stage1Minutes;
    @Value("${app.security.lockout.stage2-attempts}")
    private int stage2Attempts;
    @Value("${app.security.lockout.stage2-minutes}")
    private int stage2Minutes;
    @Value("${app.security.lockout.stage3-attempts}")
    private int stage3Attempts;
    @Value("${app.security.lockout.stage3-minutes}")
    private int stage3Minutes;

    @Override
    @Transactional
    public User register(RegisterRequest request) {
        ValidationUtil.validateName(request.getName());
        if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank()) {
            ValidationUtil.validatePhone(request.getMobileNumber());
        }

        // Check username uniqueness first — before any DB writes
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }

        // BUG FIX: Perform all role-specific uniqueness checks BEFORE saving the User entity.
        // Previously, User was saved first, creating an orphaned User record if the subsequent
        // uniqueness check failed (even though @Transactional rolls back, it's cleaner this way).
        if (request.getRole() == Role.STUDENT) {
            if (request.getAdmissionNumber() == null || request.getAdmissionNumber().isBlank()) {
                throw new IllegalArgumentException("Admission number is required for student registration");
            }
            if (studentRepository.existsByAdmissionNumber(request.getAdmissionNumber())) {
                throw new DuplicateResourceException("Admission number already exists: " + request.getAdmissionNumber());
            }
        } else if (request.getRole() == Role.TEACHER || request.getRole() == Role.CLASS_TEACHER) {
            if (request.getEmployeeId() == null || request.getEmployeeId().isBlank()) {
                throw new IllegalArgumentException("Employee ID is required for teacher registration");
            }
            if (teacherRepository.existsByEmployeeId(request.getEmployeeId())) {
                throw new DuplicateResourceException("Employee id already exists: " + request.getEmployeeId());
            }
        }

        // All checks passed — now save the User
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .isActive(true)
                .failedAttempts(0)
                .build();
        user = userRepository.save(user);

        // Create the role-specific profile entity
        if (request.getRole() == Role.STUDENT) {
            ClassSection section = null;
            if (request.getSectionId() != null) {
                section = classSectionRepository.findById(request.getSectionId()).orElse(null);
            }
            User parent = null;
            if (request.getParentUserId() != null) {
                parent = userRepository.findById(request.getParentUserId()).orElse(null);
            }
            Student student = Student.builder()
                    .user(user)
                    .admissionNumber(request.getAdmissionNumber())
                    .name(request.getName())
                    .section(section)
                    .parent(parent)
                    .biometricDeviceRef(request.getBiometricDeviceRef())
                    .isActive(true)
                    .build();
            studentRepository.save(student);
        } else if (request.getRole() == Role.TEACHER || request.getRole() == Role.CLASS_TEACHER) {
            Teacher teacher = Teacher.builder()
                    .user(user)
                    .employeeId(request.getEmployeeId())
                    .name(request.getName())
                    .qualification(request.getQualification())
                    .subjectSpecialization(request.getSubjectSpecialization())
                    .biometricDeviceRef(request.getBiometricDeviceRef())
                    .isActive(true)
                    .build();
            teacherRepository.save(teacher);
        }

        return user;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (user.getLockTime() != null && user.getLockTime().isAfter(LocalDateTime.now())) {
            throw new AccountLockedException("Account is locked. Try again after " + user.getLockTime());
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            registerFailedAttempt(user);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new InvalidCredentialsException("Account is inactive. Please contact administrator.");
        }

        user.setFailedAttempts(0);
        user.setLockTime(null);
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getUserId());
        long expiry = jwtUtil.resolveExpiry(user.getRole().name());

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .expiresInMs(expiry)
                .build();
    }

    private void registerFailedAttempt(User user) {
        int attempts = (user.getFailedAttempts() == null ? 0 : user.getFailedAttempts()) + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= stage3Attempts) {
            user.setLockTime(LocalDateTime.now().plusMinutes(stage3Minutes));
        } else if (attempts >= stage2Attempts) {
            user.setLockTime(LocalDateTime.now().plusMinutes(stage2Minutes));
        } else if (attempts >= stage1Attempts) {
            user.setLockTime(LocalDateTime.now().plusMinutes(stage1Minutes));
        }
        userRepository.save(user);
    }
}
