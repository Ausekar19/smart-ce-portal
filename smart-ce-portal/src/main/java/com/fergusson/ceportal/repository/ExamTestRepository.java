package com.fergusson.ceportal.repository;

import com.fergusson.ceportal.model.ExamTest;
import com.fergusson.ceportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamTestRepository extends JpaRepository<ExamTest, Long> {

    List<ExamTest> findByCreatedBy(User teacher);

    /** Tests scheduled in the future (for upcoming list) */
    List<ExamTest> findByScheduledDateTimeAfterOrderByScheduledDateTimeAsc(LocalDateTime now);

    /** Tests scheduled in the past (may or may not be attempted) */
    List<ExamTest> findByScheduledDateTimeBeforeOrderByScheduledDateTimeDesc(LocalDateTime now);

    /**
     * Upcoming tests that the given student has NOT yet attempted.
     */
    @Query("""
    	    SELECT t FROM ExamTest t
    	    WHERE t.scheduledDateTime > :now

    	    AND t.department = :#{#student.department}

    	    AND t.programCode = :#{#student.programCode}

    	    AND t.division = :#{#student.division}
    		
    		AND t.classYear = :#{#student.classYear}
    		
    	    AND t.id NOT IN (
    	        SELECT a.test.id FROM TestAttempt a
    	        WHERE a.student = :student
    	        AND a.submitted = true
    	    )

    	    ORDER BY t.scheduledDateTime ASC
    	""")
    List<ExamTest> findUpcomingTestsForStudent(@Param("student") User student,
                                               @Param("now") LocalDateTime now);

    List<ExamTest> findByCreatedByOrderByScheduledDateTimeDesc(User teacher);
}
