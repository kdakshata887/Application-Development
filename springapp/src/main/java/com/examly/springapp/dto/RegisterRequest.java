package com.examly.springapp.dto;

import com.examly.springapp.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisterRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Name is required")
    private String name;

    private String email;

    private String mobileNumber;

    // Student-specific
    private String admissionNumber;
    private Long sectionId;
    private Long parentUserId;

    // Teacher-specific
    private String employeeId;
    private String qualification;
    private String subjectSpecialization;

    private String biometricDeviceRef;
}
