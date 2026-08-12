package com.company.ruanzhu.project.controller;

import com.company.ruanzhu.common.model.Result;
import com.company.ruanzhu.common.util.PageRequest;
import com.company.ruanzhu.common.util.PageResult;
import com.company.ruanzhu.file.model.vo.FileRecordVO;
import com.company.ruanzhu.file.service.FileService;
import com.company.ruanzhu.project.model.dto.ProjectCreateRequest;
import com.company.ruanzhu.project.model.dto.ProjectUpdateRequest;
import com.company.ruanzhu.project.model.dto.SoftwareSummaryUpdateRequest;
import com.company.ruanzhu.project.model.vo.ProjectVO;
import com.company.ruanzhu.project.model.vo.SoftwareSummaryVO;
import com.company.ruanzhu.project.service.ProjectService;
import com.company.ruanzhu.project.service.SoftwareSummaryService;
import com.company.ruanzhu.user.enums.UserRole;
import com.company.ruanzhu.user.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final SoftwareSummaryService softwareSummaryService;
    private final FileService fileService;

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
}
