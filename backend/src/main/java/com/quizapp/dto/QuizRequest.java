package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class QuizRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotEmpty(message = "A quiz must have at least one question")
    @Valid
    private List<QuestionRequest> questions;
}
