import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Descriptions, Button, Tabs, message, Spin } from 'antd'
import { DownloadOutlined } from '@ant-design/icons'
import api from '../api/axios'

interface Project {
  id: number
  name: string
  customerName: string
  status: string
  createdAt: string
  softwareSummary: any
}

export default function ProjectDetail() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [project, setProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchProject()
  }, [id])

  const fetchProject = async () => {
    try {
      const response = await api.get(`/api/projects/${id}`)
      setProject(response.data.data)
    } catch (error) {
      message.error('获取项目详情失败')
    } finally {
      setLoading(false)
    }
  }

  const handleExport = async () => {
    try {
      const response = await api.get(`/api/projects/${id}/export`, {
        responseType: 'blob',
      })
      const url = window.URL.createObjectURL(new Blob([response.data]))
      const link = document.createElement('a')
      link.href = url
      link.setAttribute('download', `${project?.name}.zip`)
      document.body.appendChild(link)
      link.click()
      link.remove()
      message.success('导出成功')
    } catch (error) {
      message.error('导出失败')
    }
  }

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />
  }

  if (!project) {
    return <div>项目不存在</div>
  }

  const summary = project.softwareSummary || {}

  return (
    <div>
      <Card
        title={project.name}
        extra={
          <Button type="primary" icon={<DownloadOutlined />} onClick={handleExport}>
            导出材料
          </Button>
        }
      >
        <Descriptions column={2}>
          <Descriptions.Item label="客户名称">{project.customerName || '-'}</Descriptions.Item>
          <Descriptions.Item label="状态">{project.status}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{project.createdAt}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="软件概要" style={{ marginTop: 16 }}>
        <Descriptions column={1}>
          <Descriptions.Item label="版本号">{summary.version || '-'}</Descriptions.Item>
          <Descriptions.Item label="软件分类">{summary.category || '-'}</Descriptions.Item>
          <Descriptions.Item label="编程语言">{summary.language || '-'}</Descriptions.Item>
          <Descriptions.Item label="源程序量">{summary.codeLines ? `${summary.codeLines} 行` : '-'}</Descriptions.Item>
          <Descriptions.Item label="开发目的">{summary.purpose || '-'}</Descriptions.Item>
          <Descriptions.Item label="面向领域">{summary.targetDomain || '-'}</Descriptions.Item>
          <Descriptions.Item label="主要功能">{summary.mainFunctions || '-'}</Descriptions.Item>
          <Descriptions.Item label="技术特点">{summary.techFeatures || '-'}</Descriptions.Item>
        </Descriptions>
      </Card>
    </div>
  )
}
