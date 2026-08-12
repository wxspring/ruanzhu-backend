package com.company.ruanzhu.file.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.file.enums.FileType;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.model.vo.FileRecordVO;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.service.FileService;
import com.company.ruanzhu.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String UPLOAD_BASE_DIR = "./uploads/seed-code";

    private final FileRecordRepository fileRecordRepository;
    private final ProjectRepository projectRepository;

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

        // 4. Build storage directory and save the file
        String relativeDir = projectId + "/";
        Path storageDir = Paths.get(UPLOAD_BASE_DIR, relativeDir);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", storageDir, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        String storedName = UUID.randomUUID() + "_" + originalFilename;
        Path filePath = storageDir.resolve(storedName);
        String storagePath = relativeDir + storedName;

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to save file: {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        // 5. Extract ZIP entries (source file list)
        List<String> sourceFiles = extractZipEntries(filePath);

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
        Path filePath = Paths.get(UPLOAD_BASE_DIR, storagePath);
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            log.error("Failed to read file: {}", filePath, e);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public void deleteFile(String storagePath) {
        // Delete physical file
        Path filePath = Paths.get(UPLOAD_BASE_DIR, storagePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
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

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Walk a ZIP file and return relative paths of non-hidden entries.
     * The ZIP is <em>not</em> expanded to disk; only the entry list is collected.
     */
    private List<String> extractZipEntries(Path zipPath) {
        List<String> entries = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
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
            log.error("Failed to read ZIP entries: {}", zipPath, e);
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
