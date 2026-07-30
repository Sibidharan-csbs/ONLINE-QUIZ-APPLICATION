package com.quizapp.dto;

import lombok.*;
import java.util.List;

/** Sent to STUDENT clients when starting a quiz — correct flags are stripped out. */
@Data
@Builder
public class QuizForStudentDTO {
    private Long id;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer totalMarks;
    private List<QuestionDTO> questions;

    @Data
    @Builder
    public static class QuestionDTO {
        private Long id;
        private String questionText;
        private Integer marks;
        private List<OptionDTO> options;
    }

    @Data
    @Builder
    public static class OptionDTO {
        private Long id;
        private String optionText;
    }
}
