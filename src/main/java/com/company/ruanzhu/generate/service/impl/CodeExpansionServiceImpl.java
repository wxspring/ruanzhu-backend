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
    private static final int TARGET_LINES = 8000;

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
        String techOptions = summary.getTechFeatureOptions() != null ? summary.getTechFeatureOptions() : "物联网软件";
        String functionMenu = summary.getFunctionMenu() != null ? summary.getFunctionMenu() : "";

        // 功能菜单上下文（如果有）
        String menuCtx = functionMenu.isEmpty() ? "（未提供功能菜单）" : functionMenu;

        String persona = languagePersona(language);
        String stack = techStack(language);
        String fileComment = languageFileComment(language);

        String prompt = String.format("""
                %s请为以下软件生成完整的、可编译运行的源代码（面向软件著作权申请）。
                注意：必须严格使用【编程语言】字段指定的语言生成，不得切换到其他语言。

                软件名称：%s
                编程语言：%s
                技术特点选项：%s
                开发目的：%s
                面向领域：%s
                主要功能：%s
                已确认的功能菜单（按此菜单展开业务类）：
                %s

                【技术栈强制要求】
                %s

                【代码结构强制要求 - 必须按此文件清单生成，每个文件用 %s 文件路径 注释开头】
                1. 程序入口/启动类（包名由软件名派生，不得使用 com.highwater 等固定硬编码）
                2. 实体/模型类（至少 5 个，围绕「面向领域」与「主要功能」的业务核心对象，每个 15-25 个字段）
                3. 数据访问层（Mapper/DAO/Repository，含分页与条件查询）
                4. Service 服务层接口（每个实体对应一个，6-10 个方法）
                5. Service 实现类（完整方法体 + 业务逻辑 + 日志）
                6. Controller/路由/Handler 控制层（完整 CRUD + 分页查询 + 状态变更等接口）
                7. Common 通用类：统一响应 Result、业务异常、错误码、全局异常处理
                8. Config 配置类：数据源/跨域/拦截器/异步线程池等

                【代码量强制要求 - 非常重要】
                - 总共必须生成 %d 行左右的完整代码（含 import、空行、注释）
                - 每个 Service 实现必须有完整方法体（不是空壳），方法内要有真实的业务逻辑、参数校验、日志记录
                - 每个 Controller 至少 5-8 个接口方法
                - 每个实体至少 15 个字段
                - 禁止生成简短框架代码，必须是充实的、有业务价值的完整类

                【输出格式】
                - 直接输出代码，不要任何解释、说明、markdown 代码块标记
                - 每个文件用 "%s" 注释行开头（注释风格随语言）
                - 每个文件之间空两行分隔
                - 确保所有类的包名/命名空间、导入、类名互相匹配

                现在开始生成：
                """, persona, project.getName(), language, techOptions, purpose, domain, functions, menuCtx,
                stack, fileComment, TARGET_LINES, fileComment);

        log.info("Generating code from scratch for project {}, target lines={}", projectId, TARGET_LINES);
        String generatedCode = aiClient.generate(prompt);

        // 如果 AI 生成的行数明显不够，追加一段扩展代码
        int currentLines = countLines(generatedCode);
        if (currentLines < TARGET_LINES / 2) {
            log.warn("AI 生成代码行数 {} 低于目标 {} 的一半，尝试追加扩展 prompt", currentLines, TARGET_LINES);
            String supplementPrompt = String.format("""
                    请继续为软件 "%s" 补充更多代码（编程语言严格保持为：%s）。
                    补充要求：
                    1. 再生成 3-5 个实体、3-5 个数据访问层、3-5 个 Service 接口与实现、3-5 个 Controller/路由
                    2. 按已有代码的语言与风格保持一致
                    3. 每个实体至少 12 个字段，每个 Service 实现要有完整业务方法体
                    4. 覆盖以下功能菜单对应的业务场景：
                    %s
                    5. 直接输出代码，不要解释，每个文件用 %s 开头
                    """, project.getName(), language, menuCtx, fileComment);
            String supplement = aiClient.generate(supplementPrompt);
            if (supplement != null && !supplement.trim().isEmpty()) {
                generatedCode = generatedCode + "\n\n// ============ 扩展业务模块 ============\n\n" + supplement;
                log.info("补充代码后总行数：{}", countLines(generatedCode));
            }
        }

        return generatedCode;
    }

    // ==================== 语言画像（16 种） ====================

    private String languagePersona(String language) {
        String lang = language == null ? "Java" : language.trim();
        return switch (lang.toLowerCase(Locale.ROOT)) {
            case "java" -> "你是一个专业的 Java Spring Boot 后端开发工程师。";
            case "kotlin" -> "你是一个专业的 Kotlin + Spring Boot 后端开发工程师。";
            case "python" -> "你是一个专业的 Python 后端开发工程师（FastAPI / Django / Flask 生态）。";
            case "php" -> "你是一个专业的 PHP 后端开发工程师（Laravel / ThinkPHP 生态）。";
            case "c#", "csharp", "cs" -> "你是一个专业的 C# .NET 后端开发工程师（ASP.NET Core / .NET 6+ 生态）。";
            case "go" -> "你是一个专业的 Go 后端开发工程师（Gin / Echo + GORM 生态）。";
            case "javascript", "js", "node.js", "nodejs", "node" -> "你是一个专业的 Node.js 后端开发工程师（Express / Koa / NestJS 生态）。";
            case "typescript", "ts" -> "你是一个专业的 TypeScript + Node.js 后端开发工程师（NestJS / TypeORM 生态）。";
            case "c++", "cpp" -> "你是一个专业的 C++ 后端开发工程师。";
            case "c" -> "你是一个专业的 C 语言后端/嵌入式开发工程师。";
            case "dart" -> "你是一个专业的 Dart + Flutter 开发工程师。";
            case "ruby" -> "你是一个专业的 Ruby on Rails 后端开发工程师。";
            case "objective-c", "objc", "objectivec" -> "你是一个专业的 Objective-C iOS 开发工程师。";
            case "android" -> "你是一个专业的 Android 原生（Java/Kotlin）开发工程师。";
            case "vb.net", "vbnet", "vb" -> "你是一个专业的 VB.NET 后端开发工程师（.NET 生态）。";
            case "plc" -> "你是一个专业的 PLC 工控软件开发工程师（IEC 61131-3 ST 语言）。";
            default -> "你是一个专业的后端软件开发工程师。";
        };
    }

    private String techStack(String language) {
        String lang = language == null ? "Java" : language.trim();
        return switch (lang.toLowerCase(Locale.ROOT)) {
            case "java" -> "- Spring Boot 3.x + MyBatis-Plus + MySQL 8.0 + Lombok\n" +
                    "- @RestController / @Service / @Mapper 三层架构\n" +
                    "- Entity 用 @TableName + @TableId + @TableField(fill=...)\n" +
                    "- Service 继承 IService / ServiceImpl<Mapper, Entity>\n" +
                    "- Controller 统一返回 Result<T>，全局 BusinessException + @RestControllerAdvice";
            case "python" -> "- FastAPI / Django + SQLAlchemy + MySQL\n" +
                    "- Pydantic 模型 + 依赖注入 + 路由分层\n" +
                    "- 统一响应模型 + 全局异常处理";
            case "php" -> "- Laravel / ThinkPHP + Eloquent ORM + MySQL\n" +
                    "- Model + Service + Controller 三层 + 中间件\n" +
                    "- 统一 JSON 响应 + 异常处理";
            case "c#", "csharp", "cs" -> "- ASP.NET Core 6+ Web API + EF Core + SQL Server/MySQL\n" +
                    "- Controller + Service + Repository 分层\n" +
                    "- 统一 ApiResponse<T> + 全局异常过滤器";
            case "go" -> "- Gin / Echo + GORM + MySQL\n" +
                    "- handler + service + repository 分层\n" +
                    "- 统一响应结构体 + 中间件错误处理";
            case "javascript", "js", "node.js", "nodejs", "node" -> "- Node.js + Express/Koa + Sequelize + MySQL\n" +
                    "- Router + Service + Model 分层\n" +
                    "- 统一响应封装 + 错误处理中间件";
            case "typescript", "ts" -> "- NestJS + TypeORM + MySQL\n" +
                    "- Module + Controller + Service 分层\n" +
                    "- 统一响应拦截器 + 全局异常过滤器";
            case "kotlin" -> "- Spring Boot 3.x + Kotlin + Exposed/MyBatis-Plus + MySQL\n" +
                    "- 数据类 + 协程 + 分层架构";
            case "c++", "cpp" -> "- C++17 + REST framework（如 Drogon/Crow）+ MySQL\n" +
                    "- Controller + Service + DAO 分层 + 智能指针";
            case "c" -> "- C11 + 嵌入式/后端框架 + MySQL C API\n" +
                    "- 模块分层 + 结构体 + 错误码";
            case "dart" -> "- Dart + Flutter（移动端）+ Dio 网络请求\n" +
                    "- Model + Repository + Service + UI 分层";
            case "ruby" -> "- Ruby on Rails + ActiveRecord + MySQL\n" +
                    "- Model + Service + Controller 分层 + Concern";
            case "objective-c", "objc", "objectivec" -> "- Objective-C + UIKit + Foundation\n" +
                    "- Model + ViewModel + ViewController 分层";
            case "android" -> "- Android（Java/Kotlin）+ Room + Retrofit\n" +
                    "- Activity/Fragment + ViewModel + Repository 分层";
            case "vb.net", "vbnet", "vb" -> "- VB.NET + ASP.NET Core Web API + EF Core\n" +
                    "- Controller + Service + Repository 分层";
            case "plc" -> "- IEC 61131-3 ST 结构化文本 + PLC 运行时\n" +
                    "- 功能块 FUNCTION_BLOCK 分层 + VAR 变量声明";
            default -> "- 分层架构（表现层 / 业务层 / 数据层）+ MySQL\n- 统一响应 + 全局异常处理";
        };
    }

    private String languageFileComment(String language) {
        String lang = language == null ? "Java" : language.trim();
        return switch (lang.toLowerCase(Locale.ROOT)) {
            case "python", "ruby" -> "# File:";
            case "vb.net", "vbnet", "vb" -> "' File:";
            default -> "// File:";
        };
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
