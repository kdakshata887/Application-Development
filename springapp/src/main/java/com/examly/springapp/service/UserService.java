package com.examly.springapp.service;

import com.examly.springapp.model.User;

import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Long id);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    User activateOrDeactivate(Long id, boolean active);
}
