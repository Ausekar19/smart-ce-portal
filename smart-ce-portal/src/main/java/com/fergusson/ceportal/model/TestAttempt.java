package com.fergusson.ceportal.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "test_attempts")
@Data
@NoArgsConstructor
public class TestAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private ExamTest test;

    private LocalDateTime startTime = LocalDateTime.now();
    private LocalDateTime endTime;

    private double score = 0.0;
    private int correctCount = 0;
    private int wrongCount = 0;
    private int unattemptedCount = 0;

    private boolean submitted = false;

    /** How many times the student switched tabs / left the window */
    private int tabSwitchCount = 0;

    /** Was the exam auto-submitted due to too many tab switches? */
    private boolean autoSubmitted = false;

    @OneToMany(mappedBy = "attempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StudentAnswer> answers = new ArrayList<>();
}
