package com.company.ruanzhu.generate.service;

/**
 * Service for generating operation manual content.
 */
public interface ManualGenerationService {

    /**
     * Generate operation manual HTML content based on project information.
     *
     * @param projectId Project ID
     * @return HTML content of the operation manual
     */
    String generateManual(Long projectId);
}
