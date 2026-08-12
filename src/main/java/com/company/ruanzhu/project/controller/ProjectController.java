package com.company.ruanzhu.project.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.model.vo.FileRecordVO;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.service.ExportService;
import com.company.ruanzhu.file.service.FileService;
import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;
import com.company.ruanzhu.generate.service.CodeAnalysisService;
import com.company.ruanzhu.project.model.dto.ProjectCreateRequest;
import com.company.ruanzhu.project.model.dto.ProjectUpdateRequest;
import com.company.ruanzhu.project.model.dto.SoftwareSummaryUpdateRequest;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import com.company.ruanzhu.project.service.SoftwareSummaryService;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.security.UserPrincipal;
import com.company.ruanzhu.common.exception.BusinessException;
import com.company.ruanzhu.common.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SoftwareSummaryService softwareSummaryService;
    private final FileService fileService;
    private final ExportService exportService;
    private final CodeAnalysisService codeAnalysisService;
    private final FileRecordRepository fileRecordRepository;

    @PostMapping
    public Result<ProjectVO> createProject(@Valid @RequestBody ProjectCreateRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        ProjectVO vo = projectService.createProject(request, principal.getId());
        return Result.success(vo);
    }

    @PutMapping("/{id}")
    public Result<ProjectVO> updateProject(@PathVariable Long id,
                                           @Valid @RequestBody ProjectUpdateRequest request,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        ProjectVO vo = projectService.updateProject(id, request);
        return Result.success(vo);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        projectService.deleteProject(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> getProject(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        ProjectVO vo = projectService.getProjectById(id);
        return Result.success(vo);
    }

    @GetMapping
    public Result<PageResult<ProjectVO>> listProjects(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid PageRequest pageRequest) {
        // STAFF only sees own projects; ADMIN sees all.
        Long userId = (principal.getRole() == UserRole.ADMIN) ? null : principal.getId();
        PageResult<ProjectVO> result = projectService.listProjects(userId, pageRequest);
        return Result.success(result);
    }

    @GetMapping("/{id}/summary")
    public Result<SoftwareSummaryVO> getSoftwareSummary(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        SoftwareSummaryVO vo = softwareSummaryService.getByProjectId(id);
        return Result.success(vo);
    }

    @PutMapping("/{id}/summary")
    public Result<SoftwareSummaryVO> updateSoftwareSummary(@PathVariable Long id,
                                                           @Valid @RequestBody SoftwareSummaryUpdateRequest request,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        SoftwareSummaryVO vo = softwareSummaryService.updateSummary(id, request);
        return Result.success(vo);
    }

    @PostMapping("/{id}/upload-code")
    public Result<FileRecordVO> uploadSeedCode(@PathVariable Long id,
                                               @RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        FileRecordVO vo = fileService.uploadSeedCode(id, file);
        return Result.success(vo);
    }

    @PostMapping("/{id}/analyze-code")
    public Result<CodeAnalysisResult> analyzeCode(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        CodeAnalysisResult result = codeAnalysisService.analyzeProject(id);
        return Result.success(result);
    }

    /**
     * List all file records belonging to a project.
     */
    @GetMapping("/{id}/files")
    public Result<List<FileRecordVO>> listFiles(@PathVariable Long id,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        // Ensure project exists
        projectService.getProjectById(id);
        List<FileRecord> records = fileRecordRepository.findByProjectId(id);
        List<FileRecordVO> vos = records.stream().map(this::toVO).toList();
        return Result.success(vos);
    }

    /**
     * Download a file belonging to a project, returned as an attachment.
     */
    @GetMapping("/{id}/files/{fileId}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id,
                                               @PathVariable Long fileId,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        FileRecord record = fileRecordRepository.selectById(fileId);
        if (record == null || !record.getProjectId().equals(id)) {
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        }
        byte[] data = fileService.downloadFile(record.getStoragePath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + record.getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }

    /**
     * Export all materials for a project as a ZIP file.
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportProject(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        try {
            byte[] zipBytes = exportService.exportProject(id);
            String fileName = exportService.getZipFileName(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipBytes);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private FileRecordVO toVO(FileRecord record) {
        FileRecordVO vo = new FileRecordVO();
        vo.setId(record.getId());
        vo.setProjectId(record.getProjectId());
        vo.setFileType(record.getFileType());
        vo.setFileName(record.getFileName());
        vo.setStoragePath(record.getStoragePath());
        vo.setFileSize(record.getFileSize());
        vo.setVersion(record.getVersion());
        vo.setCreatedAt(record.getCreatedAt());
        return vo;
    }
}
