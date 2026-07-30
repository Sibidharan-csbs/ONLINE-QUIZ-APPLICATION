package com.quizapp.controller;

import com.quizapp.dto.*;
import com.quizapp.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attempts")
@RequiredArgsConstructor
public class QuizAttemptController {

    private final QuizAttemptService attemptService;

    /** STUDENT: begin a quiz. Fails with 409 if already attempted. */
    @PostMapping("/start/{quizId}")
    public ResponseEntity<Map<String, Long>> start(@PathVariable Long quizId, Authentication auth) {
        Long attemptId = attemptService.startAttempt(quizId, auth);
        return ResponseEntity.ok(Map.of("attemptId", attemptId));
    }

    /** STUDENT: submit answers for grading. Returns the score immediately. */
    @PostMapping("/{attemptId}/submit")
    public ResponseEntity<ResultDTO> submit(@PathVariable Long attemptId,
                                             @Valid @RequestBody SubmitQuizRequest request,
                                             Authentication auth) {
        return ResponseEntity.ok(attemptService.submitAttempt(attemptId, request, auth));
    }

    /** STUDENT: view my own attempt history. */
    @GetMapping("/my-history")
    public ResponseEntity<List<ResultDTO>> myHistory(Authentication auth) {
        return ResponseEntity.ok(attemptService.getMyHistory(auth));
    }

    /** ADMIN: leaderboard for one quiz. */
    @GetMapping("/leaderboard/{quizId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResultDTO>> leaderboard(@PathVariable Long quizId) {
        return ResponseEntity.ok(attemptService.getLeaderboard(quizId));
    }

    /** ADMIN: every submitted result, for the results/search dashboard. */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ResultDTO>> allResults() {
        return ResponseEntity.ok(attemptService.getAllResults());
    }
}
