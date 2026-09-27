package com.company.ruanzhu.project.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.file.model.FileRecord;
import com.company.ruanzhu.file.model.vo.FileRecordVO;
import com.company.ruanzhu.file.repository.FileRecordRepository;
import com.company.ruanzhu.file.service.ExportService;
import com.company.ruanzhu.file.service.FileService;
import com.company.ruanzhu.file.storage.StorageClient;
import com.company.ruanzhu.generate.model.vo.CodeAnalysisResult;
import com.company.ruanzhu.generate.service.CodeAnalysisService;
import com.company.ruanzhu.project.model.dto.EditorPayloadSaveRequest;
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

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
    private final StorageClient storageClient;

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
        String fn = URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        String disposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s", fn, fn);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(data.length)
                .body(data);
    }

    /**
     * Export all materials for a project as a ZIP file.
     *
     * @param depositType 交存方式：general（一般交存，默认）/ exceptional（例外交存，暂未实现）
     */
    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportProject(@PathVariable Long id,
                                                @RequestParam(defaultValue = "general") String depositType,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        try {
            byte[] zipBytes = exportService.exportProject(id, depositType);
            String fileName = exportService.getZipFileName(id);
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            // RFC 5987: 同时保留 filename*（UTF-8 首选）与 filename（ASCII 回退），兼容各浏览器
            String disposition = String.format(
                    "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                    encodedName, encodedName);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(zipBytes);
        } catch (BusinessException e) {
            // 例外交存未支持等业务异常直接抛出，保留原始错误信息
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Load the latest generated program document (GENERATED_CODE) for inline editing.
     */
    @GetMapping("/{id}/code")
    public Result<FileRecordVO> getProjectCode(@PathVariable Long id,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(loadEditorPayload(id, "GENERATED_CODE"));
    }

    /**
     * Save (create or update) the generated program document content.
     */
    @PutMapping("/{id}/code")
    public Result<FileRecordVO> updateProjectCode(@PathVariable Long id,
                                                  @Valid @RequestBody EditorPayloadSaveRequest request,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(saveEditorPayload(id, "GENERATED_CODE", "code", "text/plain", request));
    }

    /**
     * Load the latest generated manual (MANUAL) for inline editing.
     */
    @GetMapping("/{id}/manual")
    public Result<FileRecordVO> getProjectManual(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(loadEditorPayload(id, "MANUAL"));
    }

    /**
     * Save (create or update) the manual HTML content.
     */
    @PutMapping("/{id}/manual")
    public Result<FileRecordVO> updateProjectManual(@PathVariable Long id,
                                                    @Valid @RequestBody EditorPayloadSaveRequest request,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(saveEditorPayload(id, "MANUAL", "manual", "text/html", request));
    }

    /**
     * Load the latest file record of the given type and attach its text content.
     */
    private FileRecordVO loadEditorPayload(Long projectId, String fileType) {
        projectService.getProjectById(projectId);
        List<FileRecord> records = fileRecordRepository.findByProjectIdAndFileType(projectId, fileType);
        if (records.isEmpty()) {
            FileRecordVO empty = new FileRecordVO();
            empty.setProjectId(projectId);
            empty.setFileType(fileType);
            return empty;
        }
        FileRecord record = records.get(0);
        byte[] data = fileService.downloadFile(record.getStoragePath());
        String content = new String(data, StandardCharsets.UTF_8);
        FileRecordVO vo = toVO(record);
        vo.setContent(content);
        return vo;
    }

    /**
     * Persist editor content to storage and upsert the file record.
     */
    private FileRecordVO saveEditorPayload(Long projectId, String fileType, String subDir,
                                           String contentType, EditorPayloadSaveRequest request) {
        projectService.getProjectById(projectId);
        String content = request.getContent();
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        String fileName = (request.getFileName() == null || request.getFileName().isBlank())
                ? defaultFileName(projectId, fileType)
                : request.getFileName();
        String storagePath = "projects/" + projectId + "/" + subDir + "/" + defaultFileName(projectId, fileType);

        storageClient.upload(storagePath, new ByteArrayInputStream(contentBytes), contentType);

        List<FileRecord> existing = fileRecordRepository.findByProjectIdAndFileType(projectId, fileType);
        FileRecord record;
        if (!existing.isEmpty()) {
            record = existing.get(0);
            record.setFileName(fileName);
            record.setStoragePath(storagePath);
            record.setFileSize((long) contentBytes.length);
            record.setVersion((record.getVersion() == null ? 0 : record.getVersion()) + 1);
            fileRecordRepository.updateById(record);
        } else {
            record = new FileRecord();
            record.setProjectId(projectId);
            record.setFileType(fileType);
            record.setFileName(fileName);
            record.setStoragePath(storagePath);
            record.setFileSize((long) contentBytes.length);
            record.setVersion(1);
            fileRecordRepository.insert(record);
        }
        FileRecordVO vo = toVO(record);
        vo.setContent(content);
        return vo;
    }

    private String defaultFileName(Long projectId, String fileType) {
        return "MANUAL".equals(fileType)
                ? "manual_" + projectId + ".html"
                : "generated_code_" + projectId + ".txt";
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
