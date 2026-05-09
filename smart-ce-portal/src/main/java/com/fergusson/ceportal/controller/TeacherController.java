package com.fergusson.ceportal.controller;

import com.fergusson.ceportal.model.*;
import com.fergusson.ceportal.service.ExamService;
import com.fergusson.ceportal.service.ExcelExportService;
import com.fergusson.ceportal.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final ExamService examService;
    private final UserService userService;
    private final ExcelExportService excelExportService;

    private User currentUser(UserDetails ud) {
        return userService.findByUsername(ud.getUsername()).orElseThrow();
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User teacher = currentUser(ud);
        List<ExamTest> myTests = examService.getTestsByTeacher(teacher);
        long upcoming  = myTests.stream().filter(t -> t.getScheduledDateTime().isAfter(LocalDateTime.now())).count();
        long completed = myTests.stream().filter(t -> t.getScheduledDateTime().isBefore(LocalDateTime.now())).count();
        model.addAttribute("teacher",   teacher);
        model.addAttribute("tests",     myTests);
        model.addAttribute("upcoming",  upcoming);
        model.addAttribute("completed", completed);
        model.addAttribute("total",     myTests.size());
        return "teacher/dashboard";
    }

    // ─── Create Test ──────────────────────────────────────────────────────────

    @GetMapping("/test/new")
    public String newTestForm(Model model) {
        model.addAttribute("test", new ExamTest());
        return "teacher/create-test";
    }

    @PostMapping("/test/new")
    public String saveNewTest(@ModelAttribute ExamTest test,
                              @RequestParam String scheduledDateTimeStr,
                              @AuthenticationPrincipal UserDetails ud,
                              RedirectAttributes ra) {
        User teacher = currentUser(ud);
        test.setCreatedBy(teacher);
        test.setStatus(ExamTest.TestStatus.SCHEDULED);
        test.setScheduledDateTime(LocalDateTime.parse(scheduledDateTimeStr));
        ExamTest saved = examService.saveTest(test);
        ra.addFlashAttribute("msg", "Test created! Now add questions.");
        return "redirect:/teacher/test/" + saved.getId() + "/questions";
    }

    // ─── Edit Test ────────────────────────────────────────────────────────────

    @GetMapping("/test/{id}/edit")
    public String editTestForm(@PathVariable Long id, Model model) {
        ExamTest test = examService.findTestById(id)
                .orElseThrow(() -> new RuntimeException("Test not found"));
        model.addAttribute("test", test);
        return "teacher/create-test";
    }

    @PostMapping("/test/{id}/edit")
    public String updateTest(@PathVariable Long id,
                             @ModelAttribute ExamTest form,
                             @RequestParam String scheduledDateTimeStr,
                             @AuthenticationPrincipal UserDetails ud,
                             RedirectAttributes ra) {
        ExamTest test = examService.findTestById(id).orElseThrow();
        test.setTitle(form.getTitle());
        test.setSubject(form.getSubject());
        test.setDurationMinutes(form.getDurationMinutes());
        test.setTotalMarks(form.getTotalMarks());
        test.setNegativeMarking(form.getNegativeMarking());
        test.setScheduledDateTime(LocalDateTime.parse(scheduledDateTimeStr));
        test.setStatus(form.getStatus());
        examService.saveTest(test);
        ra.addFlashAttribute("msg", "Test updated successfully.");
        return "redirect:/teacher/dashboard";
    }

    @PostMapping("/test/{id}/delete")
    public String deleteTest(@PathVariable Long id, RedirectAttributes ra) {
        examService.deleteTest(id);
        ra.addFlashAttribute("msg", "Test deleted.");
        return "redirect:/teacher/dashboard";
    }

    // ─── Questions ────────────────────────────────────────────────────────────

    @GetMapping("/test/{testId}/questions")
    public String questionsPage(@PathVariable Long testId, Model model) {
        ExamTest test = examService.findTestById(testId).orElseThrow();
        List<Question> questions = examService.getQuestionsByTest(test);
        model.addAttribute("test",      test);
        model.addAttribute("questions", questions);
        model.addAttribute("newQ",      new Question());
        return "teacher/questions";
    }

    @PostMapping("/test/{testId}/questions/add")
    public String addQuestion(@PathVariable Long testId,
                              @ModelAttribute Question question,
                              RedirectAttributes ra) {
        ExamTest test = examService.findTestById(testId).orElseThrow();
        question.setTest(test);
        examService.saveQuestion(question);
        ra.addFlashAttribute("msg", "Question added.");
        return "redirect:/teacher/test/" + testId + "/questions";
    }

    @PostMapping("/test/{testId}/questions/{qId}/delete")
    public String deleteQuestion(@PathVariable Long testId,
                                  @PathVariable Long qId,
                                  RedirectAttributes ra) {
        examService.deleteQuestion(qId);
        ra.addFlashAttribute("msg", "Question deleted.");
        return "redirect:/teacher/test/" + testId + "/questions";
    }

    // ─── Results / Reports ────────────────────────────────────────────────────

    @GetMapping("/test/{testId}/results")
    public String viewResults(@PathVariable Long testId, Model model) {
        ExamTest test = examService.findTestById(testId).orElseThrow();
        List<TestAttempt> attempts = examService.getAttemptsForTest(test);
        model.addAttribute("test",     test);
        model.addAttribute("attempts", attempts);
        return "teacher/results";
    }

    @GetMapping("/test/{testId}/results/export")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long testId) throws IOException {
        ExamTest test = examService.findTestById(testId).orElseThrow();
        byte[] data = excelExportService.exportTestReport(test);
        String filename = "results_" + test.getSubject().replaceAll("\\s+", "_") + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
