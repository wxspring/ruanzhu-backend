import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useAuthStore } from './stores/authStore'
import LoginPage from './pages/Login'
import Dashboard from './pages/Dashboard'
import ProjectCreate from './pages/ProjectCreate'
import ProjectDetail from './pages/ProjectDetail'
import ProjectEdit from './pages/ProjectEdit'
import UserManagement from './pages/UserManagement'
import Layout from './components/Layout'

function App() {
  const { token, user } = useAuthStore()

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/"
          element={
            token ? <Layout /> : <Navigate to="/login" replace />
          }
        >
          <Route index element={<Dashboard />} />
          <Route path="projects/create" element={<ProjectCreate />} />
          <Route path="projects/:id" element={<ProjectDetail />} />
          <Route path="projects/:id/edit" element={<ProjectEdit />} />
          {user?.role === 'ADMIN' && (
            <Route path="admin/users" element={<UserManagement />} />
          )}
        </Route>
      </Routes>
    </BrowserRouter>
  )
}

export default App
