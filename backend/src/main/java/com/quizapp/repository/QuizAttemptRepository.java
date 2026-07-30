package com.quizapp.repository;

import com.quizapp.entity.QuizAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    List<QuizAttempt> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
    List<QuizAttempt> findByQuizIdAndStatusOrderByScoreDesc(Long quizId, QuizAttempt.AttemptStatus status);
    List<QuizAttempt> findByStatus(QuizAttempt.AttemptStatus status);
}
