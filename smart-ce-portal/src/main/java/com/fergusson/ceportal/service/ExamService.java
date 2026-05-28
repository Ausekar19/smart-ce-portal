package com.fergusson.ceportal.service;

import com.fergusson.ceportal.model.*;
import com.fergusson.ceportal.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamTestRepository examTestRepository;
    private final QuestionRepository questionRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final UserRepository userRepository;

    // ─── TEACHER: Test Management ────────────────────────────────────────────

    public ExamTest saveTest(ExamTest test) {
        return examTestRepository.save(test);
    }

    public List<ExamTest> getTestsByTeacher(User teacher) {
        return examTestRepository.findByCreatedByOrderByScheduledDateTimeDesc(teacher);
    }

    public Optional<ExamTest> findTestById(Long id) {
        return examTestRepository.findById(id);
    }

    public void deleteTest(Long id) {
        examTestRepository.deleteById(id);
    }

    public Question saveQuestion(Question question) {
        return questionRepository.save(question);
    }

    public List<Question> getQuestionsByTest(ExamTest test) {
        return questionRepository.findByTest(test);
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    public List<ExamTest> getAllTests() {
        return examTestRepository.findAll();
    }

    // ─── STUDENT: Dashboard Data ──────────────────────────────────────────────

    /**
     * Returns tests scheduled in the future that the student has NOT yet submitted.
     */
    public List<ExamTest> getUpcomingTestsForStudent(User student) {
        return examTestRepository.findUpcomingTestsForStudent(student, LocalDateTime.now());
    }

    /**
     * Returns all submitted attempts for the student (past/completed tests).
     */
    public List<TestAttempt> getCompletedAttemptsForStudent(User student) {
        return testAttemptRepository.findByStudentAndSubmittedTrueOrderByEndTimeDesc(student);
    }

    public boolean hasStudentAttempted(User student, ExamTest test) {
        return testAttemptRepository.existsByStudentAndTestAndSubmittedTrue(student, test);
    }

    // ─── STUDENT: Exam Taking ─────────────────────────────────────────────────

    /**
     * Start a new attempt (or return an existing in-progress one).
     */
    @Transactional
    public TestAttempt startAttempt(User student, ExamTest test) {
        Optional<TestAttempt> existing = testAttemptRepository.findByStudentAndTest(student, test);
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime start = test.getScheduledDateTime();

        LocalDateTime end =
                start.plusMinutes(test.getDurationMinutes());

        if (now.isBefore(start)) {

            throw new RuntimeException(
                    "Test has not started yet.");
        }

        if (now.isAfter(end)) {

            throw new RuntimeException(
                    "Test time window has ended.");
        }
        if (existing.isPresent()) {
            return existing.get();
        }
        TestAttempt attempt = new TestAttempt();
        attempt.setStudent(student);
        attempt.setTest(test);
        attempt.setStartTime(LocalDateTime.now());
        return testAttemptRepository.save(attempt);
    }

    /**
     * Called from the front-end JS whenever a tab-switch event is detected.
     * Returns the updated switch count.
     * If count >= maxAllowed, auto-submits the exam.
     */
    @Transactional
    public int recordTabSwitch(Long attemptId, int maxAllowed) {
        TestAttempt attempt = testAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Attempt not found"));

        if (attempt.isSubmitted()) return attempt.getTabSwitchCount();

        attempt.setTabSwitchCount(attempt.getTabSwitchCount() + 1);

        if (attempt.getTabSwitchCount() >= maxAllowed) {
            attempt.setAutoSubmitted(true);
            submitAttempt(attempt, Map.of()); // auto-submit with whatever was answered
        } else {
            testAttemptRepository.save(attempt);
        }
        return attempt.getTabSwitchCount();
    }

    /**
     * Submit the exam: evaluate answers, compute score, mark as submitted.
     * answersMap: questionId -> selected option string ("A"/"B"/"C"/"D")
     */
    @Transactional
    public TestAttempt submitAttempt(TestAttempt attempt, Map<String, String> answersMap) {
        if (attempt.isSubmitted()) return attempt;

        ExamTest test = attempt.getTest();
        List<Question> questions = questionRepository.findByTest(test);

        double score = 0;
        int correct = 0, wrong = 0, unattempted = 0;

        for (Question q : questions) {
            StudentAnswer sa = new StudentAnswer();
            sa.setAttempt(attempt);
            sa.setQuestion(q);

            String key = String.valueOf(q.getId());
            if (answersMap.containsKey(key) && !answersMap.get(key).isBlank()) {
            	String selected = answersMap.get(key);

            	sa.setSelectedOption(selected);

            	boolean isCorrect = q.getOptions().stream()
            	        .anyMatch(opt ->
            	                opt.isCorrect() &&
            	                opt.getOptionText().equals(selected));

            	if (isCorrect) {

            	    sa.setCorrect(true);

            	    score += q.getMarks();

            	    correct++;

            	} else {

            	    sa.setCorrect(false);

            	    score -= test.getNegativeMarking();

            	    wrong++;
            	}
                
            } else {
                unattempted++;
            }
            studentAnswerRepository.save(sa);
        }

        attempt.setScore(Math.max(0, score));
        attempt.setCorrectCount(correct);
        attempt.setWrongCount(wrong);
        attempt.setUnattemptedCount(unattempted);
        attempt.setSubmitted(true);
        attempt.setEndTime(LocalDateTime.now());

        return testAttemptRepository.save(attempt);
    }

    // ─── TEACHER/ADMIN: Reports ───────────────────────────────────────────────

    public List<TestAttempt> getAttemptsForTest(ExamTest test) {
        return testAttemptRepository.findByTestAndSubmittedTrue(test);
    }
}
