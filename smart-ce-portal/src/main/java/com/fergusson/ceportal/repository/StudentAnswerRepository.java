package com.fergusson.ceportal.repository;

import com.fergusson.ceportal.model.StudentAnswer;
import com.fergusson.ceportal.model.TestAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StudentAnswerRepository extends JpaRepository<StudentAnswer, Long> {
    List<StudentAnswer> findByAttempt(TestAttempt attempt);
}
