import { useEffect, useState } from 'react'
import { Table, Button, Tag, Space, Input, Select, Card } from 'antd'
import { PlusOutlined, SearchOutlined, ReloadOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

interface Project {
  id: number
  name: string
  customerName: string
  status: string
  createdAt: string
}

const statusOptions = [
  { value: '', label: '全部状态' },
  { value: 'CREATED', label: '已创建' },
  { value: 'SUMMARY_CONFIRMING', label: '概要确认中' },
  { value: 'GENERATING', label: '生成中' },
  { value: 'REVIEWING', label: '待审核' },
  { value: 'APPROVED', label: '已审核' },
  { value: 'EXPORTED', label: '已导出' },
]

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
  const [searchText, setSearchText] = useState('')
  const [statusFilter, setStatusFilter] = useState('')
  const [pagination, setPagination] = useState({ current: 1, pageSize: 10, total: 0 })

  const fetchProjects = async (page = 1, size = 10) => {
    setLoading(true)
    try {
      const response = await api.get('/api/projects', {
        params: { page, size, search: searchText, status: statusFilter }
      })
      const data = response.data.data
      setProjects(data.list || [])
      setPagination({
        current: page,
        pageSize: size,
        total: data.total || 0,
      })
    } catch (error) {
      console.error('Failed to fetch projects:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const handleSearch = () => {
    fetchProjects(1, pagination.pageSize)
  }

  const handleReset = () => {
    setSearchText('')
    setStatusFilter('')
    fetchProjects(1, pagination.pageSize)
  }

  const handleTableChange = (pag: any) => {
    fetchProjects(pag.current, pag.pageSize)
  }

  const columns = [
    {
      title: '项目名称',
      dataIndex: 'name',
      key: 'name',
      width: 250,
    },
    {
      title: '客户',
      dataIndex: 'customerName',
      key: 'customerName',
      width: 150,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 120,
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
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_: any, record: Project) => (
        <Space>
          <Button type="link" onClick={() => navigate(`/projects/${record.id}`)}>
            查看
          </Button>
          <Button type="link" onClick={() => navigate(`/projects/${record.id}/edit`)}>
            编辑
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Card style={{ marginBottom: 16 }}>
        <Space wrap>
          <Input
            placeholder="搜索项目名称"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            onPressEnter={handleSearch}
            style={{ width: 200 }}
            prefix={<SearchOutlined />}
          />
          <Select
            value={statusFilter}
            onChange={setStatusFilter}
            options={statusOptions}
            style={{ width: 150 }}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          <Button icon={<ReloadOutlined />} onClick={handleReset}>
            重置
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/projects/create')}>
            创建项目
          </Button>
        </Space>
      </Card>

      <Table
        columns={columns}
        dataSource={projects}
        rowKey="id"
        loading={loading}
        pagination={{
          current: pagination.current,
          pageSize: pagination.pageSize,
          total: pagination.total,
          showSizeChanger: true,
          showTotal: (total) => `共 ${total} 条`,
        }}
        onChange={handleTableChange}
      />
    </div>
  )
}
