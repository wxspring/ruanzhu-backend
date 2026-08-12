package com.company.ruanzhu.project.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.project.enums.ProjectStatus;
import com.company.ruanzhu.project.model.Project;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.model.dto.ProjectCreateRequest;
import com.company.ruanzhu.project.model.dto.ProjectUpdateRequest;
import com.company.ruanzhu.project.repository.ProjectRepository;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private SoftwareSummaryRepository softwareSummaryRepository;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private Project testProject;

    @BeforeEach
    void setUp() {
        testProject = new Project();
        testProject.setId(1L);
        testProject.setName("测试项目");
        testProject.setCustomerName("测试客户");
        testProject.setStatus(ProjectStatus.CREATED);
        testProject.setCreatedBy(100L);
        testProject.setCreatedAt(LocalDateTime.now());
        testProject.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void createProject_Success() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("新项目");
        request.setCustomerName("客户A");
        request.setVersion("1.0");
        request.setLanguage("Java");

        when(projectRepository.insert(any(Project.class))).thenAnswer(invocation -> {
            Project p = invocation.getArgument(0);
            p.setId(1L);
            return 1;
        });
        when(softwareSummaryRepository.insert(any(SoftwareSummary.class))).thenReturn(1);

        var result = projectService.createProject(request, 100L);

        assertNotNull(result);
        assertEquals("新项目", result.getName());
        assertEquals("客户A", result.getCustomerName());
        assertEquals(ProjectStatus.CREATED, result.getStatus());
        assertNotNull(result.getSoftwareSummary());
        assertEquals("1.0", result.getSoftwareSummary().getVersion());
        assertEquals("Java", result.getSoftwareSummary().getLanguage());
        verify(projectRepository).insert(any(Project.class));
        verify(softwareSummaryRepository).insert(any(SoftwareSummary.class));
    }

    @Test
    void getProjectById_Success() {
        when(projectRepository.selectById(1L)).thenReturn(testProject);
        when(softwareSummaryRepository.findByProjectId(1L)).thenReturn(Optional.empty());

        var result = projectService.getProjectById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试项目", result.getName());
        assertEquals(ProjectStatus.CREATED, result.getStatus());
    }

    @Test
    void getProjectById_NotFound_ThrowsException() {
        when(projectRepository.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> projectService.getProjectById(999L));
    }

    @Test
    void listProjects_Success() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(1);
        pageRequest.setSize(10);

        Page<Project> page = new Page<>(1, 10);
        page.setRecords(List.of(testProject));
        page.setTotal(1);

        when(projectRepository.selectPage(any(Page.class), any())).thenReturn(page);

        var result = projectService.listProjects(100L, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("测试项目", result.getList().get(0).getName());
    }

    @Test
    void listProjects_WithoutUserId_ReturnsAll() {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(1);
        pageRequest.setSize(10);

        Page<Project> page = new Page<>(1, 10);
        page.setRecords(List.of(testProject));
        page.setTotal(1);

        when(projectRepository.selectPage(any(Page.class), any())).thenReturn(page);

        var result = projectService.listProjects(null, pageRequest);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
    }

    @Test
    void updateProjectStatus_Success() {
        when(projectRepository.selectById(1L)).thenReturn(testProject);
        when(projectRepository.updateById(any(Project.class))).thenReturn(1);

        assertDoesNotThrow(() -> projectService.updateProjectStatus(1L, ProjectStatus.SUMMARY_CONFIRMING));

        assertEquals(ProjectStatus.SUMMARY_CONFIRMING, testProject.getStatus());
        verify(projectRepository).updateById(testProject);
    }

    @Test
    void updateProjectStatus_NotFound_ThrowsException() {
        when(projectRepository.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> projectService.updateProjectStatus(999L, ProjectStatus.GENERATING));
    }

    @Test
    void updateProject_Success() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("更新名称");
        request.setCustomerName("新客户");

        when(projectRepository.selectById(1L)).thenReturn(testProject);
        when(projectRepository.updateById(any(Project.class))).thenReturn(1);

        var result = projectService.updateProject(1L, request);

        assertNotNull(result);
        assertEquals("更新名称", result.getName());
        assertEquals("新客户", result.getCustomerName());
    }

    @Test
    void updateProject_NotFound_ThrowsException() {
        ProjectUpdateRequest request = new ProjectUpdateRequest();
        request.setName("更新名称");

        when(projectRepository.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> projectService.updateProject(999L, request));
    }

    @Test
    void deleteProject_Success() {
        when(projectRepository.selectById(1L)).thenReturn(testProject);
        when(projectRepository.deleteById(1L)).thenReturn(1);

        assertDoesNotThrow(() -> projectService.deleteProject(1L));

        verify(projectRepository).deleteById(1L);
    }

    @Test
    void deleteProject_NotFound_ThrowsException() {
        when(projectRepository.selectById(999L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> projectService.deleteProject(999L));
    }
}
