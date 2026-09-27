package com.company.ruanzhu.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.project.enums.ProjectStatus;
import com.company.ruanzhu.project.model.Project;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.model.dto.ProjectCreateRequest;
import com.company.ruanzhu.project.model.dto.ProjectUpdateRequest;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.repository.ProjectRepository;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import com.company.ruanzhu.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final SoftwareSummaryRepository softwareSummaryRepository;

    @Override
    @Transactional
    public ProjectVO createProject(ProjectCreateRequest request, Long userId) {
        Project project = new Project();
        project.setName(request.getName());
        project.setCustomerName(request.getCustomerName());
        project.setStatus(ProjectStatus.CREATED);
        project.setCreatedBy(userId);

        projectRepository.insert(project);

        // Create an empty SoftwareSummary for the project
        SoftwareSummary summary = new SoftwareSummary();
        summary.setProjectId(project.getId());
        summary.setVersion(request.getVersion());
        summary.setCategory(request.getCategory());
        summary.setDevHardware(request.getDevHardware());
        summary.setRunHardware(request.getRunHardware());
        summary.setDevOs(request.getDevOs());
        summary.setDevTools(request.getDevTools());
        summary.setRunPlatform(request.getRunPlatform());
        summary.setRunSupport(request.getRunSupport());
        summary.setLanguage(request.getLanguage());
        summary.setPurpose(request.getPurpose());
        summary.setTargetDomain(request.getTargetDomain());
        summary.setMainFunctions(request.getMainFunctions());
        summary.setTechFeatures(request.getTechFeatures());
        summary.setCreatedAt(LocalDateTime.now());
        summary.setUpdatedAt(LocalDateTime.now());

        softwareSummaryRepository.insert(summary);

        ProjectVO vo = toVO(project);
        vo.setSoftwareSummary(toSummaryVO(summary));
        return vo;
    }

    @Override
    @Transactional
    public ProjectVO updateProject(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getCustomerName() != null) {
            project.setCustomerName(request.getCustomerName());
        }

        projectRepository.updateById(project);
        return toVO(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        projectRepository.deleteById(id);
    }

    @Override
    public ProjectVO getProjectById(Long id) {
        Project project = projectRepository.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        ProjectVO vo = toVO(project);
        softwareSummaryRepository.findByProjectId(id)
                .ifPresent(summary -> vo.setSoftwareSummary(toSummaryVO(summary)));
        return vo;
    }

    @Override
    public PageResult<ProjectVO> listProjects(Long userId, PageRequest pageRequest) {
        Page<Project> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        LambdaQueryWrapper<Project> wrapper = new LambdaQueryWrapper<>();

        if (userId != null) {
            wrapper.eq(Project::getCreatedBy, userId);
        }
        wrapper.orderByDesc(Project::getCreatedAt);

        Page<Project> result = projectRepository.selectPage(page, wrapper);
        return PageResult.of(
                result.getRecords().stream().map(this::toVO).toList(),
                result.getTotal(),
                pageRequest.getPage(),
                pageRequest.getSize()
        );
    }

    @Override
    @Transactional
    public void updateProjectStatus(Long id, ProjectStatus status) {
        Project project = projectRepository.selectById(id);
        if (project == null) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        project.setStatus(status);
        projectRepository.updateById(project);
    }

    private ProjectVO toVO(Project project) {
        ProjectVO vo = new ProjectVO();
        vo.setId(project.getId());
        vo.setName(project.getName());
        vo.setCustomerName(project.getCustomerName());
        vo.setStatus(project.getStatus());
        vo.setCreatedAt(project.getCreatedAt());
        vo.setUpdatedAt(project.getUpdatedAt());
        return vo;
    }

    private SoftwareSummaryVO toSummaryVO(SoftwareSummary summary) {
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
