package com.fergusson.ceportal.repository;

import com.fergusson.ceportal.model.ExamTest;
import com.fergusson.ceportal.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTest(ExamTest test);
    long countByTest(ExamTest test);
}
