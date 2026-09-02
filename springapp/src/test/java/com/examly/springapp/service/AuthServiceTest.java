package com.examly.springapp.service;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.LoginResponse;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.exception.AccountLockedException;
import com.examly.springapp.exception.DuplicateResourceException;
import com.examly.springapp.exception.InvalidCredentialsException;
import com.examly.springapp.model.Role;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.ClassSectionRepository;
import com.examly.springapp.repository.StudentRepository;
import com.examly.springapp.repository.TeacherRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.security.JwtUtil;
import com.examly.springapp.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 * Tests registration, login success/failure, and account lockout policy.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock StudentRepository studentRepository;
    @Mock TeacherRepository teacherRepository;
    @Mock ClassSectionRepository classSectionRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks
    AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // Inject @Value fields
        ReflectionTestUtils.setField(authService, "stage1Attempts", 5);
        ReflectionTestUtils.setField(authService, "stage1Minutes", 15);
        ReflectionTestUtils.setField(authService, "stage2Attempts", 10);
        ReflectionTestUtils.setField(authService, "stage2Minutes", 30);
        ReflectionTestUtils.setField(authService, "stage3Attempts", 15);
        ReflectionTestUtils.setField(authService, "stage3Minutes", 1440);
    }

    // ─── Registration Tests ────────────────────────────────────────────────────

    @Test
    void register_shouldCreateUser_whenUsernameIsUnique() {
        RegisterRequest req = RegisterRequest.builder()
                .username("teacher1").password("Pass@123").name("John Doe")
                .role(Role.TEACHER).employeeId("EMP001").email("john@school.edu").build();

        when(userRepository.existsByUsername("teacher1")).thenReturn(false);
        when(passwordEncoder.encode("Pass@123")).thenReturn("hashed");
        when(teacherRepository.existsByEmployeeId("EMP001")).thenReturn(false);
        User savedUser = User.builder().userId(1L).username("teacher1").role(Role.TEACHER).isActive(true).failedAttempts(0).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = authService.register(req);

        assertThat(result.getUsername()).isEqualTo("teacher1");
        assertThat(result.getRole()).isEqualTo(Role.TEACHER);
        verify(teacherRepository).save(any());
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyExists() {
        RegisterRequest req = RegisterRequest.builder()
                .username("existingUser").password("Pass@123").name("Test User")
                .role(Role.ADMIN).build();

        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already exists");
    }

    // ─── Login Tests ───────────────────────────────────────────────────────────

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        User user = User.builder()
                .userId(1L).username("admin").password("hashed")
                .role(Role.ADMIN).isActive(true).failedAttempts(0).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("admin", "ADMIN", 1L)).thenReturn("test.jwt.token");
        when(jwtUtil.resolveExpiry("ADMIN")).thenReturn(86400000L);

        LoginRequest req = LoginRequest.builder().username("admin").password("Admin@123").build();
        LoginResponse response = authService.login(req);

        assertThat(response.getToken()).isEqualTo("test.jwt.token");
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getUserId()).isEqualTo(1L);
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        LoginRequest req = LoginRequest.builder().username("unknown").password("any").build();

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid username or password");
    }

    @Test
    void login_shouldThrow_whenPasswordIsWrong() {
        User user = User.builder()
                .userId(1L).username("admin").password("hashed")
                .role(Role.ADMIN).isActive(true).failedAttempts(0).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPass", "hashed")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        LoginRequest req = LoginRequest.builder().username("admin").password("WrongPass").build();

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(userRepository).save(argThat(u -> u.getFailedAttempts() == 1));
    }

    @Test
    void login_shouldThrow_whenAccountIsLocked() {
        User user = User.builder()
                .userId(1L).username("admin").password("hashed")
                .role(Role.ADMIN).isActive(true).failedAttempts(15)
                .lockTime(LocalDateTime.now().plusMinutes(30)).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        LoginRequest req = LoginRequest.builder().username("admin").password("Admin@123").build();

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("locked");
    }

    // ─── Lockout Policy Tests ──────────────────────────────────────────────────

    @Test
    void login_shouldLockAccountAtStage1_afterFiveFailures() {
        User user = User.builder()
                .userId(1L).username("teacher").password("hashed")
                .role(Role.TEACHER).isActive(true).failedAttempts(4).build();

        when(userRepository.findByUsername("teacher")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);

        LoginRequest req = LoginRequest.builder().username("teacher").password("wrong").build();
        assertThatThrownBy(() -> authService.login(req)).isInstanceOf(InvalidCredentialsException.class);

        verify(userRepository).save(argThat(u ->
                u.getFailedAttempts() == 5 && u.getLockTime() != null &&
                u.getLockTime().isAfter(LocalDateTime.now().plusMinutes(14))
        ));
    }

    @Test
    void login_shouldResetFailedAttempts_onSuccessfulLogin() {
        User user = User.builder()
                .userId(1L).username("admin").password("hashed")
                .role(Role.ADMIN).isActive(true).failedAttempts(3).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@123", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any(), any())).thenReturn("token");
        when(jwtUtil.resolveExpiry(any())).thenReturn(86400000L);
        when(userRepository.save(any())).thenReturn(user);

        LoginRequest req = LoginRequest.builder().username("admin").password("Admin@123").build();
        authService.login(req);

        verify(userRepository).save(argThat(u -> u.getFailedAttempts() == 0 && u.getLockTime() == null));
    }

    @Test
    void login_shouldThrow_whenAccountIsInactive() {
        User user = User.builder()
                .userId(1L).username("admin").password("hashed")
                .role(Role.ADMIN).isActive(false).failedAttempts(0).build();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Admin@123", "hashed")).thenReturn(true);

        LoginRequest req = LoginRequest.builder().username("admin").password("Admin@123").build();

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("inactive");
    }
}
