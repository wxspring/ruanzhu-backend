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

  const handleDelete = async (_fileId: number) => {
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
