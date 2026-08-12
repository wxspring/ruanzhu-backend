package com.company.ruanzhu.generate.service;

/**
 * Service for expanding code to meet software copyright requirements.
 * Target: 3000+ lines (60 pages, 50+ lines per page)
 */
public interface CodeExpansionService {

    /**
     * Expand the seed code for a project to meet line count requirements.
     *
     * @param projectId the project ID
     * @return the expanded code content (concatenated source files)
     */
    String expandCode(Long projectId);

    /**
     * Check if the code meets the minimum line requirement.
     *
     * @param projectId the project ID
     * @return true if code has 3000+ lines
     */
    boolean meetsLineRequirement(Long projectId);

    /**
     * Get the current line count of the project's code.
     */
    int getCurrentLineCount(Long projectId);
}
