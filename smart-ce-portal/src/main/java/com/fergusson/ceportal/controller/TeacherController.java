package com.fergusson.ceportal.controller;

import com.fergusson.ceportal.model.*;
import com.fergusson.ceportal.service.ExamService;
import com.fergusson.ceportal.service.ExcelExportService;
import com.fergusson.ceportal.service.UserService;
import com.fergusson.ceportal.repository.QuestionRepository;
import com.fergusson.ceportal.model.QuestionOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.multipart.MultipartFile;


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
    private final QuestionRepository questionRepository;

    private User currentUser(UserDetails ud) {
        return userService.findByUsername(ud.getUsername()).orElseThrow();
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails ud, Model model) {
        User teacher = currentUser(ud);
        List<ExamTest> myTests = examService.getTestsByTeacher(teacher);
        long upcoming  = myTests.stream()
                .filter(t -> t.getScheduledDateTime().isAfter(LocalDateTime.now())).count();
        long completed = myTests.stream()
                .filter(t -> t.getScheduledDateTime().isBefore(LocalDateTime.now())).count();
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

        // Basic sanity checks
        if (test.getTotalQuestions() <= 0) {
            ra.addFlashAttribute("error", "Total questions must be at least 1.");
            return "redirect:/teacher/test/new";
        }
        if (test.getTotalMarks() <= 0) {
            ra.addFlashAttribute("error", "Total marks must be at least 1.");
            return "redirect:/teacher/test/new";
        }
        if (test.getTotalMarks() < test.getTotalQuestions()) {
            ra.addFlashAttribute("error",
                "Total marks (" + test.getTotalMarks() + ") cannot be less than " +
                "total questions (" + test.getTotalQuestions() + "). " +
                "Each question needs at least 1 mark.");
            return "redirect:/teacher/test/new";
        }

        User teacher = currentUser(ud);
        test.setCreatedBy(teacher);
        test.setStatus(ExamTest.TestStatus.SCHEDULED);
        test.setScheduledDateTime(LocalDateTime.parse(scheduledDateTimeStr));
        ExamTest saved = examService.saveTest(test);
        ra.addFlashAttribute("msg",
            "Test created! Now add exactly " + saved.getTotalQuestions() +
            " questions totalling " + saved.getTotalMarks() + " marks.");
        return "redirect:/teacher/test/" + saved.getId() + "/questions";
    }

    // ─── Edit Test ────────────────────────────────────────────────────────────

    @GetMapping("/test/{id}/edit")
    public String editTestForm(@PathVariable Long id, Model model) {
        ExamTest test = examService.findTestById(id).orElseThrow();
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

        // If questions already exist, don't allow reducing marks below used marks
        int usedMarks = test.getUsedMarks();
        if (form.getTotalMarks() < usedMarks) {
            ra.addFlashAttribute("error",
                "Cannot reduce total marks to " + form.getTotalMarks() +
                " — already " + usedMarks + " marks assigned to existing questions.");
            return "redirect:/teacher/test/" + id + "/edit";
        }

        // Don't allow reducing totalQuestions below existing count
        int existingQCount = test.getQuestions().size();
        if (form.getTotalQuestions() < existingQCount) {
            ra.addFlashAttribute("error",
                "Cannot reduce total questions to " + form.getTotalQuestions() +
                " — already " + existingQCount + " questions added.");
            return "redirect:/teacher/test/" + id + "/edit";
        }

        test.setTitle(form.getTitle());
        test.setSubject(form.getSubject());
        test.setDurationMinutes(form.getDurationMinutes());
        test.setTotalMarks(form.getTotalMarks());
        test.setTotalQuestions(form.getTotalQuestions());
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

        model.addAttribute("test",           test);
        model.addAttribute("questions",      questions);
        model.addAttribute("newQ",           new Question());

        // Calculated values for the UI
        int usedMarks      = test.getUsedMarks();
        int remainingMarks = test.getRemainingMarks();
        int addedQ         = questions.size();
        int remainingQ     = test.getRemainingQuestions();
        boolean limitHit   = (addedQ >= test.getTotalQuestions())
                          || (remainingMarks <= 0);

        model.addAttribute("usedMarks",      usedMarks);
        model.addAttribute("remainingMarks", remainingMarks);
        model.addAttribute("addedQ",         addedQ);
        model.addAttribute("remainingQ",     remainingQ);
        model.addAttribute("limitHit",       limitHit);

        return "teacher/questions";
    }

    @PostMapping("/test/{testId}/questions/add")
    public String addQuestion(@PathVariable Long testId,
            @ModelAttribute Question question,
            @RequestParam("optionTexts") List<String> options,
            @RequestParam("correctOption") int correctOption,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes ra) {

        ExamTest test = examService.findTestById(testId).orElseThrow();
        List<Question> existing = examService.getQuestionsByTest(test);

        // ── Validate question count ───────────────────────────────────────────
        if (existing.size() >= test.getTotalQuestions()) {
            ra.addFlashAttribute("error",
                "❌ Cannot add more questions. This test allows exactly " +
                test.getTotalQuestions() + " question(s).");
            return "redirect:/teacher/test/" + testId + "/questions";
        }

        // ── Validate marks won't exceed total ─────────────────────────────────
        int usedSoFar = test.getUsedMarks();
        int remaining = test.getTotalMarks() - usedSoFar;

        if (question.getMarks() <= 0) {
            ra.addFlashAttribute("error", "❌ Marks per question must be at least 1.");
            return "redirect:/teacher/test/" + testId + "/questions";
        }

        if (question.getMarks() > remaining) {
            ra.addFlashAttribute("error",
                "❌ This question has " + question.getMarks() + " mark(s), but only " +
                remaining + " mark(s) remaining out of " + test.getTotalMarks() + " total.");
            return "redirect:/teacher/test/" + testId + "/questions";
        }

        // ── Check that last question exactly uses up remaining marks ──────────
        boolean isLastQuestion = (existing.size() + 1 == test.getTotalQuestions());
        if (isLastQuestion && question.getMarks() != remaining) {
            ra.addFlashAttribute("error",
                "❌ This is the last question. It must carry exactly " +
                remaining + " mark(s) so the total adds up to " +
                test.getTotalMarks() + ".");
            return "redirect:/teacher/test/" + testId + "/questions";
        }
        if (!imageFile.isEmpty()) {

            try {

                String fileName = System.currentTimeMillis()
                        + "_" +
                        imageFile.getOriginalFilename();

                java.nio.file.Path uploadPath =
                        java.nio.file.Paths.get("uploads");

                java.nio.file.Files.createDirectories(uploadPath);

                java.nio.file.Files.copy(
                        imageFile.getInputStream(),
                        uploadPath.resolve(fileName),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );

                question.setImagePath("/uploads/" + fileName);

            } catch (Exception e) {

                ra.addFlashAttribute("error",
                        "Image upload failed.");

                return "redirect:/teacher/test/" + testId + "/questions";
            }
        }
        question.setTest(test);
        for (int i = 0; i < options.size(); i++) {

            String optText = options.get(i);

            if (optText == null || optText.isBlank()) {
                continue;
            }

            QuestionOption option = new QuestionOption();

            option.setQuestion(question);

            option.setOptionText(optText);

            option.setCorrect(i == correctOption);

            question.getOptions().add(option);
        }
        examService.saveQuestion(question);
        ra.addFlashAttribute("msg", "✅ Question added. " +
            (test.getTotalQuestions() - existing.size() - 1) + " question(s) remaining.");
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
    @GetMapping("/question/{id}/edit")
    public String editQuestionForm(@PathVariable Long id,
                                   Model model) {

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        model.addAttribute("question", question);

        return "teacher/edit-question";
    }
    @PostMapping("/question/{id}/edit")
    public String updateQuestion(@PathVariable Long id,
                                 @ModelAttribute Question form,
                                 RedirectAttributes ra) {

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        question.setQuestionText(form.getQuestionText());

        
    

        question.setMarks(form.getMarks());

        questionRepository.save(question);

        ra.addFlashAttribute("msg",
                "✅ Question updated successfully.");

        return "redirect:/teacher/test/" +
                question.getTest().getId() +
                "/questions";
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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
