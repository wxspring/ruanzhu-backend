package com.company.ruanzhu.generate.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request DTO for custom AI prompt generation.
 */
@Data
public class CustomGenerateRequest {

    /**
     * The fully-composed prompt to send to the AI client.
     * The caller should have already substituted placeholders such as software name / version inside it.
     */
    @NotBlank(message = "prompt不能为空")
    private String prompt;
}
