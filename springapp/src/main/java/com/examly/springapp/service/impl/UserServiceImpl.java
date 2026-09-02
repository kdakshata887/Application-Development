package com.examly.springapp.service.impl;

import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Override
    public User updateUser(Long id, User update) {
        User user = getUserById(id);
        if (update.getEmail() != null) user.setEmail(update.getEmail());
        if (update.getMobileNumber() != null) user.setMobileNumber(update.getMobileNumber());
        if (update.getIsActive() != null) user.setIsActive(update.getIsActive());
        return userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = getUserById(id);
        userRepository.delete(user);
    }

    @Override
    public User activateOrDeactivate(Long id, boolean active) {
        User user = getUserById(id);
        user.setIsActive(active);
        return userRepository.save(user);
    }
}
