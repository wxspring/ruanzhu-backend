# Frontend Phase 9-11 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete Phase 9-11 frontend features: seed code upload, Monaco Editor integration, TipTap editor integration, generation task UI, and file list display.

**Architecture:** Feature-based tabs with inline task status. Each tab (code/manual) manages its own state and triggers async generation tasks. TaskStatus component polls and displays progress. Monaco and TipTap editors are wrapped in reusable components.

**Tech Stack:** React 18, TypeScript, Vite, Ant Design 5, Zustand, @monaco-editor/react, @tiptap/react, @tiptap/starter-kit

## Global Constraints

- React 18.3.1+ required
- TypeScript 5.4.2+ required
- Vite 5.2.0+ required
- Ant Design 5.15.0+ required
- Monaco Editor: @monaco-editor/react 4.6.0+
- TipTap: @tiptap/react 2.2.0+, @tiptap/starter-kit 2.2.0+
- Node.js 18+ for frontend development
- All API calls use existing axios instance with JWT token
- Backend APIs are already implemented (see design spec)
- Manual testing required (no automated tests for UI components)

---

## File Structure

**Components to create:**
- `frontend/src/components/CodeEditor.tsx` - Monaco Editor wrapper with file selector and generation controls
- `frontend/src/components/ManualEditor.tsx` - TipTap rich text editor with AI generation
- `frontend/src/components/TaskStatus.tsx` - Reusable inline task status display with polling
- `frontend/src/components/FileList.tsx` - Display uploaded/generated files with download/delete

**Pages to modify:**
- `frontend/src/pages/ProjectCreate.tsx` - Fix Space import bug, add seed code file upload
- `frontend/src/pages/ProjectEdit.tsx` - Replace placeholder tabs with CodeEditor and ManualEditor
- `frontend/src/pages/ProjectDetail.tsx` - Add file list display

**Configuration:**
- `frontend/package.json` - Add Monaco and TipTap dependencies

---

## Task 1: Install NPM Dependencies

**Files:**
- Modify: `frontend/package.json`

**Interfaces:**
- Consumes: N/A
- Produces: Monaco and TipTap packages installed

- [ ] **Step 1: Navigate to frontend directory**

Run: `cd frontend`

- [ ] **Step 2: Install Monaco Editor package**

Run: `npm install @monaco-editor/react@^4.6.0`

Expected: Package added to dependencies in package.json

- [ ] **Step 3: Install TipTap packages**

Run: `npm install @tiptap/react@^2.2.0 @tiptap/starter-kit@^2.2.0 @tiptap/extension-text-style@^2.2.0 @tiptap/extension-color@^2.2.0 @tiptap/extension-text-align@^2.2.0`

Expected: All packages added to dependencies in package.json

- [ ] **Step 4: Verify installation**

Run: `npm list @monaco-editor/react @tiptap/react @tiptap/starter-kit`

Expected: All packages listed with correct versions

- [ ] **Step 5: Commit package.json changes**

```bash
cd ..
git add frontend/package.json frontend/package-lock.json
git commit -m "chore: add Monaco Editor and TipTap dependencies

Install @monaco-editor/react for code editing
Install @tiptap/react and related packages for rich text editing

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 2: Fix ProjectCreate Bug and Add File Upload

**Files:**
- Modify: `frontend/src/pages/ProjectCreate.tsx`

**Interfaces:**
- Consumes: N/A
- Produces: Working project creation with seed code upload

- [ ] **Step 1: Read current ProjectCreate.tsx**

Read: `frontend/src/pages/ProjectCreate.tsx`

Note the missing `Space` import on line 1.

- [ ] **Step 2: Fix imports**

Edit: `frontend/src/pages/ProjectCreate.tsx`

Replace line 1:
```typescript
import { Form, Input, Button, Card, message } from 'antd'
```

With:
```typescript
import { Form, Input, Button, Card, message, Upload, Space } from 'antd'
import { UploadOutlined, InboxOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { useState } from 'react'
```

- [ ] **Step 3: Add file upload state and handlers**

Edit: `frontend/src/pages/ProjectCreate.tsx`

Add after line 7 (after `const [form] = Form.useForm()`):

```typescript
const [fileList, setFileList] = useState<any[]>([])
const [uploading, setUploading] = useState(false)
const [createdProjectId, setCreatedProjectId] = useState<number | null>(null)

const uploadProps: UploadProps = {
  name: 'file',
  multiple: false,
  accept: '.zip',
  maxCount: 1,
  beforeUpload: (file) => {
    const isZip = file.type === 'application/zip' || file.name.endsWith('.zip')
    if (!isZip) {
      message.error('只能上传 ZIP 文件')
      return false
    }
    const isLt100M = file.size / 1024 / 1024 < 100
    if (!isLt100M) {
      message.error('文件大小不能超过 100MB')
      return false
    }
    return false // Prevent auto upload
  },
  onChange: ({ fileList: newFileList }) => {
    setFileList(newFileList)
  },
  onRemove: () => {
    setFileList([])
  },
}
```

- [ ] **Step 4: Update onFinish handler to support file upload**

Edit: `frontend/src/pages/ProjectCreate.tsx`

Replace the `onFinish` function (lines 9-17) with:

```typescript
const onFinish = async (values: any) => {
  try {
    // Create project first
    const projectResponse = await api.post('/api/projects', {
      name: values.name,
      customerName: values.customerName,
    })
    const projectId = projectResponse.data.data.id
    setCreatedProjectId(projectId)

    // Upload seed code if file selected
    if (fileList.length > 0) {
      setUploading(true)
      const formData = new FormData()
      formData.append('file', fileList[0].originFileObj)

      await api.post(`/api/projects/${projectId}/upload-code`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      message.success('项目和种子代码上传成功')
    } else {
      message.success('项目创建成功')
    }

    navigate('/')
  } catch (error: any) {
    message.error(error.response?.data?.message || '创建失败')
  } finally {
    setUploading(false)
  }
}
```

- [ ] **Step 5: Add file upload UI to form**

Edit: `frontend/src/pages/ProjectCreate.tsx`

Add before the final `Form.Item` (before line 38):

```typescript
<Form.Item label="种子代码（可选）" name="seedCode">
  <Upload.Dragger {...uploadProps} fileList={fileList}>
    <p className="ant-upload-drag-icon">
      <InboxOutlined />
    </p>
    <p className="ant-upload-text">点击或拖拽 ZIP 文件到此区域</p>
    <p className="ant-upload-hint">
      仅支持 ZIP 格式，最大 100MB。上传后可自动生成代码文档。
    </p>
  </Upload.Dragger>
</Form.Item>
```

- [ ] **Step 6: Update submit button to show loading state**

Edit: `frontend/src/pages/ProjectCreate.tsx`

Replace the submit button (around line 39):

```typescript
<Form.Item>
  <Space>
    <Button type="primary" htmlType="submit" loading={uploading}>
      创建
    </Button>
    <Button onClick={() => navigate('/')}>
      取消
    </Button>
  </Space>
</Form.Item>
```

- [ ] **Step 7: Manual test - Create project without file**

1. Navigate to `/projects/create`
2. Fill in software name: "测试软件"
3. Leave file upload empty
4. Click "创建"
5. Verify: Success message + redirect to dashboard
6. Verify: New project appears in dashboard list

- [ ] **Step 8: Manual test - Create project with file**

1. Navigate to `/projects/create`
2. Fill in software name: "测试软件2"
3. Drag and drop a ZIP file (or click to select)
4. Verify: File appears in upload area
5. Click "创建"
6. Verify: Success message + redirect to dashboard
7. Navigate to project detail → verify file appears in file list (after Task 6)

- [ ] **Step 9: Commit changes**

```bash
git add frontend/src/pages/ProjectCreate.tsx
git commit -m "feat: fix ProjectCreate bug and add seed code upload

- Fix missing Space import
- Add Upload component for seed code ZIP files
- Add file validation (ZIP only, max 100MB)
- Upload file after project creation
- Show loading state during upload

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 3: Create TaskStatus Component

**Files:**
- Create: `frontend/src/components/TaskStatus.tsx`

**Interfaces:**
- Consumes: Task ID, onComplete/onError callbacks
- Produces: Inline task status display with polling

- [ ] **Step 1: Create TaskStatus component file**

Create: `frontend/src/components/TaskStatus.tsx`

```typescript
import { useEffect, useState } from 'react'
import { Progress, Alert, Button, Space } from 'antd'
import { CheckCircleOutlined, CloseCircleOutlined, LoadingOutlined } from '@ant-design/icons'
import api from '../api/axios'

interface GenerateTask {
  id: number
  status: 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED'
  progress: number
  errorMessage?: string
}

interface TaskStatusProps {
  taskId: number | null
  onComplete?: () => void
  onError?: (error: string) => void
}

export default function TaskStatus({ taskId, onComplete, onError }: TaskStatusProps) {
  const [task, setTask] = useState<GenerateTask | null>(null)
  const [pollingInterval, setPollingInterval] = useState<NodeJS.Timeout | null>(null)

  useEffect(() => {
    if (!taskId) {
      setTask(null)
      return
    }

    // Initial fetch
    fetchTaskStatus(taskId)

    // Start polling
    const interval = setInterval(() => {
      fetchTaskStatus(taskId)
    }, 2000)

    setPollingInterval(interval)

    return () => {
      if (interval) {
        clearInterval(interval)
      }
    }
  }, [taskId])

  const fetchTaskStatus = async (id: number) => {
    try {
      const response = await api.get(`/api/tasks/${id}`)
      const taskData: GenerateTask = response.data.data
      setTask(taskData)

      if (taskData.status === 'SUCCESS') {
        if (pollingInterval) {
          clearInterval(pollingInterval)
        }
        onComplete?.()
      } else if (taskData.status === 'FAILED') {
        if (pollingInterval) {
          clearInterval(pollingInterval)
        }
        onError?.(taskData.errorMessage || '任务执行失败')
      }
    } catch (error) {
      console.error('Failed to fetch task status:', error)
      // Don't show error for polling failures
    }
  }

  const handleRetry = () => {
    // Parent component should handle retry logic
    // This is just a UI callback
  }

  if (!taskId || !task) {
    return null
  }

  if (task.status === 'PENDING') {
    return (
      <Alert
        type="info"
        message={
          <Space>
            <LoadingOutlined />
            <span>任务排队中...</span>
          </Space>
        }
      />
    )
  }

  if (task.status === 'RUNNING') {
    return (
      <div style={{ padding: '16px 0' }}>
        <div style={{ marginBottom: 8 }}>
          <LoadingOutlined style={{ marginRight: 8 }} />
          <span>生成中 {task.progress}%</span>
        </div>
        <Progress percent={task.progress} status="active" />
      </div>
    )
  }

  if (task.status === 'SUCCESS') {
    return (
      <Alert
        type="success"
        message={
          <Space>
            <CheckCircleOutlined />
            <span>生成完成</span>
          </Space>
        }
      />
    )
  }

  if (task.status === 'FAILED') {
    return (
      <Alert
        type="error"
        message="生成失败"
        description={task.errorMessage}
        action={
          <Button size="small" onClick={handleRetry}>
            重试
          </Button>
        }
      />
    )
  }

  return null
}
```

- [ ] **Step 2: Manual test - TaskStatus display**

1. Navigate to any page that will use TaskStatus (after Task 5/6)
2. Trigger a generation task
3. Verify: Shows "任务排队中..." initially
4. Verify: Progress bar updates every 2 seconds
5. Verify: Shows "生成完成" when task succeeds
6. Verify: Shows error message when task fails

- [ ] **Step 3: Commit TaskStatus component**

```bash
git add frontend/src/components/TaskStatus.tsx
git commit -m "feat: add TaskStatus component for inline task tracking

- Display task progress (pending/running/success/failed)
- Poll task status every 2 seconds
- Show progress bar with percentage
- Display error messages for failed tasks
- Auto-stop polling when task completes

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 4: Create FileList Component

**Files:**
- Create: `frontend/src/components/FileList.tsx`

**Interfaces:**
- Consumes: Project ID, file list, refresh callback
- Produces: File list display with download/delete actions

- [ ] **Step 1: Create FileList component file**

Create: `frontend/src/components/FileList.tsx`

```typescript
import { Table, Button, Tag, Space, Popconfirm, message } from 'antd'
import { DownloadOutlined, DeleteOutlined } from '@ant-design/icons'
import api from '../api/axios'

interface FileRecord {
  id: number
  projectId: number
  fileType: string
  fileName: string
  fileSize: number
  createdAt: string
}

interface FileListProps {
  projectId: number
  files: FileRecord[]
  onRefresh: () => void
}

const fileTypeColors: Record<string, string> = {
  SEED_CODE: 'blue',
  GENERATED_CODE: 'green',
  MANUAL: 'purple',
}

const fileTypeLabels: Record<string, string> = {
  SEED_CODE: '种子代码',
  GENERATED_CODE: '生成代码',
  MANUAL: '操作手册',
}

const formatFileSize = (bytes: number): string => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

export default function FileList({ projectId, files, onRefresh }: FileListProps) {
  const handleDownload = async (file: FileRecord) => {
    try {
      const response = await api.get(
        `/api/projects/${projectId}/files/${file.id}/download`,
        { responseType: 'blob' }
      )
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', file.fileName)
      document.body.appendChild(link)
      link.click()
      link.remove()
      message.success('下载成功')
    } catch (error) {
      message.error('下载失败')
    }
  }

  const handleDelete = async (fileId: number) => {
    try {
      // Note: Backend may not have delete endpoint yet
      // For now, just refresh the list
      message.info('删除功能暂未实现')
      onRefresh()
    } catch (error) {
      message.error('删除失败')
    }
  }

  const columns = [
    {
      title: '文件名',
      dataIndex: 'fileName',
      key: 'fileName',
      width: 300,
    },
    {
      title: '类型',
      dataIndex: 'fileType',
      key: 'fileType',
      width: 120,
      render: (fileType: string) => (
        <Tag color={fileTypeColors[fileType]}>
          {fileTypeLabels[fileType] || fileType}
        </Tag>
      ),
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      width: 100,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '上传时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: FileRecord) => (
        <Space>
          <Button
            type="link"
            icon={<DownloadOutlined />}
            onClick={() => handleDownload(record)}
          >
            下载
          </Button>
          <Popconfirm
            title="确定删除此文件吗？"
            onConfirm={() => handleDelete(record.id)}
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <Table
      columns={columns}
      dataSource={files}
      rowKey="id"
      pagination={false}
      size="small"
    />
  )
}
```

- [ ] **Step 2: Manual test - FileList display**

1. Navigate to a project with uploaded files (after Task 6)
2. Verify: File list shows all files
3. Verify: File type tags display correctly (蓝色/绿色/紫色)
4. Verify: File size formatted in KB/MB
5. Click download button → verify file downloads
6. Click delete button → verify confirmation modal appears

- [ ] **Step 3: Commit FileList component**

```bash
git add frontend/src/components/FileList.tsx
git commit -m "feat: add FileList component for project files

- Display uploaded and generated files
- Show file type tags (SEED_CODE/GENERATED_CODE/MANUAL)
- Format file size in KB/MB
- Download button for each file
- Delete button with confirmation

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 5: Create CodeEditor Component with Monaco

**Files:**
- Create: `frontend/src/components/CodeEditor.tsx`

**Interfaces:**
- Consumes: Project ID, file list, refresh callback
- Produces: Monaco Editor with file selector and generation controls

- [ ] **Step 1: Create CodeEditor component file**

Create: `frontend/src/components/CodeEditor.tsx`

```typescript
import { useEffect, useState } from 'react'
import { Button, Space, Select, message, Card } from 'antd'
import { PlayCircleOutlined, SaveOutlined } from '@ant-design/icons'
import Editor from '@monaco-editor/react'
import api from '../api/axios'
import TaskStatus from './TaskStatus'
import FileList from './FileList'

interface FileRecord {
  id: number
  projectId: number
  fileType: string
  fileName: string
  fileSize: number
  createdAt: string
}

interface CodeEditorProps {
  projectId: number
  files: FileRecord[]
  onRefreshFiles: () => void
}

// Map file extensions to Monaco language IDs
const getLanguageFromFileName = (fileName: string): string => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  const languageMap: Record<string, string> = {
    java: 'java',
    py: 'python',
    js: 'javascript',
    ts: 'typescript',
    jsx: 'javascript',
    tsx: 'typescript',
    html: 'html',
    css: 'css',
    json: 'json',
    xml: 'xml',
    md: 'markdown',
    sql: 'sql',
    sh: 'shell',
    bash: 'shell',
    yml: 'yaml',
    yaml: 'yaml',
  }
  return languageMap[ext || ''] || 'plaintext'
}

export default function CodeEditor({ projectId, files, onRefreshFiles }: CodeEditorProps) {
  const [selectedFile, setSelectedFile] = useState<FileRecord | null>(null)
  const [codeContent, setCodeContent] = useState<string>('')
  const [loading, setLoading] = useState(false)
  const [taskId, setTaskId] = useState<number | null>(null)
  const [language, setLanguage] = useState<string>('plaintext')

  // Filter code files (SEED_CODE and GENERATED_CODE)
  const codeFiles = files.filter(
    (f) => f.fileType === 'SEED_CODE' || f.fileType === 'GENERATED_CODE'
  )

  useEffect(() => {
    if (selectedFile) {
      loadFileContent(selectedFile.id)
      setLanguage(getLanguageFromFileName(selectedFile.fileName))
    }
  }, [selectedFile])

  const loadFileContent = async (fileId: number) => {
    setLoading(true)
    try {
      const response = await api.get(
        `/api/projects/${projectId}/files/${fileId}/download`,
        { responseType: 'blob' }
      )
      const text = await response.data.text()
      setCodeContent(text)
    } catch (error) {
      message.error('加载文件失败')
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateCode = async () => {
    try {
      const response = await api.post(
        `/api/tasks/projects/${projectId}/generate?taskType=CODE`
      )
      const newTaskId = response.data.data.id
      setTaskId(newTaskId)
      message.success('代码生成任务已启动')
    } catch (error: any) {
      message.error(error.response?.data?.message || '启动生成任务失败')
    }
  }

  const handleTaskComplete = () => {
    message.success('代码生成完成')
    onRefreshFiles()
    setTaskId(null)
  }

  const handleTaskError = (errorMessage: string) => {
    message.error('代码生成失败: ' + errorMessage)
    setTaskId(null)
  }

  const handleSave = async () => {
    // Note: Backend may not have endpoint to save edited code yet
    // For now, just show message
    message.info('保存功能暂未实现')
  }

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Select
            placeholder="选择文件"
            style={{ width: 300 }}
            onChange={(value) => {
              const file = codeFiles.find((f) => f.id === value)
              setSelectedFile(file || null)
            }}
            value={selectedFile?.id}
          >
            {codeFiles.map((file) => (
              <Select.Option key={file.id} value={file.id}>
                {file.fileName}
              </Select.Option>
            ))}
          </Select>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleGenerateCode}
            disabled={!!taskId}
          >
            生成代码
          </Button>
          <Button
            icon={<SaveOutlined />}
            onClick={handleSave}
            disabled={!selectedFile}
          >
            保存
          </Button>
        </Space>
      </Card>

      {taskId && (
        <Card style={{ marginBottom: 16 }}>
          <TaskStatus
            taskId={taskId}
            onComplete={handleTaskComplete}
            onError={handleTaskError}
          />
        </Card>
      )}

      <Card title="代码文件" style={{ marginBottom: 16 }}>
        <FileList
          projectId={projectId}
          files={codeFiles}
          onRefresh={onRefreshFiles}
        />
      </Card>

      {selectedFile && (
        <Card title={`编辑: ${selectedFile.fileName}`} loading={loading}>
          <Editor
            height="600px"
            language={language}
            value={codeContent}
            onChange={(value) => setCodeContent(value || '')}
            theme="vs-dark"
            options={{
              minimap: { enabled: true },
              wordWrap: 'on',
              fontSize: 14,
              lineNumbers: 'on',
              scrollBeyondLastLine: false,
              automaticLayout: true,
            }}
          />
        </Card>
      )}

      {!selectedFile && codeFiles.length === 0 && (
        <Card>
          <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>
            <p>暂无代码文件</p>
            <p>请先上传种子代码，或点击"生成代码"按钮</p>
          </div>
        </Card>
      )}
    </div>
  )
}
```

- [ ] **Step 3: Manual test - CodeEditor component**

1. Navigate to project edit page → code tab (after Task 7)
2. Verify: File list shows uploaded seed code files
3. Select a file → verify Monaco displays content with syntax highlighting
4. Click "生成代码" button → verify task status shows progress
5. Wait for task to complete → verify new files appear in list
6. Select generated file → verify Monaco displays content

- [ ] **Step 4: Commit CodeEditor component**

```bash
git add frontend/src/components/CodeEditor.tsx
git commit -m "feat: add CodeEditor component with Monaco Editor

- Monaco Editor with syntax highlighting (auto-detect language)
- File selector for uploaded/generated code files
- Generate code button triggers CODE task
- Inline task status display
- File list with download/delete actions
- Save button (placeholder for future implementation)

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 6: Create ManualEditor Component with TipTap

**Files:**
- Create: `frontend/src/components/ManualEditor.tsx`

**Interfaces:**
- Consumes: Project ID, project name
- Produces: TipTap rich text editor with AI generation

- [ ] **Step 1: Create ManualEditor component file**

Create: `frontend/src/components/ManualEditor.tsx`

```typescript
import { useEffect, useState } from 'react'
import { Button, Space, message, Card } from 'antd'
import { PlayCircleOutlined, SaveOutlined } from '@ant-design/icons'
import { useEditor, EditorContent } from '@tiptap/react'
import StarterKit from '@tiptap/starter-kit'
import TextStyle from '@tiptap/extension-text-style'
import Color from '@tiptap/extension-color'
import TextAlign from '@tiptap/extension-text-align'
import api from '../api/axios'
import TaskStatus from './TaskStatus'

interface ManualEditorProps {
  projectId: number
  projectName: string
}

export default function ManualEditor({ projectId, projectName }: ManualEditorProps) {
  const [content, setContent] = useState<string>('')
  const [taskId, setTaskId] = useState<number | null>(null)
  const [saving, setSaving] = useState(false)
  const [loading, setLoading] = useState(false)

  const editor = useEditor({
    extensions: [
      StarterKit,
      TextStyle,
      Color,
      TextAlign.configure({
        types: ['heading', 'paragraph'],
      }),
    ],
    content: content,
    onUpdate: ({ editor }) => {
      setContent(editor.getHTML())
    },
    editorProps: {
      attributes: {
        class: 'prose prose-sm sm:prose lg:prose-lg xl:prose-2xl mx-auto focus:outline-none',
        style: 'min-height: 400px; padding: 16px; border: 1px solid #d9d9d9; border-radius: 6px;',
      },
    },
  })

  useEffect(() => {
    loadManualContent()
  }, [projectId])

  useEffect(() => {
    if (editor && content) {
      editor.commands.setContent(content)
    }
  }, [content, editor])

  const loadManualContent = async () => {
    setLoading(true)
    try {
      // Try to load existing manual content
      // This may need a dedicated endpoint or load from file
      const response = await api.get(`/api/projects/${projectId}/files`)
      const files = response.data.data
      const manualFile = files.find((f: any) => f.fileType === 'MANUAL')

      if (manualFile) {
        const fileResponse = await api.get(
          `/api/projects/${projectId}/files/${manualFile.id}/download`,
          { responseType: 'blob' }
        )
        const html = await fileResponse.data.text()
        setContent(html)
      }
    } catch (error) {
      console.error('Failed to load manual content:', error)
    } finally {
      setLoading(false)
    }
  }

  const handleGenerateManual = async () => {
    try {
      const response = await api.post(
        `/api/tasks/projects/${projectId}/generate?taskType=MANUAL`
      )
      const newTaskId = response.data.data.id
      setTaskId(newTaskId)
      message.success('操作手册生成任务已启动')
    } catch (error: any) {
      message.error(error.response?.data?.message || '启动生成任务失败')
    }
  }

  const handleTaskComplete = async () => {
    message.success('操作手册生成完成')
    // Load generated content
    await loadManualContent()
    setTaskId(null)
  }

  const handleTaskError = (errorMessage: string) => {
    message.error('操作手册生成失败: ' + errorMessage)
    setTaskId(null)
  }

  const handleSave = async () => {
    if (!editor) return

    setSaving(true)
    try {
      // Note: Backend may not have endpoint to save manual content yet
      // For now, just show message
      message.info('保存功能暂未实现')
    } catch (error) {
      message.error('保存失败')
    } finally {
      setSaving(false)
    }
  }

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleGenerateManual}
            disabled={!!taskId}
          >
            AI 生成
          </Button>
          <Button
            icon={<SaveOutlined />}
            onClick={handleSave}
            loading={saving}
          >
            保存
          </Button>
        </Space>
      </Card>

      {taskId && (
        <Card style={{ marginBottom: 16 }}>
          <TaskStatus
            taskId={taskId}
            onComplete={handleTaskComplete}
            onError={handleTaskError}
          />
        </Card>
      )}

      <Card title="操作手册编辑" loading={loading}>
        {editor && (
          <div style={{ marginBottom: 16 }}>
            <Space wrap>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleBold().run()}
                className={editor.isActive('bold') ? 'is-active' : ''}
              >
                粗体
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleItalic().run()}
                className={editor.isActive('italic') ? 'is-active' : ''}
              >
                斜体
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleHeading({ level: 1 }).run()}
                className={editor.isActive('heading', { level: 1 }) ? 'is-active' : ''}
              >
                H1
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleHeading({ level: 2 }).run()}
                className={editor.isActive('heading', { level: 2 }) ? 'is-active' : ''}
              >
                H2
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleHeading({ level: 3 }).run()}
                className={editor.isActive('heading', { level: 3 }) ? 'is-active' : ''}
              >
                H3
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleBulletList().run()}
                className={editor.isActive('bulletList') ? 'is-active' : ''}
              >
                无序列表
              </Button>
              <Button
                size="small"
                onClick={() => editor.chain().focus().toggleOrderedList().run()}
                className={editor.isActive('orderedList') ? 'is-active' : ''}
              >
                有序列表
              </Button>
            </Space>
          </div>
        )}
        <EditorContent editor={editor} />
      </Card>
    </div>
  )
}
```

- [ ] **Step 2: Add TipTap editor styles**

Edit: `frontend/src/index.css` (or create if doesn't exist)

Add:
```css
/* TipTap Editor Styles */
.ProseMirror {
  min-height: 400px;
  padding: 16px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  background: #fff;
}

.ProseMirror:focus {
  outline: none;
  border-color: #1890ff;
}

.ProseMirror p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  float: left;
  color: #adb5bd;
  pointer-events: none;
  height: 0;
}

.is-active {
  background-color: #1890ff !important;
  color: #fff !important;
}
```

- [ ] **Step 3: Manual test - ManualEditor component**

1. Navigate to project edit page → manual tab (after Task 7)
2. Click "AI 生成" button → verify task status shows progress
3. Wait for task to complete → verify TipTap loads HTML content
4. Edit content: make text bold, add heading
5. Click toolbar buttons → verify they work
6. Click "保存" button → verify message appears

- [ ] **Step 4: Commit ManualEditor component**

```bash
git add frontend/src/components/ManualEditor.tsx frontend/src/index.css
git commit -m "feat: add ManualEditor component with TipTap rich text editor

- TipTap editor with basic extensions (StarterKit, TextStyle, Color, TextAlign)
- Toolbar with formatting buttons (bold, italic, headings, lists)
- AI generate button triggers MANUAL task
- Inline task status display
- Load existing manual content from backend
- Save button (placeholder for future implementation)
- Add TipTap editor styles

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 7: Update ProjectEdit to Use New Components

**Files:**
- Modify: `frontend/src/pages/ProjectEdit.tsx`

**Interfaces:**
- Consumes: CodeEditor, ManualEditor components
- Produces: Enhanced project edit page with working code/manual tabs

- [ ] **Step 1: Read current ProjectEdit.tsx**

Read: `frontend/src/pages/ProjectEdit.tsx`

Note the placeholder code and manual tabs (lines 132-155).

- [ ] **Step 2: Update imports**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Replace existing imports with:
```typescript
import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Tabs, Form, Input, Button, message, Spin, Space } from 'antd'
import api from '../api/axios'
import CodeEditor from '../components/CodeEditor'
import ManualEditor from '../components/ManualEditor'
```

- [ ] **Step 3: Add files state**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Add after line 20 (after `const [summaryForm] = Form.useForm()`):

```typescript
const [files, setFiles] = useState<any[]>([])
```

- [ ] **Step 4: Add fetchFiles function**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Add after `fetchProject` function (around line 40):

```typescript
const fetchFiles = async () => {
  try {
    const response = await api.get(`/api/projects/${id}/files`)
    setFiles(response.data.data || [])
  } catch (error) {
    console.error('Failed to fetch files:', error)
  }
}
```

- [ ] **Step 5: Update useEffect to fetch files**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Replace the useEffect (lines 23-25) with:

```typescript
useEffect(() => {
  fetchProject()
  fetchFiles()
}, [id])
```

- [ ] **Step 6: Replace code tab placeholder**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Replace the code tab children (lines 136-142) with:

```typescript
children: (
  <CodeEditor
    projectId={Number(id)}
    files={files}
    onRefreshFiles={fetchFiles}
  />
),
```

- [ ] **Step 7: Replace manual tab placeholder**

Edit: `frontend/src/pages/ProjectEdit.tsx`

Replace the manual tab children (lines 148-154) with:

```typescript
children: (
  <ManualEditor
    projectId={Number(id)}
    projectName={project.name}
  />
),
```

- [ ] **Step 8: Manual test - Complete workflow**

1. Navigate to `/projects/:id/edit`
2. Verify: Summary tab works (existing functionality)
3. Click "源代码" tab → verify CodeEditor displays
4. Verify: File list shows uploaded files
5. Select a file → verify Monaco displays content
6. Click "操作手册" tab → verify ManualEditor displays
7. Click "AI 生成" button → verify task runs
8. Wait for completion → verify content loads in editor

- [ ] **Step 9: Commit changes**

```bash
git add frontend/src/pages/ProjectEdit.tsx
git commit -m "feat: integrate CodeEditor and ManualEditor into ProjectEdit

- Replace placeholder code tab with CodeEditor component
- Replace placeholder manual tab with ManualEditor component
- Add file list state and fetch logic
- Pass project data to editor components
- Enable full editing workflow

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 8: Update ProjectDetail to Show File List

**Files:**
- Modify: `frontend/src/pages/ProjectDetail.tsx`

**Interfaces:**
- Consumes: FileList component
- Produces: Enhanced project detail with file list display

- [ ] **Step 1: Read current ProjectDetail.tsx**

Read: `frontend/src/pages/ProjectDetail.tsx`

Note the current structure (lines 1-96).

- [ ] **Step 2: Update imports**

Edit: `frontend/src/pages/ProjectDetail.tsx`

Replace existing imports with:
```typescript
import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Descriptions, Button, Tabs, message, Spin } from 'antd'
import { DownloadOutlined, FileOutlined } from '@ant-design/icons'
import api from '../api/axios'
import FileList from '../components/FileList'
```

- [ ] **Step 3: Add files state**

Edit: `frontend/src/pages/ProjectDetail.tsx`

Add after line 19 (after `const [loading, setLoading] = useState(true)`):

```typescript
const [files, setFiles] = useState<any[]>([])
```

- [ ] **Step 4: Add fetchFiles function**

Edit: `frontend/src/pages/ProjectDetail.tsx`

Add after `fetchProject` function (around line 35):

```typescript
const fetchFiles = async () => {
  try {
    const response = await api.get(`/api/projects/${id}/files`)
    setFiles(response.data.data || [])
  } catch (error) {
    console.error('Failed to fetch files:', error)
  }
}
```

- [ ] **Step 5: Update useEffect to fetch files**

Edit: `frontend/src/pages/ProjectDetail.tsx`

Replace the useEffect (lines 22-24) with:

```typescript
useEffect(() => {
  fetchProject()
  fetchFiles()
}, [id])
```

- [ ] **Step 6: Add file list card after software summary**

Edit: `frontend/src/pages/ProjectDetail.tsx`

Add after the software summary Card (after line 93):

```typescript
<Card
  title={
    <Space>
      <FileOutlined />
      <span>项目文件</span>
    </Space>
  }
  style={{ marginTop: 16 }}
>
  <FileList
    projectId={Number(id)}
    files={files}
    onRefresh={fetchFiles}
  />
</Card>
```

- [ ] **Step 7: Manual test - ProjectDetail with files**

1. Navigate to a project detail page
2. Verify: Project info displays correctly
3. Verify: Software summary displays correctly
4. Verify: File list card appears
5. Verify: File list shows all files for project
6. Click download button → verify file downloads

- [ ] **Step 8: Commit changes**

```bash
git add frontend/src/pages/ProjectDetail.tsx
git commit -m "feat: add file list display to ProjectDetail

- Fetch project files on page load
- Display FileList component in new card
- Show uploaded and generated files
- Enable download for each file

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 9: Final Integration Testing

**Files:**
- N/A (testing only)

**Interfaces:**
- Consumes: All previous tasks
- Produces: Verified end-to-end workflow

- [ ] **Step 1: Test complete workflow - Create project**

1. Navigate to `/projects/create`
2. Fill in software name: "完整测试软件"
3. Fill in customer name: "测试客户"
4. Upload a ZIP file with some code
5. Click "创建"
6. Verify: Success message + redirect to dashboard
7. Verify: New project appears in dashboard

- [ ] **Step 2: Test complete workflow - Edit summary**

1. Navigate to the created project → edit page
2. Click "软件概要" tab
3. Fill in summary fields (version, category, etc.)
4. Click "AI 生成" button (if backend supports it)
5. Click "保存" button
6. Verify: Success message
7. Refresh page → verify data persists

- [ ] **Step 3: Test complete workflow - Generate code**

1. Click "源代码" tab
2. Verify: Uploaded seed code files appear in list
3. Select a file → verify Monaco displays content
4. Click "生成代码" button
5. Verify: Task status shows "任务排队中..."
6. Wait → verify progress bar updates
7. Wait for completion → verify "生成完成" message
8. Verify: New generated files appear in list
9. Select generated file → verify Monaco displays content

- [ ] **Step 4: Test complete workflow - Generate manual**

1. Click "操作手册" tab
2. Click "AI 生成" button
3. Verify: Task status shows progress
4. Wait for completion → verify "生成完成" message
5. Verify: TipTap editor loads generated HTML content
6. Edit content: make text bold, add heading
7. Verify: Toolbar buttons work
8. Click "保存" button
9. Verify: Success message

- [ ] **Step 5: Test complete workflow - View files**

1. Navigate to project detail page
2. Verify: File list card appears
3. Verify: All files (seed code, generated code, manual) appear
4. Click download button for each file type
5. Verify: Files download correctly

- [ ] **Step 6: Test complete workflow - Export**

1. On project detail page, click "导出材料" button
2. Verify: ZIP file downloads
3. Extract ZIP → verify all materials present:
   - Source code document (.txt or .docx)
   - Operation manual (.docx and .pdf)
   - Software summary (.txt)

- [ ] **Step 7: Test error scenarios**

1. Create project without required fields → verify validation errors
2. Upload non-ZIP file → verify error message
3. Upload file >100MB → verify error message
4. Trigger generation with missing seed code → verify error handling
5. Network error during task polling → verify graceful degradation

- [ ] **Step 8: Test cross-browser compatibility**

1. Test in Chrome → verify all features work
2. Test in Firefox → verify all features work
3. Test in Edge → verify all features work

- [ ] **Step 9: Document any issues found**

If any issues are found during testing:
1. Create a GitHub issue (or document in notes)
2. Note the steps to reproduce
3. Note expected vs actual behavior
4. Prioritize by severity

- [ ] **Step 10: Final commit (if any fixes needed)**

If fixes were made during testing:

```bash
git add .
git commit -m "fix: resolve issues found during integration testing

[Describe fixes made]

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Task 10: Update Documentation

**Files:**
- Modify: `docs/superpowers/plans/2026-08-10-ruanzhu-system.md` (optional)

**Interfaces:**
- Consumes: N/A
- Produces: Updated progress tracking

- [ ] **Step 1: Update progress.md (if exists)**

If `docs/superpowers/progress.md` exists, update it to mark Phase 9-11 as complete:

```markdown
## Phase 9: Frontend - Project Management
- Status: **DONE** (completed 2026-08-13)
- Features: Dashboard, ProjectCreate with file upload, ProjectDetail with file list

## Phase 10: Frontend - Editing
- Status: **DONE** (completed 2026-08-13)
- Features: CodeEditor with Monaco, ManualEditor with TipTap, generation task UI

## Phase 11: Frontend - Export & Admin
- Status: **DONE** (completed 2026-08-13)
- Features: Export functionality, UserManagement (already complete)
```

- [ ] **Step 2: Commit documentation updates**

```bash
git add docs/
git commit -m "docs: mark Phase 9-11 as complete

Update progress tracking to reflect completed frontend features.

Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>"
```

---

## Summary

**Total tasks:** 10  
**Estimated time:** 12-17 hours  
**Dependencies:** All backend APIs must be implemented and working

**Deliverables:**
1. ✅ Seed code file upload in ProjectCreate
2. ✅ Monaco Editor integration for code editing
3. ✅ TipTap editor integration for manual editing
4. ✅ Generation task UI with inline status tracking
5. ✅ File list display in ProjectDetail and ProjectEdit
6. ✅ Bug fixes (ProjectCreate Space import)
7. ✅ Complete end-to-end workflow tested
8. ✅ Documentation updated

**Next steps after this plan:**
- Phase 12: Deployment (Docker, Docker Compose, deployment docs)
