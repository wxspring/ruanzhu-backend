import { Layout, Menu, Dropdown, Space } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import {
  HomeOutlined,
  PlusOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons'
import { useAuthStore } from '../stores/authStore'

const { Header, Content, Sider } = Layout

export default function AppLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuthStore()

  const menuItems = [
    {
      key: '/',
      icon: <HomeOutlined />,
      label: '项目列表',
    },
    {
      key: '/projects/create',
      icon: <PlusOutlined />,
      label: '创建项目',
    },
    ...(user?.role === 'ADMIN' ? [{
      key: '/admin/users',
      icon: <UserOutlined />,
      label: '用户管理',
    }] : []),
  ]

  const userMenuItems = [
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: () => {
        logout()
        navigate('/login')
      },
    },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{
        background: '#fff',
        padding: '0 24px',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        boxShadow: '0 2px 8px rgba(0,0,0,0.06)',
      }}>
        <h1 style={{ margin: 0, fontSize: 20 }}>软著材料 AI 生产系统</h1>
        <Dropdown menu={{ items: userMenuItems }}>
          <Space style={{ cursor: 'pointer' }}>
            <UserOutlined />
            <span>{user?.realName || user?.username}</span>
          </Space>
        </Dropdown>
      </Header>
      <Layout>
        <Sider width={200} style={{ background: '#fff' }}>
          <Menu
            mode="inline"
            selectedKeys={[location.pathname]}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
            style={{ height: '100%', borderRight: 0 }}
          />
        </Sider>
        <Layout style={{ padding: 24 }}>
          <Content
            style={{
              background: '#fff',
              padding: 24,
              minHeight: 280,
              borderRadius: 8,
            }}
          >
            <Outlet />
          </Content>
        </Layout>
      </Layout>
    </Layout>
  )
}
