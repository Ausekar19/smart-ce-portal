package com.fergusson.ceportal.controller;

import com.fergusson.ceportal.model.*;
import com.fergusson.ceportal.repository.TestAttemptRepository;
import com.fergusson.ceportal.service.ExamService;
import com.fergusson.ceportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentController {

    private final ExamService examService;
    private final UserService userService;
    private final TestAttemptRepository attemptRepository;

    @Value("${app.exam.max-tab-switches:3}")
    private int maxTabSwitches;

    private User currentUser(UserDetails ud) {
        return userService.findByUsername(ud.getUsername()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User student = currentUser(ud);
        List<ExamTest>    upcoming = examService.getUpcomingTestsForStudent(student);
        List<TestAttempt> past     = examService.getCompletedAttemptsForStudent(student);
        model.addAttribute("student",       student);
        model.addAttribute("upcomingTests", upcoming);
        model.addAttribute("pastAttempts",  past);
        model.addAttribute("maxSwitches",   maxTabSwitches);
        return "student/dashboard";
    }

    @GetMapping("/exam/{testId}/start")
    public String startExam(@PathVariable Long testId,
                            @AuthenticationPrincipal UserDetails ud,
                            RedirectAttributes ra) {
        User student = currentUser(ud);
        ExamTest test = examService.findTestById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));
        if (examService.hasStudentAttempted(student, test)) {
            ra.addFlashAttribute("error", "You have already attempted this test.");
            return "redirect:/student/dashboard";
        }
        TestAttempt attempt = examService.startAttempt(student, test);
        return "redirect:/student/exam/" + attempt.getId();
    }

    @GetMapping("/exam/{attemptId}")
    public String examPage(@PathVariable Long attemptId,
                           @AuthenticationPrincipal UserDetails ud,
                           Model model) {
        User student = currentUser(ud);
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!attempt.getStudent().getId().equals(student.getId()))
            return "redirect:/student/dashboard";
        if (attempt.isSubmitted())
            return "redirect:/student/result/" + attemptId;
        List<Question> questions = examService.getQuestionsByTest(attempt.getTest());
        model.addAttribute("attempt",     attempt);
        model.addAttribute("test",        attempt.getTest());
        model.addAttribute("questions",   questions);
        model.addAttribute("maxSwitches", maxTabSwitches);
        return "student/exam";
    }

    @PostMapping("/exam/{attemptId}/tabswitch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> recordTabSwitch(
            @PathVariable Long attemptId,
            @AuthenticationPrincipal UserDetails ud) {
        User student = currentUser(ud);
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!attempt.getStudent().getId().equals(student.getId()))
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        int count       = examService.recordTabSwitch(attemptId, maxTabSwitches);
        boolean autoSub = (count >= maxTabSwitches);
        Map<String, Object> resp = new HashMap<>();
        resp.put("switchCount",   count);
        resp.put("maxAllowed",    maxTabSwitches);
        resp.put("autoSubmitted", autoSub);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/exam/{attemptId}/submit")
    public String submitExam(@PathVariable Long attemptId,
                             @RequestParam Map<String, String> allParams,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes ra) {
        User student = currentUser(ud);
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!attempt.getStudent().getId().equals(student.getId()))
            return "redirect:/student/dashboard";
        Map<String, String> answers = new HashMap<>();
        allParams.forEach((key, val) -> {
            if (key.startsWith("q_")) answers.put(key.replace("q_", ""), val);
        });
        examService.submitAttempt(attempt, answers);
        ra.addFlashAttribute("msg", "Exam submitted successfully!");
        return "redirect:/student/result/" + attemptId;
    }

    @GetMapping("/result/{attemptId}")
    public String resultPage(@PathVariable Long attemptId,
                             @AuthenticationPrincipal UserDetails ud,
                             Model model) {
        User student = currentUser(ud);
        TestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));
        if (!attempt.getStudent().getId().equals(student.getId()))
            return "redirect:/student/dashboard";
        model.addAttribute("attempt", attempt);
        model.addAttribute("test",    attempt.getTest());
        model.addAttribute("student", student);
        return "student/result";
    }
}
