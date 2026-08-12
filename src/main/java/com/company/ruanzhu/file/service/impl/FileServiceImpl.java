package com.company.ruanzhu.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.file.enums.FileType;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.model.vo.FileRecordVO;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.service.FileService;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final FileRecordRepository fileRecordRepository;
    private final ProjectRepository projectRepository;
    private final StorageClient storageClient;

    @Override
    @Transactional
    public FileRecordVO uploadSeedCode(Long projectId, MultipartFile file) {
        // 1. Verify project exists
        if (projectRepository.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 2. Validate file is a ZIP
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new BusinessException(ErrorCode.FILE_INVALID_TYPE);
        }

        // 3. Determine next version for this project + file-type
        List<FileRecord> existing = fileRecordRepository.findByProjectIdAndFileType(projectId, FileType.SEED_CODE);
        int nextVersion = existing.stream()
                .mapToInt(r -> r.getVersion() != null ? r.getVersion() : 0)
                .max()
                .orElse(0) + 1;

        // 4. Build storage object name and upload via StorageClient
        String relativeDir = projectId + "/";
        String storedName = UUID.randomUUID() + "_" + originalFilename;
        String objectName = relativeDir + storedName;

        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/zip";

        String storagePath;
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read multipart file bytes", e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(fileBytes)) {
            storagePath = storageClient.upload(objectName, bais, contentType);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to upload file via storage client: {}", objectName, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 5. Extract ZIP entries (source file list) from in-memory bytes
        List<String> sourceFiles = extractZipEntries(fileBytes);

        // 6. Persist file metadata
        FileRecord record = new FileRecord();
        record.setProjectId(projectId);
        record.setFileType(FileType.SEED_CODE);
        record.setFileName(originalFilename);
        record.setStoragePath(storagePath);
        record.setFileSize(file.getSize());
        record.setVersion(nextVersion);
        record.setCreatedAt(LocalDateTime.now());

        fileRecordRepository.insert(record);

        return toVO(record, sourceFiles);
    }

    @Override
    public byte[] downloadFile(String storagePath) {
        return storageClient.download(storagePath);
    }

    @Override
    @Transactional
    public void deleteFile(String storagePath) {
        // Delete physical file via storage client
        try {
            storageClient.delete(storagePath);
        } catch (Exception e) {
            log.error("Failed to delete physical file via storage client: {}", storagePath, e);
        }

        // Delete matching DB records
        List<Long> ids = fileRecordRepository.selectList(
                new LambdaQueryWrapper<FileRecord>()
                        .eq(FileRecord::getStoragePath, storagePath)
        ).stream().map(FileRecord::getId).toList();

        if (!ids.isEmpty()) {
            fileRecordRepository.deleteBatchIds(ids);
        }
    }

    @Override
    public String getPresignedUrl(String storagePath, int expirySeconds) {
        return storageClient.getPresignedUrl(storagePath, expirySeconds);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Walk a ZIP file from in-memory bytes and return relative paths of non-hidden entries.
     * The ZIP is <em>not</em> expanded to disk; only the entry list is collected.
     */
    private List<String> extractZipEntries(byte[] zipBytes) {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // Skip directories and hidden files (e.g. __MACOSX, .DS_Store)
                if (!entry.isDirectory() && !isHiddenPath(name)) {
                    entries.add(name);
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to read ZIP entries from bytes", e);
        }
        return entries;
    }

    /**
     * A path is considered hidden when any segment starts with '.' or '_'.
     */
    private boolean isHiddenPath(String path) {
        for (String segment : path.split("[/\\\\]")) {
            if (segment.startsWith(".") || segment.startsWith("_")) {
                return true;
            }
        }
        return false;
    }

    private FileRecordVO toVO(FileRecord record, List<String> sourceFiles) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(record.getId());
        vo.setProjectId(record.getProjectId());
        vo.setFileType(record.getFileType());
        vo.setFileName(record.getFileName());
        vo.setStoragePath(record.getStoragePath());
        vo.setFileSize(record.getFileSize());
        vo.setVersion(record.getVersion());
        vo.setCreatedAt(record.getCreatedAt());
        vo.setSourceFiles(sourceFiles);
        return vo;
    }
}
