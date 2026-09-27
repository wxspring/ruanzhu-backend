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
     * Generate a PDF document directly from source content (no DOCX→PDF conversion).
     *
     * @return the generated PDF as bytes
     */
    byte[] generatePdf();

    /**
     * Get the file name for this document.
     */
    String getFileName();
}
