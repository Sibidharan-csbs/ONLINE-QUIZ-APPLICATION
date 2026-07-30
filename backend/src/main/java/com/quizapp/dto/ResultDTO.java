package com.quizapp.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
public class ResultDTO {
    private Long attemptId;
    private String quizTitle;
    private String studentName;
    private Integer totalMarks;
    private Integer score;
    private Integer correctCount;
    private Integer wrongCount;
    private Double percentage;
    private Integer rank;      // rank on this quiz's leaderboard, optional
    private LocalDateTime submittedAt;
}
