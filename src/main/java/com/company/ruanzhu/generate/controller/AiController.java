package com.company.ruanzhu.generate.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;
import com.company.ruanzhu.generate.service.AiContentService;
import com.company.ruanzhu.generate.service.CodeAnalysisService;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.user.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for AI-powered content generation.
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiContentService aiContentService;
    private final CodeAnalysisService codeAnalysisService;
    private final ProjectService projectService;

    /**
     * Generate all summary fields for a project using AI.
     */
    @PostMapping("/projects/{projectId}/generate-summary")
    public Result<SoftwareSummaryVO> generateSummary(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserPrincipal principal) {

        // Get project info
        ProjectVO project = projectService.getProjectById(projectId);

        // Analyze code to get context
        String codeAnalysis = "";
        try {
            CodeAnalysisResult analysis = codeAnalysisService.analyzeProject(projectId);
            codeAnalysis = String.format("编程语言: %s, 总行数: %d, 框架: %s",
                    analysis.getLanguage(), analysis.getTotalLines(), analysis.getFrameworks());
        } catch (Exception e) {
            codeAnalysis = "代码分析不可用";
        }

        // Generate all fields
        SoftwareSummaryVO vo = aiContentService.generateAllFields(
                project.getName(),
                project.getSoftwareSummary() != null ? project.getSoftwareSummary().getCategory() : "应用软件",
                project.getSoftwareSummary() != null ? project.getSoftwareSummary().getLanguage() : "Java",
                "",
                codeAnalysis
        );

        return Result.success(vo);
    }

    /**
     * Generate a single field using AI.
     */
    @PostMapping("/projects/{projectId}/generate-field")
    public Result<String> generateField(
            @PathVariable Long projectId,
            @RequestParam String field,
            @AuthenticationPrincipal UserPrincipal principal) {

        ProjectVO project = projectService.getProjectById(projectId);

        String codeAnalysis = "";
        try {
            CodeAnalysisResult analysis = codeAnalysisService.analyzeProject(projectId);
            codeAnalysis = String.format("编程语言: %s, 总行数: %d, 框架: %s",
                    analysis.getLanguage(), analysis.getTotalLines(), analysis.getFrameworks());
        } catch (Exception e) {
            codeAnalysis = "代码分析不可用";
        }

        String result = switch (field) {
            case "purpose" -> aiContentService.generatePurpose(
                    project.getName(),
                    project.getSoftwareSummary() != null ? project.getSoftwareSummary().getCategory() : "应用软件",
                    codeAnalysis);
            case "targetDomain" -> aiContentService.generateTargetDomain(
                    project.getName(),
                    project.getSoftwareSummary() != null ? project.getSoftwareSummary().getCategory() : "应用软件",
                    codeAnalysis);
            case "mainFunctions" -> aiContentService.generateMainFunctions(
                    project.getName(),
                    project.getSoftwareSummary() != null ? project.getSoftwareSummary().getCategory() : "应用软件",
                    codeAnalysis);
            case "techFeatures" -> aiContentService.generateTechFeatures(
                    project.getName(),
                    project.getSoftwareSummary() != null ? project.getSoftwareSummary().getLanguage() : "Java",
                    "",
                    codeAnalysis);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };

        return Result.success(result);
    }
}
