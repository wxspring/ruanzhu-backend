import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Tabs, Form, Input, Button, message, Spin, Space } from 'antd'
import api from '../api/axios'
import CodeEditor from '../components/CodeEditor'
import ManualEditor from '../components/ManualEditor'

const { TextArea } = Input

interface Project {
  id: number
  name: string
  customerName: string
  status: string
  softwareSummary: any
}

export default function ProjectEdit() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [project, setProject] = useState<Project | null>(null)
  const [loading, setLoading] = useState(true)
  const [summaryForm] = Form.useForm()
  const [files, setFiles] = useState<any[]>([])

  useEffect(() => {
    fetchProject()
    fetchFiles()
  }, [id])

  const fetchProject = async () => {
    try {
      const response = await api.get(`/api/projects/${id}`)
      const data = response.data.data
      setProject(data)
      if (data.softwareSummary) {
        summaryForm.setFieldsValue(data.softwareSummary)
      }
    } catch (error) {
      message.error('获取项目详情失败')
    } finally {
      setLoading(false)
    }
  }

  const fetchFiles = async () => {
    try {
      const response = await api.get(`/api/projects/${id}/files`)
      setFiles(response.data.data || [])
    } catch (error) {
      console.error('Failed to fetch files:', error)
    }
  }

  const handleSaveSummary = async (values: any) => {
    try {
      await api.put(`/api/projects/${id}/summary`, values)
      message.success('保存成功')
      fetchProject()
    } catch (error: any) {
      message.error(error.response?.data?.message || '保存失败')
    }
  }

  const handleGenerateSummary = async () => {
    try {
      const response = await api.post(`/api/ai/projects/${id}/generate-summary`)
      const generated = response.data.data
      summaryForm.setFieldsValue(generated)
      message.success('AI 生成完成，请检查并保存')
    } catch (error: any) {
      message.error('AI 生成失败')
    }
  }

  if (loading) {
    return <Spin size="large" style={{ display: 'block', margin: '100px auto' }} />
  }

  if (!project) {
    return <div>项目不存在</div>
  }

  const tabItems = [
    {
      key: 'summary',
      label: '软件概要',
      children: (
        <Form form={summaryForm} layout="vertical" onFinish={handleSaveSummary}>
          <Form.Item name="version" label="版本号">
            <Input placeholder="例如：V1.0" />
          </Form.Item>
          <Form.Item name="category" label="软件分类">
            <Input placeholder="例如：应用软件" />
          </Form.Item>
          <Form.Item name="devHardware" label="开发的硬件环境">
            <TextArea rows={2} placeholder="例如：Intel Core i5 及以上，8GB 内存" />
          </Form.Item>
          <Form.Item name="runHardware" label="运行的硬件环境">
            <TextArea rows={2} placeholder="例如：Intel Core i3 及以上，4GB 内存" />
          </Form.Item>
          <Form.Item name="devOs" label="开发操作系统">
            <Input placeholder="例如：Windows 10/11" />
          </Form.Item>
          <Form.Item name="devTools" label="开发工具">
            <Input placeholder="例如：IntelliJ IDEA, Maven" />
          </Form.Item>
          <Form.Item name="runPlatform" label="运行平台">
            <Input placeholder="例如：Windows 10/11, Linux" />
          </Form.Item>
          <Form.Item name="runSupport" label="运行支撑环境">
            <Input placeholder="例如：JDK 17, MySQL 8.0" />
          </Form.Item>
          <Form.Item name="language" label="编程语言">
            <Input placeholder="例如：Java" />
          </Form.Item>
          <Form.Item name="purpose" label="开发目的（300-500字）">
            <TextArea rows={6} placeholder="请详细描述开发目的" />
          </Form.Item>
          <Form.Item name="targetDomain" label="面向领域（200-300字）">
            <TextArea rows={5} placeholder="请详细描述面向领域" />
          </Form.Item>
          <Form.Item name="mainFunctions" label="主要功能（800-1000字）">
            <TextArea rows={10} placeholder="请详细描述主要功能，包括各功能模块及其关系" />
          </Form.Item>
          <Form.Item name="techFeatures" label="技术特点（150-200字）">
            <TextArea rows={4} placeholder="请描述技术特点" />
          </Form.Item>
          <Form.Item>
            <Space>
              <Button type="primary" htmlType="submit">
                保存
              </Button>
              <Button onClick={handleGenerateSummary}>
                AI 生成
              </Button>
              <Button onClick={() => navigate(`/projects/${id}`)}>
                返回
              </Button>
            </Space>
          </Form.Item>
        </Form>
      ),
    },
    {
      key: 'code',
      label: '源代码',
      children: (
        <CodeEditor
          projectId={Number(id)}
          files={files}
          onRefreshFiles={fetchFiles}
        />
      ),
    },
    {
      key: 'manual',
      label: '操作手册',
      children: (
        <ManualEditor
          projectId={Number(id)}
          projectName={project.name}
        />
      ),
    },
  ]

  return (
    <Card title={`编辑项目：${project.name}`}>
      <Tabs items={tabItems} />
    </Card>
  )
}
