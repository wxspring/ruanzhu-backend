package com.company.ruanzhu.file.generator;

/**
 * Interface for generating documents.
 */
public interface DocumentGenerator {

    /**
     * Generate a Word document and return its bytes.
     *
     * @return the generated document as bytes
     */
    byte[] generateWord();

    /**
     * Get the file name for this document.
     */
    String getFileName();
}
