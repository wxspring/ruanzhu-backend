package com.company.ruanzhu.generate.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.generate.ai.AiClient;
import com.company.ruanzhu.generate.service.CodeExpansionService;
import com.company.ruanzhu.project.model.Project;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.repository.ProjectRepository;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Implementation of code expansion service.
 * Extracts seed code, analyzes it, and optionally expands using AI.
 * Can also generate code from scratch if no seed code exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CodeExpansionServiceImpl implements CodeExpansionService {

    private static final int MIN_LINES = 3000;
    private static final int TARGET_LINES = 5000;

    private final FileRecordRepository fileRecordRepository;
    private final StorageClient storageClient;
    private final AiClient aiClient;
    private final ProjectRepository projectRepository;
    private final SoftwareSummaryRepository softwareSummaryRepository;

    @Override
    public String expandCode(Long projectId) {
        // Check if seed code exists
        List<FileRecord> records = fileRecordRepository.findByProjectIdAndFileType(projectId, "SEED_CODE");

        if (records.isEmpty()) {
            // No seed code - generate from scratch using software summary
            log.info("Project {} has no seed code, generating from scratch", projectId);
            return generateFromScratch(projectId);
        }

        // Has seed code - extract and expand
        FileRecord latestRecord = records.stream()
                .max(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 0))
                .orElseThrow(() -> new BusinessException(ErrorCode.SEED_CODE_NOT_FOUND));

        // Download and extract the ZIP
        byte[] zipBytes = storageClient.download(latestRecord.getStoragePath());
        String codeContent = extractCodeFromZip(zipBytes);

        int currentLines = countLines(codeContent);
        log.info("Project {} has {} lines of code, minimum required: {}", projectId, currentLines, MIN_LINES);

        if (currentLines >= MIN_LINES) {
            // Already meets requirement, return as-is
            return codeContent;
        }

        // Need to expand - use AI to generate additional code
        log.info("Expanding code from {} to target {} lines", currentLines, TARGET_LINES);
        String expandedCode = expandWithAi(codeContent, currentLines, TARGET_LINES);

        return expandedCode;
    }

    /**
     * Generate code from scratch using software summary information.
     */
    private String generateFromScratch(Long projectId) {
        Project project = projectRepository.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        SoftwareSummary summary = softwareSummaryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUMMARY_NOT_FOUND));

        String language = summary.getLanguage() != null ? summary.getLanguage() : "Java";
        String purpose = summary.getPurpose() != null ? summary.getPurpose() : project.getName();
        String domain = summary.getTargetDomain() != null ? summary.getTargetDomain() : "企业管理";
        String functions = summary.getMainFunctions() != null ? summary.getMainFunctions() : "基础管理功能";

        String prompt = String.format("""
                你是一个专业的软件开发专家。请根据以下软件信息，生成完整的源代码以满足软件著作权申请要求。

                软件名称：%s
                编程语言：%s
                开发目的：%s
                面向领域：%s
                主要功能：%s

                要求：
                1. 生成 %d 行左右的完整代码
                2. 代码结构清晰，包含完整的类、方法、注释
                3. 实现上述主要功能
                4. 包含必要的异常处理、日志记录
                5. 代码要看起来真实、专业、可运行
                6. 直接输出代码，不要解释
                7. 每个文件用 // File: filename 注释分隔

                注意：生成的代码应该是有意义的功能代码，体现软件的核心业务逻辑。
                """, project.getName(), language, purpose, domain, functions, TARGET_LINES);

        String generatedCode = aiClient.generate(prompt);
        log.info("Generated {} lines of code from scratch for project {}", countLines(generatedCode), projectId);

        return generatedCode;
    }

    @Override
    public boolean meetsLineRequirement(Long projectId) {
        return getCurrentLineCount(projectId) >= MIN_LINES;
    }

    @Override
    public int getCurrentLineCount(Long projectId) {
        List<FileRecord> records = fileRecordRepository.findByProjectIdAndFileType(projectId, "SEED_CODE");
        if (records.isEmpty()) {
            return 0;
        }

        FileRecord latestRecord = records.stream()
                .max(Comparator.comparingInt(r -> r.getVersion() != null ? r.getVersion() : 0))
                .orElseThrow(() -> new BusinessException(ErrorCode.SEED_CODE_NOT_FOUND));

        byte[] zipBytes = storageClient.download(latestRecord.getStoragePath());
        String codeContent = extractCodeFromZip(zipBytes);
        return countLines(codeContent);
    }

    /**
     * Extract all source code files from a ZIP and concatenate them.
     */
    private String extractCodeFromZip(byte[] zipBytes) {
        StringBuilder allCode = new StringBuilder();
        List<String> codeFiles = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory() && isSourceFile(entry.getName())) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    String content = baos.toString(StandardCharsets.UTF_8);
                    codeFiles.add("// File: " + entry.getName() + "\n" + content + "\n\n");
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            log.error("Failed to extract code from ZIP", e);
            throw new BusinessException(ErrorCode.CODE_ANALYSIS_ERROR);
        }

        // Sort files by importance (main files first)
        codeFiles.sort((a, b) -> {
            int scoreA = calculateFileImportance(a);
            int scoreB = calculateFileImportance(b);
            return scoreB - scoreA;
        });

        for (String code : codeFiles) {
            allCode.append(code);
        }

        return allCode.toString();
    }

    private boolean isSourceFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".java") ||
               lower.endsWith(".js") ||
               lower.endsWith(".ts") ||
               lower.endsWith(".tsx") ||
               lower.endsWith(".jsx") ||
               lower.endsWith(".py") ||
               lower.endsWith(".go") ||
               lower.endsWith(".vue") ||
               lower.endsWith(".cs") ||
               lower.endsWith(".php") ||
               lower.endsWith(".rb");
    }

    private int calculateFileImportance(String code) {
        int score = 0;
        if (code.contains("public static void main")) score += 100;
        if (code.contains("@Controller") || code.contains("@RestController")) score += 50;
        if (code.contains("@Service")) score += 40;
        if (code.contains("Controller")) score += 30;
        if (code.contains("Service")) score += 20;
        return score;
    }

    private int countLines(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        return content.split("\n").length;
    }

    /**
     * Use AI to expand the code to meet target line count.
     */
    private String expandWithAi(String existingCode, int currentLines, int targetLines) {
        int linesToAdd = targetLines - currentLines;

        String prompt = String.format("""
                你是一个专业的软件开发专家。现有以下代码（%d行），需要扩展到%d行以满足软件著作权申请要求。

                现有代码：
                ```
                %s
                ```

                请生成额外的代码来扩展这个软件的功能。要求：
                1. 保持与现有代码相同的编程语言和技术栈
                2. 添加新的功能模块（如：日志管理、数据备份、系统监控、报表导出等）
                3. 添加完善的注释说明
                4. 代码要看起来真实、专业
                5. 生成约%d行新代码
                6. 直接输出代码，不要解释

                注意：生成的代码应该是有意义的功能代码，不是无意义的重复。
                """, currentLines, targetLines, truncateCode(existingCode, 2000), linesToAdd);

        String aiGenerated = aiClient.generate(prompt);

        // Combine existing code with AI-generated code
        return existingCode + "\n\n// ============ 扩展功能模块 ============\n\n" + aiGenerated;
    }

    private String truncateCode(String code, int maxLines) {
        String[] lines = code.split("\n");
        if (lines.length <= maxLines) {
            return code;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLines; i++) {
            sb.append(lines[i]).append("\n");
        }
        sb.append("\n... (代码已截断) ...\n");
        return sb.toString();
    }
}
