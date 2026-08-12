package com.company.ruanzhu.project.service.impl;

import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.project.model.SoftwareSummary;
import com.company.ruanzhu.project.model.dto.SoftwareSummaryUpdateRequest;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.repository.SoftwareSummaryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SoftwareSummaryServiceImplTest {

    @Mock
    private SoftwareSummaryRepository softwareSummaryRepository;

    @InjectMocks
    private SoftwareSummaryServiceImpl softwareSummaryService;

    private SoftwareSummary testSummary;

    @BeforeEach
    void setUp() {
        testSummary = new SoftwareSummary();
        testSummary.setId(1L);
        testSummary.setProjectId(100L);
        testSummary.setVersion("1.0");
        testSummary.setCategory("工具软件");
        testSummary.setDevHardware("Intel i7");
        testSummary.setRunHardware("Intel i5");
        testSummary.setDevOs("Windows 11");
        testSummary.setDevTools("IntelliJ IDEA");
        testSummary.setRunPlatform("Windows");
        testSummary.setRunSupport("无");
        testSummary.setLanguage("Java");
        testSummary.setCodeLines(5000);
        testSummary.setPurpose("项目管理");
        testSummary.setTargetDomain("企业");
        testSummary.setMainFunctions("任务分配、进度跟踪");
        testSummary.setTechFeatures("微服务架构");
        testSummary.setTechFeatureOptions("支持插件扩展");
        testSummary.setCreatedAt(LocalDateTime.now());
        testSummary.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getByProjectId_Success() {
        when(softwareSummaryRepository.findByProjectId(100L)).thenReturn(Optional.of(testSummary));

        SoftwareSummaryVO result = softwareSummaryService.getByProjectId(100L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getProjectId());
        assertEquals("1.0", result.getVersion());
        assertEquals("工具软件", result.getCategory());
        assertEquals("Intel i7", result.getDevHardware());
        assertEquals("Intel i5", result.getRunHardware());
        assertEquals("Windows 11", result.getDevOs());
        assertEquals("IntelliJ IDEA", result.getDevTools());
        assertEquals("Windows", result.getRunPlatform());
        assertEquals("无", result.getRunSupport());
        assertEquals("Java", result.getLanguage());
        assertEquals(5000, result.getCodeLines());
        assertEquals("项目管理", result.getPurpose());
        assertEquals("企业", result.getTargetDomain());
        assertEquals("任务分配、进度跟踪", result.getMainFunctions());
        assertEquals("微服务架构", result.getTechFeatures());
        assertEquals("支持插件扩展", result.getTechFeatureOptions());
        verify(softwareSummaryRepository).findByProjectId(100L);
    }

    @Test
    void getByProjectId_NotFound_ThrowsException() {
        when(softwareSummaryRepository.findByProjectId(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> softwareSummaryService.getByProjectId(999L));
        verify(softwareSummaryRepository).findByProjectId(999L);
    }

    @Test
    void updateSummary_Success() {
        SoftwareSummaryUpdateRequest request = new SoftwareSummaryUpdateRequest();
        request.setVersion("2.0");
        request.setLanguage("Python");
        request.setPurpose("数据分析");

        when(softwareSummaryRepository.findByProjectId(100L)).thenReturn(Optional.of(testSummary));
        when(softwareSummaryRepository.updateById(any(SoftwareSummary.class))).thenReturn(1);

        SoftwareSummaryVO result = softwareSummaryService.updateSummary(100L, request);

        assertNotNull(result);
        assertEquals("2.0", result.getVersion());
        assertEquals("Python", result.getLanguage());
        assertEquals("数据分析", result.getPurpose());
        // Unchanged fields should remain the same
        assertEquals("工具软件", result.getCategory());
        assertEquals("Intel i7", result.getDevHardware());
        assertEquals(5000, result.getCodeLines());
        verify(softwareSummaryRepository).findByProjectId(100L);
        verify(softwareSummaryRepository).updateById(any(SoftwareSummary.class));
    }

    @Test
    void updateSummary_PartialUpdate_OnlyUpdatesProvidedFields() {
        SoftwareSummaryUpdateRequest request = new SoftwareSummaryUpdateRequest();
        request.setTechFeatureOptions("支持多语言");

        when(softwareSummaryRepository.findByProjectId(100L)).thenReturn(Optional.of(testSummary));
        when(softwareSummaryRepository.updateById(any(SoftwareSummary.class))).thenReturn(1);

        SoftwareSummaryVO result = softwareSummaryService.updateSummary(100L, request);

        assertNotNull(result);
        assertEquals("支持多语言", result.getTechFeatureOptions());
        // All other fields should remain unchanged
        assertEquals("1.0", result.getVersion());
        assertEquals("Java", result.getLanguage());
        verify(softwareSummaryRepository).updateById(any(SoftwareSummary.class));
    }

    @Test
    void updateSummary_NotFound_ThrowsException() {
        SoftwareSummaryUpdateRequest request = new SoftwareSummaryUpdateRequest();
        request.setVersion("2.0");

        when(softwareSummaryRepository.findByProjectId(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> softwareSummaryService.updateSummary(999L, request));
        verify(softwareSummaryRepository, never()).updateById(any());
    }

    @Test
    void updateCodeLines_Success() {
        when(softwareSummaryRepository.findByProjectId(100L)).thenReturn(Optional.of(testSummary));
        when(softwareSummaryRepository.updateById(any(SoftwareSummary.class))).thenReturn(1);

        assertDoesNotThrow(() -> softwareSummaryService.updateCodeLines(100L, 10000));

        assertEquals(10000, testSummary.getCodeLines());
        verify(softwareSummaryRepository).findByProjectId(100L);
        verify(softwareSummaryRepository).updateById(any(SoftwareSummary.class));
    }

    @Test
    void updateCodeLines_NotFound_ThrowsException() {
        when(softwareSummaryRepository.findByProjectId(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> softwareSummaryService.updateCodeLines(999L, 10000));
        verify(softwareSummaryRepository, never()).updateById(any());
    }
}
