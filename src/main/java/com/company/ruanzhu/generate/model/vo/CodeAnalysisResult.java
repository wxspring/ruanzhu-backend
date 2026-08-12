package com.company.ruanzhu.generate.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of seed-code analysis: line counts, language detection, framework detection,
 * directory structure and file-extension distribution.
 */
@Data
public class CodeAnalysisResult {

    /** Total lines of code across all source files. */
    private int totalLines;

    /** Number of source files analysed. */
    private int totalFiles;

    /** Detected primary language (Java, JavaScript, Python, etc.). */
    private String language;

    /** Detected frameworks (Spring Boot, React, etc.). */
    private List<String> frameworks = new ArrayList<>();

    /** Directory -> file count. */
    private Map<String, Integer> directoryStructure = new HashMap<>();

    /** Extension -> file count. */
    private Map<String, Integer> fileExtensions = new HashMap<>();
}
