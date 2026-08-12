import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import api from '../api/axios'

interface User {
  id: number
  username: string
  realName: string
  role: string
}

interface AuthState {
  token: string | null
  user: User | null
  login: (username: string, password: string) => Promise<void>
  logout: () => void
  setUser: (user: User) => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,

      login: async (username: string, password: string) => {
        const response = await api.post('/api/auth/login', { username, password })
        const { token, user } = response.data.data
        set({ token, user })
      },

      logout: () => {
        set({ token: null, user: null })
      },

      setUser: (user: User) => {
        set({ user })
      },
    }),
    {
      name: 'auth-storage',
    }
  )
)
