package com.fergusson.ceportal.repository;

import com.fergusson.ceportal.model.QuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuestionOptionRepository
        extends JpaRepository<QuestionOption, Long> {
}