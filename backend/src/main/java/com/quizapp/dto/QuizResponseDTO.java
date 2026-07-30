package com.quizapp.dto;

import lombok.*;
import java.util.List;

/** Sent to ADMIN clients — includes which option is correct. */
@Data
@Builder
public class QuizResponseDTO {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer totalMarks;
    private boolean active;
    private List<QuestionResponseDTO> questions;

    @Data
    @Builder
    public static class QuestionResponseDTO {
        private Long id;
        private String questionText;
        private Integer marks;
        private List<OptionResponseDTO> options;
    }

    @Data
    @Builder
    public static class OptionResponseDTO {
        private Long id;
        private String optionText;
        private boolean correct;
    }
}
