package com.company.ruanzhu.generate.service;

import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;

/**
 * Service for analysing seed code of a project.
 */
public interface CodeAnalysisService {

    /**
     * Analyse the seed code ZIP for the given project.
     * <p>
     * Extracts the latest uploaded ZIP to a temporary directory, runs the {@code CodeAnalyzer},
     * updates the project's {@code SoftwareSummary} with the detected language and code-line
     * count, and returns the full analysis result.
     *
     * @param projectId the project to analyse
     * @return analysis result
     */
    CodeAnalysisResult analyzeProject(Long projectId);
}
