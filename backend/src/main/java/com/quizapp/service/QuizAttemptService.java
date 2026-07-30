package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.entity.*;
import com.quizapp.exception.DuplicateAttemptException;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles starting a quiz attempt, submitting answers, automatic scoring,
 * and preventing duplicate submissions.
 */
@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final OptionRepository optionRepository;
    private final UserRepository userRepository;
    private final AnswerRepository answerRepository;

    // ---------- Start attempt ----------
    @Transactional
    public Long startAttempt(Long quizId, Authentication auth) {
        User student = currentUser(auth);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        // "Prevent multiple submissions": a student may only ever have ONE attempt row per quiz.
        attemptRepository.findByStudentIdAndQuizId(student.getId(), quizId).ifPresent(existing -> {
            throw new DuplicateAttemptException("You have already attempted this quiz");
        });

        QuizAttempt attempt = QuizAttempt.builder()
                .student(student)
                .quiz(quiz)
                .startedAt(LocalDateTime.now())
                .status(QuizAttempt.AttemptStatus.IN_PROGRESS)
                .build();

        attemptRepository.save(attempt);
        return attempt.getId();
    }

    // ---------- Submit & auto-grade ----------
    @Transactional
    public ResultDTO submitAttempt(Long attemptId, SubmitQuizRequest request, Authentication auth) {
        User student = currentUser(auth);

        QuizAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found: " + attemptId));

        if (!attempt.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("This attempt does not belong to you");
        }
        if (attempt.getStatus() == QuizAttempt.AttemptStatus.SUBMITTED) {
            // Idempotency guard: submitting twice never re-scores or duplicates rows
            throw new DuplicateAttemptException("This attempt has already been submitted");
        }

        Quiz quiz = attempt.getQuiz();
        Map<Long, Question> questionsById = quiz.getQuestions().stream()
                .collect(Collectors.toMap(Question::getId, q -> q));

        int score = 0, correctCount = 0, wrongCount = 0;

        for (SubmitAnswerRequest ans : request.getAnswers()) {
            Question question = questionsById.get(ans.getQuestionId());
            if (question == null) continue; // ignore answers for questions not in this quiz

            Option selected = null;
            boolean isCorrect = false;

            if (ans.getSelectedOptionId() != null) {
                selected = optionRepository.findById(ans.getSelectedOptionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Option not found"));
                isCorrect = selected.isCorrect();
            }

            if (isCorrect) {
                score += question.getMarks();
                correctCount++;
            } else if (selected != null) {
                wrongCount++;
            }

            answerRepository.save(Answer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedOption(selected)
                    .isCorrect(isCorrect)
                    .build());
        }

        double percentage = quiz.getTotalMarks() == 0 ? 0
                : Math.round((score * 10000.0 / quiz.getTotalMarks())) / 100.0;

        attempt.setStatus(QuizAttempt.AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        attempt.setWrongCount(wrongCount);
        attempt.setPercentage(percentage);

        return ResultDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .studentName(student.getFullName())
                .totalMarks(quiz.getTotalMarks())
                .score(score)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .percentage(percentage)
                .rank(computeRank(quiz.getId(), score))
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    // ---------- Student: attempt history ----------
    @Transactional(readOnly = true)
    public List<ResultDTO> getMyHistory(Authentication auth) {
        User student = currentUser(auth);
        return attemptRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
                .filter(a -> a.getStatus() == QuizAttempt.AttemptStatus.SUBMITTED)
                .map(a -> ResultDTO.builder()
                        .attemptId(a.getId())
                        .quizTitle(a.getQuiz().getTitle())
                        .studentName(student.getFullName())
                        .totalMarks(a.getQuiz().getTotalMarks())
                        .score(a.getScore())
                        .correctCount(a.getCorrectCount())
                        .wrongCount(a.getWrongCount())
                        .percentage(a.getPercentage())
                        .rank(computeRank(a.getQuiz().getId(), a.getScore()))
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ---------- Admin: leaderboard for a quiz ----------
    @Transactional(readOnly = true)
    public List<ResultDTO> getLeaderboard(Long quizId) {
        List<QuizAttempt> attempts = attemptRepository
                .findByQuizIdAndStatusOrderByScoreDesc(quizId, QuizAttempt.AttemptStatus.SUBMITTED);

        return java.util.stream.IntStream.range(0, attempts.size())
                .mapToObj(i -> {
                    QuizAttempt a = attempts.get(i);
                    return ResultDTO.builder()
                            .attemptId(a.getId())
                            .quizTitle(a.getQuiz().getTitle())
                            .studentName(a.getStudent().getFullName())
                            .totalMarks(a.getQuiz().getTotalMarks())
                            .score(a.getScore())
                            .correctCount(a.getCorrectCount())
                            .wrongCount(a.getWrongCount())
                            .percentage(a.getPercentage())
                            .rank(i + 1)
                            .submittedAt(a.getSubmittedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ---------- Admin: every result across every quiz ----------
    @Transactional(readOnly = true)
    public List<ResultDTO> getAllResults() {
        return attemptRepository.findByStatus(QuizAttempt.AttemptStatus.SUBMITTED).stream()
                .map(a -> ResultDTO.builder()
                        .attemptId(a.getId())
                        .quizTitle(a.getQuiz().getTitle())
                        .studentName(a.getStudent().getFullName())
                        .totalMarks(a.getQuiz().getTotalMarks())
                        .score(a.getScore())
                        .correctCount(a.getCorrectCount())
                        .wrongCount(a.getWrongCount())
                        .percentage(a.getPercentage())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private int computeRank(Long quizId, int score) {
        List<QuizAttempt> attempts = attemptRepository
                .findByQuizIdAndStatusOrderByScoreDesc(quizId, QuizAttempt.AttemptStatus.SUBMITTED);
        for (int i = 0; i < attempts.size(); i++) {
            if (attempts.get(i).getScore() <= score) return i + 1;
        }
        return attempts.size() + 1;
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
