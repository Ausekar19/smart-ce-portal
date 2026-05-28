package com.fergusson.ceportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_tests")
@Data
@NoArgsConstructor
public class ExamTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    

    @Column(nullable = false)
    private String subject;

    private String department;

    private String programCode;

    private String division;
    
    private String classYear;
    
    private boolean showResult = true;

    private boolean discloseAnswers = false;
    
    private int durationMinutes;

    /** Total marks the entire test should sum up to */
    private int totalMarks;

    /** Exactly how many questions this test must have */
    private int totalQuestions;

    private double negativeMarking;

    @Column(nullable = false)
    private LocalDateTime scheduledDateTime;

    @Enumerated(EnumType.STRING)
    private TestStatus status = TestStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "test", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Marks already assigned across all saved questions */
    public int getUsedMarks() {
        return questions.stream().mapToInt(Question::getMarks).sum();
    }

    /** Marks still available to assign */
    public int getRemainingMarks() {
        return totalMarks - getUsedMarks();
    }

    /** Questions still allowed to be added */
    public int getRemainingQuestions() {
        return totalQuestions - questions.size();
    }

    public enum TestStatus {
        SCHEDULED, PUBLISHED, COMPLETED, CANCELLED
    }
}
