package com.company.ruanzhu.generate.analyzer;

import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;

/**
 * Analyses a directory tree of seed source code.
 */
public interface CodeAnalyzer {

    /**
     * Walk the directory tree, count lines, detect languages and frameworks.
     *
     * @param seedCodePath the directory where the seed-code ZIP was extracted
     * @return analysis result
     */
    CodeAnalysisResult analyze(String seedCodePath);
}
