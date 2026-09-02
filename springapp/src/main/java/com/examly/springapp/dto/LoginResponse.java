package com.examly.springapp.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private Long userId;
    private String username;
    private String role;
    private long expiresInMs;
}
