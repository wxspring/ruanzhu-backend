import { Form, Input, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import api from '../api/axios'

export default function ProjectCreate() {
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const onFinish = async (values: any) => {
    try {
      await api.post('/api/projects', values)
      message.success('项目创建成功')
      navigate('/')
    } catch (error: any) {
      message.error(error.response?.data?.message || '创建失败')
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

        <Form.Item>
          <Space>
            <Button type="primary" htmlType="submit">
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
