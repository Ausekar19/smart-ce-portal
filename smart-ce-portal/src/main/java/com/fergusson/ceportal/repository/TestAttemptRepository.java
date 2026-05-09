package com.fergusson.ceportal.repository;

import com.fergusson.ceportal.model.ExamTest;
import com.fergusson.ceportal.model.TestAttempt;
import com.fergusson.ceportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt> findByStudentAndTest(User student, ExamTest test);

    boolean existsByStudentAndTestAndSubmittedTrue(User student, ExamTest test);

    /** All completed attempts by a student – for "Past Tests" on dashboard */
    List<TestAttempt> findByStudentAndSubmittedTrueOrderByEndTimeDesc(User student);

    /** All attempts for a specific test – for teacher's report */
    List<TestAttempt> findByTestAndSubmittedTrue(ExamTest test);
}
