package com.quizapp.service;

import com.quizapp.dto.*;
import com.quizapp.entity.*;
import com.quizapp.exception.ResourceNotFoundException;
import com.quizapp.repository.QuizRepository;
import com.quizapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;

    // ---------- ADMIN: create ----------
    @Transactional
    public QuizResponseDTO createQuiz(QuizRequest request, Authentication auth) {
        User admin = currentUser(auth);

        int totalMarks = request.getQuestions().stream()
                .mapToInt(QuestionRequest::getMarks)
                .sum();

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .durationMinutes(request.getDurationMinutes())
                .totalMarks(totalMarks)
                .active(true)
                .createdBy(admin)
                .build();

        request.getQuestions().forEach(qReq -> {
            // Every question must have exactly one correct option
            long correctCount = qReq.getOptions().stream().filter(OptionRequest::isCorrect).count();
            if (correctCount != 1) {
                throw new IllegalArgumentException(
                        "Question '" + qReq.getQuestionText() + "' must have exactly one correct option");
            }

            Question question = Question.builder()
                    .questionText(qReq.getQuestionText())
                    .marks(qReq.getMarks())
                    .quiz(quiz)
                    .build();

            qReq.getOptions().forEach(oReq -> question.getOptions().add(
                    Option.builder()
                            .optionText(oReq.getOptionText())
                            .correct(oReq.isCorrect())
                            .question(question)
                            .build()));

            quiz.getQuestions().add(question);
        });

        quizRepository.save(quiz);
        return toAdminDTO(quiz);
    }

    // ---------- ADMIN: edit ----------
    @Transactional
    public QuizResponseDTO updateQuiz(Long quizId, QuizRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setDurationMinutes(request.getDurationMinutes());

        quiz.getQuestions().clear(); // orphanRemoval = true deletes old questions/options
        int totalMarks = 0;
        for (QuestionRequest qReq : request.getQuestions()) {
            long correctCount = qReq.getOptions().stream().filter(OptionRequest::isCorrect).count();
            if (correctCount != 1) {
                throw new IllegalArgumentException(
                        "Question '" + qReq.getQuestionText() + "' must have exactly one correct option");
            }
            totalMarks += qReq.getMarks();

            Question question = Question.builder()
                    .questionText(qReq.getQuestionText())
                    .marks(qReq.getMarks())
                    .quiz(quiz)
                    .build();

            qReq.getOptions().forEach(oReq -> question.getOptions().add(
                    Option.builder()
                            .optionText(oReq.getOptionText())
                            .correct(oReq.isCorrect())
                            .question(question)
                            .build()));

            quiz.getQuestions().add(question);
        }
        quiz.setTotalMarks(totalMarks);

        return toAdminDTO(quiz);
    }

    // ---------- ADMIN: delete ----------
    @Transactional
    public void deleteQuiz(Long quizId) {
        if (!quizRepository.existsById(quizId)) {
            throw new ResourceNotFoundException("Quiz not found: " + quizId);
        }
        quizRepository.deleteById(quizId);
    }

    // ---------- ADMIN: list all (active + inactive) ----------
    @Transactional(readOnly = true)
    public List<QuizResponseDTO> getAllQuizzesForAdmin() {
        return quizRepository.findAll().stream().map(this::toAdminDTO).collect(Collectors.toList());
    }

    // ---------- STUDENT: list only active quizzes, no answers revealed ----------
    @Transactional(readOnly = true)
    public List<QuizForStudentDTO> getActiveQuizzesForStudent() {
        return quizRepository.findByActiveTrue().stream().map(this::toStudentListDTO).collect(Collectors.toList());
    }

    // ---------- STUDENT: start a quiz — full questions, correct flags stripped ----------
    @Transactional(readOnly = true)
    public QuizForStudentDTO getQuizForAttempt(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        if (!quiz.isActive()) {
            throw new IllegalStateException("This quiz is not currently available");
        }

        return toStudentDetailDTO(quiz);
    }

    Quiz getQuizEntity(Long quizId) {
        return quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    // ---------- Mapping helpers ----------
    private QuizResponseDTO toAdminDTO(Quiz quiz) {
        return QuizResponseDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .totalMarks(quiz.getTotalMarks())
                .active(quiz.isActive())
                .questions(quiz.getQuestions().stream().map(q -> QuizResponseDTO.QuestionResponseDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .marks(q.getMarks())
                        .options(q.getOptions().stream().map(o -> QuizResponseDTO.OptionResponseDTO.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .correct(o.isCorrect())
                                        .build())
                                .collect(Collectors.toList()))
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private QuizForStudentDTO toStudentListDTO(Quiz quiz) {
        return QuizForStudentDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .totalMarks(quiz.getTotalMarks())
                .questions(List.of()) // question list omitted on the browse page for a lighter payload
                .build();
    }

    private QuizForStudentDTO toStudentDetailDTO(Quiz quiz) {
        return QuizForStudentDTO.builder()
                .id(quiz.getId())
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .durationMinutes(quiz.getDurationMinutes())
                .totalMarks(quiz.getTotalMarks())
                .questions(quiz.getQuestions().stream().map(q -> QuizForStudentDTO.QuestionDTO.builder()
                        .id(q.getId())
                        .questionText(q.getQuestionText())
                        .marks(q.getMarks())
                        .options(q.getOptions().stream().map(o -> QuizForStudentDTO.OptionDTO.builder()
                                        .id(o.getId())
                                        .optionText(o.getOptionText())
                                        .build()) // correct flag intentionally omitted
                                .collect(Collectors.toList()))
                        .build()).collect(Collectors.toList()))
                .build();
    }
}
