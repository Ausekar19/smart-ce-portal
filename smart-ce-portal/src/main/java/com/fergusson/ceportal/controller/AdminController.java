package com.fergusson.ceportal.controller;

import com.fergusson.ceportal.model.*;
import com.fergusson.ceportal.service.ExamService;
import com.fergusson.ceportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final ExamService examService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<User> students  = userService.getUsersByRole(User.Role.STUDENT);
        List<User> teachers  = userService.getUsersByRole(User.Role.TEACHER);
        List<ExamTest> tests = examService.getAllTests();
        model.addAttribute("studentCount", students.size());
        model.addAttribute("teacherCount", teachers.size());
        model.addAttribute("testCount",    tests.size());
        model.addAttribute("students",     students);
        model.addAttribute("teachers",     teachers);
        model.addAttribute("tests",        tests);
        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.toggleUserEnabled(id);
        ra.addFlashAttribute("msg", "User status updated.");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes ra) {
        userService.deleteUser(id);
        ra.addFlashAttribute("msg", "User deleted.");
        return "redirect:/admin/users";
    }

    // Admin can register a teacher directly
    @GetMapping("/teacher/new")
    public String newTeacherForm(Model model) {
        model.addAttribute("user", new User());
        return "admin/new-teacher";
    }

    @PostMapping("/teacher/new")
    public String saveTeacher(@ModelAttribute User user, RedirectAttributes ra) {
        if (userService.usernameExists(user.getUsername())) {
            ra.addFlashAttribute("error", "Username already exists.");
            return "redirect:/admin/teacher/new";
        }
        user.setRole(User.Role.TEACHER);
        userService.registerUser(user);
        ra.addFlashAttribute("msg", "Teacher account created successfully.");
        return "redirect:/admin/dashboard";
    }
}
