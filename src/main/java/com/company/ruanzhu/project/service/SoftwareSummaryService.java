package com.company.ruanzhu.project.service;

import com.company.ruanzhu.project.model.dto.SoftwareSummaryUpdateRequest;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;

public interface SoftwareSummaryService {
    SoftwareSummaryVO getByProjectId(Long projectId);
    SoftwareSummaryVO updateSummary(Long projectId, SoftwareSummaryUpdateRequest request);
    void updateCodeLines(Long projectId, Integer codeLines);
}
