package com.company.ruanzhu.generate.service;

import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;

/**
 * Service for generating software summary content using AI.
 */
public interface AiContentService {

    /**
     * Generate development purpose (开发目的) for the software.
     * Should be 300-500 characters.
     */
    String generatePurpose(String softwareName, String category, String codeAnalysis);

    /**
     * Generate target domain (面向领域) for the software.
     * Should be 200-300 characters.
     */
    String generateTargetDomain(String softwareName, String category, String codeAnalysis);

    /**
     * Generate main functions description (主要功能) for the software.
     * Should be 800-1000 characters, describing each function's role and relationships.
     */
    String generateMainFunctions(String softwareName, String category, String codeAnalysis);

    /**
     * Generate technical features (技术特点) for the software.
     * Should be 150-200 characters.
     */
    String generateTechFeatures(String softwareName, String language, String frameworks, String codeAnalysis);

    /**
     * Generate all summary fields at once.
     */
    SoftwareSummaryVO generateAllFields(String softwareName, String category, String language,
                                         String frameworks, String codeAnalysis);
}
