package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class SubmitQuizRequest {
    @NotEmpty
    @Valid
    private List<SubmitAnswerRequest> answers;
}
