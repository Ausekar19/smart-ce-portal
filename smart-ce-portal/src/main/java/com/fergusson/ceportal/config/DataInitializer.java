package com.fergusson.ceportal.config;

import com.fergusson.ceportal.model.User;
import com.fergusson.ceportal.repository.UserRepository;
import com.fergusson.ceportal.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(String... args) {
        // Create default admin if none exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setFullName("System Administrator");
            admin.setEmail("admin@fergusson.edu.in");
            admin.setRole(User.Role.ADMIN);
            admin.setPassword("admin123"); // plain – gets encoded by UserService
            userService.registerUser(admin);
            log.info("✅  Default admin created → username: admin / password: admin123");
        }
    }
}
