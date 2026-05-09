package com.fergusson.ceportal.service;

import com.fergusson.ceportal.model.User;
import com.fergusson.ceportal.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public boolean usernameExists(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public void toggleUserEnabled(Long userId) {
        userRepository.findById(userId).ifPresent(u -> {
            u.setEnabled(!u.isEnabled());
            userRepository.save(u);
        });
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
