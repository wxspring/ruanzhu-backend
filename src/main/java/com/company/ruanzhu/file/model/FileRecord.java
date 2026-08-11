package com.company.ruanzhu.file.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("file_record")
public class FileRecord {
    private Long id;
    private Long projectId;
    private String fileType;
    private String fileName;
    private String storagePath;
    private Long fileSize;
    private Integer version;
    private LocalDateTime createdAt;
}
