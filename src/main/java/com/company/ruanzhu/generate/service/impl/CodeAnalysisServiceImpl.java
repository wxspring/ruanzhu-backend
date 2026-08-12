package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.file.enums.FileType;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.generate.analyzer.CodeAnalyzer;
import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;
import com.company.ruanzhu.generate.service.CodeAnalysisService;
import com.company.ruanzhu.project.repository.ProjectRepository;
import com.company.ruanzhu.project.service.SoftwareSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodeAnalysisServiceImpl implements CodeAnalysisService {

    private static final String UPLOAD_BASE_DIR = "./uploads/seed-code";

    private final FileRecordRepository fileRecordRepository;
    private final ProjectRepository projectRepository;
    private final CodeAnalyzer codeAnalyzer;
    private final SoftwareSummaryService softwareSummaryService;

    @Override
    @Transactional
    public CodeAnalysisResult analyzeProject(Long projectId) {
        // 1. Verify project exists
        if (projectRepository.selectById(projectId) == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        // 2. Get the latest seed-code file record
        List<FileRecord> records = fileRecordRepository.findByProjectIdAndFileType(projectId, FileType.SEED_CODE);
        if (records.isEmpty()) {
            throw new BusinessException(ErrorCode.SEED_CODE_NOT_FOUND);
        }
        FileRecord latestRecord = records.get(0); // already ordered desc by version

        // 3. Build absolute path to the ZIP file
        Path zipPath = Paths.get(UPLOAD_BASE_DIR, latestRecord.getStoragePath());
        if (!Files.isRegularFile(zipPath)) {
            log.error("Seed code ZIP not found on disk: {}", zipPath);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }

        // 4. Extract ZIP to a temporary directory
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("seed-code-" + projectId + "-");
            extractZip(zipPath, tempDir);
        } catch (IOException e) {
            log.error("Failed to extract ZIP: {}", zipPath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_ERROR);
        }

        try {
            // 5. Analyse the extracted directory
            CodeAnalysisResult result = codeAnalyzer.analyze(tempDir.toString());

            // 6. Update SoftwareSummary with detected language and code lines
            try {
                if (result.getLanguage() != null && !"Unknown".equals(result.getLanguage())) {
                    // We only update code lines; language would need a separate setter
                    softwareSummaryService.updateCodeLines(projectId, result.getTotalLines());
                } else {
                    softwareSummaryService.updateCodeLines(projectId, result.getTotalLines());
                }
            } catch (BusinessException e) {
                // SoftwareSummary may not exist yet; log and continue
                log.warn("Failed to update SoftwareSummary for project {}: {}", projectId, e.getMessage());
            }

            return result;
        } finally {
            // 7. Clean up temporary directory
            cleanupTempDir(tempDir);
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Extract a ZIP file to a target directory, with path-traversal protection.
     */
    private void extractZip(Path zipPath, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = targetDir.resolve(entry.getName()).normalize();

                // Path-traversal protection
                if (!entryPath.startsWith(targetDir)) {
                    log.warn("Skipping ZIP entry with path traversal: {}", entry.getName());
                    continue;
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(zis, entryPath);
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * Recursively delete a temporary directory.
     */
    private void cleanupTempDir(Path tempDir) {
        try {
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete temp file: {}", path, e);
                            }
                        });
            }
        } catch (IOException e) {
            log.warn("Failed to clean up temp directory: {}", tempDir, e);
        }
    }
}
