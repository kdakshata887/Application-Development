package com.examly.springapp.service;

import com.examly.springapp.dto.LoginRequest;
import com.examly.springapp.dto.LoginResponse;
import com.examly.springapp.dto.RegisterRequest;
import com.examly.springapp.model.User;

public interface AuthService {
    User register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}
