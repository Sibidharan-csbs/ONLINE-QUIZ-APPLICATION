package com.quizapp.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Data
public class QuestionRequest {

    @NotBlank
    private String questionText;

    @NotNull
    @Min(1)
    private Integer marks;

    @NotEmpty
    @Size(min = 2, message = "A question needs at least two options")
    @Valid
    private List<OptionRequest> options;
}
