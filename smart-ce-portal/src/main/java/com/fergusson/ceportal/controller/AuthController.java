package com.fergusson.ceportal.controller;

import com.fergusson.ceportal.model.User;
import com.fergusson.ceportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null)  model.addAttribute("error", "Invalid username or password.");
        if (logout != null) model.addAttribute("msg", "You have been logged out successfully.");
        return "login";
    }

    // ── Student Registration ──────────────────────────────────────────────────

    @GetMapping("/register/student")
    public String studentRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register-student";
    }

    @PostMapping("/register/student")
    public String registerStudent(@ModelAttribute User user,
                                  RedirectAttributes ra) {
        if (userService.usernameExists(user.getUsername())) {
            ra.addFlashAttribute("error", "PRN already registered.");
            return "redirect:/register/student";
        }
        user.setRole(User.Role.STUDENT);
        userService.registerUser(user);
        ra.addFlashAttribute("msg", "Registration successful! Please login.");
        return "redirect:/login";
    }

    // ── Teacher Registration ──────────────────────────────────────────────────

    @GetMapping("/register/teacher")
    public String teacherRegisterPage(Model model) {
        model.addAttribute("user", new User());
        return "register-teacher";
    }

    @PostMapping("/register/teacher")
    public String registerTeacher(@ModelAttribute User user,
                                   RedirectAttributes ra) {
        if (userService.usernameExists(user.getUsername())) {
            ra.addFlashAttribute("error", "Employee ID already registered.");
            return "redirect:/register/teacher";
        }
        user.setRole(User.Role.TEACHER);
        userService.registerUser(user);
        ra.addFlashAttribute("msg", "Teacher registered successfully! Please login.");
        return "redirect:/login";
    }
}
