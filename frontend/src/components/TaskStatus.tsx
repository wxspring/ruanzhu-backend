import { useEffect, useState } from 'react'
import { Progress, Alert, Button, Space } from 'antd'
import { CheckCircleOutlined, LoadingOutlined } from '@ant-design/icons'
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
  const [pollingInterval, setPollingInterval] = useState<ReturnType<typeof setInterval> | null>(null)

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
