import { useEffect, useState } from 'react'
import { Table, Button, Tag, Space } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

interface Project {
  id: number
  name: string
  customerName: string
  status: string
  createdAt: string
}

const statusColors: Record<string, string> = {
  CREATED: 'default',
  SUMMARY_CONFIRMING: 'processing',
  GENERATING: 'processing',
  REVIEWING: 'warning',
  APPROVED: 'success',
  EXPORTED: 'default',
}

const statusLabels: Record<string, string> = {
  CREATED: '已创建',
  SUMMARY_CONFIRMING: '概要确认中',
  GENERATING: '生成中',
  REVIEWING: '待审核',
  APPROVED: '已审核',
  EXPORTED: '已导出',
}

export default function Dashboard() {
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [loading, setLoading] = useState(false)

  const fetchProjects = async () => {
    setLoading(true)
    try {
      const response = await api.get('/api/projects')
      setProjects(response.data.data.list || [])
    } catch (error) {
      console.error('Failed to fetch projects:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const columns = [
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '客户',
      dataIndex: 'customerName',
      key: 'customerName',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag color={statusColors[status]}>
          {statusLabels[status] || status}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
    },
    {
      title: '操作',
      key: 'action',
      render: (_: any, record: Project) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/projects/${record.id}`)}>
            查看
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/projects/create')}>
          创建项目
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={projects}
        rowKey="id"
        loading={loading}
      />
    </div>
  )
}
