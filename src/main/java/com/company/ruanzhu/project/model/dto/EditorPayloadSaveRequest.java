package com.company.ruanzhu.project.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for saving editor content (e.g. generated code / manual HTML)
 * via PUT /api/projects/{id}/code or /api/projects/{id}/manual.
 */
@Data
public class EditorPayloadSaveRequest {

    /** Display file name to store in the file record (e.g. "项目_程序文档.txt"). */
    @NotBlank
    private String fileName;

    /** Text content to be written to storage. */
    @NotBlank
    private String content;
}
