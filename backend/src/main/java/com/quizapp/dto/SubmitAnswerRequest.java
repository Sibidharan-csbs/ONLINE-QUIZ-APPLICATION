package com.quizapp.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitAnswerRequest {
    @NotNull
    private Long questionId;

    /** May be null if the student left the question unanswered. */
    private Long selectedOptionId;
}
