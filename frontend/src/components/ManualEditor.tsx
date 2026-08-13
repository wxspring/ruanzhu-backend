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

export default function ManualEditor({ projectId, projectName: _projectName }: ManualEditorProps) {
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
