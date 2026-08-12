package com.company.ruanzhu.generate.ai;

/**
 * Pluggable AI client interface for content generation.
 */
public interface AiClient {

    /**
     * Generate text content based on a prompt.
     *
     * @param prompt the prompt/instruction for the AI
     * @return generated text content
     */
    String generate(String prompt);

    /**
     * Generate text content with system and user messages.
     *
     * @param systemPrompt system-level instructions
     * @param userPrompt user-level input
     * @return generated text content
     */
    String generate(String systemPrompt, String userPrompt);

    /**
     * Get the name/identifier of this AI client.
     */
    String getName();
}
