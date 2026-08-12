package com.company.ruanzhu.file.service;

import com.company.ruanzhu.file.model.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * Upload a seed-code ZIP for a project.
     *
     * @param projectId the project to attach the code to
     * @param file      the ZIP file
     * @return persisted file record (with transient source-file list)
     */
    FileRecordVO uploadSeedCode(Long projectId, MultipartFile file);

    /**
     * Download a file by its storage path.
     *
     * @param storagePath the relative path inside the uploads directory
     * @return raw bytes of the file
     */
    byte[] downloadFile(String storagePath);

    /**
     * Delete a file (both the DB record and the physical file) by storage path.
     *
     * @param storagePath the relative path inside the uploads directory
     */
    void deleteFile(String storagePath);
}
