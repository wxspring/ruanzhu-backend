package com.company.ruanzhu.project.service;

import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.project.enums.ProjectStatus;
import com.company.ruanzhu.project.model.dto.ProjectCreateRequest;
import com.company.ruanzhu.project.model.dto.ProjectUpdateRequest;
import com.company.ruanzhu.project.model.vo.ProjectVO;

public interface ProjectService {
    ProjectVO createProject(ProjectCreateRequest request, Long userId);
    ProjectVO updateProject(Long id, ProjectUpdateRequest request);
    void deleteProject(Long id);
    ProjectVO getProjectById(Long id);
    PageResult<ProjectVO> listProjects(Long userId, PageRequest pageRequest);
    void updateProjectStatus(Long id, ProjectStatus status);
}
