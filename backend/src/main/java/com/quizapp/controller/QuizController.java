package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.QuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    // ---------- STUDENT endpoints ----------

    /** Browse all currently active quizzes. */
    @GetMapping
    public ResponseEntity<List<QuizForStudentDTO>> browseQuizzes() {
        return ResponseEntity.ok(quizService.getActiveQuizzesForStudent());
    }

    /** Fetch full question set (without correct answers) to start taking a quiz. */
    @GetMapping("/{id}/attempt")
    public ResponseEntity<QuizForStudentDTO> getQuizForAttempt(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizForAttempt(id));
    }

    // ---------- ADMIN endpoints ----------

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponseDTO> createQuiz(@Valid @RequestBody QuizRequest request,
                                                        Authentication auth) {
        return ResponseEntity.ok(quizService.createQuiz(request, auth));
    }

    @PutMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<QuizResponseDTO> updateQuiz(@PathVariable Long id,
                                                        @Valid @RequestBody QuizRequest request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<QuizResponseDTO>> getAllQuizzesForAdmin() {
        return ResponseEntity.ok(quizService.getAllQuizzesForAdmin());
    }
}
