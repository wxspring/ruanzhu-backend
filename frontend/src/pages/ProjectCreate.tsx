import { Form, Input, Button, Card, message, Upload, Space } from 'antd'
import { UploadOutlined, InboxOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

export default function ProjectCreate() {
  const navigate = useNavigate()
  const [form] = Form.useForm()
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

  return (
    <Card title="创建项目">
      <Form form={form} layout="vertical" onFinish={onFinish} style={{ maxWidth: 600 }}>
        <Form.Item name="name" label="软件名称" rules={[{ required: true, message: '请输入软件名称' }]}>
          <Input placeholder="请输入软件名称" />
        </Form.Item>

        <Form.Item name="customerName" label="客户名称">
          <Input placeholder="请输入客户名称" />
        </Form.Item>

        <Form.Item name="version" label="版本号">
          <Input placeholder="例如：V1.0" />
        </Form.Item>

        <Form.Item name="category" label="软件分类">
          <Input placeholder="例如：应用软件" />
        </Form.Item>

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
      </Form>
    </Card>
  )
}
