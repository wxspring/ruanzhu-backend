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
            {codeFiles.length === 0 ? '从头生成代码' : '生成代码'}
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
            <p style={{ fontSize: '14px', marginTop: '12px' }}>
              您可以选择：
            </p>
            <p style={{ fontSize: '14px', marginTop: '8px' }}>
              1. 上传种子代码（ZIP 格式），系统将基于您的代码进行扩展
            </p>
            <p style={{ fontSize: '14px', marginTop: '8px' }}>
              2. 直接点击"从头生成代码"，系统将根据软件概要自动生成完整代码
            </p>
          </div>
        </Card>
      )}
    </div>
  )
}
