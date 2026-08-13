# Frontend Phase 9-11 Design Specification

**Date:** 2026-08-13  
**Status:** Design Complete  
**Scope:** Complete Phase 9-11 frontend features (project management, editing, export & admin)

---

## Overview

This design completes the frontend implementation for the 软著材料 AI 生产系统. The frontend already has basic pages in place (Dashboard, ProjectCreate, ProjectDetail, ProjectEdit, UserManagement, Login), but several critical features are incomplete or placeholder-only.

### Current State

**What exists:**
- ✅ Basic Dashboard (project list with search/filter)
- ✅ Project Create page (basic form, missing file upload)
- ✅ Project Detail page (with export button)
- ✅ Project Edit page (summary tab complete, code/manual tabs are placeholders)
- ✅ User Management page (admin)
- ✅ Login page with JWT authentication
- ✅ Layout with sidebar navigation
- ✅ Auth store (Zustand) and axios interceptors

**What's missing:**
- ❌ Seed code file upload in ProjectCreate
- ❌ Monaco Editor integration for code editing (code tab is placeholder)
- ❌ TipTap editor integration for manual editing (manual tab is placeholder)
- ❌ File list display in ProjectDetail and ProjectEdit
- ❌ Generation task UI (trigger and monitor async tasks)
- ❌ Inline task status tracking
- ❌ Bug: ProjectCreate.tsx missing `Space` import

---

## Goals

1. **Enable seed code upload** - Users can upload ZIP files containing source code
2. **Integrate Monaco Editor** - Full code editing with syntax highlighting
3. **Integrate TipTap editor** - Rich text editing for operation manuals
4. **Add generation task UI** - Trigger AI generation and track progress inline
5. **Display file lists** - Show uploaded and generated files per project
6. **Fix bugs** - Resolve ProjectCreate Space import issue

---

## Architecture

### Pattern: Feature-based tabs with inline task status

```
ProjectEdit.tsx (parent)
├── Summary Tab (already working)
│   ├── Form fields for software summary
│   └── AI Generate button → calls /api/ai/projects/{id}/generate-summary
│
├── Code Tab (enhance)
│   ├── File list display (uploaded/generated files)
│   ├── Monaco Editor (view/edit code with syntax highlighting)
│   ├── Generation controls (trigger CODE task)
│   └── Inline task status (progress bar + logs)
│
└── Manual Tab (enhance)
    ├── TipTap Editor (rich text editor)
    ├── AI Generate button → triggers MANUAL task
    └── Inline task status (progress bar + logs)
```

### Technology Stack

- **React 18** + **TypeScript** + **Vite**
- **Ant Design 5** - UI components
- **Zustand** - State management
- **Monaco Editor** - Code editing (@monaco-editor/react)
- **TipTap** - Rich text editing (@tiptap/react, @tiptap/starter-kit)
- **axios** - HTTP client

### Data Flow

**Page Load:**
```
User navigates to /projects/:id/edit
  ↓
ProjectEdit.tsx mounts
  ↓
Parallel API calls:
  - GET /api/projects/{id} → project data + softwareSummary
  - GET /api/projects/{id}/files → file list
  - GET /api/tasks/projects/{id} → recent tasks (optional)
  ↓
Populate state and render tabs
```

**Code Generation:**
```
User clicks "Generate Code" in Code tab
  ↓
POST /api/tasks/projects/{id}/generate?taskType=CODE
  ↓
Backend creates GenerateTask (status=PENDING)
  ↓
Backend executes async:
  - Fetches seed code from MinIO
  - Analyzes code structure
  - Calls AI service to expand/generate code
  - Updates task progress (0% → 100%)
  - Saves generated files to MinIO
  - Creates FileRecord entries (fileType=GENERATED_CODE)
  ↓
Frontend polls task status every 2s:
  GET /api/tasks/{taskId}
  ↓
Task completes (status=SUCCESS):
  - TaskStatus component shows "生成完成"
  - Refresh file list
  - User can select and view generated files in Monaco
```

**Manual Generation:**
```
User clicks "AI Generate" in Manual tab
  ↓
POST /api/tasks/projects/{id}/generate?taskType=MANUAL
  ↓
Backend creates GenerateTask (status=PENDING)
  ↓
Backend executes async:
  - Fetches softwareSummary for project
  - Calls AI service with summary context
  - AI generates operation manual (HTML format)
  - Saves manual content to task.resultPath
  - Updates task progress (0% → 100%)
  ↓
Frontend polls task status every 2s:
  GET /api/tasks/{taskId}
  ↓
Task completes (status=SUCCESS):
  - TaskStatus component shows "生成完成"
  - Fetch generated content:
    GET /api/projects/{id}/files/{fileId}/download
  - TipTap editor loads HTML content
  - User can edit in rich text editor
  - User clicks "Save" to persist edits
```

**Seed Code Upload:**
```
User selects ZIP file in ProjectCreate page
  ↓
Form submits:
  POST /api/projects (JSON body)
  ↓
Project created, get projectId
  ↓
Upload seed code:
  POST /api/projects/{id}/upload-code
  Content-Type: multipart/form-data
  Body: file (ZIP)
  ↓
Backend:
  - Uploads ZIP to MinIO
  - Creates FileRecord (fileType=SEED_CODE)
  - Returns FileRecordVO
  ↓
Frontend shows success message + file list
```

---

## Component Design

### Component 1: CodeEditor.tsx

**Responsibilities:**
- Display list of code files for the project
- Let user select which file to view/edit
- Monaco Editor with syntax highlighting (language detection from file extension)
- Save button to persist edits
- "Generate Code" button → triggers CODE task
- Inline task status display

**Props:**
```typescript
interface CodeEditorProps {
  projectId: number
  files: FileRecord[]
  onRefreshFiles: () => void
}
```

**State:**
- `selectedFile: FileRecord | null` - currently viewed file
- `codeContent: string` - editor content
- `taskId: number | null` - active generation task
- `taskStatus: GenerateTaskVO | null` - polling result

**Monaco Configuration:**
- Auto-detect language from file extension (.java, .py, .js, etc.)
- Read-only mode for generated files (unless user explicitly edits)
- Minimap enabled, word wrap on

---

### Component 2: ManualEditor.tsx

**Responsibilities:**
- Rich text editor for operation manual
- Load existing manual content
- "AI Generate" button → triggers MANUAL task
- Inline task status display
- Save button to persist edits

**Props:**
```typescript
interface ManualEditorProps {
  projectId: number
  projectName: string
}
```

**State:**
- `content: string` - HTML content from TipTap
- `taskId: number | null` - active generation task
- `taskStatus: GenerateTaskVO | null` - polling result
- `saving: boolean` - save in progress

**TipTap Configuration:**
- Basic extensions: StarterKit, TextStyle, Color, TextAlign
- Toolbar: Bold, Italic, Underline, Headings (H1-H3), Lists, Links
- Placeholder text: "开始编写操作手册..."

---

### Component 3: TaskStatus.tsx

**Responsibilities:**
- Display task progress (pending/running/success/failed)
- Show progress bar (0-100%)
- Poll task status every 2 seconds while running
- Display error message if failed
- Auto-refresh parent component when task completes

**Props:**
```typescript
interface TaskStatusProps {
  taskId: number | null
  onComplete?: () => void
  onError?: (error: string) => void
}
```

**UI:**
- Pending: "任务排队中..." with spinner
- Running: Progress bar + "生成中 XX%"
- Success: Green checkmark + "生成完成"
- Failed: Red error icon + error message + retry button

---

### Component 4: FileList.tsx

**Responsibilities:**
- Display all files for a project (uploaded + generated)
- Show file type, name, size, upload time
- Download button for each file
- Delete button (with confirmation)

**Props:**
```typescript
interface FileListProps {
  projectId: number
  files: FileRecord[]
  onRefresh: () => void
}
```

**UI:**
- Ant Design Table with columns: 文件名, 类型, 大小, 上传时间, 操作
- File type tags: SEED_CODE (蓝色), GENERATED_CODE (绿色), MANUAL (紫色)
- Size formatting: KB/MB

---

## API Integration

### Endpoints Used (Already Implemented)

- `POST /api/projects` - create project
- `GET /api/projects/{id}` - get project
- `PUT /api/projects/{id}/summary` - save summary
- `POST /api/projects/{id}/upload-code` - upload seed code
- `GET /api/projects/{id}/files` - list files
- `POST /api/tasks/projects/{id}/generate?taskType=...` - start generation
- `GET /api/tasks/{taskId}` - get task status
- `GET /api/projects/{id}/files/{fileId}/download` - download file

### Endpoints May Need to Add

- `PUT /api/tasks/{taskId}/content` - save edited manual content (or store in FileRecord)
- `GET /api/tasks/{taskId}/result` - download generated manual HTML

---

## Error Handling

### Error Categories

**1. Network Errors (API failures)**
- Use Ant Design message component
- Handle 400 (bad request), 500 (server error), network failures
- axios interceptor already handles 401 → redirect to login

**2. Task Execution Errors**
- Display error message from task.errorMessage
- Show retry button for failed tasks
- Log error to console for debugging

**3. File Upload Errors**
- Validate file before upload (ZIP only, max 100MB)
- Show error message if upload fails
- Offer re-upload option

**4. Editor Errors**
- Monaco: Handle large files (>5MB) gracefully with warning
- TipTap: Handle HTML parsing errors with fallback
- Show error message + offer download for large files

**5. Validation Errors**
- Form validation (Ant Design handles automatically)
- Custom validation for AI-generated content length
- Show warning if content doesn't meet requirements

**6. Polling Errors**
- Don't stop polling on transient errors
- Log errors but don't spam user with messages
- Show stale status rather than error flood

### Error Recovery Strategies

- **Auto-retry:** Network errors retry up to 3 times with exponential backoff
- **User-initiated retry:** Show retry buttons for failed operations
- **Graceful degradation:** Fallback to plain textarea if editors fail
- **Optimistic updates:** Update UI immediately, rollback on failure

---

## Testing Approach

### Strategy: Manual + Smoke Tests

Since this is primarily UI work with backend integration, automated unit tests are less valuable than manual testing and smoke tests.

### Test Plan by Component

**1. ProjectCreate.tsx**
- Can create project without seed code upload
- Can create project with seed code upload (ZIP file)
- Validation: software name is required
- File validation: only ZIP files accepted, max 100MB
- Success message after creation + redirect to dashboard

**2. CodeEditor.tsx (Monaco)**
- Can view uploaded seed code files
- Can select different files from file list
- Monaco displays syntax highlighting correctly
- Can trigger code generation
- Task status shows progress (0% → 100%)
- Can view generated code files after task completes
- Can edit generated code
- Error handling: task failure shows error message

**3. ManualEditor.tsx (TipTap)**
- Can trigger AI manual generation
- Task status shows progress
- After generation, TipTap loads HTML content
- Can edit content with rich text toolbar
- Toolbar buttons work (bold, italic, headings, lists)
- Can save edited content
- Error handling: task failure shows error message

**4. TaskStatus.tsx**
- Pending state: Shows "任务排队中..."
- Running state: Shows progress bar with percentage
- Success state: Shows green checkmark + "生成完成"
- Failed state: Shows red error icon + error message
- Polling stops when task completes
- Retry button works for failed tasks

**5. FileList.tsx**
- Displays all files for project
- Shows file type tags (SEED_CODE, GENERATED_CODE, MANUAL)
- Shows file size in KB/MB
- Download button works
- Delete button works (with confirmation)
- Refreshes after upload/generation

### Integration Tests (End-to-End)

**Scenario 1: Complete workflow**
1. Create project with seed code upload
2. Edit software summary (manually or AI generate)
3. Generate code → wait for completion
4. Review generated code in Monaco editor
5. Generate manual → wait for completion
6. Edit manual in TipTap editor
7. Export project materials
8. Verify: ZIP file downloads with all materials

**Scenario 2: Task failure recovery**
1. Trigger code generation
2. (Mock backend to fail)
3. Verify: Task shows FAILED status + error message
4. Click "Retry" button
5. Verify: New task created and runs successfully

**Scenario 3: Concurrent tasks**
1. Start code generation task
2. While running, try to start manual generation
3. Verify: System allows multiple tasks (or blocks if not allowed)
4. Verify: Both task statuses display correctly

### What We're NOT Testing

- Unit tests for React components (low value for UI-heavy code)
- Backend API tests (assumed to exist)
- Monaco/TipTap internal behavior (third-party libraries)
- Performance tests (out of scope for Phase 9-11)

---

## Implementation Approach

### Approach: Incremental Enhancement

Fix bugs first, then add features one tab at a time.

**Phase 1: Bug Fixes & File Upload**
1. Fix ProjectCreate Space import bug
2. Add seed code file upload to ProjectCreate
3. Test file upload end-to-end

**Phase 2: Code Tab Enhancement**
1. Install Monaco Editor package (@monaco-editor/react)
2. Create CodeEditor component
3. Create FileList component
4. Create TaskStatus component
5. Integrate into ProjectEdit code tab
6. Test code generation flow

**Phase 3: Manual Tab Enhancement**
1. Install TipTap packages (@tiptap/react, @tiptap/starter-kit)
2. Create ManualEditor component
3. Integrate into ProjectEdit manual tab
4. Test manual generation flow

**Phase 4: ProjectDetail Enhancement**
1. Add file list display
2. Add generation task status (if needed)
3. Test export flow

---

## Success Criteria

1. ✅ Users can upload seed code ZIP files
2. ✅ Users can view and edit generated code in Monaco Editor
3. ✅ Users can generate and edit operation manuals in TipTap
4. ✅ Generation tasks display progress inline
5. ✅ File lists show uploaded and generated files
6. ✅ All existing functionality still works
7. ✅ Error handling is comprehensive
8. ✅ Manual tests pass for all scenarios

---

## Out of Scope

- Backend API changes (assumed complete)
- Performance optimization (large file handling)
- Automated testing (Cypress/Playwright)
- Deployment configuration
- Documentation updates

---

## Risks & Mitigations

**Risk 1: Monaco Editor bundle size**
- **Impact:** Slower initial page load
- **Mitigation:** Lazy load Monaco component, use dynamic imports

**Risk 2: TipTap content persistence**
- **Impact:** Need backend endpoint to save edited content
- **Mitigation:** Store in FileRecord or add new endpoint

**Risk 3: Task polling performance**
- **Impact:** Many concurrent tasks could overload backend
- **Mitigation:** Limit polling to active tasks only, use WebSocket if needed

**Risk 4: Large file handling**
- **Impact:** Monaco may struggle with files >5MB
- **Mitigation:** Show warning, offer download instead of editing

---

## Dependencies

**NPM Packages to Install:**
```json
{
  "@monaco-editor/react": "^4.6.0",
  "@tiptap/react": "^2.2.0",
  "@tiptap/starter-kit": "^2.2.0",
  "@tiptap/extension-text-style": "^2.2.0",
  "@tiptap/extension-color": "^2.2.0",
  "@tiptap/extension-text-align": "^2.2.0"
}
```

**Backend APIs:** Already implemented (see API Integration section)

---

## Timeline Estimate

- **Phase 1 (Bug fixes + upload):** 2-3 hours
- **Phase 2 (Code tab + Monaco):** 4-5 hours
- **Phase 3 (Manual tab + TipTap):** 3-4 hours
- **Phase 4 (ProjectDetail enhancement):** 1-2 hours
- **Testing & debugging:** 2-3 hours
- **Total:** 12-17 hours

---

## Notes

- This design assumes backend APIs are complete and working
- If backend endpoints are missing, we'll need to implement them first
- Manual testing is sufficient for this phase
- Can add automated tests later if needed
