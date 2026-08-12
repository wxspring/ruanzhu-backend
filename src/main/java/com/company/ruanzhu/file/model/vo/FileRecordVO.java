package com.company.ruanzhu.file.model.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FileRecordVO {
    private Long id;
    private Long projectId;
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private Integer version;
    private LocalDateTime createdAt;

    /** List of source file paths extracted from the ZIP (transient, not persisted) */
    private List<String> sourceFiles;
}
