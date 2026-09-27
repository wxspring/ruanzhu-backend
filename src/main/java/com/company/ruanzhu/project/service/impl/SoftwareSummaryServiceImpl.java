package com.company.ruanzhu.project.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.model.dto.SoftwareSummaryUpdateRequest;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import com.company.ruanzhu.project.service.SoftwareSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SoftwareSummaryServiceImpl implements SoftwareSummaryService {

    private final SoftwareSummaryRepository softwareSummaryRepository;

    @Override
    public SoftwareSummaryVO getByProjectId(Long projectId) {
        SoftwareSummary summary = softwareSummaryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUMMARY_NOT_FOUND));
        return toVO(summary);
    }

    @Override
    @Transactional
    public SoftwareSummaryVO updateSummary(Long projectId, SoftwareSummaryUpdateRequest request) {
        SoftwareSummary summary = softwareSummaryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUMMARY_NOT_FOUND));

        if (request.getVersion() != null) {
            summary.setVersion(request.getVersion());
        }
        if (request.getCategory() != null) {
            summary.setCategory(request.getCategory());
        }
        if (request.getDevHardware() != null) {
            summary.setDevHardware(request.getDevHardware());
        }
        if (request.getRunHardware() != null) {
            summary.setRunHardware(request.getRunHardware());
        }
        if (request.getDevOs() != null) {
            summary.setDevOs(request.getDevOs());
        }
        if (request.getDevTools() != null) {
            summary.setDevTools(request.getDevTools());
        }
        if (request.getRunPlatform() != null) {
            summary.setRunPlatform(request.getRunPlatform());
        }
        if (request.getRunSupport() != null) {
            summary.setRunSupport(request.getRunSupport());
        }
        if (request.getLanguage() != null) {
            summary.setLanguage(request.getLanguage());
        }
        if (request.getPurpose() != null) {
            summary.setPurpose(request.getPurpose());
        }
        if (request.getTargetDomain() != null) {
            summary.setTargetDomain(request.getTargetDomain());
        }
        if (request.getMainFunctions() != null) {
            summary.setMainFunctions(request.getMainFunctions());
        }
        if (request.getTechFeatures() != null) {
            summary.setTechFeatures(request.getTechFeatures());
        }
        if (request.getTechFeatureOptions() != null) {
            summary.setTechFeatureOptions(request.getTechFeatureOptions());
        }
        if (request.getCodeLines() != null) {
            summary.setCodeLines(request.getCodeLines());
        }
        if (request.getSystemOverview() != null) {
            summary.setSystemOverview(request.getSystemOverview());
        }
        if (request.getFunctionalFeatures() != null) {
            summary.setFunctionalFeatures(request.getFunctionalFeatures());
        }
        if (request.getFunctionMenu() != null) {
            summary.setFunctionMenu(request.getFunctionMenu());
        }

        summary.setUpdatedAt(LocalDateTime.now());
        softwareSummaryRepository.updateById(summary);
        return toVO(summary);
    }

    @Override
    @Transactional
    public void updateCodeLines(Long projectId, Integer codeLines) {
        SoftwareSummary summary = softwareSummaryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SUMMARY_NOT_FOUND));
        summary.setCodeLines(codeLines);
        summary.setUpdatedAt(LocalDateTime.now());
        softwareSummaryRepository.updateById(summary);
    }

    private SoftwareSummaryVO toVO(SoftwareSummary summary) {
        SoftwareSummaryVO vo = new SoftwareSummaryVO();
        vo.setId(summary.getId());
        vo.setProjectId(summary.getProjectId());
        vo.setVersion(summary.getVersion());
        vo.setCategory(summary.getCategory());
        vo.setDevHardware(summary.getDevHardware());
        vo.setRunHardware(summary.getRunHardware());
        vo.setDevOs(summary.getDevOs());
        vo.setDevTools(summary.getDevTools());
        vo.setRunPlatform(summary.getRunPlatform());
        vo.setRunSupport(summary.getRunSupport());
        vo.setLanguage(summary.getLanguage());
        vo.setCodeLines(summary.getCodeLines());
        vo.setPurpose(summary.getPurpose());
        vo.setTargetDomain(summary.getTargetDomain());
        vo.setMainFunctions(summary.getMainFunctions());
        vo.setTechFeatures(summary.getTechFeatures());
        vo.setTechFeatureOptions(summary.getTechFeatureOptions());
        vo.setSystemOverview(summary.getSystemOverview());
        vo.setFunctionalFeatures(summary.getFunctionalFeatures());
        vo.setFunctionMenu(summary.getFunctionMenu());
        return vo;
    }
}
